package com.expensetracker.data.dao

import androidx.room.*
import com.expensetracker.data.entity.Transaction
import com.expensetracker.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startDate: Instant, endDate: Instant): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE sender = :sender ORDER BY timestamp DESC")
    fun getTransactionsBySender(sender: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE payee LIKE '%' || :payee || '%' ORDER BY timestamp DESC")
    fun getTransactionsByPayee(payee: String): Flow<List<Transaction>>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType,
        startDate: Instant,
        endDate: Instant
    ): Double?
    
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'DEBIT' AND category IS NOT NULL AND timestamp BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    suspend fun getCategoryWiseExpenses(startDate: Instant, endDate: Instant): List<CategoryAmount>
    
    @Query("SELECT * FROM transactions WHERE message = :message AND sender = :sender AND ABS(timestamp - :timestamp) < 60000000000") // 1 minute tolerance
    suspend fun findDuplicateTransaction(message: String, sender: String, timestamp: Instant): Transaction?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)
}

data class CategoryAmount(
    val category: String,
    val total: Double
) 