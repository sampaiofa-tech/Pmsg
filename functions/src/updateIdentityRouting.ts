import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import * as crypto from "crypto";

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes
const MAX_UPDATES_PER_WINDOW = 5; // Max 5 recovery updates per 10 min

/**
 * HTTPS Callable Cloud Function to update technical identity routing after recovery.
 * 
 * When a user restores their account using their BIP-39 mnemonic on a new device,
 * they obtain a new Firebase Auth UID. This function binds their new UID to their
 * existing immutable 256-bit cryptographic fingerprint.
 */
export const updateIdentityRouting = onCall(async (request) => {
  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("updateIdentityRouting: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para atualizar roteamento de identidade."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as { fingerprint?: string; pubKey?: string };

  // 2. Validate payload
  if (!data || !data.fingerprint || !data.pubKey) {
    throw new HttpsError(
      "invalid-argument",
      "Campos 'fingerprint' e 'pubKey' são obrigatórios."
    );
  }

  const fingerprint = data.fingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  const pubKey = data.pubKey.trim();
  if (pubKey.length === 0) {
    throw new HttpsError("invalid-argument", "Chave pública não pode ser vazia.");
  }

  // 3. Cryptographic Binding Proof: SHA-256(pubKey) == fingerprint
  try {
    const pubKeyBytes = Buffer.from(pubKey, "base64");
    if (pubKeyBytes.length !== 32) {
      throw new Error("Invalid public key length");
    }
    const computedFp = crypto.createHash("sha256").update(pubKeyBytes).digest("hex");
    if (computedFp.toLowerCase() !== fingerprint) {
      throw new HttpsError(
        "invalid-argument",
        "Fingerprint não corresponde ao hash da chave pública fornecida."
      );
    }
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    throw new HttpsError("invalid-argument", "Formato inválido de chave pública Base64.");
  }

  const db = admin.firestore();

  // 4. Rate Limiting via userRateLimits
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || { count: 0, windowStart: now };
      if (now - limitData.windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now });
      } else if (limitData.count >= MAX_UPDATES_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de atualizações de roteamento excedido. Tente novamente mais tarde."
        );
      } else {
        t.update(rateLimitRef, { count: limitData.count + 1 });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in updateIdentityRouting:", err);
  }

  // 5. Update technical identity directory
  const identityRef = db.collection("identities").doc(fingerprint);
  await identityRef.set(
    {
      currentAuthUid: callerUid,
      pubKey,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  logger.info(`updateIdentityRouting: Fingerprint ${fingerprint.substring(0, 8)}... bound to new UID ${callerUid}`);

  return {
    success: true,
    fingerprint,
    currentAuthUid: callerUid,
  };
});
