import { createInvite } from "../src/createInvite";
import { acceptInvite } from "../src/acceptInvite";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

describe("Modelo C: createInvite and acceptInvite Test Suite", () => {
  let mockDocSet: jest.Mock;
  let mockDocGet: jest.Mock;
  let mockDocDelete: jest.Mock;
  let mockDocUpdate: jest.Mock;
  let mockRunTransaction: jest.Mock;

  // Generate 32 bytes valid pubKey and its sha256 fingerprint
  const validPubKeyBytes = crypto.randomBytes(32);
  const validPubKeyBase64 = validPubKeyBytes.toString("base64");
  const validFingerprint = crypto.createHash("sha256").update(validPubKeyBytes).digest("hex");

  beforeEach(() => {
    mockDocSet = jest.fn().mockResolvedValue(undefined);
    mockDocGet = jest.fn();
    mockDocDelete = jest.fn();
    mockDocUpdate = jest.fn().mockResolvedValue(undefined);

    mockRunTransaction = jest.fn().mockImplementation(async (cb: any) => {
      const mockTx = {
        get: mockDocGet,
        set: mockDocSet,
        update: mockDocUpdate,
        delete: mockDocDelete,
      };
      return cb(mockTx);
    });

    jest.spyOn(admin, "firestore").mockReturnValue({
      runTransaction: mockRunTransaction,
      collection: (_name: string) => ({
        doc: (_id: string) => ({
          get: mockDocGet,
          set: mockDocSet,
          update: mockDocUpdate,
          delete: mockDocDelete,
        }),
      }),
    } as any);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe("createInvite", () => {
    it("should reject unauthenticated calls", async () => {
      const request: any = {
        auth: null,
        data: {
          creatorFingerprint: validFingerprint,
          creatorPubKey: validPubKeyBase64,
        },
      };

      await expect((createInvite as any).run(request)).rejects.toThrow(
        /Autenticação obrigatória/
      );
    });

    it("should reject payload with missing or invalid fields", async () => {
      const request: any = {
        auth: { uid: "alice" },
        data: {
          creatorFingerprint: "short_fp",
          creatorPubKey: validPubKeyBase64,
        },
      };

      await expect((createInvite as any).run(request)).rejects.toThrow(
        /Fingerprint deve conter exatamente 64 caracteres/
      );
    });

    it("should reject mismatch between fingerprint and SHA-256(pubKey)", async () => {
      const fakeFp = "0".repeat(64);
      const request: any = {
        auth: { uid: "alice" },
        data: {
          creatorFingerprint: fakeFp,
          creatorPubKey: validPubKeyBase64,
        },
      };

      await expect((createInvite as any).run(request)).rejects.toThrow(
        /Fingerprint não corresponde ao hash SHA-256/
      );
    });

    it("should successfully generate invite token and save document with 24h TTL", async () => {
      const request: any = {
        auth: { uid: "alice" },
        data: {
          creatorFingerprint: validFingerprint,
          creatorPubKey: validPubKeyBase64,
        },
      };

      const result = await (createInvite as any).run(request);

      expect(result).toHaveProperty("inviteToken");
      expect(result.inviteToken).toHaveLength(64);
      expect(result.inviteLink).toBe(
        `pmsg://invite?token=${result.inviteToken}&fp=${validFingerprint}`
      );
      expect(result.expiresAtMillis).toBeGreaterThan(Date.now() + 23 * 3600 * 1000);

      expect(mockDocSet).toHaveBeenCalledWith(
        expect.objectContaining({
          creatorUid: "alice",
          creatorFingerprint: validFingerprint,
          creatorPubKey: validPubKeyBase64,
          used: false,
          acceptedByUid: null,
        })
      );
    });
  });

  describe("acceptInvite", () => {
    const inviteToken = "b".repeat(64);

    it("should reject unauthenticated caller", async () => {
      const request: any = {
        auth: null,
        data: { inviteToken },
      };

      await expect((acceptInvite as any).run(request)).rejects.toThrow(
        /Autenticação obrigatória/
      );
    });

    it("should reject invalid inviteToken format", async () => {
      const request: any = {
        auth: { uid: "bob" },
        data: { inviteToken: "short_token" },
      };

      await expect((acceptInvite as any).run(request)).rejects.toThrow(
        /Token de convite inválido/
      );
    });

    it("should reject if invite document does not exist or was incinerated", async () => {
      mockDocGet.mockResolvedValue({
        exists: false,
      });

      const request: any = {
        auth: { uid: "bob" },
        data: { inviteToken },
      };

      await expect((acceptInvite as any).run(request)).rejects.toThrow(
        /Convite não encontrado ou já incinerado/
      );
    });

    it("should reject if invite has already been used", async () => {
      mockDocGet.mockResolvedValue({
        exists: true,
        data: () => ({
          creatorUid: "alice",
          creatorFingerprint: validFingerprint,
          creatorPubKey: validPubKeyBase64,
          expiresAtMillis: Date.now() + 100000,
          used: true,
        }),
      });

      const request: any = {
        auth: { uid: "bob" },
        data: { inviteToken },
      };

      await expect((acceptInvite as any).run(request)).rejects.toThrow(
        /Convite já utilizado/
      );
    });

    it("should reject if invite is expired (> 24h)", async () => {
      mockDocGet.mockResolvedValue({
        exists: true,
        data: () => ({
          creatorUid: "alice",
          creatorFingerprint: validFingerprint,
          creatorPubKey: validPubKeyBase64,
          expiresAtMillis: Date.now() - 5000, // Expired
          used: false,
        }),
      });

      const request: any = {
        auth: { uid: "bob" },
        data: { inviteToken },
      };

      await expect((acceptInvite as any).run(request)).rejects.toThrow(
        /Convite expirado/
      );
    });

    it("should reject if creator attempts to accept their own invite", async () => {
      mockDocGet.mockResolvedValue({
        exists: true,
        data: () => ({
          creatorUid: "alice",
          creatorFingerprint: validFingerprint,
          creatorPubKey: validPubKeyBase64,
          expiresAtMillis: Date.now() + 100000,
          used: false,
        }),
      });

      const request: any = {
        auth: { uid: "alice" }, // Self-acceptance
        data: { inviteToken },
      };

      await expect((acceptInvite as any).run(request)).rejects.toThrow(
        /Não é permitido aceitar o próprio convite/
      );
    });

    it("should successfully accept invite and vanish from server (delete document)", async () => {
      mockDocGet.mockResolvedValue({
        exists: true,
        data: () => ({
          creatorUid: "alice",
          creatorFingerprint: validFingerprint,
          creatorPubKey: validPubKeyBase64,
          expiresAtMillis: Date.now() + 100000,
          used: false,
        }),
      });

      const request: any = {
        auth: { uid: "bob" },
        data: { inviteToken },
      };

      const result = await (acceptInvite as any).run(request);

      expect(result).toEqual({
        creatorFingerprint: validFingerprint,
        creatorPubKey: validPubKeyBase64,
      });

      // Verify vanish-after-accept deletion
      expect(mockDocDelete).toHaveBeenCalled();
    });
  });
});
