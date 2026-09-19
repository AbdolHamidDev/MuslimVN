package com.example.muslimvn.presentation.screens.youtube.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ChannelSection(
    uploaderName: String,
    uploaderAvatarUrl: String,
    modifier: Modifier = Modifier
) {
    var isFollowed by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uploaderAvatarUrl.isNotEmpty()) {
            AsyncImage(
                model = uploaderAvatarUrl,
                contentDescription = uploaderName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uploaderName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uploaderName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isFollowed) {
            OutlinedButton(
                onClick = { isFollowed = false },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "Đã theo dõi",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Button(
                onClick = { isFollowed = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "Theo dõi",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
