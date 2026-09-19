package com.example.muslimvn.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.example.muslimvn.ui.theme.extendedColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import java.time.temporal.ChronoField
import java.util.Date
import java.util.Locale

@Composable
fun NextPrayerHero(
    prayerTimes: PrayerTimes,
    hijriOffset: Int,
    onCountdownFinished: () -> Unit,
    modifier: Modifier = Modifier,
    userLocation: String? = null,
    onMasjidClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
) {
    var refreshRequested by remember(prayerTimes.nextPrayerTime) {
        mutableStateOf(false)
    }

    // Pulse animation for LIVE indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    // Auto-refresh when next prayer time is reached
    LaunchedEffect(prayerTimes.nextPrayerTime) {
        while (true) {
            val now = System.currentTimeMillis()
            val remainingMs = prayerTimes.nextPrayerTime.time - now

            if (!refreshRequested && remainingMs <= 0L) {
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

    // Active prayer name for dynamic background image (prioritizes active current prayer over next prayer)
    val activePrayerNameForImage = remember(
        prayerTimes.currentPrayerName,
        prayerTimes.nextPrayerName,
        prayerTimes.isCurrentPrayerActive
    ) {
        if (prayerTimes.isCurrentPrayerActive && !prayerTimes.currentPrayerName.isNullOrEmpty()) {
            prayerTimes.currentPrayerName
        } else {
            prayerTimes.nextPrayerName
        }
    }

    val backgroundImage = remember(activePrayerNameForImage) {
        getHeroPrayerImage(activePrayerNameForImage)
    }

    val gregorianDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val hijriLabel = remember(hijriOffset) {
        HijriCalendarUtils.hijriDateFor(LocalDate.now(), hijriOffset)?.let { hijri ->
            val day = hijri.get(ChronoField.DAY_OF_MONTH)
            val month = HijriMonthNames.monthName(hijri.get(ChronoField.MONTH_OF_YEAR))
            val year = hijri.get(ChronoField.YEAR)
            "$day $month $year AH"
        }.orEmpty()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // Background Image
        AsyncImage(
            model = backgroundImage,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Scrim overlay for contrast & calm aesthetic
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Content layout
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP BAR: Date Information & Lottie Masjid Button with Thought Bubble
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Clickable Date Column -> Navigates to Hijri Calendar
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDateClick
                        )
                ) {
                    Text(
                        text = gregorianDate,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color.White.copy(alpha = 0.95f)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = hijriLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                letterSpacing = 0.3.sp
                            ),
                            color = Color.White.copy(alpha = 0.75f)
                        )

                        if (!userLocation.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(2.dp)
                                    .background(Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = userLocation,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 10.sp,
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. CENTER CONTENT: REFINED HIERARCHY (CURRENT PRAYER PRIMARY, NEXT PRAYER SECONDARY)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                if (prayerTimes.isCurrentPrayerActive && prayerTimes.currentPrayerName != null) {
                    val currentPrayerRes = remember(prayerTimes.currentPrayerName) {
                        getPrayerNameRes(prayerTimes.currentPrayerName)
                    }

                    // 1. Current Prayer Indicator (Subtle Live Badge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(pulseAlpha)
                                .background(MaterialTheme.extendedColors.success, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ĐANG TRONG GIỜ",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontSize = 11.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // 2. Current Prayer Name (PRIMARY FOCAL POINT)
                    AnimatedContent(
                        targetState = currentPrayerRes,
                        transitionSpec = {
                            fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                        },
                        label = "currentPrayerName"
                    ) { resId ->
                        Text(
                            text = stringResource(resId).uppercase(),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Next Prayer (SECONDARY INFORMATION PILL)
                    Surface(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = CircleShape,
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "GIỜ LỄ TIẾP THEO: ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    fontSize = 11.sp
                                ),
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "${stringResource(prayerNameRes).uppercase()} · $prayerTimeStr",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 12.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Fallback when no current prayer is active
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "GIỜ LỄ TIẾP THEO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${stringResource(prayerNameRes).uppercase()} · $prayerTimeStr",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NextPrayerHeroPreview() {
    val sampleTimes = PrayerTimes(
        fajr = Date(),
        sunrise = Date(),
        dhuhr = Date(),
        asr = Date(),
        maghrib = Date(),
        isha = Date(),
        nextPrayerName = "Fajr",
        nextPrayerTime = Date(System.currentTimeMillis() + 1000 * 60 * 45),
        nextPrayerCountdown = "00:45:00",
        previousPrayerTime = Date(System.currentTimeMillis() - 1000 * 60 * 120),
        currentPrayerName = "Isha",
        isCurrentPrayerActive = true
    )
    MaterialTheme {
        NextPrayerHero(
            prayerTimes = sampleTimes,
            hijriOffset = 0,
            onCountdownFinished = {}
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
