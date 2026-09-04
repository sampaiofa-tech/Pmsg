import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

export interface GetMessageKeyData {
  messageId: string;
}

export interface GetMessageKeyResult {
  success: boolean;
  messageId: string;
  ephemeralPubKey: string;
  wrappedDek: string;
  expiresAtMillis: number;
}

/**
 * HTTPS Callable Cloud Function to deliver a message's wrapped Data Encryption Key (DEK)
 * strictly to an authorized participant (recipient or sender).
 *
 * ZERO-TRACE / ZERO-KNOWLEDGE DESIGN:
 * 1. Clients have NO direct read access to 'messageKeys' in Firestore (rules: allow read: if false).
 * 2. This callable validates that the authenticated caller (request.auth.uid) matches the message's
 *    recipientId (or senderId).
 * 3. Expired keys are rejected and cannot be read.
 * 4. The server returns ONLY opaque bytes (ephemeralPubKey, wrappedDek). It never sees or learns plaintext DEKs.
 * 5. Vanish-After-Read Semantics: Once the recipient decrypts and marks the message as read,
 *    the message doc in 'messages' is deleted by the client, triggering 'onDeleteMessage'
 *    which permanently shreds the wrapped DEK in 'messageKeys'.
 */
export const getMessageKey = onCall(async (request): Promise<GetMessageKeyResult> => {
  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("getMessageKey: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Authentication required to retrieve message encryption key."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as GetMessageKeyData;

  // 2. Validate payload structure
  if (!data || !data.messageId || typeof data.messageId !== "string" || data.messageId.trim() === "") {
    logger.warn("getMessageKey: Invalid or missing messageId.");
    throw new HttpsError(
      "invalid-argument",
      "Missing or invalid required field: messageId."
    );
  }

  const messageId = data.messageId.trim();

  // 3. Retrieve Key Document via Firebase Admin SDK
  const db = admin.firestore();
  const keyDocRef = db.collection("messageKeys").doc(messageId);
  const keyDocSnap = await keyDocRef.get();

  if (!keyDocSnap.exists) {
    logger.warn(`getMessageKey: Key document for message ${messageId} not found or already shredded.`);
    throw new HttpsError(
      "not-found",
      "Encryption key not found or already shredded."
    );
  }

  const keyData = keyDocSnap.data();
  if (!keyData || !keyData.ephemeralPubKey || !keyData.wrappedDek || !keyData.expiresAt) {
    logger.error(`getMessageKey: Corrupt key document structure for message ${messageId}.`);
    throw new HttpsError(
      "internal",
      "Corrupt encryption key metadata."
    );
  }

  // 4. Validate Expiration
  const expiresAtMillis = typeof keyData.expiresAt.toMillis === "function"
    ? keyData.expiresAt.toMillis()
    : Number(keyData.expiresAt);

  const nowMillis = Date.now();
  if (expiresAtMillis <= nowMillis) {
    logger.warn(`getMessageKey: Message ${messageId} has expired (expiresAt: ${expiresAtMillis}, now: ${nowMillis}).`);
    throw new HttpsError(
      "failed-precondition",
      "Message has expired and its encryption key is no longer accessible."
    );
  }

  // 5. Enforce Participant Authorization (caller must be recipientId or senderId)
  const recipientId = keyData.recipientId;
  const senderId = keyData.senderId;

  if (callerUid !== recipientId && callerUid !== senderId) {
    logger.warn(
      `getMessageKey: Unauthorized access attempt. Caller ${callerUid} is not a participant (sender: ${senderId}, recipient: ${recipientId}) for message ${messageId}.`
    );
    throw new HttpsError(
      "permission-denied",
      "Caller is not authorized to retrieve this encryption key."
    );
  }

  // Zero-trace logging: audit access without logging secret key material
  logger.info(`getMessageKey: Opaque wrapped DEK securely released to participant ${callerUid} for message ${messageId}.`);

  return {
    success: true,
    messageId: messageId,
    ephemeralPubKey: keyData.ephemeralPubKey,
    wrappedDek: keyData.wrappedDek,
    expiresAtMillis: expiresAtMillis,
  };
});
