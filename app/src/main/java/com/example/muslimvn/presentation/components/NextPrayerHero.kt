package com.example.muslimvn.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.util.HijriCalendarUtils
import com.example.muslimvn.domain.util.HijriMonthNames
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

@Composable
fun NextPrayerHero(
    prayerTimes: PrayerTimes,
    hijriOffset: Int,
    onCountdownFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var remainingMs by remember(prayerTimes.nextPrayerTime) {
        mutableLongStateOf(prayerTimes.nextPrayerTime.time - System.currentTimeMillis())
    }
    var refreshRequested by remember(prayerTimes.nextPrayerTime) { 
        mutableStateOf(value = false) 
    }

    // Progress calculation
    var progress by remember { mutableFloatStateOf(0f) }

    // Pulse animation for LIVE indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    LaunchedEffect(prayerTimes.nextPrayerTime, prayerTimes.previousPrayerTime) {
        while (true) {
            val now = System.currentTimeMillis()
            remainingMs = prayerTimes.nextPrayerTime.time - now
            
            val totalDuration = prayerTimes.nextPrayerTime.time - prayerTimes.previousPrayerTime.time
            val elapsed = now - prayerTimes.previousPrayerTime.time
            progress = if (totalDuration > 0) (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f

            if ((!refreshRequested) && (remainingMs <= 0L)) {
                refreshRequested = true
                onCountdownFinished()
            }
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val prayerTimeStr = remember(prayerTimes.nextPrayerTime) { timeFormat.format(prayerTimes.nextPrayerTime) }
    
    val prayerNameRes = remember(prayerTimes.nextPrayerName) {
        getPrayerNameRes(prayerTimes.nextPrayerName)
    }

    val prayerIcon = remember(prayerTimes.nextPrayerName) {
        getPrayerIcon(prayerTimes.nextPrayerName)
    }

    val backgroundImage = remember(prayerTimes.nextPrayerName) {
        getHeroPrayerImage(prayerTimes.nextPrayerName)
    }

    val gregorianDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val hijriLabel = remember(hijriOffset) {
        HijriCalendarUtils.hijriDateFor(LocalDate.now(), hijriOffset)?.let { hijri ->
            val day = hijri.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            val month = HijriMonthNames.monthName(hijri.get(java.time.temporal.ChronoField.MONTH_OF_YEAR))
            val year = hijri.get(java.time.temporal.ChronoField.YEAR)
            "$day $month $year AH"
        }.orEmpty()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        AsyncImage(
            model = backgroundImage,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Date Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = gregorianDate,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = hijriLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                // Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp).alpha(0.8f),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = prayerIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).alpha(0.6f),
                        tint = Color.White
                    )
                }
            }

            // Middle: Prayer Focus
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = prayerNameRes,
                    transitionSpec = {
                        fadeIn(tween(600)) togetherWith fadeOut(tween(600))
                    },
                    label = "nextPrayerName"
                ) { resId ->
                    Text(
                        text = stringResource(resId).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Light
                    )
                }
                
                Text(
                    text = prayerTimeStr,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Thin
                    ),
                    color = Color.White
                )

                // Countdown sub-text
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Tiếp theo trong ${formatRemaining(remainingMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom: Current Prayer Status (Live)
            if (prayerTimes.isCurrentPrayerActive && prayerTimes.currentPrayerName != null) {
                val currentPrayerRes = remember(prayerTimes.currentPrayerName) {
                    getPrayerNameRes(prayerTimes.currentPrayerName)
                }
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulse Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(pulseAlpha)
                                .background(Color(0xFF4CAF50), CircleShape) // Standard Success Green
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ĐANG TRONG GIỜ LỄ ${stringResource(currentPrayerRes).uppercase()}",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Progress Bar at the very bottom (Material 3 standard)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            trackColor = Color.White.copy(alpha = 0.15f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

private fun getHeroPrayerImage(prayerName: String): String {
    val base = "file:///android_asset/images/praytime/"
    return when {
        prayerName.contains("Fajr", ignoreCase = true) -> "${base}fajr.webp"
        prayerName.contains("Dhuhr", ignoreCase = true) -> "${base}dhuhr.jpg"
        prayerName.contains("Asr", ignoreCase = true) -> "${base}asr.jpg"
        prayerName.contains("Maghrib", ignoreCase = true) -> "${base}maghrib.jpg"
        prayerName.contains("Isha", ignoreCase = true) -> "${base}isha.jpg"
        else -> "${base}vietnammosque.jpg"
    }
}

private fun getPrayerNameRes(prayerName: String): Int {
    return when {
        prayerName.contains("Fajr", ignoreCase = true) -> R.string.prayer_fajr
        prayerName.contains("Sunrise", ignoreCase = true) -> R.string.prayer_sunrise
        prayerName.contains("Dhuhr", ignoreCase = true) -> R.string.prayer_dhuhr
        prayerName.contains("Asr", ignoreCase = true) -> R.string.prayer_asr
        prayerName.contains("Maghrib", ignoreCase = true) -> R.string.prayer_maghrib
        prayerName.contains("Isha", ignoreCase = true) -> R.string.prayer_isha
        else -> R.string.utility_prayer
    }
}

private fun getPrayerIcon(prayerName: String): ImageVector {
    return when {
        prayerName.contains("Fajr", ignoreCase = true) -> Icons.Default.NightsStay
        prayerName.contains("Sunrise", ignoreCase = true) -> Icons.Default.WbTwilight
        prayerName.contains("Dhuhr", ignoreCase = true) -> Icons.Default.WbSunny
        prayerName.contains("Asr", ignoreCase = true) -> Icons.Default.WbCloudy
        prayerName.contains("Maghrib", ignoreCase = true) -> Icons.Default.WbTwilight
        prayerName.contains("Isha", ignoreCase = true) -> Icons.Default.Bedtime
        else -> Icons.Default.Schedule
    }
}

private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}
