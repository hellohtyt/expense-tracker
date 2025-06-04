package com.expensetracker.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.expensetracker.data.entity.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsReader(private val context: Context) {
    
    private val smsParser = SmsParser()
    
    suspend fun readBankSms(limitDays: Int = 30): List<Transaction> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) {
            return@withContext emptyList()
        }
        
        val transactions = mutableListOf<Transaction>()
        val uri = Uri.parse("content://sms/inbox")
        
        // Calculate timestamp for limiting days
        val limitTimestamp = System.currentTimeMillis() - (limitDays * 24 * 60 * 60 * 1000L)
        
        val projection = arrayOf(
            "_id",
            "address", // sender
            "body",    // message content
            "date"     // timestamp
        )
        
        val selection = "date >= ?"
        val selectionArgs = arrayOf(limitTimestamp.toString())
        val sortOrder = "date DESC"
        
        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                
                val addressColumn = cursor.getColumnIndexOrThrow("address")
                val bodyColumn = cursor.getColumnIndexOrThrow("body")
                val dateColumn = cursor.getColumnIndexOrThrow("date")
                
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressColumn) ?: continue
                    val message = cursor.getString(bodyColumn) ?: continue
                    val timestamp = cursor.getLong(dateColumn)
                    
                    // Debug logging
                    println("SMS Reader - Found SMS from: '$sender', Message: '${message.take(50)}...'")
                    
                    // Parse the SMS message
                    val transaction = smsParser.parseTransaction(sender, message, timestamp)
                    if (transaction != null) {
                        println("SMS Reader - Successfully parsed transaction: ${transaction.amount} ${transaction.type}")
                        transactions.add(transaction)
                    } else {
                        println("SMS Reader - Failed to parse as transaction")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext transactions
    }
    
    suspend fun readSmsFromSender(sender: String, limitDays: Int = 30): List<Transaction> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) {
            return@withContext emptyList()
        }
        
        val transactions = mutableListOf<Transaction>()
        val uri = Uri.parse("content://sms/inbox")
        
        val limitTimestamp = System.currentTimeMillis() - (limitDays * 24 * 60 * 60 * 1000L)
        
        val projection = arrayOf(
            "_id",
            "address",
            "body",
            "date"
        )
        
        val selection = "address LIKE ? AND date >= ?"
        val selectionArgs = arrayOf("%$sender%", limitTimestamp.toString())
        val sortOrder = "date DESC"
        
        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                
                val addressColumn = cursor.getColumnIndexOrThrow("address")
                val bodyColumn = cursor.getColumnIndexOrThrow("body")
                val dateColumn = cursor.getColumnIndexOrThrow("date")
                
                while (cursor.moveToNext()) {
                    val senderAddress = cursor.getString(addressColumn) ?: continue
                    val message = cursor.getString(bodyColumn) ?: continue
                    val timestamp = cursor.getLong(dateColumn)
                    
                    val transaction = smsParser.parseTransaction(senderAddress, message, timestamp)
                    if (transaction != null) {
                        transactions.add(transaction)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext transactions
    }
    
    suspend fun getAllBankSenders(): List<String> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) {
            return@withContext emptyList()
        }
        
        val senders = mutableSetOf<String>()
        val uri = Uri.parse("content://sms/inbox")
        
        val projection = arrayOf("address")
        val limitTimestamp = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L) // 90 days
        val selection = "date >= ?"
        val selectionArgs = arrayOf(limitTimestamp.toString())
        
        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                
                val addressColumn = cursor.getColumnIndexOrThrow("address")
                
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressColumn) ?: continue
                    
                    // Check if this sender sends bank-like messages
                    if (isPotentialBankSender(sender)) {
                        senders.add(sender)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext senders.toList().sorted()
    }
    
    private fun hasReadSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun isPotentialBankSender(sender: String): Boolean {
        // Be very liberal - check any SMS that might contain transaction info
        // The parser will do the real filtering based on message content
        return true // Process all SMS messages and let the parser decide
    }
} 