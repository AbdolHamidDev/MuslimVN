package com.example.muslimvn.presentation.screens.zakat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.core.utils.VietnameseNumberReader
import com.example.muslimvn.core.utils.toDotString
import com.example.muslimvn.core.utils.toVndString
import com.example.muslimvn.domain.models.zakat.NisabMethod
import com.example.muslimvn.presentation.viewmodels.zakat.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatScreen(
    onBackClick: () -> Unit,
    viewModel: ZakatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.zakat_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep == ZakatStep.INTRO) {
                            onBackClick()
                        } else {
                            viewModel.previousStep()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.currentStep == ZakatStep.INTRO || uiState.currentStep == ZakatStep.RESULT) {
                        IconButton(onClick = { viewModel.viewHistory() }) {
                            Icon(Icons.Default.History, contentDescription = "Lịch sử")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.currentStep != ZakatStep.INTRO && uiState.currentStep != ZakatStep.RESULT) {
                WizardProgressIndicator(uiState.currentStep)
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "ZakatWizardTransition"
                ) { step ->
                    WizardStepContent(
                        step = step,
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun WizardProgressIndicator(currentStep: ZakatStep) {
    val steps = ZakatStep.entries.filter { it != ZakatStep.INTRO && it != ZakatStep.RESULT }
    val currentIndex = steps.indexOf(currentStep)
    if (currentIndex == -1) return

    val progress = (currentIndex + 1).toFloat() / steps.size

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text(
            text = "${currentIndex + 1} / ${steps.size}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
        )
    }
}

@Composable
fun WizardStepContent(
    step: ZakatStep,
    uiState: ZakatUiState,
    viewModel: ZakatViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (step) {
            ZakatStep.INTRO -> IntroStep(onStart = viewModel::nextStep)
            
            ZakatStep.MONEY_QUESTION -> QuestionStep(
                question = stringResource(R.string.zakat_q_money),
                onAnswer = viewModel::onHasMoneyChange
            )
            ZakatStep.MONEY_INPUT -> AssetInputStep(
                title = stringResource(R.string.zakat_section_money),
                inputs = listOf(
                    ZakatInput(stringResource(R.string.zakat_cash_in_hand), uiState.cash, viewModel::onCashChange),
                    ZakatInput(stringResource(R.string.zakat_bank_balance), uiState.bank, viewModel::onBankChange)
                ),
                onContinue = viewModel::nextStep
            )

            ZakatStep.GOLD_QUESTION -> QuestionStep(
                question = stringResource(R.string.zakat_q_gold),
                onAnswer = viewModel::onHasGoldChange
            )
            ZakatStep.GOLD_INPUT -> GoldInputStep(
                uiState = uiState,
                onAmountChange = viewModel::onGoldAmountChange,
                onUnitChange = viewModel::onGoldUnitChange,
                onPurityChange = viewModel::onGoldPurityChange,
                onSilverChange = viewModel::onSilverGramsChange,
                onContinue = viewModel::nextStep
            )

            ZakatStep.INVESTMENT_QUESTION -> QuestionStep(
                question = stringResource(R.string.zakat_q_investment),
                onAnswer = viewModel::onHasInvestmentChange
            )
            ZakatStep.INVESTMENT_INPUT -> AssetInputStep(
                title = stringResource(R.string.zakat_section_investments),
                inputs = listOf(
                    ZakatInput(stringResource(R.string.zakat_investments), uiState.investments, viewModel::onInvestmentsChange)
                ),
                onContinue = viewModel::nextStep
            )

            ZakatStep.BUSINESS_QUESTION -> QuestionStep(
                question = stringResource(R.string.zakat_q_business),
                onAnswer = viewModel::onHasBusinessChange
            )
            ZakatStep.BUSINESS_INPUT -> AssetInputStep(
                title = stringResource(R.string.zakat_section_investments),
                inputs = listOf(
                    ZakatInput(stringResource(R.string.zakat_business_assets), uiState.businessAssets, viewModel::onBusinessAssetsChange)
                ),
                onContinue = viewModel::nextStep
            )

            ZakatStep.RECEIVABLE_QUESTION -> QuestionStep(
                question = stringResource(R.string.zakat_q_receivable),
                onAnswer = viewModel::onHasReceivableChange
            )
            ZakatStep.RECEIVABLE_INPUT -> AssetInputStep(
                title = stringResource(R.string.zakat_receivables),
                inputs = listOf(
                    ZakatInput(stringResource(R.string.zakat_receivables), uiState.receivables, viewModel::onReceivablesChange)
                ),
                onContinue = viewModel::nextStep
            )

            ZakatStep.LIABILITY_QUESTION -> QuestionStep(
                question = stringResource(R.string.zakat_q_liability),
                onAnswer = viewModel::onHasLiabilityChange
            )
            ZakatStep.LIABILITY_INPUT -> AssetInputStep(
                title = stringResource(R.string.zakat_section_liabilities),
                inputs = listOf(
                    ZakatInput(stringResource(R.string.zakat_debts_label), uiState.liabilities, viewModel::onLiabilitiesChange)
                ),
                onContinue = viewModel::nextStep
            )

            ZakatStep.NISAB_SELECTION -> NisabSelectionStep(
                currentMethod = uiState.ruleSet.nisabMethod,
                onMethodChange = viewModel::onNisabMethodChange,
                onContinue = viewModel::nextStep
            )

            ZakatStep.HAWL_DATE_CONFIRMATION -> QuestionStep(
                question = stringResource(R.string.zakat_q_remember_date),
                onAnswer = viewModel::onRemembersHawlDateChange
            )

            ZakatStep.HAWL_DATE_PICKER -> HawlStep(
                selectedDate = uiState.hawlStartDate,
                onDateChange = viewModel::onHawlDateChange,
                onContinue = viewModel::nextStep
            )

            ZakatStep.HAWL_GUIDANCE -> HawlGuidanceStep(
                onContinue = viewModel::nextStep
            )

            ZakatStep.REVIEW -> ReviewStep(
                uiState = uiState,
                onEditClick = viewModel::jumpToStep,
                onContinue = viewModel::nextStep
            )

            ZakatStep.RESULT -> ResultStep(
                uiState = uiState,
                onSave = viewModel::saveCalculation
            )

            ZakatStep.HISTORY -> HistoryStep(
                history = viewModel.history.collectAsState().value
            )
        }
    }
}

@Composable
fun IntroStep(onStart: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.zakat_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.zakat_overview_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.zakat_start_calc))
            }
        }
    }
}

