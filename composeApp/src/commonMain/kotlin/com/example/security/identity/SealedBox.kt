package com.example.security.identity

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * Encapsulated envelope containing the opaque cryptographic payload for the recipient.
 * The server stores and transfers this as opaque bytes without knowledge of the DEK.
 */
@Serializable
data class SealedBoxEnvelope(
    val ephemeralPubKeyHex: String,
    val wrappedDekBase64: String
)

/**
 * Multiplatform Sealed-Box primitive for E2E DEK encryption.
 *
 * Algorithm specification:
 * 1. Alice generates an ephemeral X25519 keypair (sender anonymity).
 * 2. sharedSecret = X25519(ephemeralPriv, recipientPubKey)
 * 3. KEK = HKDF-SHA256(sharedSecret, salt=SHA-256(ephemeralPubKey), info="pmsg-dek-wrap-v1", length=32)
 * 4. wrappedDek = AES-256-GCM(plaintext=DEK, key=KEK, iv=nonce)
 * 5. Bob recovers DEK via: sharedSecret = X25519(recipientPrivKey, ephemeralPubKey) -> same KEK -> unwrap.
 */
object SealedBox {

    private const val INFO_LABEL = "pmsg-dek-wrap-v1"
    const val NONCE_SIZE = 12 // 96 bits

    @OptIn(ExperimentalEncodingApi::class)
    fun seal(
        dek: ByteArray,
        recipientPubKey: ByteArray,
        customEphemeralPriv: ByteArray? = null,
        customNonce: ByteArray? = null
    ): SealedBoxEnvelope {
        require(recipientPubKey.size == 32) { "Recipient public key must be 32 bytes" }

        // 1. Generate ephemeral X25519 keypair
        val ephemeralPriv = customEphemeralPriv ?: ByteArray(32).also { Random.nextBytes(it) }
        val clampedPriv = Curve25519Engine.clampPrivateKey(ephemeralPriv)
        val ephemeralPub = IdentityCurve25519.generatePublicKey(clampedPriv)

        // 2. Compute shared secret
        val sharedSecret = IdentityCurve25519.computeSharedSecret(clampedPriv, recipientPubKey)

        // 3. Derive KEK via HKDF-SHA256
        val salt = Sha256Digest.digest(ephemeralPub)
        val info = INFO_LABEL.encodeToByteArray()
        val kek = HkdfSha256.deriveKey(ikm = sharedSecret, salt = salt, info = info, length = 32)

        // 4. Encrypt DEK with AES-256-GCM
        val nonce = customNonce ?: ByteArray(NONCE_SIZE).also { Random.nextBytes(it) }
        require(nonce.size == NONCE_SIZE) { "Nonce must be 12 bytes" }
        val encryptedPayload = AesGcm.encrypt(plaintext = dek, key = kek, iv = nonce)

        // 5. Pack wrappedDek = nonce (12 bytes) + encryptedPayload (ciphertext + 16-byte tag)
        val wrappedBytes = ByteArray(NONCE_SIZE + encryptedPayload.size)
        nonce.copyInto(wrappedBytes, 0, 0, NONCE_SIZE)
        encryptedPayload.copyInto(wrappedBytes, NONCE_SIZE, 0, encryptedPayload.size)

        return SealedBoxEnvelope(
            ephemeralPubKeyHex = bytesToHex(ephemeralPub),
            wrappedDekBase64 = Base64.encode(wrappedBytes)
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun unseal(
        envelope: SealedBoxEnvelope,
        recipientPrivKey: ByteArray
    ): ByteArray {
        require(recipientPrivKey.size == 32) { "Recipient private key must be 32 bytes" }

        val ephemeralPub = hexToBytes(envelope.ephemeralPubKeyHex)
        require(ephemeralPub.size == 32) { "Ephemeral public key must be 32 bytes" }

        val wrappedBytes = Base64.decode(envelope.wrappedDekBase64)
        require(wrappedBytes.size >= NONCE_SIZE + 16) { "Wrapped DEK payload is too short" }

        val nonce = ByteArray(NONCE_SIZE)
        wrappedBytes.copyInto(nonce, 0, 0, NONCE_SIZE)

        val cipherWithTag = ByteArray(wrappedBytes.size - NONCE_SIZE)
        wrappedBytes.copyInto(cipherWithTag, 0, NONCE_SIZE, wrappedBytes.size)

        // 1. Compute shared secret using Bob's private key and ephemeral public key
        val sharedSecret = IdentityCurve25519.computeSharedSecret(recipientPrivKey, ephemeralPub)

        // 2. Derive same KEK via HKDF-SHA256
        val salt = Sha256Digest.digest(ephemeralPub)
        val info = INFO_LABEL.encodeToByteArray()
        val kek = HkdfSha256.deriveKey(ikm = sharedSecret, salt = salt, info = info, length = 32)

        // 3. Decrypt DEK with AES-256-GCM
        return AesGcm.decrypt(ciphertext = cipherWithTag, key = kek, iv = nonce)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim().lowercase()
        require(clean.length % 2 == 0) { "Hex string must have even length" }
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val high = clean[i * 2].digitToInt(16)
            val low = clean[i * 2 + 1].digitToInt(16)
            result[i] = ((high shl 4) or low).toByte()
        }
        return result
    }
}
