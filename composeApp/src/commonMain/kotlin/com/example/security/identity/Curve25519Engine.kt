package com.example.security.identity

/**
 * Pure Kotlin implementation of X25519 scalar multiplication (RFC 7748 §5).
 * Operates over the prime field GF(2^255 - 19).
 *
 * Fully deterministic across all Kotlin Multiplatform targets (Android, Desktop, iOS, WasmJS).
 */
object Curve25519Engine {

    private const val A24 = 121665L

    /**
     * Clamps private key per RFC 7748 §5:
     * - Clear bits 0, 1, 2 of byte 0
     * - Clear bit 7 of byte 31
     * - Set bit 6 of byte 31
     */
    fun clampPrivateKey(key: ByteArray): ByteArray {
        require(key.size == 32) { "Key must be 32 bytes" }
        val clamped = key.copyOf(32)
        clamped[0] = (clamped[0].toInt() and 248).toByte()
        clamped[31] = (clamped[31].toInt() and 127).toByte()
        clamped[31] = (clamped[31].toInt() or 64).toByte()
        return clamped
    }

    /**
     * Computes scalar * base_point (u = 9).
     */
    fun scalarMultBase(scalar: ByteArray): ByteArray {
        val basePoint = ByteArray(32)
        basePoint[0] = 9
        return scalarMult(scalar, basePoint)
    }

    /**
     * Computes scalar * u-coordinate per RFC 7748 §5.
     */
    fun scalarMult(scalar: ByteArray, uCoord: ByteArray): ByteArray {
        require(scalar.size == 32) { "Scalar must be 32 bytes" }
        require(uCoord.size == 32) { "uCoord must be 32 bytes" }

        val k = clampPrivateKey(scalar)

        // Decode u-coordinate: mask the most significant bit
        val uBytes = uCoord.copyOf(32)
        uBytes[31] = (uBytes[31].toInt() and 127).toByte()

        val x1 = decode(uBytes)
        var x2 = fromLong(1)
        var z2 = fromLong(0)
        var x3 = x1.copyOf()
        var z3 = fromLong(1)
        var swap = 0

        for (t in 254 downTo 0) {
            val byteIdx = t / 8
            val bitIdx = t % 8
            val kt = ((k[byteIdx].toInt() ushr bitIdx) and 1)
            swap = swap xor kt

            cswap(swap, x2, x3)
            cswap(swap, z2, z3)
            swap = kt

            val a = add(x2, z2)
            val aa = sqr(a)
            val b = sub(x2, z2)
            val bb = sqr(b)
            val e = sub(aa, bb)
            val c = add(x3, z3)
            val d = sub(x3, z3)
            val da = mul(d, a)
            val cb = mul(c, b)
            x3 = sqr(add(da, cb))
            z3 = mul(x1, sqr(sub(da, cb)))
            x2 = mul(aa, bb)
            z2 = mul(e, add(aa, mulSmall(e, A24)))
        }

        cswap(swap, x2, x3)
        cswap(swap, z2, z3)

        val result = mul(x2, inv(z2))
        return encode(result)
    }

    // -------------------------------------------------------------------------
    // Field arithmetic modulo 2^255 - 19 using 16 limbs of 16-bit integers
    // -------------------------------------------------------------------------

    private fun fromLong(value: Long): LongArray {
        val r = LongArray(16)
        r[0] = value and 0xFFFF
        r[1] = (value ushr 16) and 0xFFFF
        return r
    }

