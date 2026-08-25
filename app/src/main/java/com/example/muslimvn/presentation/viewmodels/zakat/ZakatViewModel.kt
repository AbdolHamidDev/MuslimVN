package com.example.muslimvn.presentation.viewmodels.zakat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.zakat.*
import com.example.muslimvn.domain.repository.ZakatRepository
import com.example.muslimvn.domain.zakat.ZakatCalculationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Stack

enum class ZakatStep {
    INTRO,
    MONEY_QUESTION, MONEY_INPUT,
    GOLD_QUESTION, GOLD_INPUT,
    INVESTMENT_QUESTION, INVESTMENT_INPUT,
    BUSINESS_QUESTION, BUSINESS_INPUT,
    RECEIVABLE_QUESTION, RECEIVABLE_INPUT,
    LIABILITY_QUESTION, LIABILITY_INPUT,
    NISAB_SELECTION,
    HAWL_DATE_CONFIRMATION, HAWL_DATE_PICKER, HAWL_GUIDANCE,
    REVIEW,
    RESULT,
    HISTORY
}

enum class GoldUnit {
    GRAM, CHI, LUONG
}

enum class GoldPurity(val ratio: BigDecimal) {
    PURITY_24K(BigDecimal("1.0")),
    PURITY_22K(BigDecimal("0.916")),
    PURITY_18K(BigDecimal("0.75")),
    PURITY_14K(BigDecimal("0.583"))
}

