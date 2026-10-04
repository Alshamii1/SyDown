package com.example.ytdlp

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun SyDownApp(
    languagePreference: AppLanguage,
    themeMode: AppThemeMode,
    sharedUrl: String?,
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeChanged: (AppThemeMode) -> Unit
) {
    var currentDestination by remember {
        mutableStateOf(
            SyDownDestination.HOME
        )
    }

    /*
     * إذا تم فتح SyDown من Android Share Sheet
     * نضمن عرض الصفحة الرئيسية حتى لو كان المستخدم
     * سابقًا في التنزيلات أو الإعدادات.
     */
    LaunchedEffect(sharedUrl) {
        if (
            !sharedUrl.isNullOrBlank()
        ) {
            currentDestination =
                SyDownDestination.HOME
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
                            sharedUrl
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
}

@Composable
private fun SyDownBottomBar(
    destinations: List<NavigationDestination>,
    currentDestination: SyDownDestination,
    onDestinationSelected: (SyDownDestination) -> Unit
) {
    val barShape =
        RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(barShape)
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color(0xE60A100D),
                                    Color(0xF2050806)
                                )
                        )
                )
                .border(
                    width = 1.dp,
                    brush =
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
                        ),
                    shape = barShape
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
    onClick: () -> Unit
) {
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
                    Modifier.size(
                        25.dp
                    ),
                tint =
                    if (selected) {
                        GlassGreen
                    } else {
                        Color.White.copy(
                            alpha = 0.78f
                        )
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
                        Color.White.copy(
                            alpha = 0.70f
                        )
                    },
                fontSize = 10.5.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
                textAlign =
                    TextAlign.Center,
                maxLines = 1
            )
        }
    }
}