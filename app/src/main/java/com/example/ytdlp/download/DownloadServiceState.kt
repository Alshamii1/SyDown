package com.example.ytdlp.download

import com.example.ytdlp.ytdlp.DownloadProgress
import com.example.ytdlp.ytdlp.DownloadStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BackgroundDownloadState(

    val isRunning: Boolean = false,

    val isCancelling: Boolean = false,

    val qualityLabel: String = "",

    val progress: DownloadProgress? = null,

    val message: String = "",

    val completedSuccessfully: Boolean = false,

    val cancelled: Boolean = false,

    val errorMessage: String? = null
)

object DownloadServiceState {

    private val _state =
        MutableStateFlow(
            BackgroundDownloadState()
        )

    val state: StateFlow<BackgroundDownloadState> =
        _state.asStateFlow()

    val isRunning: Boolean
        get() =
            _state.value.isRunning

    val isCancelling: Boolean
        get() =
            _state.value.isCancelling

    val qualityLabel: String
        get() =
            _state.value.qualityLabel

    val progress: DownloadProgress?
        get() =
            _state.value.progress

    val message: String
        get() =
            _state.value.message

    val completedSuccessfully: Boolean
        get() =
            _state.value.completedSuccessfully

    val cancelled: Boolean
        get() =
            _state.value.cancelled

    val errorMessage: String?
        get() =
            _state.value.errorMessage

    /*
     * =====================================================
     * START
     * =====================================================
     */

    fun onStarting(
        quality: String
    ) {

        _state.value =
            BackgroundDownloadState(
                isRunning =
                    true,

                isCancelling =
                    false,

                qualityLabel =
                    quality,

                progress =
                    DownloadProgress(
                        stage =
                            DownloadStage.PREPARING,

                        percent =
                            0f
                    ),

                message =
                    "Preparing download..."
            )
    }

    /*
     * =====================================================
     * PROGRESS
     * =====================================================
     */

    fun onProgress(
        newProgress: DownloadProgress
    ) {

        val oldState =
            _state.value

        _state.value =
            oldState.copy(
                isRunning =
                    true,

                isCancelling =
                    false,

                progress =
                    newProgress,

                message =
                    when (
                        newProgress.stage
                    ) {

                        DownloadStage.PREPARING ->
                            "Preparing download..."

                        DownloadStage.DOWNLOADING ->
                            "Downloading..."

                        DownloadStage.MERGING ->
                            "Merging with FFmpeg..."

                        DownloadStage.SAVING ->
                            "Saving to Downloads..."

                        DownloadStage.FINISHED ->
                            "Download finished."
                    },

                completedSuccessfully =
                    false,

                cancelled =
                    false,

                errorMessage =
                    null
            )
    }

    /*
     * =====================================================
     * CANCELLING
     * =====================================================
     */

    fun onCancelling() {

        val oldState =
            _state.value

        if (
            !oldState.isRunning
        ) {
            return
        }

        _state.value =
            oldState.copy(
                isCancelling =
                    true,

                message =
                    "Cancelling download..."
            )
    }

    /*
     * =====================================================
     * CANCELLED
     * =====================================================
     */

    fun onCancelled() {

        val oldState =
            _state.value

        _state.value =
            oldState.copy(
                isRunning =
                    false,

                isCancelling =
                    false,

                progress =
                    null,

                message =
                    "Download cancelled.",

                completedSuccessfully =
                    false,

                cancelled =
                    true,

                errorMessage =
                    null
            )
    }

    /*
     * =====================================================
     * COMPLETED
     * =====================================================
     */

    fun onCompleted(
        finalMessage: String
    ) {

        val oldState =
            _state.value

        _state.value =
            oldState.copy(
                isRunning =
                    false,

                isCancelling =
                    false,

                progress =
                    DownloadProgress(
                        stage =
                            DownloadStage.FINISHED,

                        percent =
                            100f
                    ),

                message =
                    finalMessage,

                completedSuccessfully =
                    true,

                cancelled =
                    false,

                errorMessage =
                    null
            )
    }

    /*
     * =====================================================
     * ERROR
     * =====================================================
     */

    fun onError(
        error: Throwable
    ) {

        onErrorMessage(
            friendlyErrorMessage(
                error
            )
        )
    }

    /*
     * تستعمله DownloadService عندما تكون قد جهزت
     * رسالة مترجمة وآمنة للمستخدم مسبقًا.
     */
    fun onErrorMessage(
        friendlyMessage: String
    ) {

        val oldState =
            _state.value

        _state.value =
            oldState.copy(
                isRunning =
                    false,

                isCancelling =
                    false,

                progress =
                    null,

                message =
                    "Download failed.",

                completedSuccessfully =
                    false,

                cancelled =
                    false,

                errorMessage =
                    friendlyMessage
            )
    }

    /*
     * =====================================================
     * LEGACY FRIENDLY ERROR
     * =====================================================
     *
     * نبقي هذه الدوال مؤقتًا لأي مسار قديم يعتمد عليها.
     * الواجهة الرئيسية وخدمة التنزيل الجديدة لا تحتاجان
     * لعرض raw yt-dlp للمستخدم.
     */

    fun friendlyErrorMessage(
        error: Throwable
    ): String {

        val rawError =
            error.message
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: error.toString()

        return friendlyErrorMessage(
            rawError
        )
    }

    fun friendlyErrorMessage(
        rawError: String
    ): String {

        val lower =
            rawError.lowercase()

        return when {

            lower.contains(
                "429"
            ) ||
                    lower.contains(
                        "too many requests"
                    ) ||
                    lower.contains(
                        "confirm you're not a bot"
                    ) -> {

                "YouTube temporarily limited requests from the current network.\n\n" +
                        "HTTP 429: Too Many Requests.\n\n" +
                        "Wait before trying again. Repeated rapid retries may make the restriction last longer."
            }

            lower.contains(
                "403"
            ) ||
                    lower.contains(
                        "forbidden"
                    ) -> {

                "YouTube refused access to the media stream.\n\n" +
                        "HTTP 403: Forbidden.\n\n" +
                        "The media request may have expired or been rejected. Analyze the URL again later before retrying."
            }

            lower.contains(
                "unable to resolve host"
            ) ||
                    lower.contains(
                        "name or service not known"
                    ) ||
                    lower.contains(
                        "temporary failure in name resolution"
                    ) -> {

                "Could not reach the server.\n\n" +
                        "Check the internet connection and try again later."
            }

            lower.contains(
                "timed out"
            ) ||
                    lower.contains(
                        "timeout"
                    ) -> {

                "The network request timed out.\n\n" +
                        "Check the connection and try again later."
            }

            else -> {

                rawError
            }
        }
    }

    /*
     * =====================================================
     * CLEAR FINISHED STATE
     * =====================================================
     */

    fun clearFinishedState() {

        if (
            _state.value.isRunning
        ) {
            return
        }

        _state.value =
            BackgroundDownloadState()
    }
}