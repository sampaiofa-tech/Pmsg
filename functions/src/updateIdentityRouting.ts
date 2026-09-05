import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import * as crypto from "crypto";
import { recordConnectionLog } from "./connectionLogs";

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes
const MAX_UPDATES_PER_WINDOW = 5; // Max 5 recovery updates per 10 min
const MAX_TIMESTAMP_SKEW_MS = 5 * 60 * 1000; // 5 minutes anti-replay window

// RFC 8410 SPKI prefix for 32-byte Ed25519 raw public key:
// 30 2a (SEQUENCE of 42 bytes)
//   30 05 (SEQUENCE of 5 bytes)
//     06 03 2b 65 70 (OID 1.3.101.112 - id-Ed25519)
//   03 21 00 (BIT STRING of 33 bytes, 0 unused bits)
//     <32-byte raw public key>
const ED25519_SPKI_PREFIX = Buffer.from("302a300506032b6570032100", "hex");

export interface UpdateIdentityRoutingData {
  fingerprint?: string;
  pubKey?: string;
  signature?: string;
  timestamp?: number;
  signingPubKey?: string;
}

/**
 * HTTPS Callable Cloud Function to update technical identity routing after recovery.
 * 
 * SECURITY SPECIFICATIONS (F0 - Proof-of-Possession via Ed25519):
 * 1. Requires authenticated caller (callerUid = request.auth.uid).
 * 2. Cryptographic binding check: SHA-256(pubKey) == fingerprint.
 * 3. Anti-replay tolerance window: |now - timestamp| <= 5 minutes.
 * 4. Ed25519 Signature Verification:
 *    Payload: "pmsg-routing-v1|<fingerprint>|<callerUid>|<timestamp>"
 *    Verified against the immutable `signingPubKey` registered in `identities/{fingerprint}`.
 *    Any signature mismatch or non-possession throws 'permission-denied'.
 * 5. Sliding window rate limiting prevents abuse.
 */
export const updateIdentityRouting = onCall(async (request) => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "updateIdentityRouting");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("updateIdentityRouting: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para atualizar roteamento de identidade."
    );
  }

  const callerUid = request.auth.uid;
  const data = (request.data || {}) as UpdateIdentityRoutingData;

  // 2. Validate basic input fields
  if (!data.fingerprint || !data.pubKey) {
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

  if (!data.signature || typeof data.signature !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'signature' (prova de posse Ed25519) é obrigatório."
    );
  }

  if (typeof data.timestamp !== "number" || !Number.isFinite(data.timestamp)) {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'timestamp' numérico é obrigatório."
    );
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

  // 4. Anti-Replay Protection: check timestamp skew <= 5 min
  const now = Date.now();
  if (Math.abs(now - data.timestamp) > MAX_TIMESTAMP_SKEW_MS) {
    throw new HttpsError(
      "invalid-argument",
      "Timestamp expirado ou fora da tolerância de 5 minutos (anti-replay)."
    );
  }

  const db = admin.firestore();

  // 5. Retrieve existing signing public key from Firestore doc
  const identityRef = db.collection("identities").doc(fingerprint);
  const identityDoc = await identityRef.get();
  let targetSigningPubKey: string;

  if (identityDoc.exists) {
    const existingData = identityDoc.data()!;
    if (existingData.revoked === true || existingData.status === "revoked") {
      logger.warn(`updateIdentityRouting: Attempt to update revoked identity ${fingerprint.substring(0, 8)}...`);
      throw new HttpsError(
        "permission-denied",
        "Esta identidade criptográfica foi revogada por moderação e não pode ser atualizada."
      );
    }
    if (existingData.signingPubKey) {
      targetSigningPubKey = existingData.signingPubKey;
    } else if (data.signingPubKey) {
      targetSigningPubKey = data.signingPubKey.trim();
    } else {
      throw new HttpsError(
        "permission-denied",
        "Identidade existente sem chave de assinatura Ed25519 registrada."
      );
    }
  } else {
    // Initial identity creation requires signingPubKey
    if (!data.signingPubKey || typeof data.signingPubKey !== "string") {
      throw new HttpsError(
        "invalid-argument",
        "Campo 'signingPubKey' obrigatório para registrar nova identidade."
      );
    }
    targetSigningPubKey = data.signingPubKey.trim();
  }

  // 6. Cryptographic Proof of Possession: Verify Ed25519 signature
  const canonicalPayload = `pmsg-routing-v1|${fingerprint}|${callerUid}|${data.timestamp}`;
  const messageBytes = Buffer.from(canonicalPayload, "utf8");

  try {
    const rawSigningPubKey = Buffer.from(targetSigningPubKey, "base64");
    if (rawSigningPubKey.length !== 32) {
      throw new Error("Invalid raw signing key length (must be 32 bytes)");
    }

    const signatureBytes = Buffer.from(data.signature.trim(), "base64");
    if (signatureBytes.length !== 64) {
      throw new Error("Invalid signature length (must be 64 bytes)");
    }

    const spkiBuffer = Buffer.concat([ED25519_SPKI_PREFIX, rawSigningPubKey]);
    const keyObject = crypto.createPublicKey({ key: spkiBuffer, format: "der", type: "spki" });

    const isValid = crypto.verify(null, messageBytes, keyObject, signatureBytes);
    if (!isValid) {
      logger.warn(`updateIdentityRouting: Invalid Ed25519 signature for fingerprint ${fingerprint.substring(0, 8)}...`);
      throw new HttpsError(
        "permission-denied",
        "Prova de posse inválida: assinatura Ed25519 rejeitada."
      );
    }
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("updateIdentityRouting: Ed25519 verification exception:", err);
    throw new HttpsError(
      "permission-denied",
      "Falha na validação da assinatura Ed25519: " + err.message
    );
  }

  // 7. Rate Limiting via userRateLimits
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);

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

  // 8. Update technical identity directory with new currentAuthUid and signingPubKey
  await identityRef.set(
    {
      currentAuthUid: callerUid,
      pubKey,
      signingPubKey: targetSigningPubKey,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  logger.info(`updateIdentityRouting: Fingerprint ${fingerprint.substring(0, 8)}... verified & bound to UID ${callerUid}`);

  return {
    success: true,
    fingerprint,
    currentAuthUid: callerUid,
    signingPubKey: targetSigningPubKey,
  };
});
