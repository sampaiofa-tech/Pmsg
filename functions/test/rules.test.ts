import {
  initializeTestEnvironment,
  RulesTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import * as fs from "fs";
import * as path from "path";

describe("Firestore Security Rules Verification", () => {
  let testEnv: RulesTestEnvironment;

  beforeAll(async () => {
    const rulesPath = path.resolve(__dirname, "../../firestore.rules");
    const rules = fs.readFileSync(rulesPath, "utf8");

    testEnv = await initializeTestEnvironment({
      projectId: "pmsg-test-rules",
      firestore: {
        rules,
        host: "127.0.0.1",
        port: 8080,
      },
    });
  });

  afterAll(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    if (testEnv) {
      await testEnv.clearFirestore();
    }
  });

  describe("Rule Verification: messageKeys Collection (Zero-Trace DEK Isolation)", () => {
    it("CRITICAL RULE: MUST DENY any client from reading messageKeys (DEK)", async () => {
      // Set up key document via admin context (simulating Cloud Functions Admin SDK)
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("messageKeys").doc("msg_001").set({
          messageId: "msg_001",
          dek: "super_secret_encryption_key_256",
          expiresAt: new Date(Date.now() + 3600000),
        });
      });

      // Authenticated client tries to read the DEK key document
      const clientDb = testEnv.authenticatedContext("alice").firestore();
      const readPromise = clientDb.collection("messageKeys").doc("msg_001").get();

      // MUST FAIL: client has ZERO access to DEK keys
      await assertFails(readPromise);
    });

    it("CRITICAL RULE: MUST DENY any client from creating or updating messageKeys", async () => {
      const clientDb = testEnv.authenticatedContext("alice").firestore();
      const writePromise = clientDb.collection("messageKeys").doc("msg_002").set({
        messageId: "msg_002",
        dek: "attacker_injected_key",
        expiresAt: new Date(Date.now() + 3600000),
      });

      await assertFails(writePromise);
    });
  });

  describe("Rule Verification: messages Collection", () => {
    it("MUST SUCCEED when participant creates message WITH valid future expiresAt Timestamp", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      const futureTimestamp = new Date(Date.now() + 86400000); // 24h future

      const createPromise = aliceDb.collection("messages").doc("msg_100").set({
        ciphertext: "c2VjcmV0X3BheWxvYWQ=",
        iv: "cmFuZG9tX2l2",
        senderId: "alice",
        recipientId: "bob",
        expiresAt: futureTimestamp,
      });

      await assertSucceeds(createPromise);
    });

    it("MUST FAIL when creating message WITHOUT expiresAt", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const createPromise = aliceDb.collection("messages").doc("msg_101").set({
        ciphertext: "c2VjcmV0X3BheWxvYWQ=",
        iv: "cmFuZG9tX2l2",
        senderId: "alice",
        recipientId: "bob",
      });

      await assertFails(createPromise);
    });

    it("MUST FAIL when updating message content (messages are immutable)", async () => {
      // Create valid message
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("messages").doc("msg_102").set({
          ciphertext: "original_ciphertext",
          iv: "iv",
          senderId: "alice",
          recipientId: "bob",
          expiresAt: new Date(Date.now() + 86400000),
        });
      });

      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      const updatePromise = aliceDb.collection("messages").doc("msg_102").update({
        ciphertext: "tampered_ciphertext",
      });

      await assertFails(updatePromise);
    });

    it("MUST SUCCEED when participant deletes message (vanish-after-read)", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("messages").doc("msg_103").set({
          ciphertext: "ciphertext",
          iv: "iv",
          senderId: "alice",
          recipientId: "bob",
          expiresAt: new Date(Date.now() + 86400000),
        });
      });

      const bobDb = testEnv.authenticatedContext("bob").firestore();
      const deletePromise = bobDb.collection("messages").doc("msg_103").delete();

      await assertSucceeds(deletePromise);
    });
  });

  describe("Rule Verification: invites Collection (Modelo C)", () => {
    it("MUST DENY any client from reading invites", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("invites").doc("inv_001").set({
          creatorUid: "alice",
          used: false,
        });
      });

      const bobDb = testEnv.authenticatedContext("bob").firestore();
      await assertFails(bobDb.collection("invites").doc("inv_001").get());
    });

    it("MUST DENY any client from writing directly to invites", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      await assertFails(
        aliceDb.collection("invites").doc("inv_002").set({
          creatorUid: "alice",
          used: false,
        })
      );
    });
  });

  describe("Rule Verification: identities Collection", () => {
    it("MUST DENY any client from reading identities directly", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("identities").doc("fp_123").set({
          currentAuthUid: "alice",
          pubKey: "pubkey",
        });
      });

      const bobDb = testEnv.authenticatedContext("bob").firestore();
      await assertFails(bobDb.collection("identities").doc("fp_123").get());
    });

    it("MUST SUCCEED when authenticated user sets their own identity (currentAuthUid == auth.uid)", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      await assertSucceeds(
        aliceDb.collection("identities").doc("fp_alice").set({
          currentAuthUid: "alice",
          pubKey: "alice_pubkey",
        })
      );
    });

    it("MUST FAIL when authenticated user tries to impersonate another UID", async () => {
      const eveDb = testEnv.authenticatedContext("eve").firestore();
      await assertFails(
        eveDb.collection("identities").doc("fp_alice").set({
          currentAuthUid: "alice",
          pubKey: "eve_pubkey",
        })
      );
    });

    it("CRITICAL RULE: MUST FAIL when any client attempts to update an existing identity doc directly", async () => {
      // Existing identity created
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("identities").doc("fp_alice").set({
          currentAuthUid: "alice",
          pubKey: "alice_pubkey",
          signingPubKey: "alice_signing_pubkey",
        });
      });

      // Alice tries to update her own document directly from client SDK -> MUST FAIL (must use updateIdentityRouting)
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      await assertFails(
        aliceDb.collection("identities").doc("fp_alice").update({
          currentAuthUid: "alice_new_device",
        })
      );

      // Eve tries to hijack Alice's document directly -> MUST FAIL
      const eveDb = testEnv.authenticatedContext("eve").firestore();
      await assertFails(
        eveDb.collection("identities").doc("fp_alice").set({
          currentAuthUid: "eve",
          pubKey: "alice_pubkey",
        })
      );
    });
  });

  describe("Rule Verification: abuseReports & abuseMetrics Collections (Total Client Isolation)", () => {
    it("CRITICAL RULE: MUST DENY any client from reading or writing abuseReports directly", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      await assertFails(aliceDb.collection("abuseReports").doc("rep_123").get());
      await assertFails(
        aliceDb.collection("abuseReports").doc("rep_123").set({
          reportedFingerprint: "0".repeat(64),
          abuseType: "SPAM",
        })
      );
    });

    it("CRITICAL RULE: MUST DENY any client from reading or writing abuseMetrics directly", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      await assertFails(aliceDb.collection("abuseMetrics").doc("fp_123").get());
      await assertFails(
        aliceDb.collection("abuseMetrics").doc("fp_123").set({
          reportedFingerprint: "0".repeat(64),
          reportCount: 1,
        })
      );
    });
  });
});

