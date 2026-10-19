package com.example.ytdlp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.coroutineContext

data class AppUpdateDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val percent: Int?
        get() {
            if (totalBytes <= 0L) {
                return null
            }

            return (
                    (downloadedBytes * 100L) / totalBytes
                    ).coerceIn(0L, 100L).toInt()
        }
}

object AppUpdateInstaller {

    private const val UPDATE_DIRECTORY =
        "app_updates"

    private const val UPDATE_FILE_NAME =
        "SyDown-update.apk"

    private const val MAX_REDIRECTS = 5

    private const val MAX_APK_BYTES =
        1_073_741_824L

    private const val APK_MIME_TYPE =
        "application/vnd.android.package-archive"

    private val SHA256_PATTERN =
        Regex("^[a-fA-F0-9]{64}$")

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        expectedSha256: String,
        onProgress: (
            AppUpdateDownloadProgress
        ) -> Unit
    ): File =
        withContext(Dispatchers.IO) {
            val appContext =
                context.applicationContext

            val normalizedExpectedSha256 =
                normalizeSha256(expectedSha256)

            val updateDirectory =
                File(
                    appContext.filesDir,
                    UPDATE_DIRECTORY
                )

            require(
                updateDirectory.isDirectory ||
                        updateDirectory.mkdirs()
            ) {
                "Could not create update directory"
            }

            val finalFile =
                File(
                    updateDirectory,
                    UPDATE_FILE_NAME
                )

            val temporaryFile =
                File(
                    updateDirectory,
                    "$UPDATE_FILE_NAME.part"
                )

            if (temporaryFile.exists()) {
                require(
                    temporaryFile.delete()
                ) {
                    "Could not remove previous temporary APK"
                }
            }

            // Do not retain an older APK as a possible update.
            if (finalFile.exists()) {
                require(
                    finalFile.delete()
                ) {
                    "Could not remove previous APK"
                }
            }

            var connection: HttpURLConnection? = null

            try {
                var currentUrl =
                    requireHttpsUrl(apkUrl)

                var redirectCount = 0

                while (true) {
                    coroutineContext.ensureActive()

                    val activeConnection =
                        currentUrl.openConnection()
                                as HttpURLConnection

                    connection = activeConnection

                    activeConnection.requestMethod = "GET"
                    activeConnection.connectTimeout = 15_000
                    activeConnection.readTimeout = 30_000
                    activeConnection.instanceFollowRedirects = false
                    activeConnection.useCaches = false

                    activeConnection.setRequestProperty(
                        "Accept",
                        "$APK_MIME_TYPE,application/octet-stream,*/*"
                    )

                    val responseCode =
                        activeConnection.responseCode

                    if (
                        responseCode ==
                        HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode ==
                        HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode ==
                        HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 ||
                        responseCode == 308
                    ) {
                        require(
                            redirectCount < MAX_REDIRECTS
                        ) {
                            "Too many APK download redirects"
                        }

                        val location =
                            activeConnection.getHeaderField(
                                "Location"
                            )

                        require(
                            !location.isNullOrBlank()
                        ) {
                            "APK redirect is missing its destination"
                        }

                        val redirectedUrl =
                            URL(
                                currentUrl,
                                location
                            )

                        currentUrl =
                            requireHttpsUrl(
                                redirectedUrl.toExternalForm()
                            )

                        redirectCount++

                        activeConnection.disconnect()
                        connection = null

                        continue
                    }

                    require(
                        responseCode ==
                                HttpURLConnection.HTTP_OK
                    ) {
                        "APK download failed: HTTP $responseCode"
                    }

                    val totalBytes =
                        activeConnection.contentLengthLong

                    require(
                        totalBytes <= MAX_APK_BYTES
                    ) {
                        "APK exceeds maximum allowed size"
                    }

                    val messageDigest =
                        MessageDigest.getInstance(
                            "SHA-256"
                        )

                    var downloadedBytes = 0L

                    activeConnection.inputStream.use { input ->
                        temporaryFile
                            .outputStream()
                            .buffered()
                            .use { output ->
                                val buffer =
                                    ByteArray(
                                        DEFAULT_BUFFER_SIZE
                                    )

                                while (true) {
                                    coroutineContext.ensureActive()

                                    val read =
                                        input.read(buffer)

                                    if (read < 0) {
                                        break
                                    }

                                    if (read == 0) {
                                        continue
                                    }

                                    downloadedBytes +=
                                        read.toLong()

                                    require(
                                        downloadedBytes <=
                                                MAX_APK_BYTES
                                    ) {
                                        "APK exceeds maximum allowed size"
                                    }

                                    if (totalBytes >= 0L) {
                                        require(
                                            downloadedBytes <=
                                                    totalBytes
                                        ) {
                                            "APK exceeds declared download size"
                                        }
                                    }

                                    output.write(
                                        buffer,
                                        0,
                                        read
                                    )

                                    messageDigest.update(
                                        buffer,
                                        0,
                                        read
                                    )

                                    onProgress(
                                        AppUpdateDownloadProgress(
                                            downloadedBytes =
                                                downloadedBytes,
                                            totalBytes =
                                                totalBytes
                                        )
                                    )
                                }

                                output.flush()
                            }
                    }

                    require(
                        downloadedBytes > 0L
                    ) {
                        "Downloaded APK is empty"
                    }

                    if (totalBytes >= 0L) {
                        require(
                            downloadedBytes == totalBytes
                        ) {
                            "APK download is incomplete"
                        }
                    }

                    coroutineContext.ensureActive()

                    verifyFileSha256(
                        temporaryFile,
                        normalizedExpectedSha256
                    )
                    validateApk(
                        appContext,
                        temporaryFile
                    )

                    coroutineContext.ensureActive()

                    require(
                        temporaryFile.renameTo(
                            finalFile
                        )
                    ) {
                        "Could not prepare downloaded APK"
                    }

                    return@withContext finalFile
                }

                @Suppress("UNREACHABLE_CODE")
                error("Unexpected APK download state")
            } catch (throwable: Throwable) {
                temporaryFile.delete()
                throw throwable
            } finally {
                connection?.disconnect()
            }
        }

    fun canRequestPackageInstalls(
        context: Context
    ): Boolean {
        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            context.packageManager
                .canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownAppsSettings(
        context: Context
    ) {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val intent =
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse(
                    "package:${context.packageName}"
                )
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        context.startActivity(intent)
    }

    fun installApk(
        context: Context,
        apkFile: File,
        expectedSha256: String
    ) {
        val appContext =
            context.applicationContext

        val expectedDirectory =
            File(
                appContext.filesDir,
                UPDATE_DIRECTORY
            ).canonicalFile

        val expectedFile =
            File(
                expectedDirectory,
                UPDATE_FILE_NAME
            ).canonicalFile

        require(
            apkFile.canonicalFile == expectedFile
        ) {
            "Invalid APK file location"
        }

        // Verify again immediately before handing the APK
        // to Android's package installer.
        verifyFileSha256(
            expectedFile,
            expectedSha256
        )

        validateApk(
            appContext,
            expectedFile
        )

        require(
            canRequestPackageInstalls(appContext)
        ) {
            "Permission to install updates is required"
        }

        val apkUri =
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                expectedFile
            )

        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {
                setDataAndType(
                    apkUri,
                    APK_MIME_TYPE
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                clipData =
                    android.content.ClipData.newUri(
                        appContext.contentResolver,
                        "SyDown update",
                        apkUri
                    )
            }

        context.startActivity(intent)
    }

    private fun normalizeSha256(
        value: String
    ): String {
        val normalized =
            value.trim()
                .lowercase(Locale.ROOT)

        require(
            SHA256_PATTERN.matches(normalized)
        ) {
            "Invalid expected APK SHA-256"
        }

        return normalized
    }

    private fun digestToHex(
        digest: ByteArray
    ): String {
        val hexCharacters =
            "0123456789abcdef"

        return buildString(
            digest.size * 2
        ) {
            for (byte in digest) {
                val unsigned =
                    byte.toInt() and 0xff

                append(
                    hexCharacters[
                        unsigned ushr 4
                    ]
                )

                append(
                    hexCharacters[
                        unsigned and 0x0f
                    ]
                )
            }
        }
    }

    private fun verifyFileSha256(
        file: File,
        expectedSha256: String
    ) {
        val normalizedExpectedSha256 = normalizeSha256(expectedSha256)

        require(
            file.isFile &&
                file.length() > 0L &&
                file.length() <= MAX_APK_BYTES
        ) {
            "APK file is missing or invalid"
        }

        // Independent software SHA-256 implementation from Bouncy Castle.
        val digest = org.bouncycastle.crypto.digests.SHA256Digest()

        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)

            while (true) {
                val count = input.read(buffer)

                if (count < 0) break

                if (count > 0) {
                    digest.update(buffer, 0, count)
                }
            }
        }

        val actualDigest = ByteArray(digest.digestSize)
        digest.doFinal(actualDigest, 0)

        val expectedDigest = ByteArray(32) { index ->
            val position = index * 2
            val high = Character.digit(normalizedExpectedSha256[position], 16)
            val low = Character.digit(normalizedExpectedSha256[position + 1], 16)

            require(high >= 0 && low >= 0) {
                "Invalid expected APK SHA-256"
            }

            ((high shl 4) or low).toByte()
        }

        require(MessageDigest.isEqual(actualDigest, expectedDigest)) {
            "APK SHA-256 verification failed"
        }
    }

    private fun requireHttpsUrl(
        value: String
    ): URL {
        val url =
            URL(value)

        require(
            url.protocol.equals(
                "https",
                ignoreCase = true
            ) &&
                    url.host.isNotBlank() &&
                    url.userInfo == null
        ) {
            "APK URL must use HTTPS"
        }

        return url
    }

    private fun validateApk(
        context: Context,
        apkFile: File
    ) {
        require(
            apkFile.isFile &&
                    apkFile.length() > 0L &&
                    apkFile.length() <= MAX_APK_BYTES
        ) {
            "APK file is missing or invalid"
        }

        val packageManager =
            context.packageManager

        val archiveInfo =
            getArchivePackageInfo(
                packageManager,
                apkFile
            ) ?: throw IllegalStateException(
                "Downloaded file is not a valid APK"
            )

        require(
            archiveInfo.packageName ==
                    context.packageName
        ) {
            "APK belongs to a different application"
        }

        val installedInfo =
            getInstalledPackageInfo(
                packageManager,
                context.packageName
            )

        require(
            getVersionCode(archiveInfo) >
                    getVersionCode(installedInfo)
        ) {
            "APK version must be newer than installed version"
        }

        val archiveSignatures =
            getSignatureDigests(
                archiveInfo
            )

        val installedSignatures =
            getSignatureDigests(
                installedInfo
            )



require(
            archiveSignatures.isNotEmpty() &&
                    installedSignatures.isNotEmpty() &&
                    archiveSignatures ==
                    installedSignatures
        ) {
            "APK signing certificate does not match installed application"
        }
    }

    private fun getArchivePackageInfo(
        packageManager: PackageManager,
        apkFile: File
    ): PackageInfo? {
        return if (
            Build.VERSION.SDK_INT >= 33
        ) {
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(
                    PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                signatureFlags()
            )
        }
    }

    private fun getInstalledPackageInfo(
        packageManager: PackageManager,
        packageName: String
    ): PackageInfo {
        return if (
            Build.VERSION.SDK_INT >= 33
        ) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(
                    PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(
                packageName,
                signatureFlags()
            )
        }
    }

    private fun signatureFlags(): Int {
        return if (
            Build.VERSION.SDK_INT >= 28
        ) {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNING_CERTIFICATES or
                PackageManager.GET_SIGNATURES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
    }

    private fun getVersionCode(
        packageInfo: PackageInfo
    ): Long {
        return if (
            Build.VERSION.SDK_INT >= 28
        ) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private fun getSignatureDigests(
        packageInfo: PackageInfo
    ): Set<String> {
        val signatures =
            if (
                Build.VERSION.SDK_INT >= 28
            ) {
                val signingInfo =
                    packageInfo.signingInfo

                val modernSignatures =
                    signingInfo?.apkContentsSigners
                        ?: emptyArray()

                if (modernSignatures.isNotEmpty()) {
                    modernSignatures
                } else {
                    @Suppress("DEPRECATION")
                    val legacySignatures =
                        packageInfo.signatures

                    legacySignatures ?: emptyArray()
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
                    ?: emptyArray()
            }

        return signatures.map { signature ->
            val digest =
                MessageDigest.getInstance(
                    "SHA-256"
                ).digest(
                    signature.toByteArray()
                )

            digestToHex(digest)
        }.toSet()
    }
}