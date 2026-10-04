package com.example.ytdlp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytdlp.owner.OwnerYtDlpTools
import kotlinx.coroutines.launch

private const val INSTAGRAM_URL =
    "https://www.instagram.com/osama.alaswad1/"

private const val FACEBOOK_URL =
    "https://web.facebook.com/profile.php?id=61594652085768"

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    languagePreference: AppLanguage,
    themeMode: AppThemeMode,
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeChanged: (AppThemeMode) -> Unit
) {
    val context =
        LocalContext.current

    val currentScreenLayoutDirection =
        LocalLayoutDirection.current

    val dialogLayoutDirection =
        when (languagePreference) {
            AppLanguage.ARABIC ->
                LayoutDirection.Rtl

            AppLanguage.ENGLISH ->
                LayoutDirection.Ltr

            AppLanguage.SYSTEM ->
                currentScreenLayoutDirection
        }

    var showLanguageDialog by remember {
        mutableStateOf(false)
    }

    var showThemeDialog by remember {
        mutableStateOf(false)
    }

    var showAboutDialog by remember {
        mutableStateOf(false)
    }

    var showContactDialog by remember {
        mutableStateOf(false)
    }

    val cancelText =
        stringResource(
            R.string.cancel
        )

    val doneText =
        stringResource(
            R.string.done
        )

    val versionText =
        stringResource(
            R.string.version
        )

    val localDownloadDescriptionText =
        stringResource(
            R.string.local_download_description
        )

    val chooseLanguageText =
        stringResource(
            R.string.choose_app_language
        )

    val languageSystemText =
        stringResource(
            R.string.language_system
        )

    val arabicText =
        stringResource(
            R.string.arabic
        )

    val englishText =
        stringResource(
            R.string.english
        )

    val chooseThemeText =
        stringResource(
            R.string.choose_theme
        )

    val themeSystemText =
        stringResource(
            R.string.theme_system
        )

    val themeDarkText =
        stringResource(
            R.string.theme_dark
        )

    val themeLightText =
        stringResource(
            R.string.theme_light
        )

    val contactUsTitleText =
        stringResource(
            R.string.contact_us_title
        )

    val contactUsMessageText =
        stringResource(
            R.string.contact_us_message
        )

    val facebookText =
        stringResource(
            R.string.open_facebook
        )

    val instagramText =
        stringResource(
            R.string.open_instagram
        )

    val languageValue =
        when (languagePreference) {
            AppLanguage.SYSTEM ->
                languageSystemText

            AppLanguage.ARABIC ->
                arabicText

            AppLanguage.ENGLISH ->
                englishText
        }

    val themeValue =
        when (themeMode) {
            AppThemeMode.SYSTEM ->
                themeSystemText

            AppThemeMode.DARK ->
                themeDarkText

            AppThemeMode.LIGHT ->
                themeLightText
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
                    horizontal = 18.dp,
                    vertical = 22.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        Text(
            modifier =
                Modifier.fillMaxWidth(),
            text =
                stringResource(
                    R.string.settings
                ),
            color =
                Color.White,
            fontSize = 27.sp,
            fontWeight =
                FontWeight.Black,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(2.dp)
        )

        AppIdentityCard()

        SettingsGroup {
            SettingsRow(
                icon =
                    Icons.Rounded.Language,
                title =
                    stringResource(
                        R.string.language
                    ),
                subtitle =
                    languageValue,
                onClick = {
                    showLanguageDialog =
                        true
                }
            )

            SettingDivider()

            SettingsRow(
                icon =
                    Icons.Rounded.DarkMode,
                title =
                    stringResource(
                        R.string.theme
                    ),
                subtitle =
                    themeValue,
                onClick = {
                    showThemeDialog =
                        true
                }
            )

            SettingDivider()

            SettingsRow(
                icon =
                    Icons.Rounded.Folder,
                title =
                    stringResource(
                        R.string.download_folder
                    ),
                subtitle =
                    stringResource(
                        R.string.download_folder_value
                    ),
                onClick = {
                    openDownloadFolder(
                        context = context
                    )
                }
            )

            SettingDivider()

            SettingsRow(
                icon =
                    Icons.Rounded.Notifications,
                title =
                    stringResource(
                        R.string.notifications
                    ),
                subtitle =
                    stringResource(
                        R.string.notifications_description
                    ),
                onClick = {
                    openNotificationSettings(
                        context = context
                    )
                }
            )

            SettingDivider()

            SettingsRow(
                icon =
                    Icons.Rounded.Info,
                title =
                    stringResource(
                        R.string.about
                    ),
                subtitle =
                    stringResource(
                        R.string.app_information
                    ),
                onClick = {
                    showAboutDialog =
                        true
                }
            )
        }

        SettingsGroup {
            SettingsRow(
                icon =
                    Icons.Rounded.Share,
                title =
                    stringResource(
                        R.string.share_app
                    ),
                subtitle =
                    stringResource(
                        R.string.share_app_description
                    )
            )

            SettingDivider()

            SettingsRow(
                icon =
                    Icons.Rounded.Star,
                title =
                    stringResource(
                        R.string.rate_app
                    ),
                subtitle =
                    stringResource(
                        R.string.rate_app_description
                    )
            )

            SettingDivider()

            SettingsRow(
                icon =
                    Icons.Rounded.SupportAgent,
                title =
                    stringResource(
                        R.string.contact_us
                    ),
                subtitle =
                    stringResource(
                        R.string.contact_us_description
                    ),
                onClick = {
                    showContactDialog =
                        true
                }
            )
        }

        if (BuildConfig.IS_OWNER_BUILD) {
            OwnerToolsCard()
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        MadeForSyriaText()

        Text(
            modifier =
                Modifier.fillMaxWidth(),
            text =
                "SyDown v${BuildConfig.VERSION_NAME}",
            color =
                Color.White.copy(
                    alpha = 0.48f
                ),
            fontSize = 12.sp,
            textAlign =
                TextAlign.Center
        )

        if (BuildConfig.IS_OWNER_BUILD) {
            Text(
                modifier =
                    Modifier.fillMaxWidth(),
                text =
                    "OWNER BUILD",
                color =
                    GlassGreen.copy(
                        alpha = 0.72f
                    ),
                fontSize = 10.sp,
                fontWeight =
                    FontWeight.Bold,
                letterSpacing = 1.2.sp,
                textAlign =
                    TextAlign.Center
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title =
                chooseLanguageText,
            options =
                listOf(
                    ChoiceItem(
                        value =
                            AppLanguage.SYSTEM,
                        label =
                            languageSystemText
                    ),
                    ChoiceItem(
                        value =
                            AppLanguage.ARABIC,
                        label =
                            arabicText
                    ),
                    ChoiceItem(
                        value =
                            AppLanguage.ENGLISH,
                        label =
                            englishText
                    )
                ),
            selected =
                languagePreference,
            cancelText =
                cancelText,
            layoutDirection =
                dialogLayoutDirection,
            onSelected = {
                onLanguageChanged(it)
                showLanguageDialog =
                    false
            },
            onDismiss = {
                showLanguageDialog =
                    false
            }
        )
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title =
                chooseThemeText,
            options =
                listOf(
                    ChoiceItem(
                        value =
                            AppThemeMode.SYSTEM,
                        label =
                            themeSystemText
                    ),
                    ChoiceItem(
                        value =
                            AppThemeMode.DARK,
                        label =
                            themeDarkText
                    ),
                    ChoiceItem(
                        value =
                            AppThemeMode.LIGHT,
                        label =
                            themeLightText
                    )
                ),
            selected =
                themeMode,
            cancelText =
                cancelText,
            layoutDirection =
                dialogLayoutDirection,
            onSelected = {
                onThemeChanged(it)
                showThemeDialog =
                    false
            },
            onDismiss = {
                showThemeDialog =
                    false
            }
        )
    }

    if (showAboutDialog) {
        SyDownAboutDialog(
            versionText =
                versionText,
            descriptionText =
                localDownloadDescriptionText,
            doneText =
                doneText,
            layoutDirection =
                dialogLayoutDirection,
            onDismiss = {
                showAboutDialog =
                    false
            }
        )
    }

    if (showContactDialog) {
        SyDownContactDialog(
            titleText =
                contactUsTitleText,
            messageText =
                contactUsMessageText,
            facebookText =
                facebookText,
            instagramText =
                instagramText,
            doneText =
                doneText,
            layoutDirection =
                dialogLayoutDirection,
            onFacebookClick = {
                openExternalLink(
                    context = context,
                    url = FACEBOOK_URL
                )
            },
            onInstagramClick = {
                openExternalLink(
                    context = context,
                    url = INSTAGRAM_URL
                )
            },
            onDismiss = {
                showContactDialog =
                    false
            }
        )
    }
}

private fun openDownloadFolder(
    context: Context
) {
    val authority =
        "com.android.externalstorage.documents"

    val syDownUri =
        DocumentsContract.buildDocumentUri(
            authority,
            "primary:Download/SyDown"
        )

    val downloadsUri =
        DocumentsContract.buildDocumentUri(
            authority,
            "primary:Download"
        )

    if (
        tryOpenFolder(
            context = context,
            uri = syDownUri
        )
    ) {
        return
    }

    tryOpenFolder(
        context = context,
        uri = downloadsUri
    )
}

private fun tryOpenFolder(
    context: Context,
    uri: Uri
): Boolean {
    val intent =
        Intent(
            Intent.ACTION_VIEW
        ).apply {
            setDataAndType(
                uri,
                DocumentsContract.Document.MIME_TYPE_DIR
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    return try {
        context.startActivity(
            intent
        )

        true
    } catch (
        ignored: Throwable
    ) {
        false
    }
}

private fun openNotificationSettings(
    context: Context
) {
    val notificationIntent =
        Intent(
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        ).apply {
            putExtra(
                Settings.EXTRA_APP_PACKAGE,
                context.packageName
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

    try {
        context.startActivity(
            notificationIntent
        )
    } catch (
        throwable: Throwable
    ) {
        val applicationDetailsIntent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data =
                    Uri.parse(
                        "package:${context.packageName}"
                    )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        try {
            context.startActivity(
                applicationDetailsIntent
            )
        } catch (
            ignored: Throwable
        ) {
        }
    }
}

private fun openExternalLink(
    context: Context,
    url: String
) {
    val intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

    try {
        context.startActivity(
            intent
        )
    } catch (
        ignored: Throwable
    ) {
    }
}

@Composable
private fun SyDownAboutDialog(
    versionText: String,
    descriptionText: String,
    doneText: String,
    layoutDirection: LayoutDirection,
    onDismiss: () -> Unit
) {
    val appDisplayName =
        if (BuildConfig.IS_OWNER_BUILD) {
            "SyDown Owner"
        } else {
            "SyDown"
        }

    SyDownSettingsDialog(
        onDismissRequest =
            onDismiss,
        layoutDirection =
            layoutDirection
    ) {
        SyDownGlassCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 30.dp
                    ),
            radius = 26.dp,
            strong = true
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,
                            vertical = 22.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(58.dp)
                            .clip(
                                RoundedCornerShape(
                                    18.dp
                                )
                            )
                            .background(
                                GlassGreen.copy(
                                    alpha = 0.10f
                                )
                            )
                            .syDownGlassBorder(
                                radius = 18.dp,
                                selected = true
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = "S",
                        color =
                            GlassGreen,
                        fontSize = 28.sp,
                        fontWeight =
                            FontWeight.Black
                    )
                }

                Text(
                    modifier =
                        Modifier.fillMaxWidth(),
                    text =
                        appDisplayName,
                    color =
                        Color.White,
                    fontSize = 21.sp,
                    fontWeight =
                        FontWeight.Black,
                    textAlign =
                        TextAlign.Center
                )

                Box(
                    modifier =
                        Modifier
                            .clip(
                                CircleShape
                            )
                            .background(
                                GlassGreen.copy(
                                    alpha = 0.09f
                                )
                            )
                            .syDownGlassBorder(
                                radius = 50.dp
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                ) {
                    Text(
                        text =
                            "$versionText ${BuildConfig.VERSION_NAME}",
                        color =
                            GlassGreen,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                HorizontalDivider(
                    color =
                        Color.White.copy(
                            alpha = 0.07f
                        )
                )

                Text(
                    modifier =
                        Modifier.fillMaxWidth(),
                    text =
                        descriptionText,
                    color =
                        Color.White.copy(
                            alpha = 0.68f
                        ),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign =
                        TextAlign.Center
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.End
                ) {
                    TextButton(
                        onClick =
                            onDismiss
                    ) {
                        Text(
                            text =
                                doneText,
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

@Composable
private fun SyDownContactDialog(
    titleText: String,
    messageText: String,
    facebookText: String,
    instagramText: String,
    doneText: String,
    layoutDirection: LayoutDirection,
    onFacebookClick: () -> Unit,
    onInstagramClick: () -> Unit,
    onDismiss: () -> Unit
) {
    SyDownSettingsDialog(
        onDismissRequest =
            onDismiss,
        layoutDirection =
            layoutDirection
    ) {
        SyDownGlassCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 30.dp
                    ),
            radius = 26.dp,
            strong = true
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,
                            vertical = 22.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(58.dp)
                            .clip(
                                RoundedCornerShape(
                                    18.dp
                                )
                            )
                            .background(
                                GlassGreen.copy(
                                    alpha = 0.10f
                                )
                            )
                            .syDownGlassBorder(
                                radius = 18.dp,
                                selected = true
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.SupportAgent,
                        contentDescription =
                            null,
                        tint =
                            GlassGreen,
                        modifier =
                            Modifier.size(29.dp)
                    )
                }

                Text(
                    modifier =
                        Modifier.fillMaxWidth(),
                    text =
                        titleText,
                    color =
                        Color.White,
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Black,
                    textAlign =
                        TextAlign.Center
                )

                Text(
                    modifier =
                        Modifier.fillMaxWidth(),
                    text =
                        messageText,
                    color =
                        Color.White.copy(
                            alpha = 0.64f
                        ),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign =
                        TextAlign.Center
                )

                HorizontalDivider(
                    color =
                        Color.White.copy(
                            alpha = 0.07f
                        )
                )

                ContactSocialRow(
                    badgeText = "f",
                    title =
                        facebookText,
                    subtitle =
                        "Facebook",
                    onClick =
                        onFacebookClick
                )

                ContactSocialRow(
                    badgeText = "◎",
                    title =
                        instagramText,
                    subtitle =
                        "@osama.alaswad1",
                    onClick =
                        onInstagramClick
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.End
                ) {
                    TextButton(
                        onClick =
                            onDismiss
                    ) {
                        Text(
                            text =
                                doneText,
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

@Composable
private fun ContactSocialRow(
    badgeText: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val layoutDirection =
        LocalLayoutDirection.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        17.dp
                    )
                )
                .background(
                    GlassGreen.copy(
                        alpha = 0.045f
                    )
                )
                .syDownGlassBorder(
                    radius = 17.dp
                )
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 13.dp,
                    vertical = 12.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .background(
                        GlassGreen.copy(
                            alpha = 0.10f
                        )
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text =
                    badgeText,
                color =
                    GlassGreen,
                fontSize = 21.sp,
                fontWeight =
                    FontWeight.Black
            )
        }

        Column(
            modifier =
                Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(
                    2.dp
                )
        ) {
            Text(
                text =
                    title,
                color =
                    Color.White.copy(
                        alpha = 0.94f
                    ),
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    subtitle,
                color =
                    Color.White.copy(
                        alpha = 0.47f
                    ),
                fontSize = 11.sp,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector =
                Icons.Rounded.ChevronRight,
            contentDescription =
                null,
            tint =
                Color.White.copy(
                    alpha = 0.28f
                ),
            modifier =
                Modifier
                    .size(18.dp)
                    .then(
                        if (
                            layoutDirection ==
                            LayoutDirection.Rtl
                        ) {
                            Modifier.graphicsLayer(
                                scaleX = -1f
                            )
                        } else {
                            Modifier
                        }
                    )
        )
    }
}

@Composable
private fun OwnerToolsCard() {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var currentVersion by remember {
        mutableStateOf<String?>(null)
    }

    var statusMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isWorking by remember {
        mutableStateOf(false)
    }

    fun checkVersion() {
        if (isWorking) {
            return
        }

        scope.launch {
            isWorking = true
            statusMessage = null

            try {
                currentVersion =
                    OwnerYtDlpTools
                        .getVersion(context)
            } catch (
                throwable: Throwable
            ) {
                statusMessage =
                    throwable.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "yt-dlp error"
            } finally {
                isWorking = false
            }
        }
    }

    fun updateYtDlp() {
        if (isWorking) {
            return
        }

        scope.launch {
            isWorking = true
            statusMessage = null

            try {
                val result =
                    OwnerYtDlpTools
                        .update(context)

                currentVersion =
                    result.currentVersion

                statusMessage =
                    if (result.changed) {
                        "${result.previousVersion}  →  ${result.currentVersion}"
                    } else {
                        result.currentVersion
                    }
            } catch (
                throwable: Throwable
            ) {
                statusMessage =
                    throwable.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "yt-dlp error"
            } finally {
                isWorking = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isWorking) {
            isWorking = true

            try {
                currentVersion =
                    OwnerYtDlpTools
                        .getVersion(context)
            } catch (
                throwable: Throwable
            ) {
                statusMessage =
                    throwable.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "yt-dlp error"
            } finally {
                isWorking = false
            }
        }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 4.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.Terminal,
                contentDescription =
                    null,
                tint =
                    GlassGreen,
                modifier =
                    Modifier.size(18.dp)
            )

            Text(
                text =
                    stringResource(
                        R.string.owner_tools
                    ),
                color =
                    Color.White.copy(
                        alpha = 0.88f
                    ),
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        SyDownGlassCard(
            modifier =
                Modifier.fillMaxWidth(),
            radius = 22.dp,
            strong = false
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 15.dp,
                                vertical = 14.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(43.dp)
                                .clip(
                                    RoundedCornerShape(
                                        13.dp
                                    )
                                )
                                .background(
                                    GlassGreen.copy(
                                        alpha = 0.09f
                                    )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        21.dp
                                    ),
                                color =
                                    GlassGreen,
                                strokeWidth =
                                    2.dp
                            )
                        } else {
                            Icon(
                                imageVector =
                                    Icons.Rounded.Terminal,
                                contentDescription =
                                    null,
                                tint =
                                    GlassGreen,
                                modifier =
                                    Modifier.size(
                                        22.dp
                                    )
                            )
                        }
                    }

                    Column(
                        modifier =
                            Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                4.dp
                            )
                    ) {
                        Text(
                            text = "yt-dlp",
                            color =
                                Color.White.copy(
                                    alpha = 0.94f
                                ),
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                currentVersion
                                    ?: if (isWorking) {
                                        "..."
                                    } else {
                                        "—"
                                    },
                            color =
                                GlassGreen.copy(
                                    alpha = 0.88f
                                ),
                            fontSize = 12.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                SettingDivider()

                OwnerToolAction(
                    icon =
                        Icons.Rounded.Refresh,
                    title =
                        stringResource(
                            R.string.check_ytdlp_version
                        ),
                    enabled =
                        !isWorking,
                    onClick = {
                        checkVersion()
                    }
                )

                SettingDivider()

                OwnerToolAction(
                    icon =
                        Icons.Rounded.SystemUpdate,
                    title =
                        stringResource(
                            R.string.update_ytdlp
                        ),
                    enabled =
                        !isWorking,
                    onClick = {
                        updateYtDlp()
                    }
                )

                if (
                    !statusMessage.isNullOrBlank()
                ) {
                    SettingDivider()

                    Text(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 12.dp
                                ),
                        text =
                            statusMessage.orEmpty(),
                        color =
                            Color.White.copy(
                                alpha = 0.52f
                            ),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        maxLines = 5,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerToolAction(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) {
                        Modifier.clickable(
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    horizontal = 15.dp,
                    vertical = 13.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .background(
                        GlassGreen.copy(
                            alpha =
                                if (enabled) {
                                    0.085f
                                } else {
                                    0.035f
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
                    GlassGreen.copy(
                        alpha =
                            if (enabled) {
                                0.92f
                            } else {
                                0.35f
                            }
                    ),
                modifier =
                    Modifier.size(20.dp)
            )
        }

        Text(
            modifier =
                Modifier.weight(1f),
            text = title,
            color =
                Color.White.copy(
                    alpha =
                        if (enabled) {
                            0.88f
                        } else {
                            0.38f
                        }
                ),
            fontSize = 13.sp,
            fontWeight =
                FontWeight.SemiBold
        )

        Icon(
            imageVector =
                Icons.Rounded.ChevronRight,
            contentDescription =
                null,
            tint =
                Color.White.copy(
                    alpha =
                        if (enabled) {
                            0.26f
                        } else {
                            0.10f
                        }
                ),
            modifier =
                Modifier
                    .size(18.dp)
                    .then(
                        if (
                            LocalLayoutDirection.current ==
                            LayoutDirection.Rtl
                        ) {
                            Modifier.graphicsLayer(
                                scaleX = -1f
                            )
                        } else {
                            Modifier
                        }
                    )
        )
    }
}

@Composable
private fun AppIdentityCard() {
    SyDownGlassCard(
        modifier =
            Modifier.fillMaxWidth(),
        radius = 24.dp
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(
                            RoundedCornerShape(
                                19.dp
                            )
                        )
                        .background(
                            GlassGreen.copy(
                                alpha = 0.09f
                            )
                        )
                        .syDownGlassBorder(
                            radius = 19.dp
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text = "S",
                    color =
                        GlassGreen,
                    fontSize = 31.sp,
                    fontWeight =
                        FontWeight.Black
                )
            }

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text =
                        if (
                            BuildConfig.IS_OWNER_BUILD
                        ) {
                            "SyDown Owner"
                        } else {
                            "SyDown"
                        },
                    color =
                        Color.White,
                    fontSize = 19.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}",
                    color =
                        Color.White.copy(
                            alpha = 0.55f
                        ),
                    fontSize = 12.sp
                )

                if (
                    BuildConfig.IS_OWNER_BUILD
                ) {
                    Box(
                        modifier =
                            Modifier
                                .padding(
                                    top = 3.dp
                                )
                                .clip(CircleShape)
                                .background(
                                    GlassGreen.copy(
                                        alpha = 0.10f
                                    )
                                )
                                .syDownGlassBorder(
                                    radius = 50.dp
                                )
                                .padding(
                                    horizontal = 9.dp,
                                    vertical = 4.dp
                                )
                    ) {
                        Text(
                            text =
                                "OWNER BUILD",
                            color =
                                GlassGreen,
                            fontSize = 9.sp,
                            fontWeight =
                                FontWeight.Bold,
                            letterSpacing =
                                0.8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    content: @Composable () -> Unit
) {
    SyDownGlassCard(
        modifier =
            Modifier.fillMaxWidth(),
        radius = 22.dp
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val layoutDirection =
        LocalLayoutDirection.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 13.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(13.dp)
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
                            alpha = 0.09f
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
                    GlassGreen.copy(
                        alpha = 0.92f
                    ),
                modifier =
                    Modifier.size(22.dp)
            )
        }

        Column(
            modifier =
                Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                color =
                    Color.White.copy(
                        alpha = 0.94f
                    ),
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                color =
                    Color.White.copy(
                        alpha = 0.47f
                    ),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector =
                Icons.Rounded.ChevronRight,
            contentDescription =
                null,
            tint =
                Color.White.copy(
                    alpha = 0.28f
                ),
            modifier =
                Modifier
                    .size(19.dp)
                    .then(
                        if (
                            layoutDirection ==
                            LayoutDirection.Rtl
                        ) {
                            Modifier.graphicsLayer(
                                scaleX = -1f
                            )
                        } else {
                            Modifier
                        }
                    )
        )
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier =
            Modifier.padding(
                horizontal = 17.dp
            ),
        thickness = 1.dp,
        color =
            Color.White.copy(
                alpha = 0.055f
            )
    )
}

@Composable
private fun MadeForSyriaText() {
    val phrase =
        "صٍنْعُ بّـَ♥ لٍسْوُرٌيّاَ"

    val styledText =
        buildAnnotatedString {
            append(phrase)

            val heartIndex =
                phrase.indexOf('♥')

            if (heartIndex >= 0) {
                addStyle(
                    style =
                        SpanStyle(
                            color = GlassRed
                        ),
                    start =
                        heartIndex,
                    end =
                        heartIndex + 1
                )
            }
        }

    Text(
        modifier =
            Modifier.fillMaxWidth(),
        text =
            styledText,
        color =
            Color.White.copy(
                alpha = 0.78f
            ),
        fontSize = 13.sp,
        fontWeight =
            FontWeight.SemiBold,
        textAlign =
            TextAlign.Center
    )
}

private data class ChoiceItem<T>(
    val value: T,
    val label: String
)

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<ChoiceItem<T>>,
    selected: T,
    cancelText: String,
    layoutDirection: LayoutDirection,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    SyDownSettingsDialog(
        onDismissRequest =
            onDismiss,
        layoutDirection =
            layoutDirection
    ) {
        SyDownGlassCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 30.dp
                    ),
            radius = 24.dp,
            strong = true
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 20.dp
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
                        Color.White,
                    fontSize = 19.sp,
                    fontWeight =
                        FontWeight.Bold,
                    textAlign =
                        TextAlign.Center
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    16.dp
                                )
                            )
                            .background(
                                GlassGreen.copy(
                                    alpha = 0.035f
                                )
                            )
                            .syDownGlassBorder(
                                radius = 16.dp
                            )
                ) {
                    options.forEachIndexed {
                            index,
                            option ->

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(
                                            option.value
                                        )
                                    }
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 7.dp
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    option.value ==
                                            selected,
                                onClick = {
                                    onSelected(
                                        option.value
                                    )
                                },
                                colors =
                                    RadioButtonDefaults.colors(
                                        selectedColor =
                                            GlassGreen,
                                        unselectedColor =
                                            Color.White.copy(
                                                alpha = 0.45f
                                            )
                                    )
                            )

                            Text(
                                modifier =
                                    Modifier.padding(
                                        start = 8.dp
                                    ),
                                text =
                                    option.label,
                                color =
                                    Color.White.copy(
                                        alpha = 0.90f
                                    ),
                                fontSize = 14.sp
                            )
                        }

                        if (
                            index <
                            options.lastIndex
                        ) {
                            HorizontalDivider(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 14.dp
                                    ),
                                color =
                                    Color.White.copy(
                                        alpha = 0.055f
                                    )
                            )
                        }
                    }
                }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.End
                ) {
                    TextButton(
                        onClick =
                            onDismiss
                    ) {
                        Text(
                            text =
                                cancelText,
                            color =
                                GlassGreen,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}