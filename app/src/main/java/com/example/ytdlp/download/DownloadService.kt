package com.example.ytdlp.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.ytdlp.AppLanguage
import com.example.ytdlp.AppPreferences
import com.example.ytdlp.MainActivity
import com.example.ytdlp.R
import com.example.ytdlp.SyDownFriendlyErrors
import com.example.ytdlp.SyDownLocale
import com.example.ytdlp.ytdlp.DownloadProgress
import com.example.ytdlp.ytdlp.DownloadRequest
import com.example.ytdlp.ytdlp.DownloadStage
import com.example.ytdlp.ytdlp.DownloadType
import com.example.ytdlp.ytdlp.YtDlpManager
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

class DownloadService : Service() {

    companion object {

        private const val TAG =
            "YTDLP_DOWNLOAD_SERVICE"

        const val ACTION_START_DOWNLOAD =
            "com.example.ytdlp.download.action.START_DOWNLOAD"

        const val ACTION_CANCEL_DOWNLOAD =
            "com.example.ytdlp.download.action.CANCEL_DOWNLOAD"

        private const val EXTRA_URL =
            "extra_url"

        private const val EXTRA_TYPE =
            "extra_type"

        private const val EXTRA_HEIGHT =
            "extra_height"

        private const val EXTRA_LABEL =
            "extra_label"

        private const val EXTRA_FORMAT_SELECTOR =
            "extra_format_selector"

        private const val EXTRA_REQUIRES_MERGE =
            "extra_requires_merge"

        private const val EXTRA_EXPECTED_EXTENSION =
            "extra_expected_extension"

        private const val EXTRA_MEDIA_TITLE =
            "extra_media_title"

        private const val EXTRA_MEDIA_ID =
            "extra_media_id"

        private const val EXTRA_UPLOADER =
            "extra_uploader"

        private const val EXTRA_THUMBNAIL_URL =
            "extra_thumbnail_url"

        private const val CHANNEL_ID =
            "ytdlp_downloads"

        private const val CHANNEL_NAME =
            "SyDown Downloads"

        private const val NOTIFICATION_ID =
            1001

        fun createStartIntent(
            context: Context,
            request: DownloadRequest
        ): Intent {

            return Intent(
                context,
                DownloadService::class.java
            ).apply {

                action =
                    ACTION_START_DOWNLOAD

                putExtra(
                    EXTRA_URL,
                    request.url
                )

                putExtra(
                    EXTRA_TYPE,
                    request.type.name
                )

                putExtra(
                    EXTRA_HEIGHT,
                    request.height ?: -1
                )

                putExtra(
                    EXTRA_LABEL,
                    request.label
                )

                putExtra(
                    EXTRA_FORMAT_SELECTOR,
                    request.formatSelector
                )

                putExtra(
                    EXTRA_REQUIRES_MERGE,
                    request.requiresMerge
                )

                putExtra(
                    EXTRA_EXPECTED_EXTENSION,
                    request.expectedExtension
                )

                putExtra(
                    EXTRA_MEDIA_TITLE,
                    request.mediaTitle
                )

                putExtra(
                    EXTRA_MEDIA_ID,
                    request.mediaId
                )

                putExtra(
                    EXTRA_UPLOADER,
                    request.uploader
                )

                putExtra(
                    EXTRA_THUMBNAIL_URL,
                    request.thumbnailUrl
                )
            }
        }

        fun createCancelIntent(
            context: Context
        ): Intent {

            return Intent(
                context,
                DownloadService::class.java
            ).apply {

                action =
                    ACTION_CANCEL_DOWNLOAD
            }
        }
    }

    private val serviceJob =
        SupervisorJob()

    private val serviceScope =
        CoroutineScope(
            serviceJob +
                    Dispatchers.IO
        )

    private var downloadJob: Job? =
        null