    private fun decode(bytes: ByteArray): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            val b0 = bytes[i * 2].toLong() and 0xFF
            val b1 = bytes[i * 2 + 1].toLong() and 0xFF
            r[i] = b0 or (b1 shl 8)
        }
        return r
    }

    private fun encode(a: LongArray): ByteArray {
        val r = carryAndReduce(a)
        val bytes = ByteArray(32)
        for (i in 0 until 16) {
            bytes[i * 2] = (r[i] and 0xFF).toByte()
            bytes[i * 2 + 1] = ((r[i] ushr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun cswap(swap: Int, a: LongArray, b: LongArray) {
        val mask = (if (swap != 0) 0xFFFFL else 0L)
        for (i in 0 until 16) {
            val t = mask and (a[i] xor b[i])
            a[i] = a[i] xor t
            b[i] = b[i] xor t
        }
    }

    private fun add(a: LongArray, b: LongArray): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = a[i] + b[i]
        }
        return carry(r)
    }

    private fun sub(a: LongArray, b: LongArray): LongArray {
        // Add 2 * (2^255 - 19) = 2^256 - 38 to ensure result is positive
        val r = LongArray(16)
        r[0] = a[0] + 0xFFDA - b[0] // 0x10000 - 38 = 0xFFDA with carry into next limb
        var carry = -1L // because we borrowed 0x10000 for limb 0, but added 2^256 = 16 limbs of 0xFFFF
        // 2p = [0xFFFF - 37, 0xFFFF, 0xFFFF, ..., 0x7FFF * 2] = [0xFFDA, 0xFFFF, ..., 0xFFFF]
        // 2 * (2^255 - 19) = 2^256 - 38:
        // Limb 0 = 0x10000 - 38 = 65498. Then limbs 1..15 are 0xFFFF.
        r[0] = a[0] + 0x10000L - 38L - b[0]
        for (i in 1 until 16) {
            r[i] = a[i] + 0xFFFFL - b[i]
        }
        return carry(r)
    }

    private fun mul(a: LongArray, b: LongArray): LongArray {
        val prod = LongArray(31)
        for (i in 0 until 16) {
            val ai = a[i]
            for (j in 0 until 16) {
                prod[i + j] += ai * b[j]
            }
        }
        // Fold high limbs (16..30) back into (0..14) with factor 38
        // because 2^256 = 2 * 2^255 = 2 * 19 = 38 mod p
        for (i in 0 until 15) {
            prod[i] += prod[i + 16] * 38L
        }
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = prod[i]
        }
        return carry(r)
    }

    private fun mulSmall(a: LongArray, b: Long): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = a[i] * b
        }
        return carry(r)
    }

    private fun sqr(a: LongArray): LongArray = mul(a, a)

    private fun carry(r: LongArray): LongArray {
        for (step in 0 until 2) {
            for (i in 0 until 15) {
                val c = r[i] shr 16
                r[i] = r[i] and 0xFFFF
                r[i + 1] += c
            }
            val c = r[15] shr 15
            r[15] = r[15] and 0x7FFF
            r[0] += c * 19
        }
        return r
    }

    /**
     * Performs exact canonical reduction modulo 2^255 - 19
     * (subtracts p if r >= p).
     */
    private fun carryAndReduce(a: LongArray): LongArray {
        val r = a.copyOf()
        carry(r)

        // Test if r >= p (p = 2^255 - 19: limbs 0=0xFFED, 1..14=0xFFFF, 15=0x7FFF)
        // Candidate: r - p = r + 19 - 2^255
        val cand = LongArray(16)
        cand[0] = r[0] + 19
        for (i in 0 until 15) {
            val c = cand[i] shr 16
            cand[i] = cand[i] and 0xFFFF
            cand[i + 1] = r[i + 1] + c
        }
        val c = cand[15] shr 15 // If bit 15 is set, r + 19 >= 2^255 <=> r >= p
        cand[15] = cand[15] and 0x7FFF

        return if (c != 0L) cand else r
    }

    /**
     * Inversion via Fermat's Little Theorem: a^(p-2) mod p
     * where p - 2 = 2^255 - 21.
     */
    private fun inv(z: LongArray): LongArray {
        val a = sqr(z)               // 2
        val t0 = sqr(a)              // 4
        val t1 = sqr(t0)             // 8
        val b = mul(t1, z)           // 9 = 2^3 + 1
        val c = mul(b, a)            // 11 = 2^3 + 2^1 + 1
        val t2 = sqr(c)              // 22
        val d = mul(t2, a)           // 24
        val t3 = sqr(d)              // 48
        val t4 = sqr(t3)             // 96
        val t5 = sqr(t4)             // 192
        val t6 = sqr(t5)             // 384
        val e = mul(t6, b)           // 2^5 - 1

        // Standard addition chain for 2^255 - 21
        var t = sqr(e)
        for (i in 1 until 5) t = sqr(t)
        val f = mul(t, e)            // 2^10 - 1

        t = sqr(f)
        for (i in 1 until 10) t = sqr(t)
        val g = mul(t, f)            // 2^20 - 1

        t = sqr(g)
        for (i in 1 until 20) t = sqr(t)
        val h = mul(t, g)            // 2^40 - 1

        t = sqr(h)
        for (i in 1 until 10) t = sqr(t)
        val k = mul(t, f)            // 2^50 - 1

        t = sqr(k)
        for (i in 1 until 50) t = sqr(t)
        val l = mul(t, k)            // 2^100 - 1

        t = sqr(l)
        for (i in 1 until 100) t = sqr(t)
        val m = mul(t, l)            // 2^200 - 1

        t = sqr(m)
        for (i in 1 until 50) t = sqr(t)
        val n = mul(t, k)            // 2^250 - 1

        t = sqr(n)
        for (i in 1 until 5) t = sqr(t)
        return mul(t, c)             // 2^255 - 21
    }
}
