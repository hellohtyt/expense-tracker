package com.expensetracker.ui.screen.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.entity.Transaction
import com.expensetracker.data.entity.TransactionType
import com.expensetracker.data.entity.Category
import com.expensetracker.repository.TransactionRepository
import com.expensetracker.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedTransactionIds: Set<String> = emptySet(),
    val isAllSelected: Boolean = false
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        loadTransactions()
    }
    
    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                transactionRepository.getAllTransactions().collect { transactions ->
                    _uiState.value = _uiState.value.copy(
                        transactions = transactions,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun enterSelectionMode() {
        _uiState.value = _uiState.value.copy(isSelectionMode = true)
    }
    
    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedTransactionIds = emptySet(),
            isAllSelected = false
        )
    }
    
    fun toggleTransactionSelection(transactionId: String) {
        val currentSelected = _uiState.value.selectedTransactionIds
        val newSelected = if (currentSelected.contains(transactionId)) {
            currentSelected - transactionId
        } else {
            currentSelected + transactionId
        }
        
        val isAllSelected = newSelected.size == _uiState.value.transactions.size
        
        _uiState.value = _uiState.value.copy(
            selectedTransactionIds = newSelected,
            isAllSelected = isAllSelected
        )
    }
    
    fun toggleSelectAll() {
        val allTransactionIds = _uiState.value.transactions.map { it.id }.toSet()
        val newSelected = if (_uiState.value.isAllSelected) {
            emptySet()
        } else {
            allTransactionIds
        }
        
        _uiState.value = _uiState.value.copy(
            selectedTransactionIds = newSelected,
            isAllSelected = !_uiState.value.isAllSelected
        )
    }
    
    fun deleteSelectedTransactions() {
        viewModelScope.launch {
            try {
                val selectedIds = _uiState.value.selectedTransactionIds.toList()
                selectedIds.forEach { id ->
                    transactionRepository.deleteTransactionById(id)
                }
                // Exit selection mode after deletion
                exitSelectionMode()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun updateTransactionCategory(transaction: Transaction, categoryName: String?) {
        viewModelScope.launch {
            try {
                val updatedTransaction = transaction.copy(category = categoryName)
                transactionRepository.updateTransaction(updatedTransaction)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
} 