    @Volatile
    private var cancellationRequested =
        false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        DownloadHistoryRepository.initialize(
            applicationContext
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START_DOWNLOAD -> {
                handleStartDownload(
                    intent
                )
            }

            ACTION_CANCEL_DOWNLOAD -> {
                handleCancelDownload()
            }

            else -> {
                if (
                    downloadJob?.isActive != true
                ) {
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    /*
     * =====================================================
     * LOCALIZED CONTEXT
     * =====================================================
     */

    private fun localizedAppContext():
            Context {

        val preferences =
            AppPreferences(
                applicationContext
            )

        val preferredLanguage =
            preferences.getLanguage()

        val resolvedLanguage =
            when (
                preferredLanguage
            ) {

                AppLanguage.SYSTEM ->
                    SyDownLocale
                        .resolveDeviceLanguage()

                else ->
                    preferredLanguage
            }

        return SyDownLocale
            .localizedContext(
                context =
                    applicationContext,

                language =
                    resolvedLanguage
            )
    }

    /*
     * =====================================================
     * START DOWNLOAD
     * =====================================================
     */

    private fun handleStartDownload(
        intent: Intent
    ) {

        if (
            downloadJob?.isActive == true ||
            DownloadServiceState.isRunning
        ) {

            Log.w(
                TAG,
                "A download is already running"
            )

            return
        }

        val request =
            readDownloadRequest(
                intent
            )

        if (request == null) {

            val error =
                IllegalArgumentException(
                    "Invalid background download request"
                )

            Log.e(
                TAG,
                "Invalid download request",
                error
            )

            val friendlyMessage =
                friendlyMessageFor(
                    error
                )

            DownloadServiceState
                .onErrorMessage(
                    friendlyMessage
                )

            showErrorNotification(
                label = "SyDown",
                friendlyMessage =
                    friendlyMessage
            )

            stopSelf()

            return
        }

        cancellationRequested =
            false

        DownloadServiceState.onStarting(
            request.label
        )

        startAsForeground(
            buildPreparingNotification(
                request.label
            )
        )

        downloadJob =
            serviceScope.launch {

                try {

                    YtDlpManager.initialize(
                        applicationContext
                    )

                    val result =
                        YtDlpManager.downloadVideo(
                            context =
                                applicationContext,

                            downloadRequest =
                                request,

                            onProgress = {
                                    progress ->

                                DownloadServiceState
                                    .onProgress(
                                        progress
                                    )

                                updateProgressNotification(
                                    label =
                                        request.label,

                                    progress =
                                        progress
                                )
                            }
                        )

                    try {

                        addSuccessfulDownloadToHistory(
                            request = request,
                            successMessage = result
                        )

                    } catch (
                        historyError: Throwable
                    ) {

                        Log.e(
                            TAG,
                            "Download succeeded but history registration failed",
                            historyError
                        )
                    }

                    DownloadServiceState
                        .onCompleted(
                            result
                        )

                    showFinishedNotification(
                        label =
                            request.label
                    )

                } catch (
                    e: YoutubeDL.CanceledException
                ) {

                    Log.i(
                        TAG,
                        "Download cancelled",
                        e
                    )

                    DownloadServiceState
                        .onCancelled()

                    showCancelledNotification(
                        label =
                            request.label
                    )

                } catch (
                    e: Throwable
                ) {

                    Log.e(
                        TAG,
                        "Background download failed",
                        e
                    )

                    if (
                        cancellationRequested
                    ) {

                        DownloadServiceState
                            .onCancelled()

                        showCancelledNotification(
                            label =
                                request.label
                        )

                    } else {

                        val friendlyMessage =
                            friendlyMessageFor(
                                e
                            )

                        DownloadServiceState
                            .onErrorMessage(
                                friendlyMessage
                            )

                        showErrorNotification(
                            label =
                                request.label,

                            friendlyMessage =
                                friendlyMessage
                        )
                    }

                } finally {

                    cancellationRequested =
                        false

                    downloadJob =
                        null

                    stopForeground(
                        STOP_FOREGROUND_DETACH
                    )

                    stopSelf()
                }
            }
    }

    /*
     * =====================================================
     * FRIENDLY ERRORS
     * =====================================================
     */

    private fun friendlyMessageFor(
        error: Throwable
    ): String {

        return SyDownFriendlyErrors
            .message(
                context =
                    localizedAppContext(),

                error =
                    error
            )
    }

    /*
     * =====================================================
     * DOWNLOAD HISTORY
     * =====================================================
     */

    private suspend fun addSuccessfulDownloadToHistory(
        request: DownloadRequest,
        successMessage: String
    ) {

        val media =
            DownloadedMediaResolver.resolve(
                context =
                    applicationContext,

                request =
                    request,

                successMessage =
                    successMessage
            )

        if (media == null) {

            Log.w(
                TAG,
                "Download succeeded but saved media could not be resolved for history"
            )

            return
        }

        val historyTitle =
            request.mediaTitle
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: media.fileName
                    .substringBeforeLast(
                        '.',
                        media.fileName
                    )

        val historyId =
            UUID
                .randomUUID()
                .toString()

        val thumbnailPath =
            if (
                request.type ==
                DownloadType.VIDEO
            ) {

                DownloadThumbnailStore.save(
                    context =
                        applicationContext,

                    thumbnailUrl =
                        request.thumbnailUrl,

                    historyId =
                        historyId
                )

            } else {

                null
            }

        val item =
            DownloadHistoryItem(
                id =
                    historyId,

                title =
                    historyTitle,

                uploader =
                    request.uploader
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        },

                sourceUrl =
                    request.url,

                type =
                    request.type.name,

                qualityLabel =
                    request.label,

                fileName =
                    media.fileName,

                mimeType =
                    media.mimeType,

                sizeBytes =
                    media.sizeBytes,

                contentUri =
                    media.contentUri,

                filePath =
                    media.filePath,

                downloadedAt =
                    System.currentTimeMillis(),

                thumbnailPath =
                    thumbnailPath
            )

        try {

            DownloadHistoryRepository.add(
                context =
                    applicationContext,

                item =
                    item
            )

        } catch (
            historyError: Throwable
        ) {

            if (
                thumbnailPath != null
            ) {

                DownloadThumbnailStore.delete(
                    context =
                        applicationContext,

                    historyId =
                        historyId
                )
            }

            throw historyError
        }

        Log.i(
            TAG,
            "Download added to history: ${media.fileName}"
        )
    }

    /*
     * =====================================================
     * CANCEL
     * =====================================================
     */

    private fun handleCancelDownload() {

        if (
            !DownloadServiceState.isRunning
        ) {
            return
        }

        cancellationRequested =
            true

        DownloadServiceState
            .onCancelling()

        val localizedContext =
            localizedAppContext()

        updateSimpleNotification(
            title =
                localizedContext.getString(
                    R.string.cancelling_download
                ),

            text =
                DownloadServiceState
                    .qualityLabel
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: "SyDown"
        )

        val cancelled =
            try {

                YtDlpManager
                    .cancelActiveDownload()

            } catch (
                e: Throwable
            ) {

                Log.e(
                    TAG,
                    "Could not request cancellation",
                    e
                )

                false
            }

        if (!cancelled) {

            downloadJob
                ?.cancel()
        }
    }

    /*
     * =====================================================
     * INTENT -> DOWNLOAD REQUEST
     * =====================================================
     */

    private fun readDownloadRequest(
        intent: Intent
    ): DownloadRequest? {

        val url =
            intent
                .getStringExtra(
                    EXTRA_URL
                )
                ?.trim()
                .orEmpty()

        val typeName =
            intent
                .getStringExtra(
                    EXTRA_TYPE
                )
                .orEmpty()

        val type =
            try {

                DownloadType.valueOf(
                    typeName
                )

            } catch (
                _: IllegalArgumentException
            ) {

                null

            } catch (
                _: NullPointerException
            ) {

                null
            }

        val rawHeight =
            intent.getIntExtra(
                EXTRA_HEIGHT,
                -1
            )

        val height =
            rawHeight
                .takeIf {
                    it > 0
                }

        val label =
            intent
                .getStringExtra(
                    EXTRA_LABEL
                )
                .orEmpty()

        val formatSelector =
            intent
                .getStringExtra(
                    EXTRA_FORMAT_SELECTOR
                )
                .orEmpty()

        val requiresMerge =
            intent.getBooleanExtra(
                EXTRA_REQUIRES_MERGE,
                false
            )

        val expectedExtension =
            intent
                .getStringExtra(
                    EXTRA_EXPECTED_EXTENSION
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val mediaTitle =
            intent
                .getStringExtra(
                    EXTRA_MEDIA_TITLE
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val mediaId =
            intent
                .getStringExtra(
                    EXTRA_MEDIA_ID
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val uploader =
            intent
                .getStringExtra(
                    EXTRA_UPLOADER
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val thumbnailUrl =
            intent
                .getStringExtra(
                    EXTRA_THUMBNAIL_URL
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            url.isBlank() ||
            type == null ||
            label.isBlank() ||
            formatSelector.isBlank()
        ) {
            return null
        }

        return DownloadRequest(
            url =
                url,

            type =
                type,

            height =
                height,

            label =
                label,

            formatSelector =
                formatSelector,

            requiresMerge =
                requiresMerge,

            expectedExtension =
                expectedExtension,

            mediaTitle =
                mediaTitle,

            mediaId =
                mediaId,

            uploader =
                uploader,

            thumbnailUrl =
                thumbnailUrl
        )
    }

    /*
     * =====================================================
     * FOREGROUND
     * =====================================================
     */

    private fun startAsForeground(
        notification: Notification
    ) {

        val foregroundServiceType =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_DATA_SYNC

            } else {

                0
            }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundServiceType
        )
    }

    /*
     * =====================================================
     * ACTIVE NOTIFICATIONS
     * =====================================================
     */

    private fun buildPreparingNotification(
        label: String
    ): Notification {

        val localizedContext =
            localizedAppContext()

        return createBaseNotificationBuilder()
            .setContentTitle(
                localizedContext.getString(
                    R.string.preparing_download
                )
            )
            .setContentText(
                label
            )
            .setOngoing(
                true
            )
            .setProgress(
                100,
                0,
                true
            )
            .addAction(
                0,
                localizedContext.getString(
                    R.string.cancel
                ),
                createCancelPendingIntent()
            )
            .build()
    }

    private fun updateProgressNotification(
        label: String,
        progress: DownloadProgress
    ) {

        val localizedContext =
            localizedAppContext()

        val percent =
            progress
                .safePercent
                .toInt()
                .coerceIn(
                    0,
                    100
                )

        val title =
            when (
                progress.stage
            ) {

                DownloadStage.PREPARING ->
                    localizedContext.getString(
                        R.string.preparing_download
                    )

                DownloadStage.DOWNLOADING ->
                    if (
                        percent <= 0 &&
                        progress.speed.isNullOrBlank()
                    ) {

                        localizedContext.getString(
                            R.string.starting_download
                        )

                    } else {

                        localizedContext.getString(
                            R.string.downloading_percent,
                            percent
                        )
                    }

                DownloadStage.MERGING ->
                    localizedContext.getString(
                        R.string.merging_ffmpeg
                    )

                DownloadStage.SAVING ->
                    localizedContext.getString(
                        R.string.saving_to_downloads
                    )

                DownloadStage.FINISHED ->
                    localizedContext.getString(
                        R.string.download_finished
                    )
            }

        val details =
            buildProgressDetails(
                label =
                    label,

                progress =
                    progress,

                localizedContext =
                    localizedContext
            )

        val builder =
            createBaseNotificationBuilder()
                .setContentTitle(
                    title
                )
                .setContentText(
                    details
                )
                .setOngoing(
                    progress.stage !=
                            DownloadStage.FINISHED
                )
                .setOnlyAlertOnce(
                    true
                )

        when (
            progress.stage
        ) {

            DownloadStage.DOWNLOADING -> {

                if (
                    percent <= 0 &&
                    progress.speed.isNullOrBlank()
                ) {

                    builder.setProgress(
                        100,
                        0,
                        true
                    )

                } else {

                    builder.setProgress(
                        100,
                        percent,
                        false
                    )
                }

                builder.addAction(
                    0,
                    localizedContext.getString(
                        R.string.cancel
                    ),
                    createCancelPendingIntent()
                )
            }

            DownloadStage.PREPARING -> {

                builder.setProgress(
                    100,
                    0,
                    true
                )

                builder.addAction(
                    0,
                    localizedContext.getString(
                        R.string.cancel
                    ),
                    createCancelPendingIntent()
                )
            }

            DownloadStage.MERGING,
            DownloadStage.SAVING -> {

                builder.setProgress(
                    100,
                    percent,
                    false
                )

                builder.addAction(
                    0,
                    localizedContext.getString(
                        R.string.cancel
                    ),
                    createCancelPendingIntent()
                )
            }

            DownloadStage.FINISHED -> {

                builder.setProgress(
                    0,
                    0,
                    false
                )
            }
        }

        notifyDownload(
            builder.build()
        )
    }

    private fun updateSimpleNotification(
        title: String,
        text: String
    ) {

        val notification =
            createBaseNotificationBuilder()
                .setContentTitle(
                    title
                )
                .setContentText(
                    text
                )
                .setOngoing(
                    true
                )
                .setProgress(
                    100,
                    0,
                    true
                )
                .build()

        notifyDownload(
            notification
        )
    }

    /*
     * =====================================================
     * FINAL NOTIFICATIONS
     * =====================================================
     */

    private fun showFinishedNotification(
        label: String
    ) {

        val localizedContext =
            localizedAppContext()

        val notification =
            createBaseNotificationBuilder()
                .setContentTitle(
                    localizedContext.getString(
                        R.string.download_finished
                    )
                )
                .setContentText(
                    "$label • ${
                        localizedContext.getString(
                            R.string.saved_to_downloads_ytdlp
                        )
                    }"
                )
                .setOngoing(
                    false
                )
                .setAutoCancel(
                    true
                )
                .setProgress(
                    0,
                    0,
                    false
                )
                .build()

        notifyDownload(
            notification
        )
    }

    private fun showCancelledNotification(
        label: String
    ) {

        val localizedContext =
            localizedAppContext()

        val notification =
            createBaseNotificationBuilder()
                .setContentTitle(
                    localizedContext.getString(
                        R.string.download_cancelled
                    )
                )
                .setContentText(
                    label
                )
                .setOngoing(
                    false
                )
                .setAutoCancel(
                    true
                )
                .setProgress(
                    0,
                    0,
                    false
                )
                .build()

        notifyDownload(
            notification
        )
    }

    private fun showErrorNotification(
        label: String,
        friendlyMessage: String
    ) {

        val localizedContext =
            localizedAppContext()

        val notification =
            createBaseNotificationBuilder()
                .setContentTitle(
                    localizedContext.getString(
                        R.string.download_failed
                    )
                )
                .setContentText(
                    "$label • $friendlyMessage"
                )
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(
                            "$label\n\n$friendlyMessage"
                        )
                )
                .setOngoing(
                    false
                )
                .setAutoCancel(
                    true
                )
                .setProgress(
                    0,
                    0,
                    false
                )
                .build()

        notifyDownload(
            notification
        )
    }

    /*
     * =====================================================
     * NOTIFICATION HELPERS
     * =====================================================
     */

    private fun createBaseNotificationBuilder():
            NotificationCompat.Builder {

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setSmallIcon(
                R.mipmap.ic_launcher
            )
            .setContentIntent(
                createOpenAppPendingIntent()
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .setCategory(
                NotificationCompat.CATEGORY_PROGRESS
            )
            .setOnlyAlertOnce(
                true
            )
    }

    private fun buildProgressDetails(
        label: String,
        progress: DownloadProgress,
        localizedContext: Context
    ): String {

        val parts =
            mutableListOf<String>()

        parts.add(
            label
        )

        if (
            progress.stage ==
            DownloadStage.DOWNLOADING
        ) {

            progress.speed
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    parts.add(
                        it
                    )
                }

            progress.etaSeconds
                ?.takeIf {
                    it >= 0
                }
                ?.let {

                    parts.add(
                        localizedContext.getString(
                            R.string.eta_value,
                            formatEta(it)
                        )
                    )
                }
        }

        return parts.joinToString(
            separator =
                " • "
        )
    }

    private fun notifyDownload(
        notification: Notification
    ) {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    /*
     * =====================================================
     * PENDING INTENTS
     * =====================================================
     */

    private fun createOpenAppPendingIntent():
            PendingIntent {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        return PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCancelPendingIntent():
            PendingIntent {

        val intent =
            createCancelIntent(
                this
            )

        return PendingIntent.getService(
            this,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    /*
     * =====================================================
     * CHANNEL
     * =====================================================
     */

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {

                description =
                    "SyDown download progress"
            }

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    private fun formatEta(
        seconds: Long
    ): String {

        val hours =
            seconds / 3600

        val minutes =
            (seconds % 3600) /
                    60

        val secs =
            seconds % 60

        return if (
            hours > 0
        ) {

            "%d:%02d:%02d".format(
                hours,
                minutes,
                secs
            )

        } else {

            "%02d:%02d".format(
                minutes,
                secs
            )
        }
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    override fun onDestroy() {

        serviceScope.cancel()

        super.onDestroy()
    }
}