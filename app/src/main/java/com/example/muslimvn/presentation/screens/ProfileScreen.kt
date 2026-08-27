package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.muslimvn.presentation.components.MadhabBadge
import com.example.muslimvn.presentation.viewmodels.ProfileViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cloudProfile by viewModel.currentUserProfile.collectAsState()
    
    LaunchedEffect(cloudProfile) {
        cloudProfile?.let { viewModel.setInitialProfile(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.resetSavedState()
            // Pass success result back to Settings screen
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = !uiState.isLoading
                    ) {
                        Text("Lưu", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                // Avatar Section
                Box(contentAlignment = Alignment.TopEnd) {
                    val avatarSource = uiState.userProfile.photoUrl
                    val context = LocalContext.current
                    
                    if (avatarSource.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarSource.trim())
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.AccountCircle)
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Box(modifier = Modifier.padding(top = 4.dp, end = 4.dp)) {
                        MadhabBadge(madhab = uiState.userProfile.madhab)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Basic Info Section
                ProfileInfoCard(title = "Thông tin cơ bản") {
                    OutlinedTextField(
                        value = uiState.userProfile.displayName,
                        onValueChange = { viewModel.onDisplayNameChange(it) },
                        label = { Text("Tên hiển thị") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.userProfile.bio,
                        onValueChange = { viewModel.onBioChange(it) },
                        label = { Text("Giới thiệu") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Religious Info Section
                ProfileInfoCard(title = "Cá nhân hóa tôn giáo") {
                    MadhabDropdown(
                        selectedMadhab = uiState.userProfile.madhab,
                        onMadhabSelected = { viewModel.onMadhabChange(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.userProfile.location,
                        onValueChange = { viewModel.onLocationChange(it) },
                        label = { Text("Khu vực") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Thông tin của bạn được bảo mật và chỉ sử dụng để cá nhân hóa trải nghiệm tôn giáo của bạn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileInfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MadhabDropdown(
    selectedMadhab: String,
    onMadhabSelected: (String) -> Unit
) {
    val madhabs = listOf("Hanafi", "Shafi'i", "Maliki", "Hanbali")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedMadhab,
            onValueChange = {},
            readOnly = true,
            label = { Text("Trường phái (Madhab)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.HistoryEdu, contentDescription = null) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            madhabs.forEach { madhab ->
                DropdownMenuItem(
                    text = { Text(madhab) },
                    onClick = {
                        onMadhabSelected(madhab)
                        expanded = false
                    }
                )
            }
        }
    }
}
