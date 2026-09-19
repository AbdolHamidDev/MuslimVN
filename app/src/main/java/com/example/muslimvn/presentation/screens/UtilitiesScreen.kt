package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.presentation.RoadmapData
import com.example.muslimvn.presentation.components.PrayerList
import com.example.muslimvn.presentation.components.PrayerReminderBottomSheet
import com.example.muslimvn.presentation.components.RoadmapSection
import com.example.muslimvn.presentation.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPrayerTimesClick: () -> Unit = {},
    onQiblaClick: () -> Unit = {},
    onHijriCalendarClick: () -> Unit = {},
    onNamesOfAllahClick: () -> Unit = {},
    onZakatClick: () -> Unit = {},
    onPodcastClick: () -> Unit = {},
    onFeatureClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPrayerSheet by remember { mutableStateOf(false) }
    var selectedPrayerForReminder by remember { mutableStateOf<String?>(null) }

    val ibadahCategory = remember(
        onPrayerTimesClick, onQiblaClick, onHijriCalendarClick, onNamesOfAllahClick, onFeatureClick
    ) {
        RoadmapData.getIbadahCategory(
            onPrayerTimesClick = {
                onPrayerTimesClick()
                if (uiState.prayerTimes == null) {
                    viewModel.refreshPrayerTimes()
                }
                showPrayerSheet = true
            },
            onQiblaClick = onQiblaClick,
            onHijriCalendarClick = onHijriCalendarClick,
            onNamesOfAllahClick = onNamesOfAllahClick,
            onFeatureClick = onFeatureClick
        )
    }

    val knowledgeCategory = remember(onPodcastClick) {
        RoadmapData.getKnowledgeCategory(
            onPodcastClick = onPodcastClick
        )
    }

    val utilitiesCategory = remember(onZakatClick) {
        RoadmapData.getUtilitiesCategory(
            onZakatClick = onZakatClick
        )
    }

    val localCategory = remember {
        RoadmapData.getLocalCategory()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_utilities), fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 16.dp
            )
        ) {
            item { RoadmapSection(ibadahCategory) }
            item { RoadmapSection(knowledgeCategory) }
            item { RoadmapSection(utilitiesCategory) }
            if (localCategory.items.isNotEmpty()) {
                item { RoadmapSection(localCategory) }
            }
        }

        if (showPrayerSheet) {
            val prayerTimes = uiState.prayerTimes
            if (prayerTimes != null) {
                ModalBottomSheet(
                    onDismissRequest = { showPrayerSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                        Text(
                            text = stringResource(R.string.utility_prayer),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        PrayerList(
                            prayerTimes = prayerTimes,
                            reminders = uiState.reminders,
                            onReminderClick = { selectedPrayerForReminder = it }
                        )
                    }
                }
            }
        }

        selectedPrayerForReminder?.let { prayerName ->
            val reminder = uiState.reminders[prayerName] ?: PrayerReminder(prayerName)
            PrayerReminderBottomSheet(
                prayerName = prayerName,
                currentReminder = reminder,
                onDismiss = { selectedPrayerForReminder = null },
                onSave = {
                    viewModel.updateReminder(it)
                    selectedPrayerForReminder = null
                }
            )
        }
    }
}
