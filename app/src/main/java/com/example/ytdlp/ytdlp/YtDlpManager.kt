package com.example.ytdlp.ytdlp

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

object YtDlpManager {

    private var initialized = false

    @Volatile
    private var activeDownloadProcessId: String? = null

    suspend fun initialize(context: Context) {
        withContext(Dispatchers.IO) {
            if (!initialized) {
                val appContext =
                    context.applicationContext

                YoutubeDL
                    .getInstance()
                    .init(appContext)

                FFmpeg.init(appContext)

                initialized = true
            }
        }
    }

    /*
     * =====================================================
     * YT-DLP VERSION / UPDATE
     * =====================================================
     */

    suspend fun getYtDlpVersion(): String {
        return withContext(Dispatchers.IO) {
            check(initialized) {
                "yt-dlp has not been initialized"
            }

            val request =
                YoutubeDLRequest("")

            request.addOption(
                "--version"
            )

            YoutubeDL
                .getInstance()
                .execute(request)
                .out
                .trim()
        }
    }

    suspend fun updateYtDlp(
        context: Context
    ): String {

        return withContext(Dispatchers.IO) {
            check(initialized) {
                "yt-dlp has not been initialized"
            }

            val appContext =
                context.applicationContext

            val oldVersion =
                getYtDlpVersion()

            val updateStatus =
                YoutubeDL
                    .getInstance()
                    .updateYoutubeDL(
                        appContext,
                        YoutubeDL.UpdateChannel.STABLE
                    )

            val newVersion =
                getYtDlpVersion()

            when (updateStatus?.toString()) {

                "DONE" ->
                    "yt-dlp update completed!\n\n" +
                            "Previous version:\n$oldVersion\n\n" +
                            "Current version:\n$newVersion"

                "ALREADY_UP_TO_DATE" ->
                    "yt-dlp is already up to date.\n\n" +
                            "Current version:\n$newVersion"

                else ->
                    "yt-dlp update finished.\n\n" +
                            "Update status:\n$updateStatus\n\n" +
                            "Previous version:\n$oldVersion\n\n" +
                            "Current version:\n$newVersion"
            }
        }
    }

    /*
     * =====================================================
     * VIDEO ANALYSIS
     * =====================================================
     */

    suspend fun getVideoJson(
        url: String
    ): String {

        return withContext(Dispatchers.IO) {
            check(initialized) {
                "yt-dlp has not been initialized"
            }

            require(url.isNotBlank()) {
                "URL cannot be empty"
            }

            val request =
                YoutubeDLRequest(
                    url.trim()
                )

            request.addOption(
                "--skip-download"
            )

            request.addOption(
                "--dump-single-json"
            )

            request.addOption(
                "--no-playlist"
            )

            YoutubeDL
                .getInstance()
                .execute(request)
                .out
                .trim()
        }
    }

    suspend fun getVideoInfo(
        url: String
    ): VideoInfo {

        val jsonText =
            getVideoJson(url)

        val json =
            JSONObject(jsonText)

        val formatsJson =
            json.optJSONArray(
                "formats"
            )

        val formats =
            mutableListOf<VideoFormat>()

        if (formatsJson != null) {

            for (
            i in 0 until formatsJson.length()
            ) {

                val item =
                    formatsJson.optJSONObject(i)
                        ?: continue

                val formatId =
                    item.optString(
                        "format_id",
                        ""
                    )

                if (formatId.isBlank()) {
                    continue
                }

                val fileSize =
                    item.optNullableLong(
                        "filesize"
                    )
                        ?: item.optNullableLong(
                            "filesize_approx"
                        )

                formats.add(
                    VideoFormat(
                        formatId =
                            formatId,

                        extension =
                            item.optNullableString(
                                "ext"
                            ),

                        width =
                            item.optNullableInt(
                                "width"
                            ),

                        height =
                            item.optNullableInt(
                                "height"
                            ),

                        fps =
                            item.optNullableDouble(
                                "fps"
                            ),

                        videoCodec =
                            item.optNullableString(
                                "vcodec"
                            ),

                        audioCodec =
                            item.optNullableString(
                                "acodec"
                            ),

                        videoExtension =
                            item.optNullableString(
                                "video_ext"
                            ),

                        audioExtension =
                            item.optNullableString(
                                "audio_ext"
                            ),

                        fileSize =
                            fileSize,

                        bitrate =
                            item.optNullableDouble(
                                "tbr"
                            ),

                        videoBitrate =
                            item.optNullableDouble(
                                "vbr"
                            ),

                        audioBitrate =
                            item.optNullableDouble(
                                "abr"
                            ),

                        formatNote =
                            item.optNullableString(
                                "format_note"
                            ),

                        dynamicRange =
                            item.optNullableString(
                                "dynamic_range"
                            ),

                        sourcePreference =
                            item.optNullableDouble(
                                "source_preference"
                            ),

                        quality =
                            item.optNullableDouble(
                                "quality"
                            )
                    )
                )
            }
        }

        return VideoInfo(
            id =
                json.optString(
                    "id",
                    ""
                ),

            title =
                json.optString(
                    "title",
                    "Unknown title"
                ),

            uploader =
                json.optNullableString(
                    "uploader"
                )
                    ?: json.optNullableString(
                        "channel"
                    ),

            duration =
                json.optNullableDouble(
                    "duration"
                )
                    ?.toLong(),

            thumbnail =
                json.optNullableString(
                    "thumbnail"
                ),

            webpageUrl =
                json.optNullableString(
                    "webpage_url"
                ),

            formats =
                formats
        )
    }

