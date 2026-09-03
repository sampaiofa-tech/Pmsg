import { storeMessageKey } from "../src/storeMessageKey";
import * as admin from "firebase-admin";

describe("storeMessageKey Cloud Function Test Suite", () => {
  let mockDocSet: jest.Mock;

  beforeEach(() => {
    mockDocSet = jest.fn().mockResolvedValue({ writeTime: { seconds: 12345 } });
    jest.spyOn(admin, "firestore").mockReturnValue({
      collection: (name: string) => ({
        doc: (id: string) => ({
          set: mockDocSet,
        }),
      }),
    } as any);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should reject unauthenticated caller with 'unauthenticated'", async () => {
    const request: any = {
      auth: null,
      data: {
        messageId: "msg_123",
        senderId: "alice",
        recipientId: "bob",
        dek: "secret_dek",
        expiresAtMillis: Date.now() + 60000,
      },
    };

    await expect((storeMessageKey as any).run(request)).rejects.toThrow(
      /Authentication required/
    );
  });

  it("CRITICAL RULE: should reject caller who is not the sender with 'permission-denied'", async () => {
    const request: any = {
      auth: { uid: "attacker_eve" },
      data: {
        messageId: "msg_123",
        senderId: "alice", // Eve is trying to store a key for Alice's message
        recipientId: "bob",
        dek: "malicious_dek",
        expiresAtMillis: Date.now() + 60000,
      },
    };

    await expect((storeMessageKey as any).run(request)).rejects.toThrow(
      /Caller must be the message sender/
    );
  });

  it("should reject missing required fields with 'invalid-argument'", async () => {
    const request: any = {
      auth: { uid: "alice" },
      data: {
        messageId: "msg_123",
        // missing senderId, recipientId, etc.
      },
    };

    await expect((storeMessageKey as any).run(request)).rejects.toThrow(
      /Missing or invalid required fields/
    );
  });

  it("should clamp TTL to maximum 24h and successfully save DEK when caller is authorized sender", async () => {
    const now = Date.now();
    const excessiveExpiration = now + 48 * 60 * 60 * 1000; // 48h in future

    const request: any = {
      auth: { uid: "alice" },
      data: {
        messageId: "msg_123",
        senderId: "alice",
        recipientId: "bob",
        dek: "super_secure_dek_256_bit",
        expiresAtMillis: excessiveExpiration,
      },
    };

    const response = await (storeMessageKey as any).run(request);

    expect(response.success).toBe(true);
    expect(response.messageId).toBe("msg_123");
    // Clamped to at most now + 24h + 100ms tolerance
    expect(response.expiresAtMillis).toBeLessThanOrEqual(now + 24 * 60 * 60 * 1000 + 500);

    expect(mockDocSet).toHaveBeenCalledTimes(1);
    const savedData = mockDocSet.mock.calls[0][0];
    expect(savedData.messageId).toBe("msg_123");
    expect(savedData.senderId).toBe("alice");
    expect(savedData.recipientId).toBe("bob");
    expect(savedData.dek).toBe("super_secure_dek_256_bit");
  });
});
