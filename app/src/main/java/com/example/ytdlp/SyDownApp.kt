package com.example.ytdlp

import android.content.Context
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private enum class SyDownDestination {
    HOME,
    DOWNLOADS,
    SETTINGS
}

private data class NavigationDestination(
    val destination: SyDownDestination,
    val icon: ImageVector,
    val contentDescription: Int
)

private enum class AppUpdateUiStage {
    AVAILABLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    NEED_INSTALL_PERMISSION,
    ERROR
}

@Composable
fun SyDownApp(
    languagePreference: AppLanguage,
    themeMode: AppThemeMode,
    sharedUrl: String?,
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeChanged: (AppThemeMode) -> Unit
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var currentDestination by remember {
        mutableStateOf(
            SyDownDestination.HOME
        )
    }

    val homeUiState =
        remember {
            HomeUiState()
        }

    var availableUpdate by remember {
        mutableStateOf<AppUpdateInfo?>(null)
    }

    var updateStage by remember {
        mutableStateOf(
            AppUpdateUiStage.AVAILABLE
        )
    }

    var updateProgress by remember {
        mutableStateOf<Int?>(null)
    }

    var downloadedApk by remember {
        mutableStateOf<File?>(null)
    }

    /*
     * Public فقط يفحص تحديث APK العام.
     * أي خطأ شبكة يبقى صامتًا.
     */
    LaunchedEffect(Unit) {
        if (!BuildConfig.IS_OWNER_BUILD) {
            try {
                val updateInfo =
                    AppUpdateChecker.check()

                if (
                    AppUpdateChecker.isUpdateAvailable(
                        updateInfo
                    ) &&
                    updateInfo.hasDownloadUrl
                ) {
                    availableUpdate =
                        updateInfo

                    updateStage =
                        AppUpdateUiStage.AVAILABLE
                }
            } catch (
                ignored: Throwable
            ) {
            }
        }
    }

    /*
     * إذا تم فتح SyDown من Android Share Sheet
     * نضمن عرض الصفحة الرئيسية.
     */
    LaunchedEffect(sharedUrl) {
        if (
            !sharedUrl.isNullOrBlank()
        ) {
            currentDestination =
                SyDownDestination.HOME
        }
    }

    /*
     * عند الرجوع من شاشة "تثبيت تطبيقات غير معروفة"
     * نحاول فتح المثبت إذا أصبحت الصلاحية متاحة.
     */
    DisposableEffect(context) {
        val lifecycleOwner =
            context as? androidx.lifecycle.LifecycleOwner

        val observer =
            androidx.lifecycle.LifecycleEventObserver {
                    _,
                    event ->

                if (
                    event ==
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                    updateStage ==
                    AppUpdateUiStage.NEED_INSTALL_PERMISSION
                ) {
                    val apk =
                        downloadedApk

                    if (
                        apk != null &&
                        AppUpdateInstaller
                            .canRequestPackageInstalls(
                                context
                            )
                    ) {
                        updateStage =
                            AppUpdateUiStage.READY_TO_INSTALL

                        try {
                            AppUpdateInstaller.installApk(
                                context = context,
                                apkFile = apk
                            )
                        } catch (
                            ignored: Throwable
                        ) {
                            updateStage =
                                AppUpdateUiStage.ERROR
                        }
                    }
                }
            }

        lifecycleOwner
            ?.lifecycle
            ?.addObserver(observer)

        onDispose {
            lifecycleOwner
                ?.lifecycle
                ?.removeObserver(observer)
        }
    }

    val destinations =
        listOf(
            NavigationDestination(
                destination =
                    SyDownDestination.HOME,
                icon =
                    Icons.Rounded.Home,
                contentDescription =
                    R.string.home
            ),
            NavigationDestination(
                destination =
                    SyDownDestination.DOWNLOADS,
                icon =
                    Icons.Outlined.FileDownload,
                contentDescription =
                    R.string.downloads
            ),
            NavigationDestination(
                destination =
                    SyDownDestination.SETTINGS,
                icon =
                    Icons.Outlined.Settings,
                contentDescription =
                    R.string.settings
            )
        )

    SyDownGlassBackground(
        modifier =
            Modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier =
                Modifier.fillMaxSize(),
            containerColor =
                Color.Transparent,
            bottomBar = {
                SyDownBottomBar(
                    destinations =
                        destinations,
                    currentDestination =
                        currentDestination,
                    onDestinationSelected = {
                        currentDestination = it
                    }
                )
            }
        ) { innerPadding ->
            when (currentDestination) {
                SyDownDestination.HOME ->
                    HomeScreen(
                        contentPadding =
                            innerPadding,
                        sharedUrl =
                            sharedUrl,
                        uiState =
                            homeUiState
                    )

                SyDownDestination.DOWNLOADS ->
                    DownloadsScreen(
                        contentPadding =
                            innerPadding
                    )

                SyDownDestination.SETTINGS ->
                    SettingsScreen(
                        contentPadding =
                            innerPadding,
                        languagePreference =
                            languagePreference,
                        themeMode =
                            themeMode,
                        onLanguageChanged =
                            onLanguageChanged,
                        onThemeChanged =
                            onThemeChanged
                    )
            }
        }
    }

    availableUpdate?.let { updateInfo ->
        AppUpdateDialog(
            updateInfo =
                updateInfo,
            languagePreference =
                languagePreference,
            stage =
                updateStage,
            progress =
                updateProgress,
            onUpdateNow = {
                if (
                    updateStage ==
                    AppUpdateUiStage.DOWNLOADING
                ) {
                    return@AppUpdateDialog
                }

                updateStage =
                    AppUpdateUiStage.DOWNLOADING

                updateProgress = 0

                scope.launch {
                    try {
                        val apkFile =
                            AppUpdateInstaller
                                .downloadApk(
                                    context = context,
                                    apkUrl =
                                        updateInfo.apkUrl
                                ) { progress ->
                                    updateProgress =
                                        progress.percent
                                }

                        downloadedApk =
                            apkFile

                        updateProgress =
                            100

                        if (
                            AppUpdateInstaller
                                .canRequestPackageInstalls(
                                    context
                                )
                        ) {
                            updateStage =
                                AppUpdateUiStage.READY_TO_INSTALL

                            AppUpdateInstaller
                                .installApk(
                                    context = context,
                                    apkFile = apkFile
                                )
                        } else {
                            updateStage =
                                AppUpdateUiStage.NEED_INSTALL_PERMISSION

                            AppUpdateInstaller
                                .openUnknownAppsSettings(
                                    context
                                )
                        }
                    } catch (
                        ignored: Throwable
                    ) {
                        updateStage =
                            AppUpdateUiStage.ERROR
                    }
                }
            },
            onOpenInstallSettings = {
                try {
                    AppUpdateInstaller
                        .openUnknownAppsSettings(
                            context
                        )
                } catch (
                    ignored: Throwable
                ) {
                    updateStage =
                        AppUpdateUiStage.ERROR
                }
            },
            onInstall = {
                val apk =
                    downloadedApk

                if (apk != null) {
                    try {
                        if (
                            AppUpdateInstaller
                                .canRequestPackageInstalls(
                                    context
                                )
                        ) {
                            AppUpdateInstaller
                                .installApk(
                                    context = context,
                                    apkFile = apk
                                )
                        } else {
                            updateStage =
                                AppUpdateUiStage.NEED_INSTALL_PERMISSION

                            AppUpdateInstaller
                                .openUnknownAppsSettings(
                                    context
                                )
                        }
                    } catch (
                        ignored: Throwable
                    ) {
                        updateStage =
                            AppUpdateUiStage.ERROR
                    }
                }
            },
            onLater = {
                if (
                    updateStage !=
                    AppUpdateUiStage.DOWNLOADING
                ) {
                    availableUpdate = null
                }
            }
        )
    }
}