    /*
     * =====================================================
     * SMART VIDEO OPTIONS
     * =====================================================
     */

    fun buildDownloadOptions(
        videoInfo: VideoInfo
    ): List<DownloadOption> {

        /*
         * أولًا نبني الخيارات الطبيعية التي نعرف
         * دقتها ونعرف أنها فيديو من خلال vcodec.
         */
        val videoFormats =
            videoInfo.formats.filter {
                it.hasVideo &&
                        displayQuality(it) != null
            }

        val options =
            mutableListOf<DownloadOption>()

        if (videoFormats.isNotEmpty()) {

            val bestAudio =
                selectBestAudio(
                    videoInfo.formats
                )

            val qualities =
                videoFormats
                    .mapNotNull {
                        displayQuality(it)
                    }
                    .distinct()
                    .sorted()

            for (quality in qualities) {

                val formatsAtQuality =
                    videoFormats.filter {
                        displayQuality(it) ==
                                quality
                    }

                val combined =
                    selectBestVideoFormat(
                        formatsAtQuality.filter {
                            it.isCombined
                        }
                    )

                val videoOnly =
                    selectBestVideoFormat(
                        formatsAtQuality.filter {
                            it.isVideoOnly
                        }
                    )

                val preferCombined =
                    combined != null &&
                            combined.isMp4 &&
                            quality <= 480

                val selectedVideo =
                    when {

                        preferCombined ->
                            combined

                        videoOnly != null ->
                            videoOnly

                        combined != null ->
                            combined

                        else ->
                            null
                    }

                if (selectedVideo == null) {
                    continue
                }

                val selectedAudio =
                    if (selectedVideo.isCombined) {
                        null
                    } else {
                        bestAudio
                    }

                if (
                    selectedVideo.isVideoOnly &&
                    selectedAudio == null
                ) {
                    continue
                }

                val estimatedSize =
                    calculateEstimatedSize(
                        video =
                            selectedVideo,

                        audio =
                            selectedAudio,

                        durationSeconds =
                            videoInfo.duration
                    )

                options.add(
                    DownloadOption(
                        height =
                            quality,

                        label =
                            qualityLabel(
                                quality
                            ),

                        videoFormat =
                            selectedVideo,

                        audioFormat =
                            selectedAudio,

                        estimatedSize =
                            estimatedSize
                    )
                )
            }
        }

        /*
         * إذا نجحت الخوارزمية الطبيعية، نعيدها كما هي.
         * هذا يحافظ على سلوك YouTube / TikTok /
         * Instagram / Facebook الحالي.
         */
        if (options.isNotEmpty()) {
            return options
        }

        /*
         * FALLBACK:
         *
         * بعض extractors ترجع ملف فيديو مباشرًا صالحًا
         * ولكن لا تعطي vcodec/acodec ولا width/height.
         *
         * لا نخترع دقة، ولا نفترض وجود audio منفصل،
         * ولا نطلب FFmpeg merge.
         *
         * نعرض الملف كما أعاده extractor باسم:
         * Original Quality
         */
        val directUnknownVideo =
            selectBestDirectUnknownVideo(
                videoInfo.formats
            )

        if (directUnknownVideo != null) {

            return listOf(
                DownloadOption(
                    height =
                        null,

                    label =
                        "Original Quality",

                    videoFormat =
                        directUnknownVideo,

                    audioFormat =
                        null,

                    estimatedSize =
                        calculateSingleFormatSize(
                            format =
                                directUnknownVideo,

                            durationSeconds =
                                videoInfo.duration,

                            audioOnly =
                                false
                        )
                )
            )
        }

        return emptyList()
    }

    /*
     * الجودة المنطقية:
     *
     * 1920x1080 -> 1080p
     * 1080x1920 -> 1080p
     */
    private fun displayQuality(
        format: VideoFormat
    ): Int? {

        val width =
            format.width
                ?.takeIf {
                    it > 0
                }

        val height =
            format.height
                ?.takeIf {
                    it > 0
                }

        return when {

            width != null &&
                    height != null ->

                min(
                    width,
                    height
                )

            height != null ->
                height

            width != null ->
                width

            else ->
                null
        }
    }

