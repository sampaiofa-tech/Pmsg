import { resolveFingerprint } from "../src/resolveFingerprint";
import * as admin from "firebase-admin";

describe("resolveFingerprint Cloud Function Test Suite", () => {
  let mockDocGet: jest.Mock;

  beforeEach(() => {
    mockDocGet = jest.fn();
    jest.spyOn(admin, "firestore").mockReturnValue({
      runTransaction: jest.fn().mockImplementation(async (cb: any) => {
        const mockTx = {
          get: jest.fn().mockResolvedValue({
            exists: false,
          }),
          set: jest.fn(),
        };
        return cb(mockTx);
      }),
      collection: (_name: string) => ({
        doc: (_id: string) => ({
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
        fingerprint: "a".repeat(64),
      },
    };

    await expect((resolveFingerprint as any).run(request)).rejects.toThrow(
      /Autenticação obrigatória/
    );
  });

  it("should reject invalid/missing fingerprint with 'invalid-argument'", async () => {
    const request: any = {
      auth: { uid: "user_alice" },
      data: {
        fingerprint: "short-fingerprint",
      },
    };

    await expect((resolveFingerprint as any).run(request)).rejects.toThrow(
      /Fingerprint deve conter exatamente 64 caracteres/
    );
  });

  it("should reject with 'not-found' if identity does not exist", async () => {
    mockDocGet.mockResolvedValue({
      exists: false,
    });

    const request: any = {
      auth: { uid: "user_alice" },
      data: {
        fingerprint: "f".repeat(64),
      },
    };

    await expect((resolveFingerprint as any).run(request)).rejects.toThrow(
      /Identidade não encontrada no diretório técnico/
    );
  });

  it("should return identity data when document exists", async () => {
    const mockIdentity = {
      currentAuthUid: "user_bob",
      pubKey: "dGVzdC1wdWJsaWMta2V5LTMyeHh4eHh4eHh4eHh4eHg=",
      signingPubKey: "dGVzdC1zaWduaW5nLXB1YmtleS0zMnh4eHg=",
      updatedAt: 1725390000000,
    };

    mockDocGet.mockResolvedValue({
      exists: true,
      data: () => mockIdentity,
    });

    const request: any = {
      auth: { uid: "user_alice" },
      data: {
        fingerprint: "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      },
    };

    const response = await (resolveFingerprint as any).run(request);
    expect(response).toEqual({
      currentAuthUid: "user_bob",
      pubKey: "dGVzdC1wdWJsaWMta2V5LTMyeHh4eHh4eHh4eHh4eHg=",
      signingPubKey: "dGVzdC1zaWduaW5nLXB1YmtleS0zMnh4eHg=",
      updatedAt: 1725390000000,
    });
  });

  it("should reject resolution of revoked identity with permission-denied (Ed25519 moderation sanction)", async () => {
    mockDocGet.mockResolvedValue({
      exists: true,
      data: () => ({
        currentAuthUid: "user_banned",
        pubKey: "dGVzdC1wdWJsaWMta2V5LTMyeHh4eHh4eHh4eHh4eHg=",
        signingPubKey: "dGVzdC1zaWduaW5nLXB1YmtleS0zMnh4eHg=",
        revoked: true,
      }),
    });

    const request: any = {
      auth: { uid: "user_alice" },
      data: {
        fingerprint: "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      },
    };

    await expect((resolveFingerprint as any).run(request)).rejects.toThrow(
      /Esta identidade criptográfica foi revogada por moderação/
    );
  });
});
