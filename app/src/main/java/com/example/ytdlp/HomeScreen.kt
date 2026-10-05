package com.example.ytdlp

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.example.ytdlp.download.BackgroundDownloadState
import com.example.ytdlp.download.DownloadService
import com.example.ytdlp.download.DownloadServiceState
import com.example.ytdlp.ytdlp.AudioDownloadOption
import com.example.ytdlp.ytdlp.DownloadOption
import com.example.ytdlp.ytdlp.DownloadRequest
import com.example.ytdlp.ytdlp.DownloadStage
import com.example.ytdlp.ytdlp.VideoInfo
import com.example.ytdlp.ytdlp.YtDlpManager
import kotlinx.coroutines.launch

private enum class DownloadSheetTab {
    VIDEO,
    AUDIO
}

private data class DownloadSheetStrings(
    val chooseFormat: String,
    val video: String,
    val audio: String,
    val cancel: String,
    val startsInBackground: String,
    val noVideoFormats: String,
    val noAudioFormats: String,
    val download: String,
    val sizeUnknown: String
)

@Composable
private fun isHomeLightTheme(): Boolean {
    return MaterialTheme
        .colorScheme
        .background
        .luminance() > 0.5f
}

@Composable
private fun homePrimaryText(): Color {
    return if (isHomeLightTheme()) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color.White
    }
}

@Composable
private fun homeSecondaryText(
    darkAlpha: Float = 0.55f
): Color {
    return if (isHomeLightTheme()) {
        MaterialTheme
            .colorScheme
            .onBackground
            .copy(
                alpha = 0.62f
            )
    } else {
        Color.White.copy(
            alpha = darkAlpha
        )
    }
}

