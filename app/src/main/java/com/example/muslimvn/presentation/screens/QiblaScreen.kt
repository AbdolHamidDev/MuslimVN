package com.example.muslimvn.presentation.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.presentation.viewmodels.QiblaViewModel
import com.example.muslimvn.ui.theme.extendedColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onBackClick: () -> Unit = {},
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle Vibration
    LaunchedEffect(state.isFacingQibla) {
        if (state.isFacingQibla) {
            vibrateDevice(context)
        }
    }

    // Compass Rotation Logic (Handle 0/360 wrap-around)
    var lastAzimuth by remember { mutableFloatStateOf(0f) }
    var rotationOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.currentAzimuth) {
        val diff = (state.currentAzimuth - lastAzimuth + 180).mod(360f) - 180f
        rotationOffset += diff
        lastAzimuth = state.currentAzimuth
    }

    val animatedRotation by animateFloatAsState(
        targetValue = rotationOffset,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "CompassRotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_qibla)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.qibla_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LocationInfo(state)

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(300.dp)
                .shadow(8.dp, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            CompassDial(
                rotation = -animatedRotation,
                qiblaBearing = state.qiblaBearing,
                isFacingQibla = state.isFacingQibla
            )
            
            // Fixed Marker (Top)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    modifier = Modifier.size(12.dp, 24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {}
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        QiblaStatus(state)

        Spacer(modifier = Modifier.height(24.dp))
    }
    }
}

@Composable
fun LocationInfo(state: QiblaViewModel.QiblaUiState) {
    // Nền trắng tinh + viền hairline nhạt thay vì surfaceVariant đậm màu
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (state.isLoading) stringResource(R.string.loading_location) else stringResource(R.string.distance_mecca, state.distanceToMecca),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.location_coordinates, state.userLatitude, state.userLongitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompassDial(
    rotation: Float,
    qiblaBearing: Float,
    isFacingQibla: Boolean
) {
    val dialColor = MaterialTheme.colorScheme.onSurface
    val qiblaColor = if (isFacingQibla) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        rotate(rotation) {
            // Draw cardinal points — kích thước quy đổi từ dp (đúng trên mọi mật độ
            // màn hình), nhân fontScale để đồng bộ với cài đặt cỡ chữ hệ thống.
            val labelSize = 16.dp.toPx() * fontScale
            val edge = 12.dp.toPx()
            val markLength = 7.dp.toPx()
            val markStroke = 1.5f.dp.toPx()
            val textPaint = android.graphics.Paint().apply {
                color = dialColor.toArgb()
                textSize = labelSize
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText("N", center.x, center.y - radius + edge + labelSize, textPaint)
                drawText("S", center.x, center.y + radius - edge, textPaint)
                drawText("E", center.x + radius - edge - labelSize / 2f, center.y + labelSize * 0.35f, textPaint)
                drawText("W", center.x - radius + edge + labelSize / 2f, center.y + labelSize * 0.35f, textPaint)
            }

            // Draw marks
            for (i in 0 until 360 step 10) {
                val angle = i * PI / 180
                val start = Offset(
                    center.x + (radius - markLength) * sin(angle).toFloat(),
                    center.y - (radius - markLength) * cos(angle).toFloat()
                )
                val end = Offset(
                    center.x + radius * sin(angle).toFloat(),
                    center.y - radius * cos(angle).toFloat()
                )
                drawLine(
                    color = dialColor.copy(alpha = 0.3f),
                    start = start,
                    end = end,
                    strokeWidth = markStroke
                )
            }

            // Draw Qibla Needle
            rotate(qiblaBearing) {
                val needleTipInset = 20.dp.toPx()
                val needleBaseInset = 37.dp.toPx()
                val needleHalfWidth = 5.dp.toPx()
                val path = Path().apply {
                    moveTo(center.x, center.y - radius + needleTipInset)
                    lineTo(center.x - needleHalfWidth, center.y - radius + needleBaseInset)
                    lineTo(center.x + needleHalfWidth, center.y - radius + needleBaseInset)
                    close()
                }
                drawPath(path, qiblaColor)
                
                drawLine(
                    color = qiblaColor,
                    start = Offset(center.x, center.y),
                    end = Offset(center.x, center.y - radius + needleBaseInset),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Draw central point
        drawCircle(
            color = dialColor,
            radius = 3.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun QiblaStatus(state: QiblaViewModel.QiblaUiState) {
    val statusColor = if (state.isFacingQibla) MaterialTheme.extendedColors.success else MaterialTheme.colorScheme.onSurface
    val statusText = if (state.isFacingQibla) stringResource(R.string.facing_qibla) else stringResource(R.string.rotate_find_qibla)
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = statusColor
        )
        if (!state.isFacingQibla) {
            Text(
                text = "${state.currentAzimuth.toInt()}°",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun vibrateDevice(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
}