    private fun selectBestVideoFormat(
        formats: List<VideoFormat>
    ): VideoFormat? {

        if (formats.isEmpty()) {
            return null
        }

        return formats.maxWithOrNull(
            compareBy<VideoFormat>(
                {
                    if (it.isMp4) {
                        1
                    } else {
                        0
                    }
                },
                {
                    if (it.isAvc) {
                        1
                    } else {
                        0
                    }
                },
                {
                    it.fps
                        ?: 0.0
                },
                {
                    it.quality
                        ?: Double.NEGATIVE_INFINITY
                },
                {
                    it.sourcePreference
                        ?: Double.NEGATIVE_INFINITY
                },
                {
                    it.effectiveVideoBitrate
                        ?: 0.0
                },
                {
                    if (it.hasKnownSize) {
                        1
                    } else {
                        0
                    }
                },
                {
                    it.fileSize
                        ?: 0L
                }
            )
        )
    }

    /*
     * اختيار أفضل ملف مباشر عندما تكون metadata ناقصة.
     * نفضّل MP4 ثم الحجم المعروف ثم المصدر الأعلى.
     */
    private fun selectBestDirectUnknownVideo(
        formats: List<VideoFormat>
    ): VideoFormat? {

        val candidates =
            formats.filter {
                it.isDirectUnknownVideo
            }

        if (candidates.isEmpty()) {
            return null
        }

        return candidates.maxWithOrNull(
            compareBy<VideoFormat>(
                {
                    if (
                        it.extension.equals(
                            "mp4",
                            ignoreCase = true
                        ) ||
                        it.videoExtension.equals(
                            "mp4",
                            ignoreCase = true
                        )
                    ) {
                        1
                    } else {
                        0
                    }
                },
                {
                    if (it.hasKnownSize) {
                        1
                    } else {
                        0
                    }
                },
                {
                    it.fileSize
                        ?: 0L
                },
                {
                    it.sourcePreference
                        ?: Double.NEGATIVE_INFINITY
                },
                {
                    it.quality
                        ?: Double.NEGATIVE_INFINITY
                }
            )
        )
    }

    /*
     * =====================================================
     * SMART AUDIO OPTIONS
     * =====================================================
     */

    fun buildAudioOptions(
        videoInfo: VideoInfo
    ): List<AudioDownloadOption> {

        val audioOnly =
            videoInfo.formats.filter {
                it.isAudioOnly
            }

        if (audioOnly.isEmpty()) {
            return emptyList()
        }

        val options =
            mutableListOf<AudioDownloadOption>()

        val bestM4a =
            selectBestAudioByExtension(
                formats =
                    audioOnly,

                extension =
                    "m4a"
            )

        if (bestM4a != null) {

            options.add(
                AudioDownloadOption(
                    label =
                        "Best Audio (M4A)",

                    audioFormat =
                        bestM4a,

                    estimatedSize =
                        calculateSingleFormatSize(
                            format =
                                bestM4a,

                            durationSeconds =
                                videoInfo.duration,

                            audioOnly =
                                true
                        )
                )
            )
        }

        val bestWebM =
            selectBestAudioByExtension(
                formats =
                    audioOnly,

                extension =
                    "webm"
            )

        if (bestWebM != null) {

            options.add(
                AudioDownloadOption(
                    label =
                        "Best Audio (WebM)",

                    audioFormat =
                        bestWebM,

                    estimatedSize =
                        calculateSingleFormatSize(
                            format =
                                bestWebM,

                            durationSeconds =
                                videoInfo.duration,

                            audioOnly =
                                true
                        )
                )
            )
        }

        if (options.isEmpty()) {

            val best =
                selectBestAudioFromList(
                    audioOnly
                )

            if (best != null) {

                options.add(
                    AudioDownloadOption(
                        label =
                            "Best Audio",

                        audioFormat =
                            best,

                        estimatedSize =
                            calculateSingleFormatSize(
                                format =
                                    best,

                                durationSeconds =
                                    videoInfo.duration,

                                audioOnly =
                                    true
                            )
                    )
                )
            }
        }

        return options
    }

    private fun selectBestAudio(
        formats: List<VideoFormat>
    ): VideoFormat? {

        val audioOnly =
            formats.filter {
                it.isAudioOnly
            }

        if (audioOnly.isEmpty()) {
            return null
        }

        val bestM4a =
            selectBestAudioByExtension(
                formats =
                    audioOnly,

                extension =
                    "m4a"
            )

        if (bestM4a != null) {
            return bestM4a
        }

        return selectBestAudioFromList(
            audioOnly
        )
    }

