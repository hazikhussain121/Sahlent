package com.sahlent.ui.zakat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Zakat Calculator based on Islamic guidelines
 * 
 * Nisab Thresholds:
 * - Gold: 87.48 grams (approximately 7.5 tola or 3 oz)
 * - Silver: 612.36 grams (approximately 52.5 tola or 21 oz)
 * 
 * Zakat Rate: 2.5% on wealth above Nisab
 */
data class ZakatUiState(
    // Asset inputs
    val cashAndBank: String = "",
    val goldValueOrWeight: String = "",
    val isGoldInGrams: Boolean = true, // true = grams, false = currency value
    val silverValueOrWeight: String = "",
    val isSilverInGrams: Boolean = true,
    val investments: String = "", // Stocks, mutual funds, etc.
    val businessInventory: String = "",
    val receivables: String = "", // Money owed to you
    val otherAssets: String = "",
    
    // Liabilities
    val debts: String = "", // Money you owe
    val expenses: String = "", // Immediate expenses due
    
    // Metal prices (per gram)
    val goldPricePerGram: String = "6500", // Default INR approximate
    val silverPricePerGram: String = "80", // Default INR approximate
    
    // Currency
    val currencySymbol: String = "₹",
    
    // Results
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netZakatableWealth: Double = 0.0,
    val nisabThreshold: Double = 0.0,
    val isAboveNisab: Boolean = false,
    val zakatDue: Double = 0.0,
    
    // UI State
    val showResults: Boolean = false,
    val selectedNisabType: NisabType = NisabType.SILVER // Silver is more commonly used
)

enum class NisabType(val displayName: String) {
    GOLD("Gold (87.48g)"),
    SILVER("Silver (612.36g)")
}

@HiltViewModel
class ZakatViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(ZakatUiState())
    val uiState: StateFlow<ZakatUiState> = _uiState.asStateFlow()
    
    companion object {
        const val ZAKAT_RATE = 0.025 // 2.5%
        const val GOLD_NISAB_GRAMS = 87.48
        const val SILVER_NISAB_GRAMS = 612.36
    }
    
    // Update functions for each field
    fun updateCashAndBank(value: String) {
        _uiState.value = _uiState.value.copy(cashAndBank = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateGoldValue(value: String) {
        _uiState.value = _uiState.value.copy(goldValueOrWeight = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateGoldInputType(inGrams: Boolean) {
        _uiState.value = _uiState.value.copy(isGoldInGrams = inGrams)
    }
    
    fun updateSilverValue(value: String) {
        _uiState.value = _uiState.value.copy(silverValueOrWeight = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateSilverInputType(inGrams: Boolean) {
        _uiState.value = _uiState.value.copy(isSilverInGrams = inGrams)
    }
    
    fun updateInvestments(value: String) {
        _uiState.value = _uiState.value.copy(investments = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateBusinessInventory(value: String) {
        _uiState.value = _uiState.value.copy(businessInventory = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateReceivables(value: String) {
        _uiState.value = _uiState.value.copy(receivables = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateOtherAssets(value: String) {
        _uiState.value = _uiState.value.copy(otherAssets = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateDebts(value: String) {
        _uiState.value = _uiState.value.copy(debts = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateExpenses(value: String) {
        _uiState.value = _uiState.value.copy(expenses = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateGoldPrice(value: String) {
        _uiState.value = _uiState.value.copy(goldPricePerGram = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateSilverPrice(value: String) {
        _uiState.value = _uiState.value.copy(silverPricePerGram = value.filter { it.isDigit() || it == '.' })
    }
    
    fun updateNisabType(type: NisabType) {
        _uiState.value = _uiState.value.copy(selectedNisabType = type)
        if (_uiState.value.showResults) {
            calculateZakat()
        }
    }
    
    fun updateCurrencySymbol(symbol: String) {
        _uiState.value = _uiState.value.copy(currencySymbol = symbol)
    }
    
    fun calculateZakat() {
        val state = _uiState.value
        
        val goldPrice = state.goldPricePerGram.toDoubleOrNull() ?: 0.0
        val silverPrice = state.silverPricePerGram.toDoubleOrNull() ?: 0.0
        
        // Calculate gold value
        val goldValue = if (state.isGoldInGrams) {
            (state.goldValueOrWeight.toDoubleOrNull() ?: 0.0) * goldPrice
        } else {
            state.goldValueOrWeight.toDoubleOrNull() ?: 0.0
        }
        
        // Calculate silver value
        val silverValue = if (state.isSilverInGrams) {
            (state.silverValueOrWeight.toDoubleOrNull() ?: 0.0) * silverPrice
        } else {
            state.silverValueOrWeight.toDoubleOrNull() ?: 0.0
        }
        
        // Total Assets
        val totalAssets = (state.cashAndBank.toDoubleOrNull() ?: 0.0) +
                goldValue +
                silverValue +
                (state.investments.toDoubleOrNull() ?: 0.0) +
                (state.businessInventory.toDoubleOrNull() ?: 0.0) +
                (state.receivables.toDoubleOrNull() ?: 0.0) +
                (state.otherAssets.toDoubleOrNull() ?: 0.0)
        
        // Total Liabilities
        val totalLiabilities = (state.debts.toDoubleOrNull() ?: 0.0) +
                (state.expenses.toDoubleOrNull() ?: 0.0)
        
        // Net Zakatable Wealth
        val netWealth = totalAssets - totalLiabilities
        
        // Calculate Nisab threshold based on selected type
        val nisabThreshold = when (state.selectedNisabType) {
            NisabType.GOLD -> GOLD_NISAB_GRAMS * goldPrice
            NisabType.SILVER -> SILVER_NISAB_GRAMS * silverPrice
        }
        
        // Check if above Nisab
        val isAboveNisab = netWealth >= nisabThreshold
        
        // Calculate Zakat due (only if above Nisab)
        val zakatDue = if (isAboveNisab) netWealth * ZAKAT_RATE else 0.0
        
        _uiState.value = state.copy(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netZakatableWealth = netWealth,
            nisabThreshold = nisabThreshold,
            isAboveNisab = isAboveNisab,
            zakatDue = zakatDue,
            showResults = true
        )
    }
    
    fun resetCalculator() {
        _uiState.value = ZakatUiState()
    }
    
    fun hideResults() {
        _uiState.value = _uiState.value.copy(showResults = false)
    }
}
