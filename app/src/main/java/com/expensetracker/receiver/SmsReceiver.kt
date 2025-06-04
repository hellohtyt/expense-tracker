package com.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.expensetracker.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    
    private val smsParser = SmsParser()
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                val sender = smsMessage.originatingAddress ?: continue
                val messageBody = smsMessage.messageBody ?: continue
                val timestamp = smsMessage.timestampMillis
                
                // Parse the SMS in background
                CoroutineScope(Dispatchers.IO).launch {
                    processIncomingSms(context, sender, messageBody, timestamp)
                }
            }
        }
    }
    
    private suspend fun processIncomingSms(
        context: Context,
        sender: String,
        message: String,
        timestamp: Long
    ) {
        try {
            val transaction = smsParser.parseTransaction(sender, message, timestamp)
            
            if (transaction != null) {
                // Here we would save to database
                // For now, we'll just log it
                android.util.Log.d(
                    "SmsReceiver", 
                    "Parsed transaction: ${transaction.type} of ₹${transaction.amount} from ${transaction.payee}"
                )
                
                // TODO: Inject repository and save transaction
                // repository.insertTransaction(transaction)
            }
        } catch (e: Exception) {
            android.util.Log.e("SmsReceiver", "Error processing SMS", e)
        }
    }
} 