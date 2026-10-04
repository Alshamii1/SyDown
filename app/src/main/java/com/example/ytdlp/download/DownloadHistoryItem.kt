package com.example.ytdlp.download

import org.json.JSONObject

data class DownloadHistoryItem(
    val id: String,
    val title: String,
    val uploader: String?,
    val sourceUrl: String,
    val type: String,
    val qualityLabel: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val contentUri: String?,
    val filePath: String?,
    val downloadedAt: Long,
    val thumbnailPath: String? = null
) {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put(
                "id",
                id
            )

            put(
                "title",
                title
            )

            put(
                "uploader",
                uploader ?: JSONObject.NULL
            )

            put(
                "sourceUrl",
                sourceUrl
            )

            put(
                "type",
                type
            )

            put(
                "qualityLabel",
                qualityLabel
            )

            put(
                "fileName",
                fileName
            )

            put(
                "mimeType",
                mimeType
            )

            put(
                "sizeBytes",
                sizeBytes
            )

            put(
                "contentUri",
                contentUri ?: JSONObject.NULL
            )

            put(
                "filePath",
                filePath ?: JSONObject.NULL
            )

            put(
                "downloadedAt",
                downloadedAt
            )

            put(
                "thumbnailPath",
                thumbnailPath ?: JSONObject.NULL
            )
        }
    }

    companion object {

        fun fromJson(
            json: JSONObject
        ): DownloadHistoryItem? {

            return try {

                val id =
                    json.optString(
                        "id",
                        ""
                    )

                val title =
                    json.optString(
                        "title",
                        ""
                    )

                val sourceUrl =
                    json.optString(
                        "sourceUrl",
                        ""
                    )

                val type =
                    json.optString(
                        "type",
                        ""
                    )

                val qualityLabel =
                    json.optString(
                        "qualityLabel",
                        ""
                    )

                val fileName =
                    json.optString(
                        "fileName",
                        ""
                    )

                val mimeType =
                    json.optString(
                        "mimeType",
                        "application/octet-stream"
                    )

                if (
                    id.isBlank() ||
                    fileName.isBlank()
                ) {
                    return null
                }

                DownloadHistoryItem(
                    id =
                        id,

                    title =
                        title.ifBlank {
                            fileName
                        },

                    uploader =
                        json.optNullableString(
                            "uploader"
                        ),

                    sourceUrl =
                        sourceUrl,

                    type =
                        type,

                    qualityLabel =
                        qualityLabel,

                    fileName =
                        fileName,

                    mimeType =
                        mimeType,

                    sizeBytes =
                        json.optLong(
                            "sizeBytes",
                            0L
                        ),

                    contentUri =
                        json.optNullableString(
                            "contentUri"
                        ),

                    filePath =
                        json.optNullableString(
                            "filePath"
                        ),

                    downloadedAt =
                        json.optLong(
                            "downloadedAt",
                            0L
                        ),

                    thumbnailPath =
                        json.optNullableString(
                            "thumbnailPath"
                        )
                )

            } catch (
                _: Throwable
            ) {

                null
            }
        }

        private fun JSONObject
                .optNullableString(
            name: String
        ): String? {

            if (
                !has(name) ||
                isNull(name)
            ) {
                return null
            }

            return optString(
                name,
                ""
            ).takeIf {
                it.isNotBlank()
            }
        }
    }
}