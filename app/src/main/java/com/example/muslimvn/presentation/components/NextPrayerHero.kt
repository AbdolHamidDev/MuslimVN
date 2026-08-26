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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PrayerTimes
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NextPrayerHero(
    prayerTimes: PrayerTimes,
    onCountdownFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var remainingMs by remember(prayerTimes.nextPrayerTime) {
        mutableLongStateOf(prayerTimes.nextPrayerTime.time - System.currentTimeMillis())
    }
    var refreshRequested by remember(prayerTimes.nextPrayerTime) { 
        mutableStateOf(value = false) 
    }

    LaunchedEffect(prayerTimes.nextPrayerTime) {
        while (true) {
            remainingMs = prayerTimes.nextPrayerTime.time - System.currentTimeMillis()
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
        getPrayerImage(prayerTimes.nextPrayerName)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp)
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
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Icon + Name + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = prayerIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedContent(
                        targetState = prayerNameRes,
                        transitionSpec = {
                            val offsetSpec = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)
                            val fadeSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                            (slideInVertically(offsetSpec) { it / 2 } + fadeIn(fadeSpec))
                                .togetherWith(slideOutVertically(offsetSpec) { -it / 2 } + fadeOut(fadeSpec))
                        },
                        label = "nextPrayerName"
                    ) { resId ->
                        Text(
                            text = stringResource(resId),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $prayerTimeStr",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Middle: Countdown
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatRemaining(remainingMs),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.countdown_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Bottom: Compact Pill
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nhấn để cài đặt lời nhắc",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
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

private fun getPrayerImage(prayerName: String): String {
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

private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}
