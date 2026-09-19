package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.muslimvn.presentation.components.RoadmapSection
import com.example.muslimvn.presentation.components.RoadmapCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapCategoryScreen(
    title: String,
    category: RoadmapCategory,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = title, fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                RoadmapSection(category)
            }
        }
    }
}
