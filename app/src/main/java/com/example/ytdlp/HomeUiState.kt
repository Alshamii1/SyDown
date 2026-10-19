package com.example.ytdlp

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ytdlp.ytdlp.VideoInfo
import com.example.ytdlp.ytdlp.YtDlpManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HomeUiState {

    var url by mutableStateOf("")
        private set

    var analyzing by mutableStateOf(false)

    var videoInfo by mutableStateOf<VideoInfo?>(null)

    var errorMessage by mutableStateOf<String?>(null)

    var showDownloadSheet by mutableStateOf(false)

    var handledSharedUrl by mutableStateOf<String?>(null)

    /*
     * نطاق مستقل عن HomeScreen.
     *
     * لا يتوقف عند الانتقال إلى الإعدادات
     * أو التنزيلات أو مغادرة التطبيق مؤقتًا.
     */
    private val analysisScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

    private var analysisJob: Job? = null

    fun updateUrl(
        value: String
    ) {
        if (url == value) {
            return
        }

        cancelAnalysis()

        url = value
        videoInfo = null
        errorMessage = null
        showDownloadSheet = false
    }

    fun setIncomingUrl(
        value: String
    ) {
        cancelAnalysis()

        url = value
        videoInfo = null
        errorMessage = null
        showDownloadSheet = false
    }

    fun startAnalysis(
        context: Context
    ) {
        val requestedUrl =
            url.trim()

        if (
            requestedUrl.isBlank() ||
            analyzing
        ) {
            return
        }

        val appContext =
            context.applicationContext

        videoInfo = null
        errorMessage = null
        analyzing = true

        val job =
            analysisScope.launch {
                try {
                    val result =
                        YtDlpManager.getVideoInfo(
                            requestedUrl
                        )

                    if (
                        url.trim() ==
                        requestedUrl
                    ) {
                        videoInfo = result
                        errorMessage = null
                    }
                } catch (
                    error: CancellationException
                ) {
                    throw error
                } catch (
                    error: Throwable
                ) {
                    Log.e(
                        "SYDOWN",
                        "Analysis failed",
                        error
                    )

                    if (
                        url.trim() ==
                        requestedUrl
                    ) {
                        errorMessage =
                            SyDownFriendlyErrors.message(
                                appContext,
                                error
                            )
                    }
                } finally {
                    analyzing = false
                }
            }

        analysisJob = job
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        analyzing = false
    }

    /*
     * يُستدعى فقط عندما تنتهي دورة حياة
     * الحالة نفسها، وليس عند تغيير الصفحة
     * أو الضغط على Home في الهاتف.
     */
    fun dispose() {
        cancelAnalysis()
        analysisScope.coroutineContext[
            Job
        ]?.cancel()
    }
}