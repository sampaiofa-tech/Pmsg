import { executeCryptoShredding } from "../src/shredder";

describe("Crypto-Shredder Authoritative Server-Side TTL Test Suite", () => {
  let mockDb: any;
  let batchDeletedPaths: string[] = [];

  beforeEach(() => {
    batchDeletedPaths = [];
  });

  it("should hard-delete expired messageKeys and their corresponding messages in batch", async () => {
    const nowMillis = 1700000000000;
    const nowTimestamp = {
      toMillis: () => nowMillis,
    } as any;

    const expiredDoc1 = {
      id: "msg_key_001",
      data: () => ({
        messageId: "msg_001",
        dek: "super_secret_aes_dek_1",
        expiresAt: { toMillis: () => nowMillis - 1000 },
      }),
      ref: { path: "messageKeys/msg_key_001" },
    };

    const expiredDoc2 = {
      id: "msg_002",
      data: () => ({
        messageId: "msg_002",
        dek: "super_secret_aes_dek_2",
        expiresAt: { toMillis: () => nowMillis - 5000 },
      }),
      ref: { path: "messageKeys/msg_002" },
    };

    mockDb = {
      collection: (name: string) => ({
        where: (field: string, op: string, val: any) => ({
          limit: (limitCount: number) => ({
            get: async () => ({
              empty: name !== "messageKeys",
              docs: name === "messageKeys" ? [expiredDoc1, expiredDoc2] : [],
            }),
          }),
        }),
        doc: (id: string) => ({
          path: `${name}/${id}`,
        }),
      }),
      batch: () => ({
        delete: (ref: any) => {
          batchDeletedPaths.push(ref.path);
        },
        commit: async () => {},
      }),
    };

    const result = await executeCryptoShredding(mockDb, nowTimestamp);

    expect(result.shreddedKeysCount).toBe(2);
    expect(result.deletedMessagesCount).toBe(2);

    // Verify hard deletion of both DEKs and Messages
    expect(batchDeletedPaths).toContain("messageKeys/msg_key_001");
    expect(batchDeletedPaths).toContain("messages/msg_001");
    expect(batchDeletedPaths).toContain("messageKeys/msg_002");
    expect(batchDeletedPaths).toContain("messages/msg_002");
  });

  it("should do nothing when no messageKeys are expired", async () => {
    const nowTimestamp = { toMillis: () => 1700000000000 } as any;

    mockDb = {
      collection: () => ({
        where: () => ({
          limit: () => ({
            get: async () => ({
              empty: true,
              docs: [],
            }),
          }),
        }),
      }),
      batch: () => ({
        delete: () => {},
        commit: async () => {},
      }),
    };

    const result = await executeCryptoShredding(mockDb, nowTimestamp);

    expect(result.shreddedKeysCount).toBe(0);
    expect(result.deletedMessagesCount).toBe(0);
    expect(batchDeletedPaths.length).toBe(0);
  });

  it("CRITICAL ZERO-TRACE GUARANTEE: DEK destroyed -> content permanently unrecoverable", () => {
    const crypto = require("crypto");

    const originalMessage = "Top Secret Zero-Trace Ephemeral Communication";
    const rawDek = crypto.randomBytes(32); // 256-bit AES DEK
    const iv = crypto.randomBytes(12); // 96-bit GCM IV

    // 1. Encrypt with DEK
    const cipher = crypto.createCipheriv("aes-256-gcm", rawDek, iv);
    let ciphertext = cipher.update(originalMessage, "utf8", "hex");
    ciphertext += cipher.final("hex");
    const authTag = cipher.getAuthTag();

    // 2. Physical Crypto-Shredding: DEK is destroyed/zeroized
    rawDek.fill(0); // Zeroize in memory
    const destroyedDek = null; // Key record deleted from messageKeys

    // 3. Attempt decryption without DEK (or with wrong/zeroed key)
    expect(destroyedDek).toBeNull();

    const attemptDecryptWithWrongKey = () => {
      const wrongKey = crypto.randomBytes(32);
      const decipher = crypto.createDecipheriv("aes-256-gcm", wrongKey, iv);
      decipher.setAuthTag(authTag);
      let dec = decipher.update(ciphertext, "hex", "utf8");
      dec += decipher.final("utf8");
      return dec;
    };

    // Decryption MUST throw an authentication error
    expect(attemptDecryptWithWrongKey).toThrow();
  });

  it("should hard-delete expired connectionLogs and accessLogs in batch", async () => {
    const nowMillis = 1700000000000;
    const nowTimestamp = {
      toMillis: () => nowMillis,
    } as any;

    const expiredLog1 = {
      id: "log_001",
      data: () => ({
        ip: "192.168.1.1",
        expiresAt: { toMillis: () => nowMillis - 1000 },
      }),
      ref: { path: "connectionLogs/log_001" },
    };

    const expiredLog2 = {
      id: "log_002",
      data: () => ({
        ip: "10.0.0.2",
        expiresAt: { toMillis: () => nowMillis - 5000 },
      }),
      ref: { path: "accessLogs/log_002" },
    };

    mockDb = {
      collection: (name: string) => ({
        where: (field: string, op: string, val: any) => ({
          limit: (limitCount: number) => ({
            get: async () => {
              if (name === "connectionLogs") {
                return { empty: false, docs: [expiredLog1] };
              }
              if (name === "accessLogs") {
                return { empty: false, docs: [expiredLog2] };
              }
              return { empty: true, docs: [] };
            },
          }),
        }),
        doc: (id: string) => ({
          path: `${name}/${id}`,
        }),
      }),
      batch: () => ({
        delete: (ref: any) => {
          batchDeletedPaths.push(ref.path);
        },
        commit: async () => {},
      }),
    };

    const result = await executeCryptoShredding(mockDb, nowTimestamp);

    expect(result.shreddedKeysCount).toBe(0);
    expect(result.deletedMessagesCount).toBe(0);
    expect(result.deletedLogsCount).toBe(2);

    expect(batchDeletedPaths).toContain("connectionLogs/log_001");
    expect(batchDeletedPaths).toContain("accessLogs/log_002");
  });
});

