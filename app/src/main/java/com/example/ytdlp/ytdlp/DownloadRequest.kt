package com.example.ytdlp.ytdlp

enum class DownloadType {
    VIDEO,
    AUDIO
}

data class DownloadRequest(
    val url: String,
    val type: DownloadType,
    val height: Int?,
    val label: String,
    val formatSelector: String,
    val requiresMerge: Boolean,
    val expectedExtension: String?,
    val mediaTitle: String?,
    val mediaId: String?,
    val uploader: String?,
    val thumbnailUrl: String? = null
) {
    companion object {

        fun from(
            url: String,
            option: DownloadOption,
            videoInfo: VideoInfo? = null
        ): DownloadRequest {

            return DownloadRequest(
                url = url,
                type = DownloadType.VIDEO,
                height = option.height,
                label = option.label,
                formatSelector = option.formatSelector,
                requiresMerge = option.requiresMerge,
                expectedExtension = option.extension,
                mediaTitle = videoInfo?.title,
                mediaId = videoInfo?.id,
                uploader = videoInfo?.uploader,
                thumbnailUrl = videoInfo?.thumbnail
            )
        }

        fun from(
            url: String,
            option: AudioDownloadOption,
            videoInfo: VideoInfo? = null
        ): DownloadRequest {

            return DownloadRequest(
                url = url,
                type = DownloadType.AUDIO,
                height = null,
                label = option.label,
                formatSelector = option.formatSelector,
                requiresMerge = false,
                expectedExtension = option.extension,
                mediaTitle = videoInfo?.title,
                mediaId = videoInfo?.id,
                uploader = videoInfo?.uploader,
                thumbnailUrl = videoInfo?.thumbnail
            )
        }
    }
}