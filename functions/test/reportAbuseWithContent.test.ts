import { reportAbuseWithContent } from "../src/reportAbuseWithContent";
import * as admin from "firebase-admin";

describe("v1.4: reportAbuseWithContent Cloud Function Test Suite (Parecer Jurídico C6)", () => {
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
        add: jest.fn().mockResolvedValue({ id: "mock_add_id" }),
      }),
    } as any);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should reject unauthenticated calls", async () => {
    const request: any = {
      auth: null,
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "HARASSMENT",
        contentSnippet: "Ofensas verbais",
        explicitConsent: true,
      },
    };

    await expect((reportAbuseWithContent as any).run(request)).rejects.toThrow(
      /Autenticação obrigatória/
    );
  });

  it("should reject calls without explicit consent (C6 / LGPD mandate)", async () => {
    const request: any = {
      auth: { uid: "reporter_bob" },
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "HARASSMENT",
        contentSnippet: "Ofensas verbais",
        explicitConsent: false,
      },
    };

    await expect((reportAbuseWithContent as any).run(request)).rejects.toThrow(
      /consentimento explícito do usuário é mandatório/
    );
  });

  it("should reject payload with unauthorized fields (strict allow-list)", async () => {
    const request: any = {
      auth: { uid: "reporter_bob" },
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "HARASSMENT",
        contentSnippet: "Ofensas verbais",
        explicitConsent: true,
        injectedMaliciousField: "drop database",
      },
    };

    await expect((reportAbuseWithContent as any).run(request)).rejects.toThrow(
      /Campo não permitido: 'injectedMaliciousField'/
    );
  });

  it("should reject empty contentSnippet", async () => {
    const request: any = {
      auth: { uid: "reporter_bob" },
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "HARASSMENT",
        contentSnippet: "   ",
        explicitConsent: true,
      },
    };

    await expect((reportAbuseWithContent as any).run(request)).rejects.toThrow(
      /Campo 'contentSnippet' é obrigatório/
    );
  });

  it("should reject contentSnippet exceeding 1000 characters", async () => {
    const request: any = {
      auth: { uid: "reporter_bob" },
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "HARASSMENT",
        contentSnippet: "a".repeat(1001),
        explicitConsent: true,
      },
    };

    await expect((reportAbuseWithContent as any).run(request)).rejects.toThrow(
      /excede o limite máximo permitido de 1000 caracteres/
    );
  });

  it("should successfully record content abuse report when explicit consent is provided", async () => {
    mockDocGet
      .mockResolvedValueOnce({ exists: false, data: () => null }) // userRateLimits
      .mockResolvedValueOnce({ exists: false, data: () => null }); // abuseMetrics

    const request: any = {
      auth: { uid: "reporter_bob" },
      data: {
        reportedFingerprint: validFingerprint,
        abuseType: "HARASSMENT",
        contentSnippet: "Mensagem de ameaça direcionada",
        explicitConsent: true,
        inviteId: "invite_abc",
      },
      rawRequest: {
        headers: { "x-forwarded-for": "189.10.20.30" },
      },
    };

    const result = await (reportAbuseWithContent as any).run(request);

    expect(result.success).toBe(true);
    expect(result.reportId).toBe("mock_auto_id_123");
    expect(result.timestamp).toBeGreaterThan(0);

    // Verify report was written with hasContent: true, snippet and consent
    expect(mockDocSet).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        reportedFingerprint: validFingerprint,
        reporterUid: "reporter_bob",
        abuseType: "HARASSMENT",
        hasContent: true,
        contentSnippet: "Mensagem de ameaça direcionada",
        explicitConsent: true,
        inviteId: "invite_abc",
      })
    );
  });
});