@Composable
fun QuestionStep(question: String, onAnswer: (Boolean) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onAnswer(true) },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(stringResource(R.string.yes))
                }
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        }
    }
}

@Composable
fun AssetInputStep(
    title: String,
    inputs: List<ZakatInput>,
    footer: String? = null,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        inputs.forEach { input ->
            val isMoneyInput = title == stringResource(R.string.zakat_section_money) ||
                               title == stringResource(R.string.zakat_section_investments) ||
                               title == stringResource(R.string.zakat_section_liabilities) ||
                               title == stringResource(R.string.zakat_receivables)
            
            val decimalValue = try { 
                val s = input.value.replace(",", "").replace(".", "")
                if (s.isBlank()) BigDecimal.ZERO else BigDecimal(s) 
            } catch (e: Exception) { BigDecimal.ZERO }

            OutlinedTextField(
                value = input.value,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() || it == '.' || it == ',' }) {
                        input.onValueChange(newValue)
                    }
                },
                label = { Text(input.label) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = if (isMoneyInput) CurrencyVisualTransformation() else VisualTransformation.None,
                supportingText = {
                    if (isMoneyInput && decimalValue > BigDecimal.ZERO) {
                        Text(
                            text = VietnameseNumberReader.readNumber(decimalValue),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                singleLine = true
            )
        }
        if (title == stringResource(R.string.zakat_section_liabilities)) {
            Text(
                text = stringResource(R.string.zakat_liabilities_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

@Composable
fun GoldInputStep(
    uiState: ZakatUiState,
    onAmountChange: (String) -> Unit,
    onUnitChange: (GoldUnit) -> Unit,
    onPurityChange: (GoldPurity) -> Unit,
    onSilverChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text(
            text = stringResource(R.string.zakat_section_gold_silver),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(text = "Vàng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.goldAmount,
                onValueChange = onAmountChange,
                label = { Text("Số lượng") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            GoldUnitDropdown(uiState.goldUnit, onUnitChange)
        }
        
        GoldPurityDropdown(uiState.goldPurity, onPurityChange)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Bạc", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = uiState.silverGrams,
            onValueChange = onSilverChange,
            label = { Text(stringResource(R.string.zakat_silver_grams)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Text(
            text = stringResource(R.string.zakat_gold_price_label, uiState.goldPrice.toVndString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldUnitDropdown(currentUnit: GoldUnit, onUnitChange: (GoldUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (currentUnit) {
        GoldUnit.GRAM -> stringResource(R.string.zakat_unit_gram)
        GoldUnit.CHI -> stringResource(R.string.zakat_unit_chi)
        GoldUnit.LUONG -> stringResource(R.string.zakat_unit_luong)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.width(120.dp)
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Đơn vị") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GoldUnit.entries.forEach { unit ->
                val unitLabel = when (unit) {
                    GoldUnit.GRAM -> stringResource(R.string.zakat_unit_gram)
                    GoldUnit.CHI -> stringResource(R.string.zakat_unit_chi)
                    GoldUnit.LUONG -> stringResource(R.string.zakat_unit_luong)
                }
                DropdownMenuItem(
                    text = { Text(unitLabel) },
                    onClick = {
                        onUnitChange(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldPurityDropdown(currentPurity: GoldPurity, onPurityChange: (GoldPurity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (currentPurity) {
        GoldPurity.PURITY_24K -> stringResource(R.string.zakat_purity_24k)
        GoldPurity.PURITY_22K -> stringResource(R.string.zakat_purity_22k)
        GoldPurity.PURITY_18K -> stringResource(R.string.zakat_purity_18k)
        GoldPurity.PURITY_14K -> stringResource(R.string.zakat_purity_14k)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Độ tinh khiết") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GoldPurity.entries.forEach { purity ->
                val purityLabel = when (purity) {
                    GoldPurity.PURITY_24K -> stringResource(R.string.zakat_purity_24k)
                    GoldPurity.PURITY_22K -> stringResource(R.string.zakat_purity_22k)
                    GoldPurity.PURITY_18K -> stringResource(R.string.zakat_purity_18k)
                    GoldPurity.PURITY_14K -> stringResource(R.string.zakat_purity_14k)
                }
                DropdownMenuItem(
                    text = { Text(purityLabel) },
                    onClick = {
                        onPurityChange(purity)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HawlStep(
    selectedDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    onContinue: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text(
            text = stringResource(R.string.zakat_hawl_start),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = selectedDate?.format(dateFormatter) ?: "Chọn ngày bắt đầu Hawl",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        if (showDatePicker) {
            ZakatDatePicker(
                initialDate = selectedDate ?: LocalDate.now(),
                onDateSelected = {
                    onDateChange(it)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatDatePicker(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)))
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun HawlGuidanceStep(onContinue: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text(
            text = stringResource(R.string.zakat_hawl_guidance_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                text = stringResource(R.string.zakat_hawl_guidance_body),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

@Composable
fun NisabSelectionStep(
    currentMethod: NisabMethod,
    onMethodChange: (NisabMethod) -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text(
            text = stringResource(R.string.zakat_nisab_method),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        NisabCard(
            label = stringResource(R.string.zakat_nisab_gold),
            selected = currentMethod == NisabMethod.GOLD,
            onClick = { onMethodChange(NisabMethod.GOLD) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        NisabCard(
            label = stringResource(R.string.zakat_nisab_silver),
            selected = currentMethod == NisabMethod.SILVER,
            onClick = { onMethodChange(NisabMethod.SILVER) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.action_continue))
        }
    }
}

@Composable
fun NisabCard(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(enabled = selected)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ReviewStep(uiState: ZakatUiState, onEditClick: (ZakatStep) -> Unit, onContinue: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text(
            text = "Kiểm tra thông tin",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        ReviewItem(
            "Tiền mặt & Ngân hàng", 
            listOf(uiState.cash, uiState.bank), 
            Icons.Default.AccountBalanceWallet, 
            isMoney = true,
            onEdit = { onEditClick(ZakatStep.MONEY_INPUT) }
        )
        if (uiState.hasGold == true) {
            ReviewItem(
                "Vàng & Bạc", 
                listOf(uiState.goldAmount + " " + uiState.goldUnit.name, uiState.silverGrams + "g"), 
                Icons.Default.MonetizationOn, 
                onEdit = { onEditClick(ZakatStep.GOLD_INPUT) }
            )
        }
        if (uiState.hasInvestment == true) {
            ReviewItem(
                "Đầu tư", 
                listOf(uiState.investments), 
                Icons.AutoMirrored.Filled.ShowChart, 
                isMoney = true,
                onEdit = { onEditClick(ZakatStep.INVESTMENT_INPUT) }
            )
        }
        if (uiState.hasBusiness == true) {
            ReviewItem(
                "Kinh doanh", 
                listOf(uiState.businessAssets), 
                Icons.Default.BusinessCenter, 
                isMoney = true,
                onEdit = { onEditClick(ZakatStep.BUSINESS_INPUT) }
            )
        }
        if (uiState.hasReceivable == true) {
            ReviewItem(
                "Khoản phải thu", 
                listOf(uiState.receivables), 
                Icons.Default.AccountBalanceWallet, 
                isMoney = true,
                onEdit = { onEditClick(ZakatStep.RECEIVABLE_INPUT) }
            )
        }
        if (uiState.hasLiability == true) {
            ReviewItem(
                "Khoản khấu trừ", 
                listOf(uiState.liabilities), 
                Icons.Default.AccountBalanceWallet, 
                isMoney = true,
                onEdit = { onEditClick(ZakatStep.LIABILITY_INPUT) }
            )
        }
        
        ReviewItem(
            "Nisab", 
            listOf(if (uiState.ruleSet.nisabMethod == NisabMethod.GOLD) "Vàng" else "Bạc"), 
            Icons.Default.MonetizationOn, 
            onEdit = { onEditClick(ZakatStep.NISAB_SELECTION) }
        )
        ReviewItem(
            "Ngày bắt đầu Hawl", 
            listOf(uiState.hawlStartDate?.toString() ?: "Chưa chọn"), 
            Icons.Default.CalendarToday, 
            onEdit = { onEditClick(ZakatStep.HAWL_DATE_CONFIRMATION) }
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Tính Zakat")
        }
    }
}

@Composable
fun ReviewItem(
    label: String, 
    values: List<String>, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    isMoney: Boolean = false,
    onEdit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.edit), style = MaterialTheme.typography.labelSmall)
            }
        }
        values.forEach { value ->
            if (value.isNotBlank()) {
                val cleanValue = value.replace(",", "").replace(".", "")
                val decimalValue = try { BigDecimal(cleanValue) } catch (e: Exception) { BigDecimal.ZERO }
                
                Column(modifier = Modifier.padding(start = 28.dp)) {
                    Text(
                        text = if (isMoney) decimalValue.toDotString() else value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isMoney && decimalValue > BigDecimal.ZERO) {
                        Text(
                            text = VietnameseNumberReader.readNumber(decimalValue),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun TrendCard(uiState: ZakatUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.result.isEligible) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.zakat_due_amount),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.result.zakatDue.toVndString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            uiState.zakatTrendPercent?.let { percent ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = if (percent >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (percent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (percent >= 0) "+" else ""}$percent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (percent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " so với lần trước",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (uiState.result.isEligible) 
                    stringResource(R.string.zakat_eligible) 
                else stringResource(R.string.zakat_not_eligible),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ResultStep(uiState: ZakatUiState, onSave: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrendCard(uiState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.result.isEligible && uiState.zakatDueDate != null) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.zakat_due_date), style = MaterialTheme.typography.labelMedium)
                    Text(text = uiState.zakatDueDate.format(dateFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    uiState.daysRemaining?.let { days ->
                        Text(
                            text = if (days >= 0) stringResource(R.string.zakat_days_left, days) else stringResource(R.string.zakat_overdue, -days),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (days >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        CalculationBreakdownWizard(uiState)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lưu kết quả")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.zakat_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun CalculationBreakdownWizard(uiState: ZakatUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.zakat_breakdown), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            BreakdownRowWizard(stringResource(R.string.zakat_total_assets), uiState.result.totalZakatableAssets.toVndString())
            BreakdownRowWizard(stringResource(R.string.zakat_total_liabilities), uiState.result.totalDeductibleLiabilities.toVndString())
            HorizontalDivider()
            BreakdownRowWizard(stringResource(R.string.zakat_net_wealth), uiState.result.netZakatableWealth.toVndString(), isBold = true)
            BreakdownRowWizard(stringResource(R.string.zakat_nisab_threshold), uiState.result.nisabThreshold.toVndString())
            BreakdownRowWizard(stringResource(R.string.zakat_rate), "2.5%")
        }
    }
}

@Composable
fun HistoryStep(history: List<com.example.muslimvn.domain.models.zakat.ZakatHistory>) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Lịch sử tính Zakat",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có lịch sử tính toán", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(history) { record ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = record.date.format(dateFormatter), style = MaterialTheme.typography.labelMedium)
                                Text(text = record.zakatDue.toVndString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "Tài sản: " + record.netWealth.toVndString(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownRowWizard(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

class CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val formattedText = StringBuilder()
        val reverseText = originalText.reversed()
        for (i in reverseText.indices) {
            formattedText.append(reverseText[i])
            if ((i + 1) % 3 == 0 && (i + 1) != reverseText.length) {
                formattedText.append('.')
            }
        }
        val out = formattedText.toString().reversed()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val dotsBefore = (offset - 1) / 3
                return offset + dotsBefore
            }

            override fun transformedToOriginal(offset: Int): Int {
                val dotsBefore = if (offset > 0) {
                    val actualDigits = out.substring(0, offset).count { it != '.' }
                    offset - actualDigits
                } else 0
                return offset - dotsBefore
            }
        }

        return TransformedText(androidx.compose.ui.text.AnnotatedString(out), offsetMapping)
    }
}

data class ZakatInput(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit
)