@HiltViewModel
class ZakatViewModel @Inject constructor(
    private val repository: ZakatRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(ZakatStep.INTRO)
    private val _stepHistory = Stack<ZakatStep>()

    private val _ruleSet = MutableStateFlow(ZakatRuleSet())
    private val _goldPrice = repository.getGoldPrice()
    private val _silverPrice = repository.getSilverPrice()
    
    private val _lastRecord = flow {
        emit(repository.getLatestRecord())
    }

    val history: StateFlow<List<ZakatHistory>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Question states
    private val _hasMoney = MutableStateFlow<Boolean?>(null)
    private val _hasGold = MutableStateFlow<Boolean?>(null)
    private val _hasInvestment = MutableStateFlow<Boolean?>(null)
    private val _hasBusiness = MutableStateFlow<Boolean?>(null)
    private val _hasReceivable = MutableStateFlow<Boolean?>(null)
    private val _hasLiability = MutableStateFlow<Boolean?>(null)
    private val _remembersHawlDate = MutableStateFlow<Boolean?>(null)

    // Inputs
    private val _cash = MutableStateFlow("")
    private val _bank = MutableStateFlow("")
    private val _receivables = MutableStateFlow("")
    
    private val _goldAmount = MutableStateFlow("")
    private val _goldUnit = MutableStateFlow(GoldUnit.GRAM)
    private val _goldPurity = MutableStateFlow(GoldPurity.PURITY_24K)
    
    private val _silverGrams = MutableStateFlow("")
    private val _investments = MutableStateFlow("")
    private val _businessAssets = MutableStateFlow("")
    private val _liabilities = MutableStateFlow("")
    
    private val _hawlStartDate = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<ZakatUiState> = combine(
        listOf(
            _currentStep, _ruleSet, _goldPrice, _silverPrice,
            _hasMoney, _hasGold, _hasInvestment, _hasBusiness, _hasReceivable, _hasLiability,
            _cash, _bank, _receivables, _goldAmount, _goldUnit, _goldPurity,
            _silverGrams, _investments, _businessAssets, _liabilities, _hawlStartDate,
            _remembersHawlDate, _lastRecord
        )
    ) { flows ->
        val currentStep = flows[0] as ZakatStep
        val ruleSet = flows[1] as ZakatRuleSet
        val goldPrice = flows[2] as BigDecimal
        val silverPrice = flows[3] as BigDecimal
        
        val hasMoney = flows[4] as? Boolean
        val hasGold = flows[5] as? Boolean
        val hasInvestment = flows[6] as? Boolean
        val hasBusiness = flows[7] as? Boolean
        val hasReceivable = flows[8] as? Boolean
        val hasLiability = flows[9] as? Boolean

        val cashVal = parseBigDecimal(flows[10] as String)
        val bankVal = parseBigDecimal(flows[11] as String)
        val receivablesVal = parseBigDecimal(flows[12] as String)
        
        val goldAmountStr = flows[13] as String
        val goldUnit = flows[14] as GoldUnit
        val goldPurity = flows[15] as GoldPurity
        val goldAmountVal = parseBigDecimal(goldAmountStr)
        
        val silverGramsVal = parseBigDecimal(flows[16] as String)
        val investmentsVal = parseBigDecimal(flows[17] as String)
        val businessAssetsVal = parseBigDecimal(flows[18] as String)
        val liabilitiesVal = parseBigDecimal(flows[19] as String)
        
        val hawlStartDate = flows[20] as? LocalDate
        val remembersHawlDate = flows[21] as? Boolean
        val lastRecord = flows[22] as? ZakatHistory

        // Gold conversion logic
        val goldGramsRaw = when (goldUnit) {
            GoldUnit.GRAM -> goldAmountVal
            GoldUnit.CHI -> goldAmountVal.multiply(BigDecimal("3.75"))
            GoldUnit.LUONG -> goldAmountVal.multiply(BigDecimal("37.5"))
        }
        val goldGramsFine = goldGramsRaw.multiply(goldPurity.ratio)
        
        val goldValueVnd = goldGramsFine.multiply(goldPrice)
        val silverValueVnd = silverGramsVal.multiply(silverPrice)

        val assets = mutableListOf<ZakatAsset>()
        if (hasMoney == true) {
            assets.add(ZakatAsset("Cash", cashVal, AssetCategory.MONEY))
            assets.add(ZakatAsset("Bank", bankVal, AssetCategory.MONEY))
        }
        if (hasReceivable == true) {
            assets.add(ZakatAsset("Receivables", receivablesVal, AssetCategory.MONEY))
        }
        if (hasGold == true) {
            assets.add(ZakatAsset("Gold", goldValueVnd, AssetCategory.GOLD_SILVER))
            assets.add(ZakatAsset("Silver", silverValueVnd, AssetCategory.GOLD_SILVER))
        }
        if (hasInvestment == true) {
            assets.add(ZakatAsset("Investments", investmentsVal, AssetCategory.INVESTMENT))
        }
        if (hasBusiness == true) {
            assets.add(ZakatAsset("Business Assets", businessAssetsVal, AssetCategory.BUSINESS))
        }

        val liabilities = mutableListOf<ZakatLiability>()
        if (hasLiability == true) {
            liabilities.add(ZakatLiability("Debts", liabilitiesVal, true))
        }

        val engine = ZakatCalculationEngine(ruleSet, goldPrice, silverPrice)
        val result = engine.calculate(assets, liabilities)

        val zakatDueDate = hawlStartDate?.let {
            val hijrahDate = HijrahDate.from(it)
            val nextYearHijrah = hijrahDate.plus(1, ChronoUnit.YEARS)
            LocalDate.from(nextYearHijrah)
        }
        val daysRemaining = zakatDueDate?.let {
            ChronoUnit.DAYS.between(LocalDate.now(), it)
        }
        
        val zakatTrendPercent = if (lastRecord != null && lastRecord.zakatDue > BigDecimal.ZERO) {
            val diff = result.zakatDue.subtract(lastRecord.zakatDue)
            diff.multiply(BigDecimal("100")).divide(lastRecord.zakatDue, 1, java.math.RoundingMode.HALF_UP).toDouble()
        } else null

        ZakatUiState(
            currentStep = currentStep,
            hasMoney = hasMoney,
            hasGold = hasGold,
            hasInvestment = hasInvestment,
            hasBusiness = hasBusiness,
            hasReceivable = hasReceivable,
            hasLiability = hasLiability,
            remembersHawlDate = remembersHawlDate,
            cash = flows[10] as String,
            bank = flows[11] as String,
            receivables = flows[12] as String,
            goldAmount = goldAmountStr,
            goldUnit = goldUnit,
            goldPurity = goldPurity,
            silverGrams = flows[16] as String,
            investments = flows[17] as String,
            businessAssets = flows[18] as String,
            liabilities = flows[19] as String,
            hawlStartDate = hawlStartDate,
            zakatDueDate = zakatDueDate,
            daysRemaining = daysRemaining,
            zakatTrendPercent = zakatTrendPercent,
            goldPrice = goldPrice,
            silverPrice = silverPrice,
            ruleSet = ruleSet,
            result = result
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ZakatUiState()
    )

    fun nextStep() {
        val next = getNextStep(_currentStep.value)
        if (next != null) {
            _stepHistory.push(_currentStep.value)
            _currentStep.value = next
        }
    }

    fun previousStep() {
        if (_stepHistory.isNotEmpty()) {
            _currentStep.value = _stepHistory.pop()
        }
    }

    fun jumpToStep(step: ZakatStep) {
        _stepHistory.push(_currentStep.value)
        _currentStep.value = step
    }

    fun viewHistory() {
        jumpToStep(ZakatStep.HISTORY)
    }

    private fun getNextStep(current: ZakatStep): ZakatStep? {
        return when (current) {
            ZakatStep.INTRO -> ZakatStep.MONEY_QUESTION
            ZakatStep.MONEY_QUESTION -> if (_hasMoney.value == true) ZakatStep.MONEY_INPUT else ZakatStep.GOLD_QUESTION
            ZakatStep.MONEY_INPUT -> ZakatStep.GOLD_QUESTION
            ZakatStep.GOLD_QUESTION -> if (_hasGold.value == true) ZakatStep.GOLD_INPUT else ZakatStep.INVESTMENT_QUESTION
            ZakatStep.GOLD_INPUT -> ZakatStep.INVESTMENT_QUESTION
            ZakatStep.INVESTMENT_QUESTION -> if (_hasInvestment.value == true) ZakatStep.INVESTMENT_INPUT else ZakatStep.BUSINESS_QUESTION
            ZakatStep.INVESTMENT_INPUT -> ZakatStep.BUSINESS_QUESTION
            ZakatStep.BUSINESS_QUESTION -> if (_hasBusiness.value == true) ZakatStep.BUSINESS_INPUT else ZakatStep.RECEIVABLE_QUESTION
            ZakatStep.BUSINESS_INPUT -> ZakatStep.RECEIVABLE_QUESTION
            ZakatStep.RECEIVABLE_QUESTION -> if (_hasReceivable.value == true) ZakatStep.RECEIVABLE_INPUT else ZakatStep.LIABILITY_QUESTION
            ZakatStep.RECEIVABLE_INPUT -> ZakatStep.LIABILITY_QUESTION
            ZakatStep.LIABILITY_QUESTION -> if (_hasLiability.value == true) ZakatStep.LIABILITY_INPUT else ZakatStep.NISAB_SELECTION
            ZakatStep.LIABILITY_INPUT -> ZakatStep.NISAB_SELECTION
            ZakatStep.NISAB_SELECTION -> ZakatStep.HAWL_DATE_CONFIRMATION
            ZakatStep.HAWL_DATE_CONFIRMATION -> if (_remembersHawlDate.value == true) ZakatStep.HAWL_DATE_PICKER else ZakatStep.HAWL_GUIDANCE
            ZakatStep.HAWL_DATE_PICKER -> ZakatStep.REVIEW
            ZakatStep.HAWL_GUIDANCE -> ZakatStep.REVIEW
            ZakatStep.REVIEW -> ZakatStep.RESULT
            ZakatStep.RESULT -> null
            ZakatStep.HISTORY -> null
        }
    }

    fun onHasMoneyChange(has: Boolean) { 
        _hasMoney.value = has
        nextStep()
    }
    fun onHasGoldChange(has: Boolean) { 
        _hasGold.value = has
        nextStep()
    }
    fun onHasInvestmentChange(has: Boolean) { 
        _hasInvestment.value = has
        nextStep()
    }
    fun onHasBusinessChange(has: Boolean) { 
        _hasBusiness.value = has
        nextStep()
    }
    fun onHasReceivableChange(has: Boolean) { 
        _hasReceivable.value = has
        nextStep()
    }
    fun onHasLiabilityChange(has: Boolean) { 
        _hasLiability.value = has
        nextStep()
    }
    fun onRemembersHawlDateChange(remembers: Boolean) {
        _remembersHawlDate.value = remembers
        nextStep()
    }

    fun saveCalculation() {
        viewModelScope.launch {
            val state = uiState.value
            val record = ZakatHistory(
                date = LocalDate.now(),
                totalAssets = state.result.totalZakatableAssets,
                totalLiabilities = state.result.totalDeductibleLiabilities,
                netWealth = state.result.netZakatableWealth,
                zakatDue = state.result.zakatDue,
                goldPrice = state.goldPrice,
                silverPrice = state.silverPrice
            )
            repository.saveRecord(record)
            // Logic to refresh _lastRecord if needed, though for now it's just saved
        }
    }

    fun onCashChange(value: String) { _cash.value = value }
    fun onBankChange(value: String) { _bank.value = value }
    fun onReceivablesChange(value: String) { _receivables.value = value }
    
    fun onGoldAmountChange(value: String) { _goldAmount.value = value }
    fun onGoldUnitChange(unit: GoldUnit) { _goldUnit.value = unit }
    fun onGoldPurityChange(purity: GoldPurity) { _goldPurity.value = purity }
    
    fun onSilverGramsChange(value: String) { _silverGrams.value = value }
    fun onInvestmentsChange(value: String) { _investments.value = value }
    fun onBusinessAssetsChange(value: String) { _businessAssets.value = value }
    fun onLiabilitiesChange(value: String) { _liabilities.value = value }
    
    fun onHawlDateChange(date: LocalDate) {
        _hawlStartDate.value = date
    }

    fun onNisabMethodChange(method: NisabMethod) {
        _ruleSet.value = _ruleSet.value.copy(nisabMethod = method)
    }

    private fun parseBigDecimal(value: String): BigDecimal {
        return try {
            val sanitized = value.replace(",", "").replace(" ", "")
            if (sanitized.count { it == '.' } > 1) {
                val lastDotIndex = sanitized.lastIndexOf('.')
                val finalValue = sanitized.substring(0, lastDotIndex).replace(".", "") + sanitized.substring(lastDotIndex)
                if (finalValue.isBlank()) BigDecimal.ZERO else BigDecimal(finalValue)
            } else {
                if (sanitized.isBlank()) BigDecimal.ZERO else BigDecimal(sanitized)
            }
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
}

data class ZakatUiState(
    val currentStep: ZakatStep = ZakatStep.INTRO,
    val hasMoney: Boolean? = null,
    val hasGold: Boolean? = null,
    val hasInvestment: Boolean? = null,
    val hasBusiness: Boolean? = null,
    val hasReceivable: Boolean? = null,
    val hasLiability: Boolean? = null,
    val remembersHawlDate: Boolean? = null,
    val cash: String = "",
    val bank: String = "",
    val receivables: String = "",
    val goldAmount: String = "",
    val goldUnit: GoldUnit = GoldUnit.GRAM,
    val goldPurity: GoldPurity = GoldPurity.PURITY_24K,
    val silverGrams: String = "",
    val investments: String = "",
    val businessAssets: String = "",
    val liabilities: String = "",
    val hawlStartDate: LocalDate? = null,
    val zakatDueDate: LocalDate? = null,
    val daysRemaining: Long? = null,
    val zakatTrendPercent: Double? = null,
    val goldPrice: BigDecimal = BigDecimal.ZERO,
    val silverPrice: BigDecimal = BigDecimal.ZERO,
    val ruleSet: ZakatRuleSet = ZakatRuleSet(),
    val result: ZakatResult = ZakatResult(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
        BigDecimal.ZERO, BigDecimal("0.025"), BigDecimal.ZERO, false
    )
)
