package com.example.ytdlp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class ApkSha256VerifierTest {

    @Test
    fun knownSha256Vector() {
        withTemporaryFile("abc") { file ->
            val expected =
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

            assertArrayEquals(
                hexToBytes(expected),
                ApkSha256Verifier.calculate(file)
            )

            ApkSha256Verifier.verify(file, expected)
        }
    }

    @Test
    fun bouncyCastleMatchesJavaSha256() {
        withTemporaryFile("SyDown SHA-256 verification test") { file ->
            val bcHash = toHex(ApkSha256Verifier.calculate(file))

            val javaHash = toHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(file.readBytes())
            )

            assertEquals(javaHash, bcHash)
            ApkSha256Verifier.verify(file, javaHash)
        }
    }

    @Test
    fun incorrectHashIsRejected() {
        withTemporaryFile("abc") { file ->
            assertThrows(IllegalArgumentException::class.java) {
                ApkSha256Verifier.verify(
                    file,
                    "0000000000000000000000000000000000000000000000000000000000000000"
                )
            }
        }
    }

    private fun withTemporaryFile(
        content: String,
        action: (File) -> Unit
    ) {
        val file = File.createTempFile("sydown-sha-test", ".bin")

        try {
            file.writeText(content, Charsets.UTF_8)
            action(file)
        } finally {
            file.delete()
        }
    }

    private fun hexToBytes(value: String): ByteArray =
        ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }

    private fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 255)
        }
}