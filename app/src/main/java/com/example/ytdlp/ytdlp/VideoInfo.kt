package com.example.ytdlp.ytdlp

data class VideoInfo(
    val id: String,
    val title: String,
    val uploader: String?,
    val duration: Long?,
    val thumbnail: String?,
    val webpageUrl: String?,
    val formats: List<VideoFormat>
)

data class VideoFormat(
    val formatId: String,
    val extension: String?,
    val width: Int?,
    val height: Int?,
    val fps: Double?,
    val videoCodec: String?,
    val audioCodec: String?,
    val videoExtension: String? = null,
    val audioExtension: String? = null,
    val fileSize: Long?,
    val bitrate: Double?,
    val videoBitrate: Double? = null,
    val audioBitrate: Double? = null,
    val formatNote: String? = null,
    val dynamicRange: String? = null,
    val sourcePreference: Double? = null,
    val quality: Double? = null
) {

    val hasVideo: Boolean
        get() =
            videoCodec != null &&
                    !videoCodec.equals(
                        "none",
                        ignoreCase = true
                    )

    val hasAudio: Boolean
        get() =
            audioCodec != null &&
                    !audioCodec.equals(
                        "none",
                        ignoreCase = true
                    )

    val isVideoOnly: Boolean
        get() =
            hasVideo && !hasAudio

    val isAudioOnly: Boolean
        get() =
            !hasVideo && hasAudio

    val isCombined: Boolean
        get() =
            hasVideo && hasAudio

    val isMp4: Boolean
        get() =
            extension.equals(
                "mp4",
                ignoreCase = true
            )

    val isWebM: Boolean
        get() =
            extension.equals(
                "webm",
                ignoreCase = true
            )

    /*
     * بعض extractors ترجع ملف فيديو مباشرًا ولكن لا
     * تعطي vcodec/acodec أو الأبعاد.
     *
     * لا نغيّر hasVideo لأن معناه يجب أن يبقى مبنيًا
     * على codec المعروفة. هذا property مستقل للحالة
     * المباشرة ذات metadata الناقصة.
     */
    val isDirectUnknownVideo: Boolean
        get() {

            if (hasVideo || hasAudio) {
                return false
            }

            val directVideoExtension =
                videoExtension
                    ?.lowercase()

            val directAudioExtension =
                audioExtension
                    ?.lowercase()

            val container =
                extension
                    ?.lowercase()

            val supportedVideoContainer =
                directVideoExtension in
                        setOf(
                            "mp4",
                            "webm",
                            "mkv",
                            "mov"
                        ) ||
                        container in
                        setOf(
                            "mp4",
                            "webm",
                            "mkv",
                            "mov"
                        )

            val explicitlyNoSeparateAudio =
                directAudioExtension == null ||
                        directAudioExtension == "none"

            return supportedVideoContainer &&
                    directVideoExtension != "none" &&
                    explicitlyNoSeparateAudio
        }

    val isAvc: Boolean
        get() {

            val codec =
                videoCodec
                    ?.lowercase()
                    ?: return false

            return codec.startsWith("avc1") ||
                    codec.startsWith("h264")
        }

    val isVp9: Boolean
        get() {

            val codec =
                videoCodec
                    ?.lowercase()
                    ?: return false

            return codec.startsWith("vp9") ||
                    codec.startsWith("vp09")
        }

    val isAv1: Boolean
        get() {

            val codec =
                videoCodec
                    ?.lowercase()
                    ?: return false

            return codec.startsWith("av01") ||
                    codec.startsWith("av1")
        }

    val effectiveVideoBitrate: Double?
        get() =
            videoBitrate ?: bitrate

    val effectiveAudioBitrate: Double?
        get() =
            audioBitrate ?: bitrate

    val hasKnownSize: Boolean
        get() =
            fileSize != null &&
                    fileSize > 0

    val resolution: String
        get() {

            return when {

                width != null &&
                        height != null ->
                    "${width}x${height}"

                height != null ->
                    "${height}p"

                width != null ->
                    "${width}p"

                else ->
                    "Unknown"
            }
        }
}

/*
 * =====================================================
 * VIDEO DOWNLOAD OPTION
 * =====================================================
 */

data class DownloadOption(
    val height: Int?,
    val label: String,
    val videoFormat: VideoFormat,
    val audioFormat: VideoFormat?,
    val estimatedSize: Long?
) {

    val requiresMerge: Boolean
        get() =
            audioFormat != null

    val formatSelector: String
        get() {

            return if (audioFormat != null) {
                "${videoFormat.formatId}+${audioFormat.formatId}"
            } else {
                videoFormat.formatId
            }
        }

    val extension: String
        get() =
            videoFormat.extension
                ?: videoFormat.videoExtension
                ?: "mp4"

    val fps: Double?
        get() =
            videoFormat.fps
}

/*
 * =====================================================
 * AUDIO DOWNLOAD OPTION
 * =====================================================
 */

data class AudioDownloadOption(
    val label: String,
    val audioFormat: VideoFormat,
    val estimatedSize: Long?
) {

    val formatSelector: String
        get() =
            audioFormat.formatId

    val extension: String
        get() =
            audioFormat.extension
                ?: "m4a"

    val bitrate: Double?
        get() =
            audioFormat.effectiveAudioBitrate
}

/*
 * =====================================================
 * DOWNLOAD PROGRESS
 * =====================================================
 */

enum class DownloadStage {
    PREPARING,
    DOWNLOADING,
    MERGING,
    SAVING,
    FINISHED
}

data class DownloadProgress(
    val stage: DownloadStage,
    val percent: Float,
    val etaSeconds: Long? = null,
    val speed: String? = null,
    val rawLine: String? = null
) {

    val safePercent: Float
        get() =
            percent.coerceIn(
                0f,
                100f
            )
}