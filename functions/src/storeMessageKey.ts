import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { Timestamp, FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { recordConnectionLog } from "./connectionLogs";

const MIN_TTL_MILLIS = 10 * 1000; // 10s
const MAX_TTL_MILLIS = 24 * 60 * 60 * 1000; // 24h

export interface StoreMessageKeyData {
  messageId: string;
  senderId: string;
  recipientId: string;
  ephemeralPubKey: string;
  wrappedDek: string;
  expiresAtMillis: number;
}

/**
 * HTTPS Callable Cloud Function to store a message's wrapped Data Encryption Key (DEK).
 * 
 * ZERO-TRACE / ZERO-KNOWLEDGE DESIGN:
 * Clients have 0 direct write access to the 'messageKeys' collection in Firestore.
 * All DEK persistence is routed strictly through this authenticated server-side function.
 * The server receives and stores ONLY opaque bytes (ephemeralPubKey, wrappedDek).
 * The server never sees the plaintext DEK or recipient private key.
 */
export const storeMessageKey = onCall(async (request) => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "storeMessageKey");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("storeMessageKey: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Authentication required to store message encryption key."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as StoreMessageKeyData;

  // 2. Validate payload structure
  if (
    !data ||
    !data.messageId ||
    !data.senderId ||
    !data.recipientId ||
    !data.ephemeralPubKey ||
    !data.wrappedDek ||
    typeof data.expiresAtMillis !== "number"
  ) {
    logger.warn("storeMessageKey: Invalid payload structure.");
    throw new HttpsError(
      "invalid-argument",
      "Missing or invalid required fields: messageId, senderId, recipientId, ephemeralPubKey, wrappedDek, expiresAtMillis."
    );
  }

  // 3. Enforce Participant Authorization (caller MUST be the sender)
  if (callerUid !== data.senderId) {
    logger.warn(
      `storeMessageKey: Caller ${callerUid} is not the sender ${data.senderId}. Access denied.`
    );
    throw new HttpsError(
      "permission-denied",
      "Caller must be the message sender to register its encryption key."
    );
  }

  // 4. Clamping of TTL (10s minimum, 24h maximum)
  const nowMillis = Date.now();
  const requestedTtl = data.expiresAtMillis - nowMillis;
  const clampedTtl = Math.max(MIN_TTL_MILLIS, Math.min(requestedTtl, MAX_TTL_MILLIS));
  const effectiveExpiresAtMillis = nowMillis + clampedTtl;
  const effectiveExpiresAt = Timestamp.fromMillis(effectiveExpiresAtMillis);

  // 5. Store Opaque Wrapped Key via Admin SDK in messageKeys collection
  const db = admin.firestore();
  await db.collection("messageKeys").doc(data.messageId).set({
    messageId: data.messageId,
    senderId: data.senderId,
    recipientId: data.recipientId,
    ephemeralPubKey: data.ephemeralPubKey,
    wrappedDek: data.wrappedDek,
    createdAt: FieldValue.serverTimestamp(),
    expiresAt: effectiveExpiresAt,
  });

  logger.info(
    `storeMessageKey: Opaque wrapped DEK stored securely for message ${data.messageId} with TTL ${clampedTtl}ms.`
  );

  return {
    success: true,
    messageId: data.messageId,
    expiresAtMillis: effectiveExpiresAtMillis,
  };
});