@Composable
private fun homeInnerSurface(
    darkAlpha: Float = 0.20f
): Color {
    return if (isHomeLightTheme()) {
        Color(0xFFEDF2EE).copy(
            alpha = 0.88f
        )
    } else {
        Color.Black.copy(
            alpha = darkAlpha
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    sharedUrl: String? = null,
    uiState: HomeUiState
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val backgroundState by
    DownloadServiceState.state.collectAsState()

    val sheetStrings =
        DownloadSheetStrings(
            chooseFormat =
                stringResource(
                    R.string.choose_download_format
                ),
            video =
                stringResource(
                    R.string.video
                ),
            audio =
                stringResource(
                    R.string.audio
                ),
            cancel =
                stringResource(
                    R.string.cancel
                ),
            startsInBackground =
                stringResource(
                    R.string.starts_in_background
                ),
            noVideoFormats =
                stringResource(
                    R.string.no_video_formats
                ),
            noAudioFormats =
                stringResource(
                    R.string.no_audio_formats
                ),
            download =
                stringResource(
                    R.string.download
                ),
            sizeUnknown =
                stringResource(
                    R.string.size_unknown
                )
        )

    var initialized by remember {
        mutableStateOf(false)
    }

    var initializing by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        try {
            YtDlpManager.initialize(
                context.applicationContext
            )

            initialized = true
        } catch (error: Throwable) {
            Log.e(
                "SYDOWN",
                "Initialization failed",
                error
            )

            uiState.errorMessage =
                SyDownFriendlyErrors
                    .message(
                        context,
                        error
                    )
        } finally {
            initializing = false
        }
    }

    LaunchedEffect(
        sharedUrl,
        initialized,
        backgroundState.isRunning
    ) {
        val incomingUrl =
            sharedUrl
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return@LaunchedEffect

        if (!initialized) {
            return@LaunchedEffect
        }

        if (backgroundState.isRunning) {
            return@LaunchedEffect
        }

        if (
            uiState.handledSharedUrl ==
            incomingUrl
        ) {
            return@LaunchedEffect
        }

        uiState.handledSharedUrl =
            incomingUrl

        uiState.setIncomingUrl(
            incomingUrl
        )

        DownloadServiceState
            .clearFinishedState()

        uiState.analyzing =
            true

        try {
            uiState.videoInfo =
                YtDlpManager
                    .getVideoInfo(
                        incomingUrl
                    )
        } catch (error: Throwable) {
            Log.e(
                "SYDOWN",
                "Shared URL analysis failed",
                error
            )

            uiState.errorMessage =
                SyDownFriendlyErrors
                    .message(
                        context,
                        error
                    )
        } finally {
            uiState.analyzing =
                false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 26.dp,
                    bottom = 18.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(18.dp)
    ) {
        CenteredGlassHeader()

        GlassLinkCard(
            url =
                uiState.url,
            onUrlChanged = {
                uiState.updateUrl(it)
            },
            onPaste = {
                val clipboard =
                    context.getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as? ClipboardManager

                val text =
                    clipboard
                        ?.primaryClip
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                if (text.isNotBlank()) {
                    uiState.updateUrl(
                        text
                    )
                }
            },
            analyzing =
                uiState.analyzing,
            initializing =
                initializing,
            analyzeEnabled =
                initialized &&
                        !backgroundState.isRunning,
            onAnalyze = {
                uiState.analyzing =
                    true

                uiState.videoInfo =
                    null

                uiState.errorMessage =
                    null

                if (!backgroundState.isRunning) {
                    DownloadServiceState
                        .clearFinishedState()
                }

                scope.launch {
                    try {
                        uiState.videoInfo =
                            YtDlpManager
                                .getVideoInfo(
                                    uiState.url.trim()
                                )
                    } catch (error: Throwable) {
                        Log.e(
                            "SYDOWN",
                            "Analysis failed",
                            error
                        )

                        uiState.errorMessage =
                            SyDownFriendlyErrors
                                .message(
                                    context,
                                    error
                                )
                    } finally {
                        uiState.analyzing =
                            false
                    }
                }
            }
        )

        uiState.errorMessage
            ?.let { message ->
                GlassErrorCard(
                    message
                )
            }

        GlassPlatformsSection()

        uiState.videoInfo
            ?.let { info ->
                GlassMediaCard(
                    info =
                        info,
                    downloadState =
                        backgroundState,
                    onDownloadClick = {
                        uiState.showDownloadSheet =
                            true
                    },
                    onCancel = {
                        try {
                            context.startService(
                                DownloadService
                                    .createCancelIntent(
                                        context
                                    )
                            )
                        } catch (error: Throwable) {
                            Log.e(
                                "SYDOWN",
                                "Could not cancel download",
                                error
                            )

                            uiState.errorMessage =
                                SyDownFriendlyErrors
                                    .message(
                                        context,
                                        error
                                    )
                        }
                    }
                )
            }

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )
    }

    val currentVideoInfo =
        uiState.videoInfo

    if (
        uiState.showDownloadSheet &&
        currentVideoInfo != null
    ) {
        GlassDownloadSheet(
            info =
                currentVideoInfo,
            strings =
                sheetStrings,
            onDismiss = {
                uiState.showDownloadSheet =
                    false
            },
            onVideoSelected = { option ->
                try {
                    val request =
                        DownloadRequest.from(
                            url =
                                uiState.url.trim(),
                            option =
                                option,
                            videoInfo =
                                currentVideoInfo
                        )

                    DownloadServiceState
                        .clearFinishedState()

                    val intent =
                        DownloadService
                            .createStartIntent(
                                context.applicationContext,
                                request
                            )

                    ContextCompat
                        .startForegroundService(
                            context.applicationContext,
                            intent
                        )

                    uiState.showDownloadSheet =
                        false
                } catch (error: Throwable) {
                    Log.e(
                        "SYDOWN",
                        "Could not start video download",
                        error
                    )

                    uiState.errorMessage =
                        SyDownFriendlyErrors
                            .message(
                                context,
                                error
                            )

                    uiState.showDownloadSheet =
                        false
                }
            },
            onAudioSelected = { option ->
                try {
                    val request =
                        DownloadRequest.from(
                            url =
                                uiState.url.trim(),
                            option =
                                option,
                            videoInfo =
                                currentVideoInfo
                        )

                    DownloadServiceState
                        .clearFinishedState()

                    val intent =
                        DownloadService
                            .createStartIntent(
                                context.applicationContext,
                                request
                            )

                    ContextCompat
                        .startForegroundService(
                            context.applicationContext,
                            intent
                        )

                    uiState.showDownloadSheet =
                        false
                } catch (error: Throwable) {
                    Log.e(
                        "SYDOWN",
                        "Could not start audio download",
                        error
                    )

                    uiState.errorMessage =
                        SyDownFriendlyErrors
                            .message(
                                context,
                                error
                            )

                    uiState.showDownloadSheet =
                        false
                }
            }
        )
    }
}

@Composable
private fun CenteredGlassHeader() {
    val nameBrush =
        Brush.horizontalGradient(
            colors =
                listOf(
                    Color(0xFF35F4C2),
                    Color(0xFF00E58A),
                    GlassGreen,
                    Color(0xFF00A95A)
                )
        )

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(7.dp)
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {
            Image(
                painter =
                    painterResource(
                        R.drawable.ic_sydown_brand
                    ),
                contentDescription =
                    "SyDown",
                modifier =
                    Modifier.size(54.dp),
                contentScale =
                    ContentScale.Fit
            )

            Text(
                text =
                    "SyDown",
                style =
                    TextStyle(
                        brush =
                            nameBrush,
                        fontSize =
                            27.sp,
                        fontWeight =
                            FontWeight.Black,
                        textDirection =
                            TextDirection.Ltr
                    )
            )
        }

        Text(
            text =
                if (
                    BuildConfig.IS_OWNER_BUILD
                ) {
                    "OWNER • MEDIA DOWNLOADER"
                } else {
                    "MEDIA DOWNLOADER"
                },
            color =
                if (isHomeLightTheme()) {
                    Color(0xFF007D45)
                } else {
                    GlassGreen.copy(
                        alpha = 0.88f
                    )
                },
            fontSize =
                9.sp,
            fontWeight =
                FontWeight.Bold,
            letterSpacing =
                1.15.sp,
            textAlign =
                TextAlign.Center
        )
    }
}

