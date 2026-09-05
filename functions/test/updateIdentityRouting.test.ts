import { updateIdentityRouting } from "../src/updateIdentityRouting";
import * as admin from "firebase-admin";
import * as crypto from "crypto";

describe("F0 Security Fix: updateIdentityRouting with Ed25519 Proof-of-Possession", () => {
  let mockDocSet: jest.Mock;
  let mockDocGet: jest.Mock;
  let mockRunTransaction: jest.Mock;

  // Alice's legitimate X25519 and Ed25519 keys
  const aliceX25519PubKeyBytes = crypto.randomBytes(32);
  const aliceX25519PubKeyBase64 = aliceX25519PubKeyBytes.toString("base64");
  const aliceFingerprint = crypto.createHash("sha256").update(new Uint8Array(aliceX25519PubKeyBytes)).digest("hex");

  const aliceEd25519 = crypto.generateKeyPairSync("ed25519");
  const aliceSigningPubKeyRaw = aliceEd25519.publicKey.export({ type: "spki", format: "der" }).subarray(12);
  const aliceSigningPubKeyBase64 = aliceSigningPubKeyRaw.toString("base64");

  // Eve's keys (Attacker / Malicious contact)
  const eveEd25519 = crypto.generateKeyPairSync("ed25519");
  const eveSigningPubKeyRaw = eveEd25519.publicKey.export({ type: "spki", format: "der" }).subarray(12);
  const eveSigningPubKeyBase64 = eveSigningPubKeyRaw.toString("base64");

  function createSignature(payload: string, privateKey: crypto.KeyObject): string {
    return crypto.sign(null, new Uint8Array(Buffer.from(payload, "utf8")), privateKey).toString("base64");
  }

  beforeEach(() => {
    mockDocSet = jest.fn().mockResolvedValue(undefined);
    mockDocGet = jest.fn().mockResolvedValue({
      exists: true,
      data: () => ({
        currentAuthUid: "alice_device_1",
        pubKey: aliceX25519PubKeyBase64,
        signingPubKey: aliceSigningPubKeyBase64,
      }),
    });

    mockRunTransaction = jest.fn().mockImplementation(async (cb: any) => {
      const mockTx = {
        get: jest.fn().mockResolvedValue({
          data: () => ({ count: 0, windowStart: Date.now() }),
        }),
        set: jest.fn(),
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
        fingerprint: aliceFingerprint,
        pubKey: aliceX25519PubKeyBase64,
        signature: "some_sig",
        timestamp: Date.now(),
      },
    };

    await expect((updateIdentityRouting as any).run(request)).rejects.toThrow(
      /Autenticação obrigatória/
    );
  });

  it("4.a: Legitimate owner (correct mnemonic/Ed25519 key) -> updates routing successfully", async () => {
    const timestamp = Date.now();
    const newAuthUid = "alice_device_2_new_uid";
    const canonicalPayload = `pmsg-routing-v1|${aliceFingerprint}|${newAuthUid}|${timestamp}`;
    const validSignature = createSignature(canonicalPayload, aliceEd25519.privateKey);

    const request: any = {
      auth: { uid: newAuthUid },
      data: {
        fingerprint: aliceFingerprint,
        pubKey: aliceX25519PubKeyBase64,
        signature: validSignature,
        timestamp,
      },
    };

    const response = await (updateIdentityRouting as any).run(request);

    expect(response).toEqual({
      success: true,
      fingerprint: aliceFingerprint,
      currentAuthUid: newAuthUid,
      signingPubKey: aliceSigningPubKeyBase64,
    });

    expect(mockDocSet).toHaveBeenCalledWith(
      expect.objectContaining({
        currentAuthUid: newAuthUid,
        pubKey: aliceX25519PubKeyBase64,
        signingPubKey: aliceSigningPubKeyBase64,
      }),
      { merge: true }
    );
  });

  it("4.b: Contact (Eve) with Bob's public key -> REJECTED with permission-denied (Hijack attempt)", async () => {
    // Eve knows Bob's fingerprint and public key, but signs with her own key (or provides an invalid signature)
    const timestamp = Date.now();
    const eveUid = "eve_malicious_attacker_uid";
    const canonicalPayload = `pmsg-routing-v1|${aliceFingerprint}|${eveUid}|${timestamp}`;

    // Eve signs with her private key
    const eveForgedSignature = createSignature(canonicalPayload, eveEd25519.privateKey);

    const request: any = {
      auth: { uid: eveUid },
      data: {
        fingerprint: aliceFingerprint,
        pubKey: aliceX25519PubKeyBase64,
        signature: eveForgedSignature,
        timestamp,
        signingPubKey: eveSigningPubKeyBase64, // Eve tries to pass her own signing key, but doc already has Alice's
      },
    };

    await expect((updateIdentityRouting as any).run(request)).rejects.toThrow(
      /Prova de posse inválida|assinatura Ed25519 rejeitada/
    );

    expect(mockDocSet).not.toHaveBeenCalled();
  });

  it("4.c: Replay of the same payload with old timestamp (> 5 min) -> REJECTED", async () => {
    const oldTimestamp = Date.now() - (6 * 60 * 1000); // 6 minutes ago (exceeds 5 min tolerance)
    const newAuthUid = "alice_device_2_new_uid";
    const canonicalPayload = `pmsg-routing-v1|${aliceFingerprint}|${newAuthUid}|${oldTimestamp}`;
    const signature = createSignature(canonicalPayload, aliceEd25519.privateKey);

    const request: any = {
      auth: { uid: newAuthUid },
      data: {
        fingerprint: aliceFingerprint,
        pubKey: aliceX25519PubKeyBase64,
        signature,
        timestamp: oldTimestamp,
      },
    };

    await expect((updateIdentityRouting as any).run(request)).rejects.toThrow(
      /Timestamp expirado ou fora da tolerância de 5 minutos/
    );

    expect(mockDocSet).not.toHaveBeenCalled();
  });

  it("4.d: Fingerprint inconsistent with pubKey -> REJECTED", async () => {
    const fakeFp = "0".repeat(64); // Does not match SHA-256(aliceX25519PubKeyBytes)
    const timestamp = Date.now();
    const newAuthUid = "alice_device_2_new_uid";

    const request: any = {
      auth: { uid: newAuthUid },
      data: {
        fingerprint: fakeFp,
        pubKey: aliceX25519PubKeyBase64,
        signature: "valid_or_invalid_sig",
        timestamp,
      },
    };

    await expect((updateIdentityRouting as any).run(request)).rejects.toThrow(
      /Fingerprint não corresponde ao hash da chave pública fornecida/
    );

    expect(mockDocSet).not.toHaveBeenCalled();
  });
});