    private fun selectBestAudioByExtension(
        formats: List<VideoFormat>,
        extension: String
    ): VideoFormat? {

        return selectBestAudioFromList(
            formats.filter {
                it.extension.equals(
                    extension,
                    ignoreCase = true
                )
            }
        )
    }

    private fun selectBestAudioFromList(
        formats: List<VideoFormat>
    ): VideoFormat? {

        if (formats.isEmpty()) {
            return null
        }

        return formats.maxWithOrNull(
            compareBy<VideoFormat>(
                {
                    it.effectiveAudioBitrate
                        ?: 0.0
                },
                {
                    it.sourcePreference
                        ?: Double.NEGATIVE_INFINITY
                },
                {
                    it.fileSize
                        ?: 0L
                }
            )
        )
    }

    /*
     * =====================================================
     * CANCELLATION
     * =====================================================
     */

    fun hasActiveDownload(): Boolean {
        return activeDownloadProcessId != null
    }

    fun cancelActiveDownload(): Boolean {

        val processId =
            activeDownloadProcessId
                ?: return false

        return YoutubeDL
            .getInstance()
            .destroyProcessById(
                processId
            )
    }

    /*
     * =====================================================
     * DOWNLOAD HELPERS
     * =====================================================
     */

    suspend fun downloadVideo(
        context: Context,
        url: String,
        option: DownloadOption,
        onProgress: (DownloadProgress) -> Unit = {}
    ): String {

        val downloadRequest =
            DownloadRequest.from(
                url =
                    url,

                option =
                    option
            )

        return downloadVideo(
            context =
                context,

            downloadRequest =
                downloadRequest,

            onProgress =
                onProgress
        )
    }

    suspend fun downloadAudio(
        context: Context,
        url: String,
        option: AudioDownloadOption,
        onProgress: (DownloadProgress) -> Unit = {}
    ): String {

        val downloadRequest =
            DownloadRequest.from(
                url =
                    url,

                option =
                    option
            )

        return downloadVideo(
            context =
                context,

            downloadRequest =
                downloadRequest,

            onProgress =
                onProgress
        )
    }

    /*
     * =====================================================
     * SHARED DOWNLOAD ENGINE
     * =====================================================
     */

