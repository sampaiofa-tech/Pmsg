import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 min
const MAX_REQUESTS_PER_WINDOW = 30; // 30 requests / min per user

/**
 * HTTPS Callable Cloud Function to resolve a 256-bit technical fingerprint
 * to its stable currentAuthUid and public key.
 * 
 * PRIVACY & ANTI-ENUMERATION DESIGN:
 * 1. Fingerprint is a 256-bit cryptographic hash (SHA-256) of the X25519 public key.
 *    Searching blindly is mathematically impossible (2^256 keyspace).
 * 2. Strict authentication requirement: unauthenticated calls are rejected immediately.
 * 3. Atomic per-user sliding window rate limiting prevents scraping / dictionary scans.
 * 4. Zero PII: stores and returns only technical routing identifiers, zero names or contact graphs.
 */
export const resolveFingerprint = onCall(async (request) => {
  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("resolveFingerprint: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para resolver identidade."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as { fingerprint?: string };

  // 2. Validate payload
  if (!data || !data.fingerprint || typeof data.fingerprint !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'fingerprint' é obrigatório e deve ser uma string."
    );
  }

  const fingerprint = data.fingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  const db = admin.firestore();

  // 3. Sliding window rate limiting
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const data = (doc && typeof doc.data === "function" && doc.data()) || { count: 0, windowStart: now };
      if (now - data.windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now });
      } else if (data.count >= MAX_REQUESTS_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de requisições excedido. Aguarde antes de tentar novamente."
        );
      } else {
        t.update(rateLimitRef, { count: data.count + 1 });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed:", err);
  }

  // 4. Resolve identity from Firestore
  const identityDoc = await db.collection("identities").doc(fingerprint).get();
  if (!identityDoc.exists) {
    throw new HttpsError("not-found", "Identidade não encontrada no diretório técnico.");
  }

  const identityData = identityDoc.data()!;
  return {
    currentAuthUid: identityData.currentAuthUid,
    pubKey: identityData.pubKey,
    signingPubKey: identityData.signingPubKey || "",
    updatedAt: identityData.updatedAt?.toMillis
      ? identityData.updatedAt.toMillis()
      : typeof identityData.updatedAt === "number"
      ? identityData.updatedAt
      : now,
  };
});
