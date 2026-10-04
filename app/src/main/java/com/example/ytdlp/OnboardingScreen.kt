package com.example.ytdlp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassBlack,
                        Color(0xFF06130D),
                        GlassBlack
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassGreen.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = 24.dp,
                    bottom = 22.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(
                    R.drawable.ic_sydown_brand
                ),
                contentDescription = "SyDown",
                modifier = Modifier.size(112.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(
                    R.string.welcome_to_sydown
                ),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.welcome_subtitle
                ),
                color = Color.White.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            SyDownGlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                strong = true
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(
                            R.string.choose_language
                        ),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        LanguageCard(
                            modifier = Modifier.weight(1f),
                            title = "العربية",
                            selected =
                                selectedLanguage ==
                                        AppLanguage.ARABIC,
                            onClick = {
                                onLanguageSelected(
                                    AppLanguage.ARABIC
                                )
                            }
                        )

                        LanguageCard(
                            modifier = Modifier.weight(1f),
                            title = "English",
                            selected =
                                selectedLanguage ==
                                        AppLanguage.ENGLISH,
                            onClick = {
                                onLanguageSelected(
                                    AppLanguage.ENGLISH
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OnboardingInfoCard(
                title = stringResource(
                    R.string.local_download_title
                ),
                description = stringResource(
                    R.string.local_download_description
                ),
                accentColor = GlassGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            OnboardingInfoCard(
                title = stringResource(
                    R.string.responsible_use_title
                ),
                description = stringResource(
                    R.string.responsible_use_description
                ),
                accentColor = GlassRed
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                onClick = onGetStarted,
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassGreen,
                    contentColor = GlassBlack
                )
            ) {
                Text(
                    text = stringResource(
                        R.string.get_started
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun LanguageCard(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape =
        RoundedCornerShape(18.dp)

    val borderColor =
        if (selected) {
            GlassGreen.copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.12f)
        }

    val containerColor =
        if (selected) {
            GlassGreen.copy(alpha = 0.12f)
        } else {
            Color.White.copy(alpha = 0.035f)
        }

    Row(
        modifier = modifier
            .height(66.dp)
            .clip(shape)
            .background(containerColor)
            .border(
                width =
                    if (selected) {
                        1.5.dp
                    } else {
                        1.dp
                    },
                color = borderColor,
                shape = shape
            )
            .clickable(
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .border(
                    width = 1.5.dp,
                    color =
                        if (selected) {
                            GlassGreen
                        } else {
                            Color.White.copy(
                                alpha = 0.30f
                            )
                        },
                    shape = CircleShape
                )
                .padding(3.dp)
                .background(
                    color =
                        if (selected) {
                            GlassGreen
                        } else {
                            Color.Transparent
                        },
                    shape = CircleShape
                )
        )

        Text(
            text = title,
            color =
                if (selected) {
                    GlassGreen
                } else {
                    Color.White.copy(
                        alpha = 0.78f
                    )
                },
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
        )
    }
}

@Composable
private fun OnboardingInfoCard(
    title: String,
    description: String,
    accentColor: Color
) {
    SyDownGlassCard(
        modifier = Modifier.fillMaxWidth(),
        radius = 22.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement =
                Arrangement.spacedBy(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = 4.dp,
                        height = 48.dp
                    )
                    .background(
                        color = accentColor,
                        shape =
                            RoundedCornerShape(10.dp)
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style =
                        MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    color =
                        Color.White.copy(
                            alpha = 0.62f
                        ),
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}