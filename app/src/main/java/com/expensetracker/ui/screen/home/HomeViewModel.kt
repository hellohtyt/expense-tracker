package com.expensetracker.ui.screen.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.entity.Transaction
import com.expensetracker.data.entity.TransactionType
import com.expensetracker.repository.CategoryRepository
import com.expensetracker.repository.TransactionRepository
import com.expensetracker.sms.SmsReader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import javax.inject.Inject

data class HomeUiState(
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val totalTransactions: Int = 0,
    val averageExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val smsReader = SmsReader(context)
    
    init {
        loadData()
        initializeDefaultCategories()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Get current month start and end
            val now = Clock.System.now()
            val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val monthStart = kotlinx.datetime.LocalDateTime(
                localDateTime.year,
                localDateTime.month,
                1,
                0, 0, 0, 0
            ).toInstant(TimeZone.currentSystemDefault())
            
            val monthEnd = now
            
            // Collect recent transactions and calculate stats
            launch {
                transactionRepository.getAllTransactions().collect { transactions ->
                    val recentTransactions = transactions.take(10)
                    val totalTransactions = transactions.size
                    
                    // Calculate average expense (debit transactions only)
                    val expenseTransactions = transactions.filter { it.type == TransactionType.DEBIT }
                    val averageExpense = if (expenseTransactions.isNotEmpty()) {
                        expenseTransactions.sumOf { it.amount } / expenseTransactions.size
                    } else {
                        0.0
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        recentTransactions = recentTransactions,
                        totalTransactions = totalTransactions,
                        averageExpense = averageExpense
                    )
                }
            }
            
            // Calculate monthly totals
            launch {
                val monthlyIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                    TransactionType.CREDIT, monthStart, monthEnd
                )
                val monthlyExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                    TransactionType.DEBIT, monthStart, monthEnd
                )
                
                _uiState.value = _uiState.value.copy(
                    monthlyIncome = monthlyIncome,
                    monthlyExpenses = monthlyExpenses
                )
            }
        }
    }
    
    private fun initializeDefaultCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.insertDefaultCategories()
            } catch (e: Exception) {
                // Categories might already exist, ignore error
            }
        }
    }
    
    fun syncSmsTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val transactions = smsReader.readBankSms(limitDays = 30)
                transactionRepository.insertTransactions(transactions)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (transactions.isEmpty()) "No bank transactions found in SMS" else null
                )
                
                // Reload data after sync
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error syncing SMS: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun <T> Flow<T>.asFlow(): Flow<T> = this
} 