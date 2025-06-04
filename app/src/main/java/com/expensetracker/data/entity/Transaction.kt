package com.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val message: String,
    val amount: Double,
    val type: TransactionType,
    val payee: String?,
    val category: String?,
    val timestamp: Instant,
    val isVerified: Boolean = false, // User can verify/confirm parsed data
    val isDuplicate: Boolean = false // For duplicate detection
)

enum class TransactionType {
    DEBIT,
    CREDIT
} 