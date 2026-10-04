package com.example.ytdlp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SyDownSplashScreen() {
    val nameBrush =
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF35F4C2),
                Color(0xFF00E58A),
                GlassGreen,
                Color(0xFF00A95A)
            )
        )

    SyDownGlassBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Image(
                painter = painterResource(
                    R.drawable.ic_sydown_brand
                ),
                contentDescription = "SyDown",
                modifier = Modifier.size(132.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "SyDown",
                style = TextStyle(
                    brush = nameBrush,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    textDirection = TextDirection.Ltr
                )
            )
        }
    }
}