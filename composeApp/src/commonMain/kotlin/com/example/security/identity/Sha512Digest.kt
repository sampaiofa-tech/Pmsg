package com.example.security.identity

/**
 * Self-contained, pure Kotlin implementation of SHA-512 (FIPS 180-4).
 * Works identically across all Kotlin Multiplatform targets with zero dependencies.
 */
object Sha512Digest {

    private val K = longArrayOf(
        0x428a2f98d728ae22L, 0x7137449123ef65cdL, -0x4a3f04312de8bc06L, -0x164a245b0bb622c9L,
        0x3956c25bf348b538L, 0x59f111f1b605d019L, -0x6dc748f142273188L, -0x54e3a12b4ba71337L,
        -0x27f855685dfb90c1L, -0x2610142475e77ad8L, 0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
        0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L,
        -0x67c1aeae2039be49L, -0x57ce39934e8f7739L, -0x4ffcd838708ba8c5L, -0x40a680387e68c6e2L,
        -0x391ff40d216503c8L, -0x2a5868b1a8f6d6eeL, 0x06ca6351e003826fL, 0x142929670a0e6e70L,
        0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL,
        0x650a73548baf63deL, 0x766a0abb3c77b2a8L, -0x7e3d36d2673bf8a7L, -0x6d8dd37b42ff2e43L,
        -0x5d40175f7344933aL, -0x57e599b50f7ff3d4L, -0x3db474900a3d463eL, -0x3893ae5d56b00c3bL,
        -0x2e6d17e721532f65L, -0x2966f9dc810e75a7L, -0xbf1ca7b03cc1a21L, 0x106aa07032bbd1b8L,
        0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L, 0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L,
        0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L,
        0x748f82ee5defb2fcL, 0x78a5636f43172f60L, -0x79fb204e6e22c1b2L, -0x773e6cf0e32f5d7cL,
        -0x6f5d4e43cf26e70fL, -0x5bfe944208a0d4c6L, -0x41f5e08b1a8d56b6L, -0x3f58c49dc75ba75fL,
        -0x351fc810d7a0c005L, -0x22c00c7047f63ddL, 0x114f8369e9613b29L, 0x14511436d80b63e4L,
        0x2a85e68260193448L, 0x2bf894fe72be5d74L, 0x469e6358e06189f8L, 0x52cfa0c1a4e44537L,
        0x560f74e32a305214L, 0x631c4c2eb7272650L, 0x67cb79c6add07ad2L, 0x749a8649bebe0e05L,
        -0x6679fb143922d4f2L, -0x60f4a3501a357f49L, -0x50c95ec379d2dd64L, -0x47cb563be244ea14L,
        -0x383537d04ea7e661L, -0x322a5fe43818e388L, -0x243a8561fbfe7b3aL, -0x1aa123602f37c688L,
        -0x164ecaa765d779f6L, -0x0b8a78a32dfc2823L, 0x0f6d416f39260e42L, 0x206705a2fe4369e5L
    )

    fun digest(message: ByteArray): ByteArray {
        var h0 = 0x6a09e667f3bcc908L
        var h1 = -0x4498517ab3109aa5L
        var h2 = 0x3c6ef372fe94f82bL
        var h3 = -0x5ab00ac564f63903L
        var h4 = 0x510e527fade682d1L
        var h5 = -0x64fa9773d4c193e1L
        var h6 = 0x1f83d9abfb41bd6bL
        var h7 = 0x5be0cd19137e2179L

        val len = message.size.toLong()
        val bitLen = len * 8L

        val padLen = ((128 - ((len + 17) % 128)) % 128).toInt()
        val padded = ByteArray(len.toInt() + 1 + padLen + 16)
        message.copyInto(padded, 0, 0, message.size)
        padded[len.toInt()] = 0x80.toByte()

        for (i in 0 until 8) {
            padded[padded.size - 8 + i] = ((bitLen ushr ((7 - i) * 8)) and 0xFF).toByte()
        }

        val w = LongArray(80)
        var offset = 0
        while (offset < padded.size) {
            for (i in 0 until 16) {
                val idx = offset + i * 8
                w[i] = ((padded[idx].toLong() and 0xFFL) shl 56) or
                        ((padded[idx + 1].toLong() and 0xFFL) shl 48) or
                        ((padded[idx + 2].toLong() and 0xFFL) shl 40) or
                        ((padded[idx + 3].toLong() and 0xFFL) shl 32) or
                        ((padded[idx + 4].toLong() and 0xFFL) shl 24) or
                        ((padded[idx + 5].toLong() and 0xFFL) shl 16) or
                        ((padded[idx + 6].toLong() and 0xFFL) shl 8) or
                        (padded[idx + 7].toLong() and 0xFFL)
            }
            for (i in 16 until 80) {
                val s0 = (w[i - 15].rotateRight(1)) xor (w[i - 15].rotateRight(8)) xor (w[i - 15] ushr 7)
                val s1 = (w[i - 2].rotateRight(19)) xor (w[i - 2].rotateRight(61)) xor (w[i - 2] ushr 6)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            var f = h5
            var g = h6
            var h = h7

            for (i in 0 until 80) {
                val s1 = (e.rotateRight(14)) xor (e.rotateRight(18)) xor (e.rotateRight(41))
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + K[i] + w[i]
                val s0 = (a.rotateRight(28)) xor (a.rotateRight(34)) xor (a.rotateRight(39))
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                h = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            h5 += f
            h6 += g
            h7 += h

            offset += 128
        }

        val result = ByteArray(64)
        val hVals = longArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
        for (i in 0 until 8) {
            val v = hVals[i]
            for (j in 0 until 8) {
                result[i * 8 + j] = ((v ushr ((7 - j) * 8)) and 0xFF).toByte()
            }
        }
        return result
    }
}
