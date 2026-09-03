import { getMessageKey } from "../src/getMessageKey";
import * as admin from "firebase-admin";

describe("getMessageKey Cloud Function Test Suite", () => {
  let mockDocGet: jest.Mock;

  beforeEach(() => {
    mockDocGet = jest.fn();
    jest.spyOn(admin, "firestore").mockReturnValue({
      collection: (name: string) => ({
        doc: (id: string) => ({
          get: mockDocGet,
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
      },
    };

    await expect((getMessageKey as any).run(request)).rejects.toThrow(
      /Authentication required/
    );
  });

  it("should reject invalid/missing messageId with 'invalid-argument'", async () => {
    const request: any = {
      auth: { uid: "bob" },
      data: {
        messageId: "",
      },
    };

    await expect((getMessageKey as any).run(request)).rejects.toThrow(
      /Missing or invalid required field/
    );
  });

  it("should reject with 'not-found' if key does not exist or was shredded", async () => {
    mockDocGet.mockResolvedValue({
      exists: false,
    });

    const request: any = {
      auth: { uid: "bob" },
      data: {
        messageId: "msg_shredded_or_missing",
      },
    };

    await expect((getMessageKey as any).run(request)).rejects.toThrow(
      /Encryption key not found or already shredded/
    );
  });

  it("should reject with 'failed-precondition' if message is expired", async () => {
    const pastTimestamp = Date.now() - 60000; // 1 min in the past
    mockDocGet.mockResolvedValue({
      exists: true,
      data: () => ({
        senderId: "alice",
        recipientId: "bob",
        dek: "secret_dek_123",
        expiresAt: { toMillis: () => pastTimestamp },
      }),
    });

    const request: any = {
      auth: { uid: "bob" },
      data: {
        messageId: "msg_expired",
      },
    };

    await expect((getMessageKey as any).run(request)).rejects.toThrow(
      /Message has expired and its encryption key is no longer accessible/
    );
  });

  it("CRITICAL RULE: should reject caller who is not a participant with 'permission-denied'", async () => {
    const futureTimestamp = Date.now() + 60000;
    mockDocGet.mockResolvedValue({
      exists: true,
      data: () => ({
        senderId: "alice",
        recipientId: "bob",
        dek: "secret_dek_123",
        expiresAt: { toMillis: () => futureTimestamp },
      }),
    });

    const request: any = {
      auth: { uid: "attacker_eve" }, // Eve tries to steal Bob's message key
      data: {
        messageId: "msg_confidential",
      },
    };

    await expect((getMessageKey as any).run(request)).rejects.toThrow(
      /Caller is not authorized to retrieve this encryption key/
    );
  });

  it("should deliver DEK to the authorized recipient", async () => {
    const futureTimestamp = Date.now() + 300000;
    mockDocGet.mockResolvedValue({
      exists: true,
      data: () => ({
        senderId: "alice",
        recipientId: "bob",
        dek: "valid_dek_base64_payload",
        expiresAt: { toMillis: () => futureTimestamp },
      }),
    });

    const request: any = {
      auth: { uid: "bob" }, // Bob is the authorized recipient
      data: {
        messageId: "msg_valid_01",
      },
    };

    const result = await (getMessageKey as any).run(request);

    expect(result.success).toBe(true);
    expect(result.messageId).toBe("msg_valid_01");
    expect(result.dek).toBe("valid_dek_base64_payload");
    expect(result.expiresAtMillis).toBe(futureTimestamp);
  });

  it("should deliver DEK to the authorized sender (for local resync)", async () => {
    const futureTimestamp = Date.now() + 300000;
    mockDocGet.mockResolvedValue({
      exists: true,
      data: () => ({
        senderId: "alice",
        recipientId: "bob",
        dek: "valid_dek_base64_payload",
        expiresAt: { toMillis: () => futureTimestamp },
      }),
    });

    const request: any = {
      auth: { uid: "alice" }, // Alice is the sender
      data: {
        messageId: "msg_valid_01",
      },
    };

    const result = await (getMessageKey as any).run(request);

    expect(result.success).toBe(true);
    expect(result.dek).toBe("valid_dek_base64_payload");
  });
});
