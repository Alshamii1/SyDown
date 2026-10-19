package com.example.ytdlp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotesAr: String,
    val releaseNotesEn: String,
    val apkSha256: String = ""
) {
    val hasDownloadUrl: Boolean
        get() = apkUrl.isNotBlank() &&
                apkSha256.isNotBlank()
}

object AppUpdateChecker {

    private const val UPDATE_MANIFEST_URL =
        "https://alshamii1.github.io/SyDown/update.json"

    private val SHA256_PATTERN =
        Regex("^[a-fA-F0-9]{64}$")

    suspend fun check(): AppUpdateInfo =
        withContext(Dispatchers.IO) {
            val connection =
                URL(UPDATE_MANIFEST_URL)
                    .openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.useCaches = false

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val responseCode =
                    connection.responseCode

                require(
                    responseCode == HttpURLConnection.HTTP_OK
                ) {
                    "Update check failed: HTTP $responseCode"
                }

                val jsonText =
                    connection.inputStream
                        .bufferedReader(
                            Charsets.UTF_8
                        )
                        .use {
                            it.readText()
                        }

                parseManifest(jsonText)
            } finally {
                connection.disconnect()
            }
        }

    fun isUpdateAvailable(
        info: AppUpdateInfo
    ): Boolean {
        return info.versionCode >
                BuildConfig.VERSION_CODE
    }

    private fun parseManifest(
        jsonText: String
    ): AppUpdateInfo {
        val json =
            JSONObject(jsonText)

        val versionCode =
            json.getInt("versionCode")

        val versionName =
            json.getString("versionName")
                .trim()

        val apkUrl =
            json.optString("apkUrl")
                .trim()

        val apkSha256 =
            json.optString("apkSha256")
                .trim()
                .lowercase(Locale.ROOT)

        val releaseNotesAr =
            json.optString("releaseNotesAr")
                .trim()

        val releaseNotesEn =
            json.optString("releaseNotesEn")
                .trim()

        require(versionCode > 0) {
            "Invalid versionCode"
        }

        require(versionName.isNotBlank()) {
            "Invalid versionName"
        }

        if (apkUrl.isNotBlank()) {
            val url = URL(apkUrl)

            require(
                url.protocol.equals(
                    "https",
                    ignoreCase = true
                ) &&
                        url.host.isNotBlank() &&
                        url.userInfo == null
            ) {
                "Invalid apkUrl"
            }

            require(
                SHA256_PATTERN.matches(apkSha256)
            ) {
                "Missing or invalid APK SHA-256"
            }
        } else {
            require(
                apkSha256.isBlank()
            ) {
                "APK SHA-256 must be empty when apkUrl is empty"
            }
        }

        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            releaseNotesAr = releaseNotesAr,
            releaseNotesEn = releaseNotesEn,
            apkSha256 = apkSha256
        )
    }
}