@Composable
private fun GlassLinkCard(
    url: String,
    onUrlChanged: (String) -> Unit,
    onPaste: () -> Unit,
    analyzing: Boolean,
    initializing: Boolean,
    analyzeEnabled: Boolean,
    onAnalyze: () -> Unit
) {
    val lightTheme =
        isHomeLightTheme()

    val primaryText =
        homePrimaryText()

    SyDownGlassCard(
        modifier =
            Modifier.fillMaxWidth(),
        radius =
            26.dp,
        strong =
            true
    ) {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(13.dp)
        ) {
            Text(
                text =
                    stringResource(
                        R.string.paste_media_link
                    ),
                color =
                    primaryText,
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    15.sp
            )

            OutlinedTextField(
                value =
                    url,
                onValueChange =
                    onUrlChanged,
                modifier =
                    Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text =
                            "https://...",
                        color =
                            if (lightTheme) {
                                MaterialTheme
                                    .colorScheme
                                    .onBackground
                                    .copy(
                                        alpha = 0.38f
                                    )
                            } else {
                                Color.White.copy(
                                    alpha = 0.38f
                                )
                            }
                    )
                },
                singleLine =
                    true,
                shape =
                    RoundedCornerShape(18.dp),
                colors =
                    OutlinedTextFieldDefaults
                        .colors(
                            focusedTextColor =
                                primaryText,
                            unfocusedTextColor =
                                primaryText,
                            cursorColor =
                                GlassGreen,
                            focusedBorderColor =
                                GlassGreen.copy(
                                    alpha = 0.85f
                                ),
                            unfocusedBorderColor =
                                if (lightTheme) {
                                    Color(0xFF597063).copy(
                                        alpha = 0.28f
                                    )
                                } else {
                                    Color.White.copy(
                                        alpha = 0.12f
                                    )
                                },
                            focusedContainerColor =
                                if (lightTheme) {
                                    Color(0xFFE7EDE8).copy(
                                        alpha = 0.82f
                                    )
                                } else {
                                    Color.Black.copy(
                                        alpha = 0.18f
                                    )
                                },
                            unfocusedContainerColor =
                                if (lightTheme) {
                                    Color(0xFFE7EDE8).copy(
                                        alpha = 0.76f
                                    )
                                } else {
                                    Color.Black.copy(
                                        alpha = 0.14f
                                    )
                                }
                        ),
                trailingIcon = {
                    IconButton(
                        onClick =
                            onPaste
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded.ContentPaste,
                            contentDescription =
                                stringResource(
                                    R.string.paste
                                ),
                            tint =
                                if (lightTheme) {
                                    Color(0xFF009A51)
                                } else {
                                    GlassGreen
                                }
                        )
                    }
                }
            )

            Button(
                onClick =
                    onAnalyze,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                enabled =
                    analyzeEnabled &&
                            !initializing &&
                            !analyzing &&
                            url.isNotBlank(),
                shape =
                    RoundedCornerShape(
                        18.dp
                    ),
                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                GlassGreen,
                            contentColor =
                                Color.Black,
                            disabledContainerColor =
                                if (lightTheme) {
                                    Color(0xFFCDD5CF)
                                } else {
                                    Color.White.copy(
                                        alpha = 0.07f
                                    )
                                },
                            disabledContentColor =
                                if (lightTheme) {
                                    Color(0xFF657069)
                                } else {
                                    Color.White.copy(
                                        alpha = 0.28f
                                    )
                                }
                        )
            ) {
                if (
                    initializing ||
                    analyzing
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                19.dp
                            ),
                        strokeWidth =
                            2.dp,
                        color =
                            if (
                                analyzeEnabled
                            ) {
                                Color.Black
                            } else {
                                if (lightTheme) {
                                    Color(0xFF657069)
                                } else {
                                    Color.White.copy(
                                        alpha = 0.4f
                                    )
                                }
                            }
                    )

                    Spacer(
                        modifier =
                            Modifier.size(8.dp)
                    )
                }

                Icon(
                    imageVector =
                        Icons.Rounded.Search,
                    contentDescription =
                        null
                )

                Text(
                    modifier =
                        Modifier.padding(
                            start = 8.dp
                        ),
                    text =
                        if (analyzing) {
                            stringResource(
                                R.string.analyzing
                            )
                        } else {
                            stringResource(
                                R.string.analyze
                            )
                        },
                    fontWeight =
                        FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun GlassPlatformsSection() {
    val primaryText =
        homePrimaryText()

    val secondaryText =
        homeSecondaryText(
            darkAlpha = 0.50f
        )

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(11.dp)
    ) {
        Text(
            modifier =
                Modifier.fillMaxWidth(),
            text =
                stringResource(
                    R.string.supported_platforms
                ),
            color =
                primaryText,
            style =
                MaterialTheme
                    .typography
                    .titleMedium,
            fontWeight =
                FontWeight.Black,
            textAlign =
                TextAlign.Center
        )

        SyDownGlassCard(
            modifier =
                Modifier.fillMaxWidth(),
            radius =
                24.dp
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 7.dp,
                            vertical = 13.dp
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        2.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                GlassPlatformItem(
                    modifier =
                        Modifier.weight(1f),
                    iconRes =
                        R.drawable.ic_platform_youtube,
                    name =
                        stringResource(
                            R.string.youtube
                        )
                )

                GlassPlatformItem(
                    modifier =
                        Modifier.weight(1f),
                    iconRes =
                        R.drawable.ic_platform_facebook,
                    name =
                        stringResource(
                            R.string.facebook
                        )
                )

                GlassPlatformItem(
                    modifier =
                        Modifier.weight(1f),
                    iconRes =
                        R.drawable.ic_platform_instagram,
                    name =
                        stringResource(
                            R.string.instagram
                        )
                )

                GlassPlatformItem(
                    modifier =
                        Modifier.weight(1f),
                    iconRes =
                        R.drawable.ic_platform_tiktok,
                    name =
                        stringResource(
                            R.string.tiktok
                        )
                )

                GlassPlatformItem(
                    modifier =
                        Modifier.weight(1f),
                    iconRes =
                        R.drawable.ic_platform_telegram,
                    name =
                        stringResource(
                            R.string.telegram
                        )
                )
            }
        }

        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp
                    ),
            text =
                stringResource(
                    R.string.supported_platforms_note
                ),
            color =
                secondaryText,
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            textAlign =
                TextAlign.Center
        )
    }
}