@Composable
private fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    languagePreference: AppLanguage,
    stage: AppUpdateUiStage,
    progress: Int?,
    onUpdateNow: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onInstall: () -> Unit,
    onLater: () -> Unit
) {
    val currentDirection =
        LocalLayoutDirection.current

    val dialogDirection =
        when (languagePreference) {
            AppLanguage.ARABIC ->
                LayoutDirection.Rtl

            AppLanguage.ENGLISH ->
                LayoutDirection.Ltr

            AppLanguage.SYSTEM ->
                currentDirection
        }

    val arabic =
        dialogDirection ==
                LayoutDirection.Rtl

    val title =
        if (arabic) {
            "تحديث جديد متوفر"
        } else {
            "New update available"
        }

    val versionLabel =
        if (arabic) {
            "الإصدار ${updateInfo.versionName}"
        } else {
            "Version ${updateInfo.versionName}"
        }

    val whatsNew =
        if (arabic) {
            "ما الجديد"
        } else {
            "What's new"
        }

    val notes =
        if (arabic) {
            updateInfo.releaseNotesAr
                .ifBlank {
                    updateInfo.releaseNotesEn
                }
        } else {
            updateInfo.releaseNotesEn
                .ifBlank {
                    updateInfo.releaseNotesAr
                }
        }

    val statusText =
        when (stage) {
            AppUpdateUiStage.AVAILABLE ->
                null

            AppUpdateUiStage.DOWNLOADING ->
                if (arabic) {
                    if (progress != null) {
                        "جارٍ تنزيل التحديث... $progress%"
                    } else {
                        "جارٍ تنزيل التحديث..."
                    }
                } else {
                    if (progress != null) {
                        "Downloading update... $progress%"
                    } else {
                        "Downloading update..."
                    }
                }

            AppUpdateUiStage.READY_TO_INSTALL ->
                if (arabic) {
                    "اكتمل التنزيل. التحديث جاهز للتثبيت."
                } else {
                    "Download complete. The update is ready to install."
                }

            AppUpdateUiStage.NEED_INSTALL_PERMISSION ->
                if (arabic) {
                    "اسمح لـ SyDown بتثبيت التطبيقات، ثم ارجع لإكمال التحديث."
                } else {
                    "Allow SyDown to install apps, then return to continue the update."
                }

            AppUpdateUiStage.ERROR ->
                if (arabic) {
                    "تعذر إكمال التحديث. تحقق من اتصال الإنترنت وحاول مرة أخرى."
                } else {
                    "The update could not be completed. Check your connection and try again."
                }
        }

    val primaryButtonText =
        when (stage) {
            AppUpdateUiStage.AVAILABLE ->
                if (arabic) {
                    "تحديث الآن"
                } else {
                    "Update now"
                }

            AppUpdateUiStage.DOWNLOADING ->
                if (arabic) {
                    "جارٍ التنزيل"
                } else {
                    "Downloading"
                }

            AppUpdateUiStage.READY_TO_INSTALL ->
                if (arabic) {
                    "تثبيت"
                } else {
                    "Install"
                }

            AppUpdateUiStage.NEED_INSTALL_PERMISSION ->
                if (arabic) {
                    "فتح الإعدادات"
                } else {
                    "Open settings"
                }

            AppUpdateUiStage.ERROR ->
                if (arabic) {
                    "إعادة المحاولة"
                } else {
                    "Try again"
                }
        }

    val later =
        if (arabic) {
            "لاحقًا"
        } else {
            "Later"
        }

    SyDownSettingsDialog(
        onDismissRequest = {
            if (
                stage !=
                AppUpdateUiStage.DOWNLOADING
            ) {
                onLater()
            }
        },
        layoutDirection =
            dialogDirection
    ) {
        SyDownGlassCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 28.dp
                    ),
            radius = 26.dp,
            strong = true
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 22.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    modifier =
                        Modifier.fillMaxWidth(),
                    text =
                        title,
                    color =
                        syDownPrimaryTextColor(),
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Black,
                    textAlign =
                        TextAlign.Center
                )

                Box(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterHorizontally
                            )
                            .clip(
                                RoundedCornerShape(
                                    50.dp
                                )
                            )
                            .background(
                                GlassGreen.copy(
                                    alpha = 0.10f
                                )
                            )
                            .syDownGlassBorder(
                                radius = 50.dp,
                                selected = true
                            )
                            .padding(
                                horizontal = 13.dp,
                                vertical = 6.dp
                            )
                ) {
                    Text(
                        text =
                            versionLabel,
                        color =
                            GlassGreen,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                if (notes.isNotBlank()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        18.dp
                                    )
                                )
                                .background(
                                    GlassGreen.copy(
                                        alpha = 0.05f
                                    )
                                )
                                .syDownGlassBorder(
                                    radius = 18.dp
                                )
                                .padding(15.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            modifier =
                                Modifier.fillMaxWidth(),
                            text =
                                whatsNew,
                            color =
                                syDownPrimaryTextColor(),
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            modifier =
                                Modifier.fillMaxWidth(),
                            text =
                                notes,
                            color =
                                syDownSecondaryTextColor(
                                    0.68f
                                ),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                if (
                    stage ==
                    AppUpdateUiStage.DOWNLOADING
                ) {
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = {
                                progress
                                    .coerceIn(
                                        0,
                                        100
                                    ) / 100f
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            50.dp
                                        )
                                    ),
                            color =
                                GlassGreen,
                            trackColor =
                                GlassGreen.copy(
                                    alpha = 0.13f
                                )
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            50.dp
                                        )
                                    ),
                            color =
                                GlassGreen,
                            trackColor =
                                GlassGreen.copy(
                                    alpha = 0.13f
                                )
                        )
                    }
                }

                if (statusText != null) {
                    Text(
                        modifier =
                            Modifier.fillMaxWidth(),
                        text =
                            statusText,
                        color =
                            if (
                                stage ==
                                AppUpdateUiStage.ERROR
                            ) {
                                GlassRed
                            } else {
                                syDownSecondaryTextColor(
                                    0.68f
                                )
                            },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign =
                            TextAlign.Center
                    )
                }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.End,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    if (
                        stage !=
                        AppUpdateUiStage.DOWNLOADING
                    ) {
                        TextButton(
                            onClick =
                                onLater
                        ) {
                            Text(
                                text =
                                    later,
                                color =
                                    syDownSecondaryTextColor(
                                        0.70f
                                    ),
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }

                    TextButton(
                        enabled =
                            stage !=
                                    AppUpdateUiStage.DOWNLOADING,
                        onClick = {
                            when (stage) {
                                AppUpdateUiStage.AVAILABLE,
                                AppUpdateUiStage.ERROR ->
                                    onUpdateNow()

                                AppUpdateUiStage.READY_TO_INSTALL ->
                                    onInstall()

                                AppUpdateUiStage.NEED_INSTALL_PERMISSION ->
                                    onOpenInstallSettings()

                                AppUpdateUiStage.DOWNLOADING ->
                                    Unit
                            }
                        }
                    ) {
                        if (
                            stage ==
                            AppUpdateUiStage.DOWNLOADING
                        ) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(17.dp),
                                color =
                                    GlassGreen,
                                strokeWidth =
                                    2.dp
                            )
                        } else {
                            Text(
                                text =
                                    primaryButtonText,
                                color =
                                    GlassGreen,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyDownBottomBar(
    destinations: List<NavigationDestination>,
    currentDestination: SyDownDestination,
    onDestinationSelected: (SyDownDestination) -> Unit
) {
    val lightTheme =
        MaterialTheme
            .colorScheme
            .background
            .luminance() > 0.5f

    val barShape =
        RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp
        )

    val barBrush =
        if (lightTheme) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        Color.White.copy(
                            alpha = 0.94f
                        ),
                        Color(0xFFF1F6F2).copy(
                            alpha = 0.98f
                        )
                    )
            )
        } else {
            Brush.verticalGradient(
                colors =
                    listOf(
                        Color(0xE60A100D),
                        Color(0xF2050806)
                    )
            )
        }

    val barBorderBrush =
        if (lightTheme) {
            Brush.horizontalGradient(
                colors =
                    listOf(
                        Color(0xFF65736A).copy(
                            alpha = 0.16f
                        ),
                        GlassGreen.copy(
                            alpha = 0.30f
                        ),
                        Color(0xFF65736A).copy(
                            alpha = 0.16f
                        )
                    )
            )
        } else {
            Brush.horizontalGradient(
                colors =
                    listOf(
                        Color.White.copy(
                            alpha = 0.08f
                        ),
                        GlassGreen.copy(
                            alpha = 0.22f
                        ),
                        Color.White.copy(
                            alpha = 0.08f
                        )
                    )
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(barShape)
                .background(
                    brush =
                        barBrush
                )
                .border(
                    width = 1.dp,
                    brush =
                        barBorderBrush,
                    shape =
                        barShape
                )
                .navigationBarsPadding()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            horizontalArrangement =
                Arrangement.SpaceEvenly,
            verticalAlignment =
                Alignment.Top
        ) {
            destinations.forEach { item ->
                GlassBottomNavigationItem(
                    icon =
                        item.icon,
                    label =
                        stringResource(
                            item.contentDescription
                        ),
                    selected =
                        currentDestination ==
                                item.destination,
                    lightTheme =
                        lightTheme,
                    onClick = {
                        onDestinationSelected(
                            item.destination
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun GlassBottomNavigationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    lightTheme: Boolean,
    onClick: () -> Unit
) {
    val inactiveColor =
        if (lightTheme) {
            MaterialTheme
                .colorScheme
                .onBackground
                .copy(
                    alpha = 0.68f
                )
        } else {
            Color.White.copy(
                alpha = 0.78f
            )
        }

    Column(
        modifier =
            Modifier
                .size(
                    width = 104.dp,
                    height = 72.dp
                )
                .clip(
                    RoundedCornerShape(
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
                .clickable(
                    onClick = onClick
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Box(
            modifier =
                Modifier
                    .size(
                        width = 42.dp,
                        height = 2.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 50.dp,
                            bottomEnd = 50.dp
                        )
                    )
                    .background(
                        if (selected) {
                            GlassGreen
                        } else {
                            Color.Transparent
                        }
                    )
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        top = 8.dp,
                        bottom = 7.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Icon(
                imageVector =
                    icon,
                contentDescription =
                    label,
                modifier =
                    Modifier.size(25.dp),
                tint =
                    if (selected) {
                        GlassGreen
                    } else {
                        inactiveColor
                    }
            )

            Text(
                modifier =
                    Modifier.padding(
                        top = 4.dp
                    ),
                text =
                    label,
                color =
                    if (selected) {
                        GlassGreen
                    } else {
                        inactiveColor.copy(
                            alpha =
                                if (lightTheme) {
                                    0.78f
                                } else {
                                    0.70f
                                }
                        )
                    },
                fontSize =
                    10.5.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
                textAlign =
                    TextAlign.Center,
                maxLines =
                    1
            )
        }
    }
}