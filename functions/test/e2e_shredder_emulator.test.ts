import * as admin from "firebase-admin";
import { Timestamp } from "firebase-admin/firestore";
import { executeCryptoShredding } from "../src/shredder";

describe("E2E Real Firestore Emulator - Authoritative Crypto-Shredder", () => {
  let db: admin.firestore.Firestore;

  beforeAll(() => {
    process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080";
    process.env.GCLOUD_PROJECT = "pmsg-e2e-project";

    if (!admin.apps.length) {
      admin.initializeApp({ projectId: "pmsg-e2e-project" });
    }
    db = admin.firestore();
  });

  it("CRITICAL E2E: should hard-delete both messageKeys and messages documents when expired", async () => {
    const nowMillis = Date.now();
    const pastTimestamp = Timestamp.fromMillis(nowMillis - 60 * 1000); // Expired 1 min ago
    const futureTimestamp = Timestamp.fromMillis(nowMillis + 3600 * 1000); // 1h in future

    // 1. Seed Expired Message and Key
    await db.collection("messages").doc("e2e_msg_expired").set({
      senderId: "alice",
      recipientId: "bob",
      ciphertext: "U2VjcmV0Q2lwaGVydGV4dFRoYXRNdXN0QmVEZXN0cm95ZWQ=",
      expiresAt: pastTimestamp,
    });

    await db.collection("messageKeys").doc("e2e_msg_expired").set({
      messageId: "e2e_msg_expired",
      senderId: "alice",
      recipientId: "bob",
      dek: "QWVzRmluYWxEZWs4MzI5Mjg0NzkzODQ3MjM5OA==",
      expiresAt: pastTimestamp,
    });

    // 2. Seed Active (Unexpired) Message and Key
    await db.collection("messages").doc("e2e_msg_active").set({
      senderId: "alice",
      recipientId: "bob",
      ciphertext: "QWN0aXZlTWVzc2FnZUNpcGhlcnRleHQ=",
      expiresAt: futureTimestamp,
    });

    await db.collection("messageKeys").doc("e2e_msg_active").set({
      messageId: "e2e_msg_active",
      senderId: "alice",
      recipientId: "bob",
      dek: "QWN0aXZlRGVrU2hvdWxkTm90QmVEZWxldGVk=",
      expiresAt: futureTimestamp,
    });

    // Verify initial existence
    const beforeKeyDoc = await db.collection("messageKeys").doc("e2e_msg_expired").get();
    const beforeMsgDoc = await db.collection("messages").doc("e2e_msg_expired").get();
    expect(beforeKeyDoc.exists).toBe(true);
    expect(beforeMsgDoc.exists).toBe(true);

    // 3. Run real executeCryptoShredding against Emulator
    const shredResult = await executeCryptoShredding(db, Timestamp.now());
    expect(shredResult.shreddedKeysCount).toBeGreaterThanOrEqual(1);
    expect(shredResult.deletedMessagesCount).toBeGreaterThanOrEqual(1);

    // 4. Verify post-shredder state: Expired docs are completely deleted!
    const afterKeyDoc = await db.collection("messageKeys").doc("e2e_msg_expired").get();
    const afterMsgDoc = await db.collection("messages").doc("e2e_msg_expired").get();
    expect(afterKeyDoc.exists).toBe(false);
    expect(afterMsgDoc.exists).toBe(false);

    // Verify Active docs are preserved
    const activeKeyDoc = await db.collection("messageKeys").doc("e2e_msg_active").get();
    const activeMsgDoc = await db.collection("messages").doc("e2e_msg_active").get();
    expect(activeKeyDoc.exists).toBe(true);
    expect(activeMsgDoc.exists).toBe(true);
  });
});