@Composable
private fun GlassPlatformItem(
    modifier: Modifier,
    iconRes: Int,
    name: String
) {
    val lightTheme =
        isHomeLightTheme()

    Column(
        modifier =
            modifier.padding(
                horizontal = 1.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(
                        RoundedCornerShape(
                            14.dp
                        )
                    )
                    .background(
                        if (lightTheme) {
                            Color(0xFFE8EEE9)
                        } else {
                            Color.Black.copy(
                                alpha = 0.18f
                            )
                        }
                    )
                    .syDownGlassBorder(
                        radius = 14.dp,
                        selected = true
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                painter =
                    painterResource(
                        iconRes
                    ),
                contentDescription =
                    name,
                modifier =
                    Modifier.size(
                        22.dp
                    ),
                tint =
                    if (lightTheme) {
                        Color(0xFF009A51)
                    } else {
                        GlassGreen
                    }
            )
        }

        Text(
            modifier =
                Modifier.fillMaxWidth(),
            text =
                name,
            color =
                if (lightTheme) {
                    MaterialTheme
                        .colorScheme
                        .onBackground
                        .copy(
                            alpha = 0.80f
                        )
                } else {
                    Color.White.copy(
                        alpha = 0.82f
                    )
                },
            fontSize =
                9.sp,
            fontWeight =
                FontWeight.SemiBold,
            textAlign =
                TextAlign.Center,
            maxLines =
                1,
            overflow =
                TextOverflow.Clip
        )
    }
}

