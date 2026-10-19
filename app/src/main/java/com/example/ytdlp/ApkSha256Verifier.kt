package com.example.ytdlp

import org.bouncycastle.crypto.digests.SHA256Digest
import java.io.File
import java.security.MessageDigest

/**
 * Independent SHA-256 verification for downloaded APK files.
 *
 * Uses Bouncy Castle's software digest implementation directly,
 * without Android's JCA security providers.
 */
internal object ApkSha256Verifier {

    private val SHA256_PATTERN = Regex("^[0-9a-fA-F]{64}$")

    fun calculate(file: File): ByteArray {
        require(file.isFile) {
            "APK file does not exist"
        }

        val digest = SHA256Digest()
        val buffer = ByteArray(8192)

        file.inputStream().use { input ->
            while (true) {
                val count = input.read(buffer)

                if (count == -1) break

                if (count > 0) {
                    digest.update(buffer, 0, count)
                }
            }
        }

        val result = ByteArray(32)
        digest.doFinal(result, 0)

        return result
    }

    fun verify(file: File, expectedSha256: String) {
        require(SHA256_PATTERN.matches(expectedSha256)) {
            "Invalid expected APK SHA-256"
        }

        val expected = ByteArray(32) { index ->
            val position = index * 2

            val value = expectedSha256.substring(
                position,
                position + 2
            )

            value.toInt(16).toByte()
        }

        val actual = calculate(file)

        require(MessageDigest.isEqual(actual, expected)) {
            "APK SHA-256 verification failed"
        }
    }
}