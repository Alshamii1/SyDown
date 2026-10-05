package com.example.ytdlp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ytdlp.ytdlp.VideoInfo

class HomeUiState {

    var url by mutableStateOf("")
        private set

    var analyzing by mutableStateOf(false)

    var videoInfo by mutableStateOf<VideoInfo?>(null)

    var errorMessage by mutableStateOf<String?>(null)

    var showDownloadSheet by mutableStateOf(false)

    var handledSharedUrl by mutableStateOf<String?>(null)

    fun updateUrl(
        value: String
    ) {
        url = value
        videoInfo = null
        errorMessage = null
    }

    fun setIncomingUrl(
        value: String
    ) {
        url = value
        videoInfo = null
        errorMessage = null
        showDownloadSheet = false
    }
}