@Composable
private fun GlassMediaCard(
    info: VideoInfo,
    downloadState: BackgroundDownloadState,
    onDownloadClick: () -> Unit,
    onCancel: () -> Unit
) {
    SyDownGlassCard(
        modifier =
            Modifier.fillMaxWidth(),
        radius =
            28.dp,
        strong =
            true
    ) {
        Column {
            if (
                !info.thumbnail
                    .isNullOrBlank()
            ) {
                Box {
                    AsyncImage(
                        model =
                            info.thumbnail,
                        contentDescription =
                            info.title,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    16f / 9f
                                ),
                        contentScale =
                            ContentScale.Crop
                    )

                    info.duration
                        ?.let { duration ->
                            Box(
                                modifier =
                                    Modifier
                                        .align(
                                            Alignment.BottomStart
                                        )
                                        .padding(
                                            12.dp
                                        )
                                        .clip(
                                            RoundedCornerShape(
                                                10.dp
                                            )
                                        )
                                        .background(
                                            Color.Black.copy(
                                                alpha = 0.72f
                                            )
                                        )
                                        .padding(
                                            horizontal = 9.dp,
                                            vertical = 5.dp
                                        )
                            ) {
                                Text(
                                    text =
                                        formatMediaDuration(
                                            duration
                                        ),
                                    color =
                                        Color.White,
                                    fontWeight =
                                        FontWeight.Bold,
                                    fontSize =
                                        12.sp
                                )
                            }
                        }
                }
            }

            Column(
                modifier =
                    Modifier.padding(
                        17.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                Text(
                    text =
                        info.title,
                    color =
                        homePrimaryText(),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Black,
                    maxLines =
                        2,
                    overflow =
                        TextOverflow.Ellipsis
                )

                info.uploader
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let { uploader ->
                        Text(
                            text =
                                uploader,
                            color =
                                homeSecondaryText(
                                    darkAlpha = 0.50f
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            maxLines =
                                1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }

                if (
                    downloadState.isRunning ||
                    downloadState.isCancelling ||
                    downloadState.completedSuccessfully ||
                    downloadState.cancelled ||
                    downloadState.errorMessage != null
                ) {
                    GlassInlineDownloadStatus(
                        state =
                            downloadState,
                        onCancel =
                            onCancel
                    )
                } else {
                    Button(
                        onClick =
                            onDownloadClick,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    54.dp
                                ),
                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        GlassGreen,
                                    contentColor =
                                        Color.Black
                                )
                    ) {
                        Icon(
                            imageVector =
                                Icons.Rounded.Download,
                            contentDescription =
                                null
                        )

                        Text(
                            modifier =
                                Modifier.padding(
                                    start = 8.dp
                                ),
                            text =
                                stringResource(
                                    R.string.download
                                ),
                            fontWeight =
                                FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassInlineDownloadStatus(
    state: BackgroundDownloadState,
    onCancel: () -> Unit
) {
    val lightTheme =
        isHomeLightTheme()

    val progress =
        state.progress

    val percent =
        progress?.safePercent
            ?: 0f

    val waiting =
        state.isRunning &&
                !state.isCancelling &&
                progress?.stage ==
                DownloadStage.DOWNLOADING &&
                percent <= 0f &&
                progress.speed.isNullOrBlank()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        19.dp
                    )
                )
                .background(
                    homeInnerSurface(
                        darkAlpha = 0.20f
                    )
                )
                .syDownGlassBorder(
                    19.dp
                )
                .padding(14.dp),
        verticalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            Text(
                modifier =
                    Modifier.weight(1f),
                text =
                    when {
                        state.isCancelling ->
                            stringResource(
                                R.string.cancel_download
                            )

                        state.completedSuccessfully ->
                            stringResource(
                                R.string.download_finished
                            )

                        state.cancelled ->
                            stringResource(
                                R.string.download_cancelled
                            )

                        state.errorMessage != null ->
                            state.errorMessage

                        else ->
                            stageText(
                                progress?.stage,
                                waiting
                            )
                    },
                color =
                    when {
                        state.completedSuccessfully ->
                            if (lightTheme) {
                                Color(0xFF007A3D)
                            } else {
                                GlassGreen
                            }

                        state.cancelled ||
                                state.errorMessage != null ->
                            GlassRed

                        else ->
                            homePrimaryText()
                    },
                fontWeight =
                    FontWeight.Bold
            )

            if (
                state.qualityLabel
                    .isNotBlank()
            ) {
                Text(
                    text =
                        state.qualityLabel,
                    color =
                        if (lightTheme) {
                            Color(0xFF007A3D)
                        } else {
                            GlassGreen
                        },
                    fontWeight =
                        FontWeight.Black
                )
            }
        }

        if (state.isRunning) {
            if (waiting) {
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(
                                CircleShape
                            ),
                    color =
                        GlassGreen,
                    trackColor =
                        if (lightTheme) {
                            Color(0xFFD0D9D2)
                        } else {
                            Color.White.copy(
                                alpha = 0.08f
                            )
                        }
                )
            } else {
                LinearProgressIndicator(
                    progress = {
                        percent / 100f
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(
                                CircleShape
                            ),
                    color =
                        GlassGreen,
                    trackColor =
                        if (lightTheme) {
                            Color(0xFFD0D9D2)
                        } else {
                            Color.White.copy(
                                alpha = 0.08f
                            )
                        }
                )
            }

            Text(
                text =
                    if (waiting) {
                        stringResource(
                            R.string.waiting_for_media
                        )
                    } else {
                        formatPercent(
                            percent
                        ) +
                                buildProgressSuffix(
                                    progress?.speed,
                                    progress?.etaSeconds
                                )
                    },
                color =
                    homeSecondaryText(
                        darkAlpha = 0.56f
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            TextButton(
                modifier =
                    Modifier.align(
                        Alignment.End
                    ),
                enabled =
                    !state.isCancelling,
                onClick =
                    onCancel
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.cancel_download
                        ),
                    color =
                        GlassRed,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassDownloadSheet(
    info: VideoInfo,
    strings: DownloadSheetStrings,
    onDismiss: () -> Unit,
    onVideoSelected: (DownloadOption) -> Unit,
    onAudioSelected: (AudioDownloadOption) -> Unit
) {
    val lightTheme =
        isHomeLightTheme()

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded =
                true
        )

    var selectedTab by remember {
        mutableStateOf(
            DownloadSheetTab.VIDEO
        )
    }

    val videoOptions =
        remember(info) {
            YtDlpManager
                .buildDownloadOptions(
                    info
                )
        }

    val audioOptions =
        remember(info) {
            YtDlpManager
                .buildAudioOptions(
                    info
                )
        }

    ModalBottomSheet(
        onDismissRequest =
            onDismiss,
        sheetState =
            sheetState,
        containerColor =
            if (lightTheme) {
                Color(0xFFF7FAF8)
            } else {
                Color(0xFF09100C)
            },
        contentColor =
            homePrimaryText(),
        scrimColor =
            Color.Black.copy(
                alpha =
                    if (lightTheme) {
                        0.38f
                    } else {
                        0.72f
                    }
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        bottom = 30.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    15.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    modifier =
                        Modifier.weight(1f),
                    text =
                        strings.chooseFormat,
                    color =
                        homePrimaryText(),
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Black
                )

                IconButton(
                    onClick =
                        onDismiss
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.Close,
                        contentDescription =
                            strings.cancel,
                        tint =
                            homePrimaryText()
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                GlassSheetTab(
                    modifier =
                        Modifier.weight(1f),
                    selected =
                        selectedTab ==
                                DownloadSheetTab.VIDEO,
                    icon =
                        Icons.Rounded.Movie,
                    text =
                        strings.video,
                    onClick = {
                        selectedTab =
                            DownloadSheetTab.VIDEO
                    }
                )

                GlassSheetTab(
                    modifier =
                        Modifier.weight(1f),
                    selected =
                        selectedTab ==
                                DownloadSheetTab.AUDIO,
                    icon =
                        Icons.Rounded.AudioFile,
                    text =
                        strings.audio,
                    onClick = {
                        selectedTab =
                            DownloadSheetTab.AUDIO
                    }
                )
            }

            Text(
                text =
                    strings.startsInBackground,
                color =
                    homeSecondaryText(
                        darkAlpha = 0.48f
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            HorizontalDivider(
                color =
                    if (lightTheme) {
                        Color(0xFF617168).copy(
                            alpha = 0.18f
                        )
                    } else {
                        Color.White.copy(
                            alpha = 0.08f
                        )
                    }
            )

            when (selectedTab) {
                DownloadSheetTab.VIDEO -> {
                    if (
                        videoOptions
                            .isEmpty()
                    ) {
                        Text(
                            text =
                                strings.noVideoFormats,
                            color =
                                homeSecondaryText(
                                    darkAlpha = 0.60f
                                )
                        )
                    } else {
                        videoOptions.forEach {
                                option ->
                            GlassDownloadChoice(
                                title =
                                    option.label,
                                subtitle =
                                    formatSheetFileSize(
                                        option.estimatedSize,
                                        strings.sizeUnknown
                                    ),
                                icon =
                                    Icons.Rounded.Movie,
                                downloadDescription =
                                    strings.download,
                                onClick = {
                                    onVideoSelected(
                                        option
                                    )
                                }
                            )
                        }
                    }
                }

                DownloadSheetTab.AUDIO -> {
                    if (
                        audioOptions
                            .isEmpty()
                    ) {
                        Text(
                            text =
                                strings.noAudioFormats,
                            color =
                                homeSecondaryText(
                                    darkAlpha = 0.60f
                                )
                        )
                    } else {
                        audioOptions.forEach {
                                option ->
                            GlassDownloadChoice(
                                title =
                                    option.label,
                                subtitle =
                                    formatSheetFileSize(
                                        option.estimatedSize,
                                        strings.sizeUnknown
                                    ),
                                icon =
                                    Icons.Rounded.AudioFile,
                                downloadDescription =
                                    strings.download,
                                onClick = {
                                    onAudioSelected(
                                        option
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassSheetTab(
    modifier: Modifier,
    selected: Boolean,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val lightTheme =
        isHomeLightTheme()

    val background =
        if (selected) {
            GlassGreen.copy(
                alpha = 0.90f
            )
        } else {
            if (lightTheme) {
                Color(0xFFE7EDE8)
            } else {
                Color.Black.copy(
                    alpha = 0.24f
                )
            }
        }

    val content =
        if (selected) {
            Color.Black
        } else {
            if (lightTheme) {
                MaterialTheme
                    .colorScheme
                    .onBackground
                    .copy(
                        alpha = 0.75f
                    )
            } else {
                Color.White.copy(
                    alpha = 0.72f
                )
            }
        }

    Row(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        18.dp
                    )
                )
                .background(
                    background
                )
                .syDownGlassBorder(
                    radius = 18.dp,
                    selected = selected
                )
                .clickable(
                    onClick = onClick
                )
                .padding(
                    vertical = 14.dp
                ),
        horizontalArrangement =
            Arrangement.Center,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Icon(
            imageVector =
                icon,
            contentDescription =
                null,
            tint =
                content
        )

        Text(
            modifier =
                Modifier.padding(
                    start = 7.dp
                ),
            text =
                text,
            color =
                content,
            fontWeight =
                FontWeight.Black
        )
    }
}

@Composable
private fun GlassDownloadChoice(
    title: String,
    subtitle: String,
    icon: ImageVector,
    downloadDescription: String,
    onClick: () -> Unit
) {
    val lightTheme =
        isHomeLightTheme()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        19.dp
                    )
                )
                .background(
                    if (lightTheme) {
                        Color(0xFFEAF0EB).copy(
                            alpha = 0.90f
                        )
                    } else {
                        Color.Black.copy(
                            alpha = 0.25f
                        )
                    }
                )
                .syDownGlassBorder(
                    radius = 19.dp
                )
                .clickable(
                    onClick = onClick
                )
                .padding(15.dp),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                13.dp
            )
    ) {
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(
                        RoundedCornerShape(
                            13.dp
                        )
                    )
                    .background(
                        GlassGreen.copy(
                            alpha =
                                if (lightTheme) {
                                    0.11f
                                } else {
                                    0.13f
                                }
                        )
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    if (lightTheme) {
                        Color(0xFF008A48)
                    } else {
                        GlassGreen
                    }
            )
        }

        Column(
            modifier =
                Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(
                    3.dp
                )
        ) {
            Text(
                text =
                    title,
                color =
                    homePrimaryText(),
                fontWeight =
                    FontWeight.Black
            )

            Text(
                text =
                    subtitle,
                color =
                    homeSecondaryText(
                        darkAlpha = 0.47f
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        Icon(
            imageVector =
                Icons.Rounded.Download,
            contentDescription =
                downloadDescription,
            tint =
                if (lightTheme) {
                    Color(0xFF008A48)
                } else {
                    GlassGreen
                }
        )
    }
}

@Composable
private fun GlassErrorCard(
    message: String
) {
    SyDownGlassCard(
        modifier =
            Modifier.fillMaxWidth(),
        radius =
            18.dp
    ) {
        Text(
            modifier =
                Modifier.padding(16.dp),
            text =
                message,
            color =
                if (isHomeLightTheme()) {
                    Color(0xFFB00020)
                } else {
                    Color(0xFFFF6B76)
                },
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Composable
private fun stageText(
    stage: DownloadStage?,
    waiting: Boolean
): String {
    if (waiting) {
        return stringResource(
            R.string.starting_download
        )
    }

    return when (stage) {
        DownloadStage.PREPARING ->
            stringResource(
                R.string.preparing_download
            )

        DownloadStage.DOWNLOADING ->
            stringResource(
                R.string.downloading
            )

        DownloadStage.MERGING ->
            stringResource(
                R.string.merging
            )

        DownloadStage.SAVING ->
            stringResource(
                R.string.saving
            )

        DownloadStage.FINISHED ->
            stringResource(
                R.string.download_finished
            )

        null ->
            stringResource(
                R.string.download_in_progress
            )
    }
}

@Composable
private fun formatFileSize(
    bytes: Long?
): String {
    if (
        bytes == null ||
        bytes <= 0
    ) {
        return stringResource(
            R.string.size_unknown
        )
    }

    val mb =
        bytes.toDouble() /
                1024.0 /
                1024.0

    return "%.1f MB".format(
        mb
    )
}

private fun formatSheetFileSize(
    bytes: Long?,
    unknownText: String
): String {
    if (
        bytes == null ||
        bytes <= 0
    ) {
        return unknownText
    }

    val mb =
        bytes.toDouble() /
                1024.0 /
                1024.0

    return "%.1f MB".format(
        mb
    )
}

private fun formatMediaDuration(
    seconds: Long
): String {
    val hours =
        seconds / 3600

    val minutes =
        (seconds % 3600) / 60

    val remainingSeconds =
        seconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(
            hours,
            minutes,
            remainingSeconds
        )
    } else {
        "%d:%02d".format(
            minutes,
            remainingSeconds
        )
    }
}

private fun formatPercent(
    percent: Float
): String {
    return "%.1f%%".format(
        percent
    )
}

private fun buildProgressSuffix(
    speed: String?,
    etaSeconds: Long?
): String {
    val parts =
        mutableListOf<String>()

    speed
        ?.takeIf {
            it.isNotBlank()
        }
        ?.let {
            parts.add(it)
        }

    etaSeconds
        ?.takeIf {
            it >= 0
        }
        ?.let {
            parts.add(
                formatEta(it)
            )
        }

    return if (
        parts.isEmpty()
    ) {
        ""
    } else {
        " • " +
                parts.joinToString(
                    " • "
                )
    }
}

private fun formatEta(
    seconds: Long
): String {
    val hours =
        seconds / 3600

    val minutes =
        (seconds % 3600) / 60

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