package com.example.ytdlp.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object DownloadThumbnailLoader {

    private const val CACHE_DIRECTORY_NAME =
        "download_thumbnails"

    private const val CACHE_VERSION =
        "v2"

    suspend fun load(
        context: Context,
        item: DownloadHistoryItem
    ): Bitmap? {
        if (
            item.type.equals(
                "AUDIO",
                ignoreCase = true
            )
        ) {
            return null
        }

        return withContext(Dispatchers.IO) {

            val appContext =
                context.applicationContext

            try {

                /*
                 * الخيار الأول:
                 *
                 * الصورة الأصلية التي حفظها SyDown محليًا
                 * عند اكتمال التنزيل.
                 *
                 * هذه الصورة داخل filesDir وليست cache،
                 * لذلك لا تعتمد على الإنترنت ولا على بقاء
                 * رابط المصدر متاحًا.
                 */
                loadPersistentThumbnail(
                    item = item
                )
                    ?: loadGeneratedThumbnail(
                        context = appContext,
                        item = item
                    )

            } catch (_: Throwable) {

                null
            }
        }
    }

    /*
     * =====================================================
     * PERSISTENT PLATFORM THUMBNAIL
     * =====================================================
     */

    private fun loadPersistentThumbnail(
        item: DownloadHistoryItem
    ): Bitmap? {

        val thumbnailPath =
            item.thumbnailPath
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null

        val thumbnailFile =
            File(
                thumbnailPath
            )

        if (
            !thumbnailFile.exists() ||
            !thumbnailFile.isFile ||
            thumbnailFile.length() <= 0L
        ) {
            return null
        }

        return try {

            BitmapFactory.decodeFile(
                thumbnailFile.absolutePath
            )

        } catch (_: Throwable) {

            null
        }
    }

    /*
     * =====================================================
     * GENERATED LOCAL VIDEO THUMBNAIL
     * =====================================================
     */

    private fun loadGeneratedThumbnail(
        context: Context,
        item: DownloadHistoryItem
    ): Bitmap? {

        val cacheFile =
            getCacheFile(
                context = context,
                item = item
            )

        return loadCachedBitmap(
            cacheFile
        )
            ?: extractAndCacheThumbnail(
                context = context,
                item = item,
                cacheFile = cacheFile
            )
    }

    private fun extractAndCacheThumbnail(
        context: Context,
        item: DownloadHistoryItem,
        cacheFile: File
    ): Bitmap? {

        val retriever =
            MediaMetadataRetriever()

        return try {

            if (
                !setDataSource(
                    retriever = retriever,
                    context = context,
                    item = item
                )
            ) {
                return null
            }

            val bitmap =
                extractFrame(
                    retriever = retriever
                )
                    ?: return null

            val thumbnail =
                scaleDown(
                    bitmap = bitmap,
                    maxWidth = 480,
                    maxHeight = 480
                )

            saveToCache(
                bitmap = thumbnail,
                cacheFile = cacheFile
            )

            thumbnail

        } catch (_: Throwable) {

            null

        } finally {

            try {

                retriever.release()

            } catch (_: Throwable) {

                // لا شيء
            }
        }
    }

    private fun setDataSource(
        retriever: MediaMetadataRetriever,
        context: Context,
        item: DownloadHistoryItem
    ): Boolean {

        val contentUri =
            item.contentUri
                ?.takeIf {
                    it.isNotBlank()
                }

        if (contentUri != null) {

            try {

                retriever.setDataSource(
                    context,
                    Uri.parse(contentUri)
                )

                return true

            } catch (_: Throwable) {

                // نجرب المسار المحلي بعده.
            }
        }

        val filePath =
            item.filePath
                ?.takeIf {
                    it.isNotBlank()
                }

        if (filePath != null) {

            val file =
                File(
                    filePath
                )

            if (
                file.exists() &&
                file.isFile &&
                file.length() > 0L
            ) {

                try {

                    retriever.setDataSource(
                        file.absolutePath
                    )

                    return true

                } catch (_: Throwable) {

                    // لا شيء
                }
            }
        }

        return false
    }

    private fun extractFrame(
        retriever: MediaMetadataRetriever
    ): Bitmap? {

        /*
         * بعض الحاويات تحتوي أصلًا على صورة مضمّنة.
         * إن وُجدت فهي أرخص من محاولة فك الفيديو.
         */
        val embeddedPicture =
            try {

                retriever.embeddedPicture

            } catch (_: Throwable) {

                null
            }

        if (
            embeddedPicture != null &&
            embeddedPicture.isNotEmpty()
        ) {

            val bitmap =
                try {

                    BitmapFactory.decodeByteArray(
                        embeddedPicture,
                        0,
                        embeddedPicture.size
                    )

                } catch (_: Throwable) {

                    null
                }

            if (bitmap != null) {

                return bitmap
            }
        }

        val durationUs =
            readDurationUs(
                retriever = retriever
            )

        val timestamps =
            buildCandidateTimestamps(
                durationUs = durationUs
            )

        /*
         * OPTION_CLOSEST قد يستطيع فك frame حتى عندما
         * لا توجد keyframe مناسبة عند الزمن المطلوب.
         */
        for (timestamp in timestamps) {

            val frame =
                tryGetFrame(
                    retriever = retriever,
                    timestampUs = timestamp,
                    option =
                        MediaMetadataRetriever
                            .OPTION_CLOSEST
                )

            if (frame != null) {

                return frame
            }
        }

        /*
         * إذا لم ينجح، نجرب keyframes فقط.
         */
        for (timestamp in timestamps) {

            val frame =
                tryGetFrame(
                    retriever = retriever,
                    timestampUs = timestamp,
                    option =
                        MediaMetadataRetriever
                            .OPTION_CLOSEST_SYNC
                )

            if (frame != null) {

                return frame
            }
        }

        /*
         * بعض الأجهزة تنجح أكثر مع keyframe السابقة.
         */
        for (timestamp in timestamps) {

            val frame =
                tryGetFrame(
                    retriever = retriever,
                    timestampUs = timestamp,
                    option =
                        MediaMetadataRetriever
                            .OPTION_PREVIOUS_SYNC
                )

            if (frame != null) {

                return frame
            }
        }

        /*
         * وآخر محاولة مع keyframe التالية.
         */
        for (timestamp in timestamps) {

            val frame =
                tryGetFrame(
                    retriever = retriever,
                    timestampUs = timestamp,
                    option =
                        MediaMetadataRetriever
                            .OPTION_NEXT_SYNC
                )

            if (frame != null) {

                return frame
            }
        }

        /*
         * السماح للـ retriever باختيار frame بنفسه.
         */
        return try {

            retriever.getFrameAtTime()

        } catch (_: Throwable) {

            null
        }
    }

    private fun readDurationUs(
        retriever: MediaMetadataRetriever
    ): Long? {

        val durationMs =
            try {

                retriever
                    .extractMetadata(
                        MediaMetadataRetriever
                            .METADATA_KEY_DURATION
                    )
                    ?.toLongOrNull()

            } catch (_: Throwable) {

                null
            }

        if (
            durationMs == null ||
            durationMs <= 0L
        ) {
            return null
        }

        return try {

            Math.multiplyExact(
                durationMs,
                1000L
            )

        } catch (_: ArithmeticException) {

            null
        }
    }

    private fun buildCandidateTimestamps(
        durationUs: Long?
    ): List<Long> {

        val timestamps =
            linkedSetOf<Long>()

        /*
         * نقاط مبكرة مناسبة لمعظم الفيديوهات القصيرة.
         */
        timestamps.add(
            1_000_000L
        )

        timestamps.add(
            500_000L
        )

        timestamps.add(
            2_000_000L
        )

        timestamps.add(
            0L
        )

        if (
            durationUs != null &&
            durationUs > 0L
        ) {

            timestamps.add(
                durationUs / 10L
            )

            timestamps.add(
                durationUs / 4L
            )

            timestamps.add(
                durationUs / 2L
            )

            /*
             * لا نطلب زمنًا بعد نهاية الفيديو.
             */
            return timestamps
                .map {
                    it.coerceIn(
                        0L,
                        (durationUs - 1L)
                            .coerceAtLeast(0L)
                    )
                }
                .distinct()
        }

        return timestamps.toList()
    }

    private fun tryGetFrame(
        retriever: MediaMetadataRetriever,
        timestampUs: Long,
        option: Int
    ): Bitmap? {

        return try {

            retriever.getFrameAtTime(
                timestampUs,
                option
            )

        } catch (_: Throwable) {

            null
        }
    }

    private fun scaleDown(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {

        if (
            bitmap.width <= maxWidth &&
            bitmap.height <= maxHeight
        ) {
            return bitmap
        }

        val widthRatio =
            maxWidth.toFloat() /
                    bitmap.width.toFloat()

        val heightRatio =
            maxHeight.toFloat() /
                    bitmap.height.toFloat()

        val ratio =
            minOf(
                widthRatio,
                heightRatio
            )

        val targetWidth =
            (bitmap.width * ratio)
                .toInt()
                .coerceAtLeast(1)

        val targetHeight =
            (bitmap.height * ratio)
                .toInt()
                .coerceAtLeast(1)

        val scaled =
            Bitmap.createScaledBitmap(
                bitmap,
                targetWidth,
                targetHeight,
                true
            )

        if (scaled !== bitmap) {

            try {

                bitmap.recycle()

            } catch (_: Throwable) {

                // لا شيء
            }
        }

        return scaled
    }

    private fun saveToCache(
        bitmap: Bitmap,
        cacheFile: File
    ) {

        try {

            cacheFile.parentFile
                ?.mkdirs()

            FileOutputStream(
                cacheFile
            ).use { output ->

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    82,
                    output
                )
            }

        } catch (_: Throwable) {

            try {

                cacheFile.delete()

            } catch (_: Throwable) {

                // لا شيء
            }
        }
    }

    private fun loadCachedBitmap(
        cacheFile: File
    ): Bitmap? {

        if (
            !cacheFile.exists() ||
            !cacheFile.isFile ||
            cacheFile.length() <= 0L
        ) {
            return null
        }

        return try {

            BitmapFactory.decodeFile(
                cacheFile.absolutePath
            )

        } catch (_: Throwable) {

            null
        }
    }

    private fun getCacheFile(
        context: Context,
        item: DownloadHistoryItem
    ): File {

        val cacheDirectory =
            File(
                context.cacheDir,
                CACHE_DIRECTORY_NAME
            )

        val identity =
            buildString {

                append(CACHE_VERSION)
                append('|')
                append(item.id)
                append('|')
                append(item.contentUri.orEmpty())
                append('|')
                append(item.filePath.orEmpty())
                append('|')
                append(item.sizeBytes)
            }

        val fileName =
            sha256(
                identity
            ) + ".jpg"

        return File(
            cacheDirectory,
            fileName
        )
    }

    private fun sha256(
        value: String
    ): String {

        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256"
                )
                .digest(
                    value.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return digest.joinToString(
            separator = ""
        ) {
            "%02x".format(it)
        }
    }
}