    suspend fun downloadVideo(
        context: Context,
        downloadRequest: DownloadRequest,
        onProgress: (DownloadProgress) -> Unit = {}
    ): String {

        return withContext(Dispatchers.IO) {

            check(initialized) {
                "yt-dlp has not been initialized"
            }

            require(
                downloadRequest.url.isNotBlank()
            ) {
                "URL cannot be empty"
            }

            require(
                downloadRequest
                    .formatSelector
                    .isNotBlank()
            ) {
                "Download format cannot be empty"
            }

            /*
             * لا نشترط height للفيديو.
             * null تعني أن المصدر لم يوفر دقة معروفة.
             */
            if (
                downloadRequest.type ==
                DownloadType.VIDEO &&
                downloadRequest.height != null
            ) {

                require(
                    downloadRequest.height > 0
                ) {
                    "Invalid video height"
                }
            }

            check(
                activeDownloadProcessId == null
            ) {
                "Another download is already active"
            }

            val appContext =
                context.applicationContext

            onProgress(
                DownloadProgress(
                    stage =
                        DownloadStage.PREPARING,

                    percent =
                        0f
                )
            )

            val tempRoot =
                File(
                    appContext.cacheDir,
                    "ytdlp_downloads"
                )

            if (
                !tempRoot.exists() &&
                !tempRoot.mkdirs()
            ) {
                error(
                    "Could not create temporary download folder"
                )
            }

            val jobId =
                UUID
                    .randomUUID()
                    .toString()

            val processId =
                "download-${UUID.randomUUID()}"

            val jobDir =
                File(
                    tempRoot,
                    jobId
                )

            if (!jobDir.mkdirs()) {
                error(
                    "Could not create download job folder"
                )
            }

            activeDownloadProcessId =
                processId

            try {

                val request =
                    YoutubeDLRequest(
                        downloadRequest
                            .url
                            .trim()
                    )

                request.addOption(
                    "-f",
                    downloadRequest
                        .formatSelector
                )

                request.addOption(
                    "--no-playlist"
                )

                if (
                    downloadRequest.type ==
                    DownloadType.VIDEO &&
                    downloadRequest.requiresMerge
                ) {

                    request.addOption(
                        "--merge-output-format",
                        "mp4"
                    )
                }

                val outputSuffix =
                    when (
                        downloadRequest.type
                    ) {

                        DownloadType.VIDEO ->
                            downloadRequest.height
                                ?.let {
                                    "${it}p"
                                }
                                ?: "Original"

                        DownloadType.AUDIO ->
                            "Audio"
                    }

                /*
                 * يبقى اسم العمل الداخلي قصيرًا دائمًا.
                 * لا نضع title هنا.
                 */
                request.addOption(
                    "-o",
                    "${jobDir.absolutePath}/" +
                            "media-$outputSuffix." +
                            "%(format_id)s.%(ext)s"
                )

                YoutubeDL
                    .getInstance()
                    .execute(
                        request,
                        processId
                    ) {
                            progress,
                            etaSeconds,
                            line ->

                        val stage =
                            if (
                                progress >= 999f &&
                                downloadRequest.requiresMerge
                            ) {

                                DownloadStage.MERGING

                            } else {

                                DownloadStage.DOWNLOADING
                            }

                        val safeProgress =
                            when (stage) {

                                DownloadStage.MERGING ->
                                    100f

                                else ->
                                    progress.coerceIn(
                                        0f,
                                        100f
                                    )
                            }

                        val eta =
                            etaSeconds
                                .takeIf {
                                    it >= 0
                                }

                        val speed =
                            extractDownloadSpeed(
                                line
                            )

                        onProgress(
                            DownloadProgress(
                                stage =
                                    stage,

                                percent =
                                    safeProgress,

                                etaSeconds =
                                    eta,

                                speed =
                                    speed,

                                rawLine =
                                    line
                            )
                        )
                    }

                val finalFile =
                    findFinalDownloadedFile(
                        directory =
                            jobDir,

                        downloadRequest =
                            downloadRequest
                    )

                check(
                    finalFile.exists() &&
                            finalFile.isFile &&
                            finalFile.length() > 0
                ) {
                    "The final downloaded file was not found"
                }

                val publicFileName =
                    buildPublicFileName(
                        downloadRequest =
                            downloadRequest,

                        finalExtension =
                            finalFile.extension
                    )

                onProgress(
                    DownloadProgress(
                        stage =
                            DownloadStage.SAVING,

                        percent =
                            100f
                    )
                )

                val result =
                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.Q
                    ) {

                        val savedName =
                            saveToPublicDownloads(
                                context =
                                    appContext,

                                sourceFile =
                                    finalFile,

                                downloadType =
                                    downloadRequest.type,

                                displayName =
                                    publicFileName
                            )

                        buildSuccessfulDownloadMessage(
                            downloadRequest =
                                downloadRequest,

                            fileName =
                                savedName,

                            storageText =
                                "Downloads/SyDown"
                        )

                    } else {

                        val fallbackDirectory =
                            appContext
                                .getExternalFilesDir(
                                    Environment
                                        .DIRECTORY_DOWNLOADS
                                )
                                ?: appContext.filesDir

                        if (
                            !fallbackDirectory.exists()
                        ) {
                            fallbackDirectory.mkdirs()
                        }

                        val fallbackFile =
                            createUniqueFile(
                                directory =
                                    fallbackDirectory,

                                requestedName =
                                    publicFileName
                            )

                        FileInputStream(
                            finalFile
                        ).use { input ->

                            FileOutputStream(
                                fallbackFile
                            ).use { output ->

                                input.copyTo(
                                    output
                                )
                            }
                        }

                        buildSuccessfulDownloadMessage(
                            downloadRequest =
                                downloadRequest,

                            fileName =
                                fallbackFile.name,

                            storageText =
                                fallbackFile.absolutePath
                        )
                    }

                onProgress(
                    DownloadProgress(
                        stage =
                            DownloadStage.FINISHED,

                        percent =
                            100f
                    )
                )

                result

            } finally {

                if (
                    activeDownloadProcessId ==
                    processId
                ) {
                    activeDownloadProcessId =
                        null
                }

                jobDir.deleteRecursively()
            }
        }
    }

    /*
     * =====================================================
     * SAFE PUBLIC FILENAME
     * =====================================================
     */

    private fun buildPublicFileName(
        downloadRequest: DownloadRequest,
        finalExtension: String
    ): String {

        val extension =
            sanitizeExtension(
                finalExtension
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: downloadRequest
                        .expectedExtension
                    ?: if (
                        downloadRequest.type ==
                        DownloadType.VIDEO
                    ) {
                        "mp4"
                    } else {
                        "m4a"
                    }
            )

        val title =
            selectUsefulBaseName(
                downloadRequest
            )

        val suffix =
            when (
                downloadRequest.type
            ) {

                DownloadType.VIDEO ->

                    downloadRequest.height
                        ?.let {
                            "${it}p"
                        }
                        ?: "Original Quality"

                DownloadType.AUDIO -> {

                    val format =
                        when (
                            extension.lowercase()
                        ) {

                            "m4a" ->
                                "M4A"

                            "webm" ->
                                "WebM"

                            "mp3" ->
                                "MP3"

                            "opus" ->
                                "Opus"

                            else ->
                                extension.uppercase()
                        }

                    "Audio $format"
                }
            }

        val safeTitle =
            truncateUtf8(
                text =
                    title,

                maxBytes =
                    160
            )
                .trim()
                .trimEnd(
                    '.',
                    ' '
                )

        val actualTitle =
            safeTitle.takeIf {
                it.isNotBlank()
            }
                ?: "SyDown Media"

        return "$actualTitle - $suffix.$extension"
    }

    private fun selectUsefulBaseName(
        request: DownloadRequest
    ): String {

        val title =
            sanitizeFileNamePart(
                request.mediaTitle
                    .orEmpty()
            )

        if (
            title.isNotBlank() &&
            !isUselessTitle(
                title
            )
        ) {
            return title
        }

        val uploader =
            sanitizeFileNamePart(
                request.uploader
                    .orEmpty()
            )

        val id =
            sanitizeFileNamePart(
                request.mediaId
                    .orEmpty()
            )

        return when {

            uploader.isNotBlank() &&
                    id.isNotBlank() ->

                "$uploader - $id"

            uploader.isNotBlank() ->
                uploader

            id.isNotBlank() ->
                "Media $id"

            else ->
                "SyDown Media"
        }
    }

    private fun isUselessTitle(
        title: String
    ): Boolean {

        val normalized =
            title
                .trim()
                .lowercase()

        return normalized.isBlank() ||
                normalized == "unknown" ||
                normalized == "unknown title" ||
                normalized == "untitled" ||
                normalized == "video" ||
                normalized == "audio" ||
                normalized == "null"
    }

    private fun sanitizeFileNamePart(
        text: String
    ): String {

        if (text.isBlank()) {
            return ""
        }

        val result =
            StringBuilder()

        text.forEach { char ->

            val invalid =
                char.code < 32 ||
                        char == '/' ||
                        char == '\\' ||
                        char == ':' ||
                        char == '*' ||
                        char == '?' ||
                        char == '"' ||
                        char == '<' ||
                        char == '>' ||
                        char == '|' ||
                        char == '\u007F'

            if (invalid) {

                result.append(
                    ' '
                )

            } else {

                result.append(
                    char
                )
            }
        }

        return result
            .toString()
            .replace(
                Regex(
                    "\\s+"
                ),
                " "
            )
            .trim()
            .trimEnd(
                '.',
                ' '
            )
    }

    private fun sanitizeExtension(
        extension: String
    ): String {

        val clean =
            extension
                .lowercase()
                .filter {
                    it.isLetterOrDigit()
                }
                .take(
                    10
                )

        return clean.takeIf {
            it.isNotBlank()
        }
            ?: "bin"
    }

    private fun truncateUtf8(
        text: String,
        maxBytes: Int
    ): String {

        if (
            text
                .toByteArray(
                    Charsets.UTF_8
                )
                .size <= maxBytes
        ) {
            return text
        }

        val result =
            StringBuilder()

        var usedBytes =
            0

        var index =
            0

        while (
            index <
            text.length
        ) {

            val codePoint =
                text.codePointAt(
                    index
                )

            val chars =
                String(
                    Character.toChars(
                        codePoint
                    )
                )

            val bytes =
                chars
                    .toByteArray(
                        Charsets.UTF_8
                    )
                    .size

            if (
                usedBytes + bytes >
                maxBytes
            ) {
                break
            }

            result.append(
                chars
            )

            usedBytes +=
                bytes

            index +=
                Character.charCount(
                    codePoint
                )
        }

        return result
            .toString()
            .trim()
            .trimEnd(
                '.',
                ' '
            )
    }

    /*
     * =====================================================
     * SUCCESS MESSAGE
     * =====================================================
     */

    private fun buildSuccessfulDownloadMessage(
        downloadRequest: DownloadRequest,
        fileName: String,
        storageText: String
    ): String {

        val typeText =
            when (
                downloadRequest.type
            ) {

                DownloadType.VIDEO ->
                    "Video"

                DownloadType.AUDIO ->
                    "Audio"
            }

        val mergeText =
            if (
                downloadRequest.requiresMerge
            ) {
                "YES"
            } else {
                "NO"
            }

        return "Dynamic download finished successfully!\n\n" +
                "Type:\n" +
                typeText +
                "\n\n" +
                "Quality / Format:\n" +
                downloadRequest.label +
                "\n\n" +
                "Selector used:\n" +
                downloadRequest.formatSelector +
                "\n\n" +
                "FFmpeg merge:\n" +
                mergeText +
                "\n\n" +
                "Saved to:\n" +
                storageText +
                "\n\n" +
                "File:\n" +
                fileName
    }

    /*
     * =====================================================
     * DOWNLOAD SPEED
     * =====================================================
     */

    private fun extractDownloadSpeed(
        line: String
    ): String? {

        val regex =
            Regex(
                """(?i)\b(\d+(?:\.\d+)?)\s*([KMGT]?i?B/s)\b"""
            )

        val match =
            regex.find(
                line
            )
                ?: return null

        val number =
            match.groupValues[1]

        val unit =
            match.groupValues[2]

        return "$number $unit"
    }

    /*
     * =====================================================
     * PUBLIC DOWNLOADS - ANDROID 10+
     * =====================================================
     */

    private fun saveToPublicDownloads(
        context: Context,
        sourceFile: File,
        downloadType: DownloadType,
        displayName: String
    ): String {

        check(
            Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
        )

        val resolver =
            context.contentResolver

        val values =
            ContentValues().apply {

                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    displayName
                )

                put(
                    MediaStore.Downloads.MIME_TYPE,
                    mimeTypeForFile(
                        file =
                            sourceFile,

                        downloadType =
                            downloadType
                    )
                )

                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS +
                            "/SyDown"
                )

                put(
                    MediaStore.Downloads.IS_PENDING,
                    1
                )
            }

        val uri =
            resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )
                ?: error(
                    "Could not create file in Downloads"
                )

        try {

            resolver
                .openOutputStream(
                    uri
                )
                ?.use { output ->

                    FileInputStream(
                        sourceFile
                    ).use { input ->

                        input.copyTo(
                            output
                        )
                    }
                }
                ?: error(
                    "Could not open the Downloads file for writing"
                )

            val completedValues =
                ContentValues().apply {

                    put(
                        MediaStore.Downloads.IS_PENDING,
                        0
                    )
                }

            resolver.update(
                uri,
                completedValues,
                null,
                null
            )

            return try {

                resolver.query(
                    uri,
                    arrayOf(
                        MediaStore.Downloads.DISPLAY_NAME
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->

                    if (
                        cursor.moveToFirst()
                    ) {

                        cursor
                            .getString(
                                0
                            )
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: displayName

                    } else {

                        displayName
                    }
                }
                    ?: displayName

            } catch (
                _: Throwable
            ) {

                displayName
            }

        } catch (
            e: Throwable
        ) {

            resolver.delete(
                uri,
                null,
                null
            )

            throw e
        }
    }

    /*
     * =====================================================
     * FINAL FILE DETECTION
     * =====================================================
     */

    private fun findFinalDownloadedFile(
        directory: File,
        downloadRequest: DownloadRequest
    ): File {

        val allEntries =
            directory
                .listFiles()
                ?.toList()
                ?: emptyList()

        val files =
            allEntries.filter {
                it.isFile &&
                        it.length() > 0
            }

        if (
            files.isEmpty()
        ) {

            val entriesText =
                if (
                    allEntries.isEmpty()
                ) {

                    "Directory was empty"

                } else {

                    allEntries.joinToString(
                        separator =
                            "\n"
                    ) { entry ->

                        "${entry.name} | " +
                                "file=${entry.isFile} | " +
                                "size=${entry.length()}"
                    }
                }

            error(
                "No downloaded file was produced.\n\n" +
                        "Work directory contents:\n" +
                        entriesText
            )
        }

        if (
            downloadRequest.type ==
            DownloadType.VIDEO &&
            downloadRequest.requiresMerge
        ) {

            val mp4 =
                files
                    .filter {
                        it.extension.equals(
                            "mp4",
                            ignoreCase =
                                true
                        )
                    }
                    .maxByOrNull {
                        it.length()
                    }

            if (
                mp4 != null
            ) {
                return mp4
            }
        }

        val expectedExtension =
            downloadRequest
                .expectedExtension

        if (
            !expectedExtension.isNullOrBlank()
        ) {

            val expected =
                files
                    .filter {
                        it.extension.equals(
                            expectedExtension,
                            ignoreCase =
                                true
                        )
                    }
                    .maxByOrNull {
                        it.length()
                    }

            if (
                expected != null
            ) {
                return expected
            }
        }

        return files.maxBy {
            it.length()
        }
    }

    /*
     * =====================================================
     * MIME TYPE
     * =====================================================
     */

    private fun mimeTypeForFile(
        file: File,
        downloadType: DownloadType
    ): String {

        return when (
            file.extension.lowercase()
        ) {

            "mp4" ->
                if (
                    downloadType ==
                    DownloadType.AUDIO
                ) {
                    "audio/mp4"
                } else {
                    "video/mp4"
                }

            "m4a" ->
                "audio/mp4"

            "webm" ->
                if (
                    downloadType ==
                    DownloadType.AUDIO
                ) {
                    "audio/webm"
                } else {
                    "video/webm"
                }

            "mp3" ->
                "audio/mpeg"

            "opus" ->
                "audio/opus"

            "ogg",
            "oga" ->
                "audio/ogg"

            "aac" ->
                "audio/aac"

            "wav" ->
                "audio/wav"

            "flac" ->
                "audio/flac"

            "mkv" ->
                "video/x-matroska"

            "mov" ->
                "video/quicktime"

            else ->
                "application/octet-stream"
        }
    }

    /*
     * =====================================================
     * UNIQUE FILE - ANDROID 8/9
     * =====================================================
     */

    private fun createUniqueFile(
        directory: File,
        requestedName: String
    ): File {

        val original =
            File(
                directory,
                requestedName
            )

        if (
            !original.exists()
        ) {
            return original
        }

        val dotIndex =
            requestedName.lastIndexOf(
                '.'
            )

        val baseName =
            if (
                dotIndex > 0
            ) {

                requestedName.substring(
                    0,
                    dotIndex
                )

            } else {

                requestedName
            }

        val extension =
            if (
                dotIndex > 0
            ) {

                requestedName.substring(
                    dotIndex
                )

            } else {

                ""
            }

        var number =
            1

        while (
            true
        ) {

            val candidate =
                File(
                    directory,
                    "$baseName ($number)$extension"
                )

            if (
                !candidate.exists()
            ) {
                return candidate
            }

            number++
        }
    }

    /*
     * =====================================================
     * SIZE ESTIMATION
     * =====================================================
     */

    private fun calculateEstimatedSize(
        video: VideoFormat,
        audio: VideoFormat?,
        durationSeconds: Long?
    ): Long? {

        val videoSize =
            calculateSingleFormatSize(
                format =
                    video,

                durationSeconds =
                    durationSeconds,

                audioOnly =
                    false
            )

        val audioSize =
            if (
                audio != null
            ) {

                calculateSingleFormatSize(
                    format =
                        audio,

                    durationSeconds =
                        durationSeconds,

                    audioOnly =
                        true
                )

            } else {

                null
            }

        return when {

            audio != null &&
                    videoSize != null &&
                    audioSize != null ->

                videoSize + audioSize

            videoSize != null ->
                videoSize

            else ->
                null
        }
    }

    private fun calculateSingleFormatSize(
        format: VideoFormat,
        durationSeconds: Long?,
        audioOnly: Boolean
    ): Long? {

        val directSize =
            format.fileSize

        if (
            directSize != null &&
            directSize > 0
        ) {
            return directSize
        }

        val bitrate =
            if (
                audioOnly
            ) {

                format.effectiveAudioBitrate

            } else {

                format.effectiveVideoBitrate
            }

        if (
            bitrate == null ||
            bitrate <= 0.0 ||
            durationSeconds == null ||
            durationSeconds <= 0
        ) {
            return null
        }

        val bytes =
            bitrate *
                    1000.0 *
                    durationSeconds.toDouble() /
                    8.0

        if (
            !bytes.isFinite() ||
            bytes <= 0.0
        ) {
            return null
        }

        return bytes.toLong()
    }

    /*
     * =====================================================
     * QUALITY LABEL
     * =====================================================
     */

    private fun qualityLabel(
        height: Int
    ): String {

        return when {

            height >= 2160 ->
                "${height}p 4K"

            height >= 1440 ->
                "${height}p 2K"

            height >= 1080 ->
                "${height}p FHD"

            height >= 720 ->
                "${height}p HD"

            else ->
                "${height}p"
        }
    }

    /*
     * =====================================================
     * JSON HELPERS
     * =====================================================
     */

    private fun JSONObject.optNullableString(
        name: String
    ): String? {

        if (
            !has(
                name
            ) ||
            isNull(
                name
            )
        ) {
            return null
        }

        val value =
            optString(
                name,
                ""
            )

        return value.takeIf {
            it.isNotBlank()
        }
    }

    private fun JSONObject.optNullableInt(
        name: String
    ): Int? {

        if (
            !has(
                name
            ) ||
            isNull(
                name
            )
        ) {
            return null
        }

        return try {

            getInt(
                name
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }

    private fun JSONObject.optNullableLong(
        name: String
    ): Long? {

        if (
            !has(
                name
            ) ||
            isNull(
                name
            )
        ) {
            return null
        }

        return try {

            getLong(
                name
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }

    private fun JSONObject.optNullableDouble(
        name: String
    ): Double? {

        if (
            !has(
                name
            ) ||
            isNull(
                name
            )
        ) {
            return null
        }

        return try {

            getDouble(
                name
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }
}