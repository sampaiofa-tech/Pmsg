import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute
const MAX_ACCEPTS_PER_WINDOW = 15; // Max 15 attempts / min

/**
 * HTTPS Callable Cloud Function to accept an ephemeral remote invite (Modelo C).
 * 
 * SECURITY & PRIVACY SPECIFICATIONS:
 * 1. Requires authenticated caller.
 * 2. Strict single-use: rejected if already used or expired.
 * 3. Self-accept prevention: creator cannot accept their own invite.
 * 4. Vanish-After-Accept: document is permanently deleted from Firestore inside the transaction.
 * 5. Returns only the technical routing metadata (creator's fingerprint and public key).
 */
export const acceptInvite = onCall(async (request) => {
  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("acceptInvite: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para aceitar convite."
    );
  }

  const callerUid = request.auth.uid;
  const data = (request.data || {}) as { inviteToken?: string; token?: string; i?: string };
  const rawToken = data.inviteToken || data.token || data.i;

  // 2. Validate payload
  if (!rawToken || typeof rawToken !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'inviteToken' (ou 'i') é obrigatório e deve ser uma string."
    );
  }

  const inviteToken = rawToken.trim().toLowerCase();
  if (inviteToken.length !== 64 || !/^[0-9a-f]{64}$/.test(inviteToken)) {
    throw new HttpsError(
      "invalid-argument",
      "Token de convite inválido (deve conter 64 caracteres hexadecimais)."
    );
  }

  const db = admin.firestore();

  // 3. Rate Limiting via Firestore userRateLimits
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || { count: 0, windowStart: now };
      if (now - limitData.windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now });
      } else if (limitData.count >= MAX_ACCEPTS_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de tentativas de aceite excedido. Aguarde um momento."
        );
      } else {
        t.update(rateLimitRef, { count: limitData.count + 1 });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in acceptInvite:", err);
  }

  // 4. Atomic Transaction: Validate & Vanish-After-Accept
  const inviteRef = db.collection("invites").doc(inviteToken);
  let creatorFingerprint = "";
  let creatorPubKey = "";

  await db.runTransaction(async (t) => {
    const inviteDoc = await t.get(inviteRef);
    if (!inviteDoc.exists) {
      throw new HttpsError(
        "not-found",
        "Convite não encontrado ou já incinerado."
      );
    }

    const inviteData = inviteDoc.data()!;

    if (inviteData.used === true) {
      throw new HttpsError(
        "failed-precondition",
        "Convite já utilizado. O protocolo de uso único impede novo aceite."
      );
    }

    if (now > inviteData.expiresAtMillis) {
      throw new HttpsError(
        "failed-precondition",
        "Convite expirado (TTL de 24 horas excedido)."
      );
    }

    if (inviteData.creatorUid === callerUid) {
      throw new HttpsError(
        "invalid-argument",
        "Não é permitido aceitar o próprio convite."
      );
    }

    creatorFingerprint = inviteData.creatorFingerprint;
    creatorPubKey = inviteData.creatorPubKey;

    // Vanish-After-Accept: Delete document immediately
    t.delete(inviteRef);
  });

  logger.info(`acceptInvite: Token ${inviteToken.substring(0, 8)}... accepted by ${callerUid} and permanently incinerated.`);

  return {
    creatorFingerprint,
    creatorPubKey,
  };
});
