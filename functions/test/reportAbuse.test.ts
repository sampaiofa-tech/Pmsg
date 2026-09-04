import { reportAbuse } from "../src/reportAbuse";
import * as admin from "firebase-admin";

describe("v1.3: reportAbuse Cloud Function Test Suite", () => {
  let mockDocSet: jest.Mock;
  let mockDocGet: jest.Mock;
  let mockRunTransaction: jest.Mock;

  const validFingerprint = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90";

  beforeEach(() => {
    mockDocSet = jest.fn().mockResolvedValue(undefined);
    mockDocGet = jest.fn();

    mockRunTransaction = jest.fn().mockImplementation(async (cb: any) => {
      const mockTx = {
        get: mockDocGet,
        set: mockDocSet,
        update: jest.fn().mockResolvedValue(undefined),
        delete: jest.fn().mockResolvedValue(undefined),
      };
      return cb(mockTx);
    });

    jest.spyOn(admin, "firestore").mockReturnValue({
      runTransaction: mockRunTransaction,
      collection: (_name: string) => ({
        doc: (_id?: string) => ({
          id: _id || "mock_auto_id_123",
          get: mockDocGet,
          set: mockDocSet,
        }),
      }),
    } as any);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should reject unauthenticated calls with unauthenticated error", async () => {
    const request: any = {
      auth: null,
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "SPAM",
      },
    };

    await expect((reportAbuse as any).run(request)).rejects.toThrow(
      /Autenticação obrigatória/
    );
  });

  it("should reject payload with missing reportedFingerprint", async () => {
    const request: any = {
      auth: { uid: "reporter_alice" },
      data: {
        abuseType: "SPAM",
      },
    };

    await expect((reportAbuse as any).run(request)).rejects.toThrow(
      /Campo 'reportedFingerprint' é obrigatório/
    );
  });

  it("should reject payload with invalid hex or length for reportedFingerprint", async () => {
    const request: any = {
      auth: { uid: "reporter_alice" },
      data: {
        reportedFingerprint: "not_a_valid_64_hex_fingerprint",
        abuseType: "HARASSMENT",
      },
    };

    await expect((reportAbuse as any).run(request)).rejects.toThrow(
      /Fingerprint deve conter exatamente 64 caracteres hexadecimais/
    );
  });

  it("should reject payload with invalid abuseType", async () => {
    const request: any = {
      auth: { uid: "reporter_alice" },
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "INVALID_ABUSE_TYPE",
      },
    };

    await expect((reportAbuse as any).run(request)).rejects.toThrow(
      /Tipo de abuso inválido/
    );
  });

  it("CRITICAL ZERO-KNOWLEDGE RULE: should reject any payload attempting to include message content", async () => {
    const contentPayloads = [
      { text: "Ele me enviou uma mensagem feia" },
      { message: "conteúdo da conversa" },
      { content: "texto secreto" },
      { body: "mensagem de assédio" },
      { plaintext: "ola mundo" },
      { ciphertext: "c2VjcmV0" },
    ];

    for (const extraContent of contentPayloads) {
      const request: any = {
        auth: { uid: "reporter_alice" },
        data: {
          reportedFingerprint: validFingerprint,
          abuseType: "HARASSMENT",
          ...extraContent,
        },
      };

      await expect((reportAbuse as any).run(request)).rejects.toThrow(
        /Violação de privacidade: o servidor é cego/
      );
    }
  });

  it("CRITICAL RATE-LIMIT RULE: should reject with resource-exhausted (429) when rate limit exceeded", async () => {
    const now = Date.now();
    // Simulate user has already submitted 5 reports in current window
    mockDocGet.mockResolvedValueOnce({
      data: () => ({
        count: 5,
        windowStart: now - 30000, // 30 seconds ago
      }),
    });

    const request: any = {
      auth: { uid: "spammer_reporter" },
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "SPAM",
      },
    };

    await expect((reportAbuse as any).run(request)).rejects.toThrow(
      /Limite de denúncias excedido/
    );
  });

  it("should successfully register valid abuse report and record metrics", async () => {
    const now = Date.now();

    // 1st transaction (rate limit check): user count is 0
    mockDocGet.mockResolvedValueOnce({
      data: () => ({
        count: 0,
        windowStart: now,
      }),
    });

    // 2nd transaction (metrics check): fingerprint has no previous reports
    mockDocGet.mockResolvedValueOnce({
      data: () => ({
        reporters: [],
      }),
    });

    const request: any = {
      auth: { uid: "reporter_alice" },
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "HARASSMENT",
        inviteId: "invite_token_sample",
      },
    };

    const result = await (reportAbuse as any).run(request);

    expect(result).toHaveProperty("success", true);
    expect(result).toHaveProperty("reportId");

    // Verify report doc created
    expect(mockDocSet).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        reportedFingerprint: validFingerprint,
        reporterUid: "reporter_alice",
        abuseType: "HARASSMENT",
        inviteId: "invite_token_sample",
      })
    );

    // Verify metrics doc updated
    expect(mockDocSet).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        reportedFingerprint: validFingerprint,
        reportCount: 1,
        reporters: ["reporter_alice"],
        abuseFlag: false,
      }),
      { merge: true }
    );
  });

  it("should flag abuse (abuseFlag = true) when independent reports reach threshold (3 distinct reporters)", async () => {
    const now = Date.now();

    // 1st transaction (rate limit check): user count is 0
    mockDocGet.mockResolvedValueOnce({
      data: () => ({
        count: 1,
        windowStart: now,
      }),
    });

    // 2nd transaction (metrics check): fingerprint already reported by bob and charlie
    mockDocGet.mockResolvedValueOnce({
      data: () => ({
        reporters: ["reporter_bob", "reporter_charlie"],
      }),
    });

    const request: any = {
      auth: { uid: "reporter_alice" }, // 3rd independent reporter
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "SPAM",
      },
    };

    const result = await (reportAbuse as any).run(request);

    expect(result.success).toBe(true);

    // Verify metrics doc flagged abuse
    expect(mockDocSet).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        reportedFingerprint: validFingerprint,
        reportCount: 3,
        reporters: ["reporter_bob", "reporter_charlie", "reporter_alice"],
        abuseFlag: true,
      }),
      { merge: true }
    );
  });
});
