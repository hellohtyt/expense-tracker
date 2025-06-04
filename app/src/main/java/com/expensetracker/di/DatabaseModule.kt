package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.PayeeDao
import com.expensetracker.data.dao.TransactionDao
import com.expensetracker.data.database.ExpenseTrackerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideExpenseTrackerDatabase(
        @ApplicationContext context: Context
    ): ExpenseTrackerDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ExpenseTrackerDatabase::class.java,
            "expense_tracker_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideTransactionDao(database: ExpenseTrackerDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    fun provideCategoryDao(database: ExpenseTrackerDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    fun providePayeeDao(database: ExpenseTrackerDatabase): PayeeDao {
        return database.payeeDao()
    }
} 