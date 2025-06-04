package com.expensetracker.sms

import com.expensetracker.data.entity.TransactionType

class TransactionCategorizer {
    
    companion object {
        // Merchant to category mapping
        private val merchantCategoryMap = mapOf(
            // Food & Dining
            "ZOMATO" to "Food & Dining",
            "SWIGGY" to "Food & Dining", 
            "DOMINOS" to "Food & Dining",
            "PIZZA HUT" to "Food & Dining",
            "MCDONALD" to "Food & Dining",
            "KFC" to "Food & Dining",
            "BURGER KING" to "Food & Dining",
            "STARBUCKS" to "Food & Dining",
            "CAFE COFFEE DAY" to "Food & Dining",
            "RESTAURANT" to "Food & Dining",
            "HOTEL" to "Food & Dining",
            "FOOD" to "Food & Dining",
            
            // Groceries
            "BIG BASKET" to "Groceries",
            "GROFERS" to "Groceries", 
            "BLINKIT" to "Groceries",
            "DUNZO" to "Groceries",
            "DMART" to "Groceries",
            "RELIANCE FRESH" to "Groceries",
            "MORE" to "Groceries",
            "SPENCER" to "Groceries",
            "SUPERMARKET" to "Groceries",
            "GROCERY" to "Groceries",
            
            // Transportation  
            "UBER" to "Transportation",
            "OLA" to "Transportation",
            "RAPIDO" to "Transportation",
            "AUTO" to "Transportation",
            "TAXI" to "Transportation",
            "METRO" to "Transportation",
            "BUS" to "Transportation",
            "RAILWAY" to "Transportation",
            "IRCTC" to "Transportation",
            "FLIGHT" to "Transportation",
            "AIRLINE" to "Transportation",
            
            // Fuel
            "PETROL" to "Fuel",
            "DIESEL" to "Fuel",
            "FUEL" to "Fuel",
            "PUMP" to "Fuel",
            "BHARAT PETROLEUM" to "Fuel",
            "INDIAN OIL" to "Fuel",
            "HP PETROL" to "Fuel",
            "SHELL" to "Fuel",
            
            // Shopping
            "AMAZON" to "Shopping",
            "FLIPKART" to "Shopping",
            "MYNTRA" to "Shopping",
            "AJIO" to "Shopping",
            "NYKAA" to "Shopping",
            "MEESHO" to "Shopping",
            "SNAPDEAL" to "Shopping",
            "SHOPPING" to "Shopping",
            "MALL" to "Shopping",
            "STORE" to "Shopping",
            
            // Entertainment
            "NETFLIX" to "Entertainment",
            "AMAZON PRIME" to "Entertainment",
            "HOTSTAR" to "Entertainment",
            "SPOTIFY" to "Entertainment",
            "YOUTUBE" to "Entertainment",
            "CINEMA" to "Entertainment",
            "MOVIE" to "Entertainment",
            "THEATER" to "Entertainment",
            "GAMING" to "Entertainment",
            "GAMES" to "Entertainment",
            
            // Bills & Utilities
            "ELECTRICITY" to "Bills & Utilities",
            "POWER" to "Bills & Utilities",
            "GAS" to "Bills & Utilities",
            "WATER" to "Bills & Utilities",
            "INTERNET" to "Bills & Utilities",
            "BROADBAND" to "Bills & Utilities",
            "MOBILE" to "Bills & Utilities",
            "PHONE" to "Bills & Utilities",
            "RECHARGE" to "Bills & Utilities",
            "DTH" to "Bills & Utilities",
            "CABLE" to "Bills & Utilities",
            "BILL" to "Bills & Utilities",
            
            // Healthcare
            "HOSPITAL" to "Healthcare",
            "CLINIC" to "Healthcare",
            "DOCTOR" to "Healthcare",
            "PHARMACY" to "Healthcare",
            "MEDICAL" to "Healthcare",
            "MEDICINE" to "Healthcare",
            "APOLLO" to "Healthcare",
            "FORTIS" to "Healthcare",
            "MAX HEALTHCARE" to "Healthcare",
            
            // Education
            "SCHOOL" to "Education",
            "COLLEGE" to "Education",
            "UNIVERSITY" to "Education",
            "COURSE" to "Education",
            "TUITION" to "Education",
            "EDUCATION" to "Education",
            "EXAM" to "Education",
            "COACHING" to "Education",
            
            // Financial
            "MUTUAL FUND" to "Investment",
            "SIP" to "Investment", 
            "STOCK" to "Investment",
            "TRADING" to "Investment",
            "ZERODHA" to "Investment",
            "GROWW" to "Investment",
            "UPSTOX" to "Investment",
            "INVESTMENT" to "Investment",
            "INSURANCE" to "Investment",
            "EMI" to "Bills & Utilities",
            "LOAN" to "Bills & Utilities"
        )
        
        // Keywords that indicate salary/income
        private val incomeKeywords = listOf(
            "salary", "sal", "wages", "income", "bonus", "incentive", 
            "cashback", "refund", "reimbursement", "settlement"
        )
        
        // Keywords that indicate transfers
        private val transferKeywords = listOf(
            "transfer", "transferred", "fund transfer", "account transfer",
            "self", "own account", "wallet", "load", "top up"
        )
    }
    
    fun categorizeTransaction(
        payee: String?, 
        message: String, 
        transactionType: TransactionType
    ): String? {
        val messageUpper = message.uppercase()
        val payeeUpper = payee?.uppercase() ?: ""
        
        // Check for income-specific categories first
        if (transactionType == TransactionType.CREDIT) {
            when {
                incomeKeywords.any { messageUpper.contains(it.uppercase()) } -> return "Salary"
                messageUpper.contains("INTEREST") -> return "Investment"
                messageUpper.contains("DIVIDEND") -> return "Investment"
                messageUpper.contains("CASHBACK") || messageUpper.contains("REWARD") -> return "Other"
                messageUpper.contains("REFUND") -> return "Other"
            }
        }
        
        // Check for transfers
        if (transferKeywords.any { messageUpper.contains(it.uppercase()) || payeeUpper.contains(it.uppercase()) }) {
            return "Transfer"
        }
        
        // Check merchant mapping
        for ((merchant, category) in merchantCategoryMap) {
            if (messageUpper.contains(merchant) || payeeUpper.contains(merchant)) {
                return category
            }
        }
        
        // Check for pattern-based categorization
        return when {
            // Bills and utilities patterns
            messageUpper.contains("BILL") || messageUpper.contains("PAYMENT") -> "Bills & Utilities"
            
            // Shopping patterns
            messageUpper.contains("PURCHASE") || messageUpper.contains("SHOPPING") -> "Shopping"
            
            // Food patterns  
            messageUpper.contains("RESTAURANT") || messageUpper.contains("CAFE") -> "Food & Dining"
            
            // ATM withdrawals
            messageUpper.contains("ATM") || messageUpper.contains("CASH") -> "Other"
            
            // Default to null - let user categorize manually
            else -> null
        }
    }
} 