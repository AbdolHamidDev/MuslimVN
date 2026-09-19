package com.example.muslimvn.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.muslimvn.R
import com.example.muslimvn.ui.theme.BrandGreen
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

private val OnboardingBgColor = BrandGreen

data class OnboardingPageData(
    val title: String,
    val description: String,
    val lottieRawRes: Int
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "",
                description = "",
                lottieRawRes = R.raw.bismilla
            ),
            OnboardingPageData(
                title = "Tự Động Xác Định Vị Trí",
                description = "MuslimVN cần quyền vị trí để tự động tính chính xác Giờ Cầu Nguyện và Hướng Qibla tại khu vực của bạn.",
                lottieRawRes = R.raw.lottie_location
            ),
            OnboardingPageData(
                title = "Nhắc Nhở Giờ Cầu Nguyện",
                description = "Bật thông báo để nhận lời nhắc Adhan đúng giờ mỗi khi đến thời gian cầu nguyện trong ngày.",
                lottieRawRes = R.raw.lottie_notification
            ),
            OnboardingPageData(
                title = "Tất Cả Đã Sẵn Sàng!",
                description = "Mọi cấu hình ban đầu đã hoàn tất. Chúc bạn có trải nghiệm tuyệt vời cùng ứng dụng MuslimVN!",
                lottieRawRes = R.raw.lottie_success
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    var isBismillaFinished by remember { mutableStateOf(false) }
    var hasLocationPerm by remember { mutableStateOf(checkLocationPermission(context)) }
    var hasNotificationPerm by remember { mutableStateOf(checkNotificationPermission(context)) }

    val settingResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch { pagerState.animateScrollToPage(2) }
        } else {
            // User cancelled or failed to turn on GPS, but they gave permission,
            // so we still proceed to next page to not get stuck.
            coroutineScope.launch { pagerState.animateScrollToPage(2) }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPerm = isGranted || checkLocationPermission(context)
        if (isGranted) {
            checkAndRequestLocationSettings(
                context = context,
                onSettingsEnabled = {
                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                },
                onResolutionRequired = { intentSender ->
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    settingResultLauncher.launch(request)
                },
                onError = {
                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                }
            )
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPerm = isGranted || checkNotificationPermission(context)
        if (isGranted) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(3)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingBgColor)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with "Bỏ qua" button (chỉ hiện ở trang 1 Vị Trí và trang 2 Thông Báo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage in 1..2) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                if (pagerState.currentPage < pages.size - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    ) {
                        Text(
                            text = "Bỏ qua",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Pager Content (beyondViewportPageCount = 1 giúp preload trang liền kề, vuốt siêu mượt)
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f)
            ) { page ->
                val item = pages[page]
                val lottieSpec = remember(item.lottieRawRes) {
                    LottieCompositionSpec.RawRes(item.lottieRawRes)
                }
                val composition by rememberLottieComposition(spec = lottieSpec)

                val isCurrentPage by remember(page) {
                    derivedStateOf { pagerState.currentPage == page }
                }
                val iterations = if (page == 0 || page == 3) 1 else LottieConstants.IterateForever

                val lottieProgress by animateLottieCompositionAsState(
                    composition = composition,
                    isPlaying = isCurrentPage,
                    restartOnPlay = true,
                    iterations = iterations
                )

                // Đánh dấu khi bismilla (trang 0) chạy xong 1 lần
                LaunchedEffect(isCurrentPage, lottieProgress) {
                    if (page == 0 && isCurrentPage && lottieProgress >= 0.95f) {
                        isBismillaFinished = true
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val lottieSize = if (page == 0) 380.dp else 290.dp
                    LottieAnimation(
                        composition = composition,
                        progress = { lottieProgress },
                        modifier = Modifier.size(lottieSize)
                    )

                    if (item.title.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (item.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = item.description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            // Bottom Actions & Page Indicator
            val showBottomBar = if (pagerState.currentPage == 0) isBismillaFinished else true

            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(animationSpec = tween(600)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        repeat(pages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (isSelected) 24.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    // Primary Action Button per page
                    when (pagerState.currentPage) {
                        0 -> {
                            Button(
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                },
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = OnboardingBgColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Bắt Đầu",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        1 -> {
                            Button(
                                onClick = {
                                    if (hasLocationPerm) {
                                        checkAndRequestLocationSettings(
                                            context = context,
                                            onSettingsEnabled = {
                                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                            },
                                            onResolutionRequired = { intentSender ->
                                                val request = IntentSenderRequest.Builder(intentSender).build()
                                                settingResultLauncher.launch(request)
                                            },
                                            onError = {
                                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                            }
                                        )
                                    } else {
                                        locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = OnboardingBgColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasLocationPerm) Icons.Default.Check else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasLocationPerm) "Đã cấp quyền vị trí (Tiếp tục)" else "Cấp Quyền Vị Trí",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        2 -> {
                            Button(
                                onClick = {
                                    if (hasNotificationPerm) {
                                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = OnboardingBgColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasNotificationPerm) Icons.Default.Check else Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasNotificationPerm) "Đã bật thông báo (Tiếp tục)" else "Bật Thông Báo",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        3 -> {
                            Button(
                                onClick = onFinishOnboarding,
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = OnboardingBgColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Bắt Đầu Trải Nghiệm",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun checkLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkAndRequestLocationSettings(
    context: Context,
    onSettingsEnabled: () -> Unit,
    onResolutionRequired: (IntentSender) -> Unit,
    onError: (Exception) -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    val client: com.google.android.gms.location.SettingsClient = com.google.android.gms.location.LocationServices.getSettingsClient(context)
    val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

    task.addOnSuccessListener {
        onSettingsEnabled()
    }

    task.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            try {
                onResolutionRequired(exception.resolution.intentSender)
            } catch (sendEx: IntentSender.SendIntentException) {
                onError(sendEx)
            }
        } else {
            onError(exception)
        }
    }
}
