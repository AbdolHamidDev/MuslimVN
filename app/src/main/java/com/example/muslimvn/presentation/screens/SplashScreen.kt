package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.muslimvn.R
import kotlinx.coroutines.delay

private val SplashBgColor = Color(0xFF055136)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.bismilla)
    )

    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = true,
        restartOnPlay = true,
        iterations = 1
    )

    LaunchedEffect(lottieProgress) {
        if (lottieProgress >= 0.99f) {
            delay(200)
            onSplashFinished()
        }
    }

    // Protection timeout to make sure navigation proceeds even if animation takes too long
    LaunchedEffect(Unit) {
        delay(3500)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieAnimation(
                composition = composition,
                progress = { lottieProgress },
                modifier = Modifier.size(320.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MuslimVN",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
