import * as admin from "firebase-admin";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";

/**
 * Core Crypto-Shredding logic for authoritative server-side message expiration.
 *
 * Threat Model:
 * Client-side purge can be circumvented by offline or compromised devices.
 * Firestore native TTL is best-effort (~24-72 hours latency).
 * By physically destroying the DEK (Data Encryption Key) in `messageKeys`
 * immediately upon expiration, the ciphertext in `messages` becomes permanently
 * and cryptographically irrecuperable even before physical TTL purge.
 */
export async function executeCryptoShredding(
  db: admin.firestore.Firestore,
  currentTime: admin.firestore.Timestamp
): Promise<{ shreddedKeysCount: number; deletedMessagesCount: number }> {
  const expiredKeysQuery = db
    .collection("messageKeys")
    .where("expiresAt", "<=", currentTime)
    .limit(500);

  const snapshot = await expiredKeysQuery.get();

  if (snapshot.empty) {
    logger.info("Crypto-Shredder: No expired message keys found.");
    return { shreddedKeysCount: 0, deletedMessagesCount: 0 };
  }

  const batch = db.batch();
  let count = 0;

  for (const doc of snapshot.docs) {
    const data = doc.data();
    const messageId = data.messageId || doc.id;

    // 1. Hard-delete DEK (Irreversible Crypto-Shredding)
    batch.delete(doc.ref);

    // 2. Hard-delete matching ciphertext message document
    const messageRef = db.collection("messages").doc(messageId);
    batch.delete(messageRef);

    count++;
  }

  await batch.commit();

  logger.info(`Crypto-Shredder: Successfully shredded ${count} keys and associated messages.`);
  return { shreddedKeysCount: count, deletedMessagesCount: count };
}

/**
 * Hourly scheduled task running on Cloud Scheduler.
 * Hard-deletes expired DEK keys and messages in batch.
 */
export const scheduledMessageShredder = onSchedule(
  {
    schedule: "every 1 hours",
    timeZone: "UTC",
    retryCount: 3,
  },
  async () => {
    const db = admin.firestore();
    const now = admin.firestore.Timestamp.now();
    await executeCryptoShredding(db, now);
  }
);

/**
 * Reactive trigger on message deletion.
 * Ensures instant crypto-shredding of DEK during vanish-after-read or manual deletion.
 */
export const onDeleteMessage = onDocumentDeleted(
  "messages/{messageId}",
  async (event) => {
    const messageId = event.params.messageId;
    if (!messageId) return;

    const db = admin.firestore();
    const keyRef = db.collection("messageKeys").doc(messageId);

    try {
      await keyRef.delete();
      logger.info(`Vanish-on-Delete: DEK for message ${messageId} destroyed immediately.`);
    } catch (error) {
      logger.error(`Failed to delete DEK for message ${messageId}:`, error);
    }
  }
);
