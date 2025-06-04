package com.expensetracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.expensetracker.data.converter.Converters
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.PayeeDao
import com.expensetracker.data.dao.TransactionDao
import com.expensetracker.data.entity.Category
import com.expensetracker.data.entity.Payee
import com.expensetracker.data.entity.Transaction

@Database(
    entities = [Transaction::class, Category::class, Payee::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseTrackerDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun payeeDao(): PayeeDao
    
    companion object {
        @Volatile
        private var INSTANCE: ExpenseTrackerDatabase? = null
        
        fun getDatabase(context: Context): ExpenseTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseTrackerDatabase::class.java,
                    "expense_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 