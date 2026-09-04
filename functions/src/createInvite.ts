import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import * as crypto from "crypto";

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes
const MAX_INVITES_PER_WINDOW = 10; // Max 10 invites per 10 min window
const INVITE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

/**
 * HTTPS Callable Cloud Function to generate an ephemeral remote invite (Modelo C).
 * 
 * SECURITY & PRIVACY SPECIFICATIONS:
 * 1. Requires authenticated caller (Anonymous or Firebase Auth).
 * 2. Sliding window rate limiting using Firestore collection 'userRateLimits'.
 * 3. Ephemeral single-use cryptographic token (32 bytes = 256 bits of entropy).
 * 4. 24-hour strict TTL.
 * 5. Document stored in collection 'invites' (client read/write disabled via firestore.rules).
 */
export const createInvite = onCall(async (request) => {
  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("createInvite: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para criar convite."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as { creatorFingerprint?: string; creatorPubKey?: string };

  // 2. Validate payload
  if (!data || !data.creatorFingerprint || !data.creatorPubKey) {
    throw new HttpsError(
      "invalid-argument",
      "Campos 'creatorFingerprint' e 'creatorPubKey' são obrigatórios."
    );
  }

  const fingerprint = data.creatorFingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  const pubKey = data.creatorPubKey.trim();
  if (pubKey.length === 0) {
    throw new HttpsError("invalid-argument", "Chave pública não pode ser vazia.");
  }

  // 3. Cryptographic Binding Check: SHA-256(pubKey) == fingerprint
  try {
    const pubKeyBytes = Buffer.from(pubKey, "base64");
    if (pubKeyBytes.length !== 32) {
      throw new Error("Invalid public key length");
    }
    const computedFp = crypto.createHash("sha256").update(pubKeyBytes).digest("hex");
    if (computedFp.toLowerCase() !== fingerprint) {
      throw new HttpsError(
        "invalid-argument",
        "Fingerprint não corresponde ao hash SHA-256 da chave pública fornecida."
      );
    }
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    throw new HttpsError("invalid-argument", "Formato inválido de chave pública Base64.");
  }

  const db = admin.firestore();

  // 4. Rate Limiting via Firestore userRateLimits
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || { count: 0, windowStart: now };
      if (now - limitData.windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now });
      } else if (limitData.count >= MAX_INVITES_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de criação de convites excedido (máximo 10 a cada 10 minutos)."
        );
      } else {
        t.update(rateLimitRef, { count: limitData.count + 1 });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in createInvite:", err);
  }

  // 5. Generate secure random token (256 bits)
  const token = crypto.randomBytes(32).toString("hex");
  const expiresAtMillis = now + INVITE_TTL_MS;

  const inviteRef = db.collection("invites").doc(token);
  await inviteRef.set({
    creatorUid: callerUid,
    creatorFingerprint: fingerprint,
    creatorPubKey: pubKey,
    createdAt: FieldValue.serverTimestamp(),
    expiresAtMillis,
    used: false,
    acceptedByUid: null,
  });

  logger.info(`createInvite: Invite created by ${callerUid} with 24h TTL.`);

  return {
    inviteToken: token,
    inviteLink: `pmsg://invite?i=${token}&fp=${fingerprint}`,
    expiresAtMillis,
  };
});
