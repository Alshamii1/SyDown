package com.example.ytdlp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GlassBlack = Color(0xFF030605)
val GlassGreen = Color(0xFF00C866)
val GlassGreenDeep = Color(0xFF003D22)
val GlassRed = Color(0xFFCE1126)

val GlassSurface = Color(0x99121915)
val GlassSurfaceStrong = Color(0xC9101713)

val GlassBorder = Color(0x3336E989)
val GlassBorderSoft = Color(0x1FFFFFFF)

@Composable
fun SyDownGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseGradient =
        Brush.linearGradient(
            colors =
                listOf(
                    Color(0xFF002F1A),
                    Color(0xFF03130B),
                    GlassBlack,
                    Color(0xFF080605),
                    Color(0xFF160506)
                ),
            start =
                Offset(
                    x = 0f,
                    y = 0f
                ),
            end =
                Offset(
                    x = 1100f,
                    y = 2100f
                )
        )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(baseGradient)
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    GlassGreen.copy(
                                        alpha = 0.18f
                                    ),
                                    GlassGreen.copy(
                                        alpha = 0.05f
                                    ),
                                    Color.Transparent
                                ),
                            center =
                                Offset(
                                    x = 120f,
                                    y = 110f
                                ),
                            radius = 900f
                        )
                    )
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    GlassRed.copy(
                                        alpha = 0.11f
                                    ),
                                    GlassRed.copy(
                                        alpha = 0.025f
                                    ),
                                    Color.Transparent
                                ),
                            center =
                                Offset(
                                    x = 930f,
                                    y = 1750f
                                ),
                            radius = 1000f
                        )
                    )
        )

        content()
    }
}

@Composable
fun SyDownGlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    strong: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape =
        RoundedCornerShape(radius)

    Card(
        modifier = modifier,
        shape = shape,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (strong) {
                        GlassBorder
                    } else {
                        GlassBorderSoft
                    }
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (strong) {
                        GlassSurfaceStrong
                    } else {
                        GlassSurface
                    }
            ),
        content = content
    )
}

fun Modifier.syDownGlassBorder(
    radius: Dp = 20.dp,
    selected: Boolean = false
): Modifier {
    val shape =
        RoundedCornerShape(radius)

    return this.border(
        width =
            if (selected) {
                1.2.dp
            } else {
                1.dp
            },
        brush =
            if (selected) {
                Brush.linearGradient(
                    colors =
                        listOf(
                            GlassGreen.copy(
                                alpha = 0.78f
                            ),
                            GlassGreen.copy(
                                alpha = 0.20f
                            ),
                            Color.White.copy(
                                alpha = 0.08f
                            )
                        )
                )
            } else {
                Brush.linearGradient(
                    colors =
                        listOf(
                            Color.White.copy(
                                alpha = 0.10f
                            ),
                            GlassGreen.copy(
                                alpha = 0.13f
                            ),
                            Color.White.copy(
                                alpha = 0.04f
                            )
                        )
                )
            },
        shape = shape
    )
}

@Composable
fun glassGreenGradient(): Brush {
    return Brush.horizontalGradient(
        colors =
            listOf(
                MaterialTheme.colorScheme.primary,
                GlassGreen,
                Color(0xFF00A957)
            )
    )
}