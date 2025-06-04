package com.expensetracker.sms

import com.expensetracker.data.entity.Transaction
import com.expensetracker.data.entity.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.regex.Pattern

class SmsParser {
    
    private val categorizer = TransactionCategorizer()
    
    companion object {
        // Known bank senders
        private val BANK_SENDERS = setOf(
            "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "YES", "IDFC", "PAYTM", "PHONEPE", 
            "GPAY", "AMAZONPAY", "MOBIKWIK", "FREECHARGE", "MYNTRA", "FLIPKART", "UBER"
        )
        
        // Debit patterns
        private val DEBIT_PATTERNS = listOf(
            // ICICI Bank specific: "debited for Rs 35.00 on 04-Jun-25; Srikonda Santho credited"
            Pattern.compile("debited\\s+for\\s+Rs\\s*([\\d,]+\\.?\\d*).*?;\\s*([A-Z][A-Z\\s]+)\\s+credited", Pattern.CASE_INSENSITIVE),
            // Rs 1234.56 debited from A/c ending 1234 on 01-01-23 to PAYEE_NAME
            Pattern.compile("Rs\\s*([\\d,]+\\.?\\d*)\\s*debited.*?to\\s+([^\\s].+?)(?:\\s+at|\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            // Amount Rs 1234.56 debited from account ending 1234 at PAYEE_NAME
            Pattern.compile("(?:Amount\\s+)?Rs\\s*([\\d,]+\\.?\\d*)\\s*debited.*?at\\s+([^\\s].+?)(?:\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            // Debited Rs 1234.56 from A/c xx1234 for PAYEE_NAME
            Pattern.compile("debited\\s+Rs\\s*([\\d,]+\\.?\\d*).*?for\\s+([^\\s].+?)(?:\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            // UPI debit Rs 1234.56 to PAYEE_NAME
            Pattern.compile("UPI.*?debit.*?Rs\\s*([\\d,]+\\.?\\d*).*?to\\s+([^\\s].+?)(?:\\s+\\||\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            // Payment of Rs 1234.56 made to PAYEE_NAME
            Pattern.compile("Payment\\s+of\\s+Rs\\s*([\\d,]+\\.?\\d*).*?(?:made\\s+to|to)\\s+([^\\s].+?)(?:\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE)
        )
        
        // Credit patterns
        private val CREDIT_PATTERNS = listOf(
            // Rs 1234.56 credited to A/c ending 1234 from PAYEE_NAME
            Pattern.compile("Rs\\s*([\\d,]+\\.?\\d*)\\s*credited.*?from\\s+([^\\s].+?)(?:\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            // Amount Rs 1234.56 credited to account ending 1234 by PAYEE_NAME
            Pattern.compile("(?:Amount\\s+)?Rs\\s*([\\d,]+\\.?\\d*)\\s*credited.*?by\\s+([^\\s].+?)(?:\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            // Received Rs 1234.56 from PAYEE_NAME
            Pattern.compile("Received\\s+Rs\\s*([\\d,]+\\.?\\d*).*?from\\s+([^\\s].+?)(?:\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            // UPI credit Rs 1234.56 from PAYEE_NAME
            Pattern.compile("UPI.*?credit.*?Rs\\s*([\\d,]+\\.?\\d*).*?from\\s+([^\\s].+?)(?:\\s+\\||\\s+on|\\.|$)", Pattern.CASE_INSENSITIVE),
            // Salary credited Rs 1234.56
            Pattern.compile("(?:Salary|salary).*?credited.*?Rs\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
        )
        
        // Amount-only patterns (when payee extraction fails)
        private val AMOUNT_PATTERNS = listOf(
            Pattern.compile("Rs\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("INR\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("₹\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
        )
    }
    
    fun parseTransaction(sender: String, message: String, timestamp: Long = System.currentTimeMillis()): Transaction? {
        // Debug logging
        println("SMS Parser - Sender: '$sender', Message: '$message'")
        
        // Comprehensive transaction keyword detection
        val transactionKeywords = listOf(
            // Debit keywords
            "debited", "debit", "paid", "payment", "withdrawn", "withdraw", "purchase", "spent", 
            "charged", "deducted", "transferred", "transfer",
            // Credit keywords  
            "credited", "credit", "received", "deposited", "deposit", "refund", "salary", "income",
            // Currency indicators
            "Rs", "INR", "₹", "rupees",
            // UPI/Payment indicators
            "UPI", "NEFT", "RTGS", "IMPS", "wallet", "paytm", "phonepe", "gpay", "amazon pay"
        )
        
        val messageText = message.lowercase()
        val hasTransactionKeyword = transactionKeywords.any { keyword -> 
            messageText.contains(keyword.lowercase()) 
        }
        
        // Also check if message has amount pattern (Rs X.XX or similar)
        val hasAmountPattern = message.matches(Regex(".*(?:Rs|INR|₹)\\s*[\\d,]+(?:\\.\\d{1,2})?.*", RegexOption.IGNORE_CASE))
        
        val isBankMessage = isBankSender(sender) || hasTransactionKeyword || hasAmountPattern
        
        if (!isBankMessage) {
            println("SMS Parser - Not recognized as transaction message")
            return null
        }
        
        println("SMS Parser - Recognized as potential transaction message")
        val instantTimestamp = Instant.fromEpochMilliseconds(timestamp)
        
        // Try debit patterns first
        for (pattern in DEBIT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amount = parseAmount(matcher.group(1))
                val payee = if (matcher.groupCount() >= 2) {
                    cleanPayeeName(matcher.group(2))
                } else {
                    extractPayeeFromMessage(message)
                }
                
                return Transaction(
                    sender = sender,
                    message = message,
                    amount = amount,
                    type = TransactionType.DEBIT,
                    payee = payee,
                    category = categorizer.categorizeTransaction(payee, message, TransactionType.DEBIT),
                    timestamp = instantTimestamp
                )
            }
        }
        
        // Try credit patterns
        for (pattern in CREDIT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amount = parseAmount(matcher.group(1))
                val payee = if (matcher.groupCount() >= 2) {
                    cleanPayeeName(matcher.group(2))
                } else {
                    extractPayeeFromMessage(message)
                }
                
                return Transaction(
                    sender = sender,
                    message = message,
                    amount = amount,
                    type = TransactionType.CREDIT,
                    payee = payee ?: "Unknown",
                    category = categorizer.categorizeTransaction(payee, message, TransactionType.CREDIT),
                    timestamp = instantTimestamp
                )
            }
        }
        
        // If no specific pattern matches, try to extract amount and determine type from keywords
        val amount = extractAmountFromMessage(message)
        if (amount != null) {
            val type = determineTransactionType(message)
            val payee = extractPayeeFromMessage(message)
            
            return Transaction(
                sender = sender,
                message = message,
                amount = amount,
                type = type,
                payee = payee,
                category = categorizer.categorizeTransaction(payee, message, type),
                timestamp = instantTimestamp
            )
        }
        
        return null
    }
    
    private fun isBankSender(sender: String): Boolean {
        return BANK_SENDERS.any { sender.contains(it, ignoreCase = true) }
    }
    
    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace(",", "").replace(" ", "").toDoubleOrNull() ?: 0.0
    }
    
    private fun extractAmountFromMessage(message: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return parseAmount(matcher.group(1))
            }
        }
        return null
    }
    
    private fun determineTransactionType(message: String): TransactionType {
        val debitKeywords = listOf(
            "debited", "debit", "payment", "paid", "withdrawn", "withdraw", "purchase", "spent", 
            "charged", "deducted", "transferred", "transfer", "shopping", "bought", "bill", 
            "subscription", "fee", "emi", "loan", "card", "swipe", "online"
        )
        val creditKeywords = listOf(
            "credited", "credit", "received", "deposited", "deposit", "refund", "salary", "income",
            "bonus", "cashback", "reward", "interest", "dividend", "settlement", "reimbursement"
        )
        
        val lowerMessage = message.lowercase()
        
        // Check for explicit debit indicators first
        val hasDebitKeyword = debitKeywords.any { lowerMessage.contains(it) }
        val hasCreditKeyword = creditKeywords.any { lowerMessage.contains(it) }
        
        return when {
            hasDebitKeyword && !hasCreditKeyword -> TransactionType.DEBIT
            hasCreditKeyword && !hasDebitKeyword -> TransactionType.CREDIT
            // If both or neither, try to detect from context
            lowerMessage.contains("to") && !lowerMessage.contains("from") -> TransactionType.DEBIT
            lowerMessage.contains("from") && !lowerMessage.contains("to") -> TransactionType.CREDIT
            else -> TransactionType.DEBIT // Default to debit if uncertain
        }
    }
    
    private fun extractPayeeFromMessage(message: String): String? {
        // Common patterns to extract payee names
        val payeePatterns = listOf(
            Pattern.compile("(?:to|at|from)\\s+([A-Z][A-Z\\s]{2,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("UPI.*?-([A-Z][A-Z\\s]{2,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([A-Z]{3,}\\s*[A-Z]*\\s*[A-Z]*)\\b")
        )
        
        for (pattern in payeePatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val payee = cleanPayeeName(matcher.group(1))
                if (payee?.length ?: 0 >= 3) {
                    return payee
                }
            }
        }
        
        return null
    }
    
    private fun cleanPayeeName(payee: String?): String? {
        if (payee == null) return null
        
        // Remove common noise words and patterns
        val cleaned = payee
            .replace(Regex("\\b(UPI|NEFT|RTGS|IMPS|\\d+)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^a-zA-Z\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
        
        return if (cleaned.length >= 3) cleaned else null
    }
} 