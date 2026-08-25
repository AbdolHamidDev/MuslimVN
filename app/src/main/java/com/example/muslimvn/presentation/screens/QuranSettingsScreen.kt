package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.data.preferences.QuranDisplayMode
import com.example.muslimvn.domain.models.availableReciters
import com.example.muslimvn.presentation.viewmodels.QuranSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: QuranSettingsViewModel = hiltViewModel()
) {
    val reciterIdentifier by viewModel.reciterIdentifier.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt Quran") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSectionTitle(title = "Chế độ hiển thị", icon = Icons.Default.Visibility)
                DisplayModeSelector(
                    currentMode = displayMode,
                    onModeSelected = viewModel::onDisplayModeChanged
                )
            }

            item {
                SettingsSectionTitle(title = "Kích thước chữ", icon = Icons.Default.FontDownload)
                FontSizeSlider(
                    currentSize = fontSize,
                    onSizeChanged = viewModel::onFontSizeChanged
                )
            }

            item {
                SettingsSectionTitle(title = "Hiệu ứng & Trải nghiệm", icon = Icons.Default.TouchApp)
                ListItem(
                    headlineContent = { Text("Phản hồi rung khi đọc") },
                    supportingContent = { Text("Rung nhẹ khi highlight từng từ theo audio") },
                    trailingContent = {
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = viewModel::onHapticEnabledChanged
                        )
                    }
                )
            }

            item {
                SettingsSectionTitle(title = "Học giả (Reciters)", icon = Icons.Default.Person)
            }

            items(availableReciters) { reciter ->
                ReciterItem(
                    reciter = reciter,
                    isSelected = reciter.identifier == reciterIdentifier,
                    onClick = { viewModel.onReciterSelected(reciter) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayModeSelector(
    currentMode: QuranDisplayMode,
    onModeSelected: (QuranDisplayMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        QuranDisplayMode.values().forEachIndexed { index, mode ->
            val label = when (mode) {
                QuranDisplayMode.BOTH -> "Cả hai"
                QuranDisplayMode.ARABIC_ONLY -> "Chỉ Ả Rập"
                QuranDisplayMode.TRANSLATION_ONLY -> "Chỉ dịch"
            }
            SegmentedButton(
                selected = currentMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = QuranDisplayMode.values().size)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FontSizeSlider(
    currentSize: Float,
    onSizeChanged: (Float) -> Unit
) {
    Column {
        Slider(
            value = currentSize,
            onValueChange = onSizeChanged,
            valueRange = 14f..32f,
            steps = 8
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Nhỏ", style = MaterialTheme.typography.bodySmall)
            Text("${currentSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("Lớn", style = MaterialTheme.typography.bodySmall)
        }
        
        // Preview text
        Surface(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontSize = (currentSize * 1.4).sp,
                    lineHeight = (currentSize * 2.2).sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhân danh Allah, Đấng Rất Mực Độ Lượng, Đấng Rất Mực Khoan Dung",
                    fontSize = (currentSize * 0.8).sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ReciterItem(
    reciter: com.example.muslimvn.domain.models.Reciter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = reciter.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = reciter.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
