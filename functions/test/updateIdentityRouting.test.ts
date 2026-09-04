import { updateIdentityRouting } from "../src/updateIdentityRouting";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

describe("Fase 5: updateIdentityRouting Cloud Function Test Suite", () => {
  let mockDocSet: jest.Mock;
  let mockDocGet: jest.Mock;
  let mockRunTransaction: jest.Mock;

  const validPubKeyBytes = crypto.randomBytes(32);
  const validPubKeyBase64 = validPubKeyBytes.toString("base64");
  const validFingerprint = crypto.createHash("sha256").update(validPubKeyBytes).digest("hex");

  beforeEach(() => {
    mockDocSet = jest.fn().mockResolvedValue(undefined);
    mockDocGet = jest.fn();

    mockRunTransaction = jest.fn().mockImplementation(async (cb: any) => {
      const mockTx = {
        get: mockDocGet,
        set: mockDocSet,
        update: jest.fn(),
      };
      return cb(mockTx);
    });

    jest.spyOn(admin, "firestore").mockReturnValue({
      runTransaction: mockRunTransaction,
      collection: (_name: string) => ({
        doc: (_id: string) => ({
          get: mockDocGet,
          set: mockDocSet,
        }),
      }),
    } as any);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should reject unauthenticated caller", async () => {
    const request: any = {
      auth: null,
      data: {
        fingerprint: validFingerprint,
        pubKey: validPubKeyBase64,
      },
    };

    await expect((updateIdentityRouting as any).run(request)).rejects.toThrow(
      /Autenticação obrigatória/
    );
  });

  it("should reject mismatched fingerprint and public key", async () => {
    const fakeFp = "1".repeat(64);
    const request: any = {
      auth: { uid: "new_device_uid" },
      data: {
        fingerprint: fakeFp,
        pubKey: validPubKeyBase64,
      },
    };

    await expect((updateIdentityRouting as any).run(request)).rejects.toThrow(
      /Fingerprint não corresponde ao hash da chave pública/
    );
  });

  it("should update identities collection with new currentAuthUid and updatedAt", async () => {
    const request: any = {
      auth: { uid: "new_device_uid_999" },
      data: {
        fingerprint: validFingerprint,
        pubKey: validPubKeyBase64,
      },
    };

    const response = await (updateIdentityRouting as any).run(request);

    expect(response).toEqual({
      success: true,
      fingerprint: validFingerprint,
      currentAuthUid: "new_device_uid_999",
    });

    expect(mockDocSet).toHaveBeenCalledWith(
      expect.objectContaining({
        currentAuthUid: "new_device_uid_999",
        pubKey: validPubKeyBase64,
      }),
      { merge: true }
    );
  });
});
