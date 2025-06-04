package com.expensetracker.repository

import com.expensetracker.data.dao.CategoryAmount
import com.expensetracker.data.dao.PayeeAmount
import com.expensetracker.data.dao.TransactionDao
import com.expensetracker.data.entity.Transaction
import com.expensetracker.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByType(type)
    
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategory(category)
    
    fun getTransactionsByDateRange(startDate: Instant, endDate: Instant): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByDateRange(startDate, endDate)
    
    fun getTransactionsBySender(sender: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsBySender(sender)
    
    fun getTransactionsByPayee(payee: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByPayee(payee)
    
    suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType,
        startDate: Instant,
        endDate: Instant
    ): Double = transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate) ?: 0.0
    
    suspend fun getCategoryWiseExpenses(startDate: Instant, endDate: Instant): List<CategoryAmount> = 
        transactionDao.getCategoryWiseExpenses(startDate, endDate)
    
    suspend fun insertTransaction(transaction: Transaction) {
        // Check for duplicates before inserting
        val duplicate = transactionDao.findDuplicateTransaction(
            transaction.message,
            transaction.sender,
            transaction.timestamp
        )
        
        if (duplicate == null) {
            transactionDao.insertTransaction(transaction)
        }
        // Skip duplicate transactions entirely
    }
    
    suspend fun insertTransactions(transactions: List<Transaction>) {
        val uniqueTransactions = mutableListOf<Transaction>()
        
        for (transaction in transactions) {
            val duplicate = transactionDao.findDuplicateTransaction(
                transaction.message,
                transaction.sender,
                transaction.timestamp
            )
            
            if (duplicate == null) {
                uniqueTransactions.add(transaction)
            }
            // Skip duplicates entirely
        }
        
        if (uniqueTransactions.isNotEmpty()) {
            transactionDao.insertTransactions(uniqueTransactions)
        }
    }
    
    suspend fun updateTransaction(transaction: Transaction) = 
        transactionDao.updateTransaction(transaction)
    
    suspend fun deleteTransaction(transaction: Transaction) = 
        transactionDao.deleteTransaction(transaction)
    
    suspend fun deleteTransactionById(transactionId: String) = 
        transactionDao.deleteTransactionById(transactionId)
    
    // Smart categorization
    suspend fun getLastCategoryForPayee(payee: String): String? = 
        transactionDao.getLastCategoryForPayee(payee)
    
    // Payee analytics
    suspend fun getPayeeWiseSpending(): List<PayeeAmount> = 
        transactionDao.getPayeeWiseSpending()
    
    suspend fun getPayeeWiseSpendingByDateRange(startDate: Instant, endDate: Instant): List<PayeeAmount> = 
        transactionDao.getPayeeWiseSpendingByDateRange(startDate, endDate)
} 