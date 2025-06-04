package com.expensetracker.ui.screen.insights

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.dao.CategoryAmount
import com.expensetracker.data.entity.TransactionType
import com.expensetracker.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

data class ChartData(
    val label: String,
    val value: Float,
    val date: LocalDate
)

data class PeriodData(
    val income: Double,
    val expenses: Double,
    val period: String
)

enum class TimePeriod {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
}

data class InsightsUiState(
    val selectedPeriod: TimePeriod = TimePeriod.MONTHLY,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val categoryExpenses: List<CategoryAmount> = emptyList(),
    val totalTransactions: Int = 0,
    val avgTransactionAmount: Double = 0.0,
    val largestExpense: Double = 0.0,
    val periodData: List<PeriodData> = emptyList(),
    val expenseChartData: List<ChartData> = emptyList(),
    val incomeChartData: List<ChartData> = emptyList(),
    
    // Enhanced analytics
    val previousPeriodComparison: PreviousPeriodComparison? = null,
    val dayOfWeekSpending: Map<String, Double> = emptyMap(),
    val timeOfDaySpending: Map<String, Double> = emptyMap(),
    val financialInsights: List<FinancialInsight> = emptyList(),
    val savingsRate: Double = 0.0,
    val expenseFrequency: Double = 0.0,
    val budgetStatus: List<BudgetStatus> = emptyList(),
    
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    
    init {
        loadInsights()
    }
    
    fun selectTimePeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadInsights()
    }
    
    fun refreshInsights() {
        loadInsights()
    }
    
    private fun loadInsights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val selectedPeriod = _uiState.value.selectedPeriod
                
                // Get current period boundaries
                val now = Clock.System.now()
                val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                val (periodStart, periodEnd) = getPeriodBounds(localDateTime, selectedPeriod)
                
                // Launch multiple coroutines to gather data in parallel
                launch {
                    // Period totals
                    val periodIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.CREDIT, periodStart, periodEnd
                    )
                    val periodExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.DEBIT, periodStart, periodEnd
                    )
                    
                    // Calculate savings rate
                    val savingsRate = if (periodIncome > 0) {
                        ((periodIncome - periodExpenses) / periodIncome) * 100
                    } else 0.0
                    
                    _uiState.value = _uiState.value.copy(
                        monthlyIncome = periodIncome,
                        monthlyExpenses = periodExpenses,
                        savingsRate = savingsRate
                    )
                }
                
                launch {
                    // Category-wise expenses
                    val categoryExpenses = transactionRepository.getCategoryWiseExpenses(periodStart, periodEnd)
                        .sortedByDescending { it.total }
                    
                    _uiState.value = _uiState.value.copy(categoryExpenses = categoryExpenses)
                }
                
                launch {
                    // Chart data generation
                    val chartData = generateChartData(selectedPeriod, localDateTime)
                    _uiState.value = _uiState.value.copy(
                        expenseChartData = chartData.first,
                        incomeChartData = chartData.second,
                        periodData = chartData.third
                    )
                }
                
                launch {
                    // Previous period comparison
                    val previousComparison = calculatePreviousPeriodComparison(
                        localDateTime, selectedPeriod, 
                        _uiState.value.monthlyIncome, _uiState.value.monthlyExpenses
                    )
                    _uiState.value = _uiState.value.copy(previousPeriodComparison = previousComparison)
                }
                
                launch {
                    // Spending patterns analysis
                    val allTransactions = transactionRepository.getAllTransactions().first()
                    val periodTransactions = allTransactions.filter { transaction ->
                        transaction.timestamp >= periodStart && transaction.timestamp <= periodEnd
                    }
                    
                    val dayOfWeekSpending = analyzeDayOfWeekSpending(periodTransactions)
                    val timeOfDaySpending = analyzeTimeOfDaySpending(periodTransactions)
                    
                    _uiState.value = _uiState.value.copy(
                        dayOfWeekSpending = dayOfWeekSpending,
                        timeOfDaySpending = timeOfDaySpending
                    )
                }
                
                launch {
                    // Financial insights generation
                    val insights = generateFinancialInsights(
                        _uiState.value.monthlyIncome,
                        _uiState.value.monthlyExpenses,
                        _uiState.value.categoryExpenses
                    )
                    _uiState.value = _uiState.value.copy(financialInsights = insights)
                }
                
                launch {
                    // General transaction stats
                    transactionRepository.getAllTransactions().first().let { transactions ->
                        val periodTransactions = transactions.filter { transaction ->
                            transaction.timestamp >= periodStart && transaction.timestamp <= periodEnd
                        }
                        
                        val totalTransactions = periodTransactions.size
                        val avgAmount = if (totalTransactions > 0) {
                            periodTransactions.map { it.amount }.average()
                        } else 0.0
                        
                        val largestExpense = periodTransactions
                            .filter { it.type == TransactionType.DEBIT }
                            .maxOfOrNull { it.amount } ?: 0.0
                        
                        // Calculate daily spending frequency
                        val daysDiff = periodStart.daysUntil(periodEnd, TimeZone.currentSystemDefault())
                        val expenseFrequency = if (daysDiff > 0) {
                            _uiState.value.monthlyExpenses / daysDiff
                        } else 0.0
                        
                        _uiState.value = _uiState.value.copy(
                            totalTransactions = totalTransactions,
                            avgTransactionAmount = avgAmount,
                            largestExpense = largestExpense,
                            expenseFrequency = expenseFrequency,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    private suspend fun calculatePreviousPeriodComparison(
        current: LocalDateTime,
        period: TimePeriod,
        currentIncome: Double,
        currentExpenses: Double
    ): PreviousPeriodComparison? {
        return try {
            val (prevStart, prevEnd) = getPreviousPeriodBounds(current, period)
            
            val prevIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.CREDIT, prevStart, prevEnd
            )
            val prevExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.DEBIT, prevStart, prevEnd
            )
            
            val incomeChange = if (prevIncome > 0) {
                ((currentIncome - prevIncome) / prevIncome) * 100
            } else 0.0
            
            val expenseChange = if (prevExpenses > 0) {
                ((currentExpenses - prevExpenses) / prevExpenses) * 100
            } else 0.0
            
            val currentBalance = currentIncome - currentExpenses
            val prevBalance = prevIncome - prevExpenses
            val balanceChange = if (prevBalance != 0.0) {
                ((currentBalance - prevBalance) / kotlin.math.abs(prevBalance)) * 100
            } else 0.0
            
            PreviousPeriodComparison(
                incomeChange = incomeChange,
                expenseChange = expenseChange,
                balanceChange = balanceChange
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun analyzeDayOfWeekSpending(transactions: List<com.expensetracker.data.entity.Transaction>): Map<String, Double> {
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val expensesByDay = mutableMapOf<String, Double>()
        
        dayNames.forEach { day -> expensesByDay[day] = 0.0 }
        
        transactions.filter { it.type == TransactionType.DEBIT }.forEach { transaction ->
            val localDateTime = transaction.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
            val dayOfWeek = localDateTime.dayOfWeek.ordinal // 0 = Monday
            val dayName = dayNames[dayOfWeek]
            expensesByDay[dayName] = (expensesByDay[dayName] ?: 0.0) + transaction.amount
        }
        
        return expensesByDay.toMap()
    }
    
    private fun analyzeTimeOfDaySpending(transactions: List<com.expensetracker.data.entity.Transaction>): Map<String, Double> {
        val timeSlots = mapOf(
            "Morning (6-12)" to (6..11),
            "Afternoon (12-18)" to (12..17),
            "Evening (18-24)" to (18..23),
            "Night (0-6)" to (0..5)
        )
        
        val expensesByTime = mutableMapOf<String, Double>()
        timeSlots.keys.forEach { slot -> expensesByTime[slot] = 0.0 }
        
        transactions.filter { it.type == TransactionType.DEBIT }.forEach { transaction ->
            val localDateTime = transaction.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
            val hour = localDateTime.hour
            
            timeSlots.forEach { (slotName, range) ->
                if (hour in range) {
                    expensesByTime[slotName] = (expensesByTime[slotName] ?: 0.0) + transaction.amount
                }
            }
        }
        
        return expensesByTime.toMap()
    }
    
    private fun generateFinancialInsights(
        income: Double,
        expenses: Double,
        categoryExpenses: List<CategoryAmount>
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        
        // Savings rate insight
        val savingsRate = if (income > 0) ((income - expenses) / income) * 100 else 0.0
        when {
            savingsRate >= 20 -> insights.add(
                FinancialInsight(
                    title = "Excellent Savings!",
                    description = "You're saving ${savingsRate.toInt()}% of your income. Keep up the great work!",
                    icon = Icons.Filled.TrendingUp,
                    color = Color(0xFF4CAF50)
                )
            )
            savingsRate >= 10 -> insights.add(
                FinancialInsight(
                    title = "Good Savings Rate",
                    description = "You're saving ${savingsRate.toInt()}% of your income. Consider increasing to 20%.",
                    icon = Icons.Filled.Savings,
                    color = Color(0xFFFF9800)
                )
            )
            savingsRate > 0 -> insights.add(
                FinancialInsight(
                    title = "Room for Improvement",
                    description = "You're saving ${savingsRate.toInt()}% of your income. Try to aim for at least 10%.",
                    icon = Icons.Filled.Warning,
                    color = Color(0xFFE57373)
                )
            )
            else -> insights.add(
                FinancialInsight(
                    title = "Focus on Saving",
                    description = "Your expenses exceed your income. Consider reducing spending in top categories.",
                    icon = Icons.Filled.Error,
                    color = Color(0xFFE57373)
                )
            )
        }
        
        // Top spending category insight
        if (categoryExpenses.isNotEmpty()) {
            val topCategory = categoryExpenses.first()
            val percentage = if (expenses > 0) (topCategory.total / expenses) * 100 else 0.0
            
            if (percentage > 40) {
                insights.add(
                    FinancialInsight(
                        title = "High Category Spending",
                        description = "${topCategory.category} accounts for ${percentage.toInt()}% of your expenses. Consider reducing this category.",
                        icon = Icons.Filled.Category,
                        color = Color(0xFFFF9800)
                    )
                )
            }
        }
        
        // Expense frequency insight
        if (expenses > 0) {
            val dailySpending = expenses / 30 // Assuming monthly period
            insights.add(
                FinancialInsight(
                    title = "Daily Spending",
                    description = "You spend an average of ₹${(dailySpending).toInt()} per day.",
                    icon = Icons.Filled.CalendarToday,
                    color = Color(0xFF2196F3)
                )
            )
        }
        
        return insights
    }
    
    private fun getPreviousPeriodBounds(current: LocalDateTime, period: TimePeriod): Pair<Instant, Instant> {
        val timeZone = TimeZone.currentSystemDefault()
        
        return when (period) {
            TimePeriod.DAILY -> {
                val prevDay = current.date.minus(1, DateTimeUnit.DAY)
                val start = prevDay.atTime(0, 0, 0, 0)
                val end = prevDay.plus(1, DateTimeUnit.DAY).atTime(0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
            TimePeriod.WEEKLY -> {
                val currentWeekStart = current.date.minus(current.dayOfWeek.ordinal, DateTimeUnit.DAY)
                val prevWeekStart = currentWeekStart.minus(7, DateTimeUnit.DAY)
                val start = prevWeekStart.atTime(0, 0, 0, 0)
                val end = prevWeekStart.plus(7, DateTimeUnit.DAY).atTime(0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
            TimePeriod.MONTHLY -> {
                val prevMonth = current.date.minus(1, DateTimeUnit.MONTH)
                val start = LocalDateTime(prevMonth.year, prevMonth.month, 1, 0, 0, 0, 0)
                val end = start.date.plus(1, DateTimeUnit.MONTH).atTime(0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
            TimePeriod.QUARTERLY -> {
                val currentQuarterStart = when (current.month.number) {
                    in 1..3 -> Month.JANUARY
                    in 4..6 -> Month.APRIL
                    in 7..9 -> Month.JULY
                    else -> Month.OCTOBER
                }
                val prevQuarterStart = when (currentQuarterStart) {
                    Month.JANUARY -> LocalDateTime(current.year - 1, Month.OCTOBER, 1, 0, 0, 0, 0)
                    Month.APRIL -> LocalDateTime(current.year, Month.JANUARY, 1, 0, 0, 0, 0)
                    Month.JULY -> LocalDateTime(current.year, Month.APRIL, 1, 0, 0, 0, 0)
                    else -> LocalDateTime(current.year, Month.JULY, 1, 0, 0, 0, 0)
                }
                val end = prevQuarterStart.date.plus(3, DateTimeUnit.MONTH).atTime(0, 0, 0, 0)
                prevQuarterStart.toInstant(timeZone) to end.toInstant(timeZone)
            }
            TimePeriod.YEARLY -> {
                val start = LocalDateTime(current.year - 1, Month.JANUARY, 1, 0, 0, 0, 0)
                val end = LocalDateTime(current.year, Month.JANUARY, 1, 0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
        }
    }
    
    private fun getPeriodBounds(current: LocalDateTime, period: TimePeriod): Pair<Instant, Instant> {
        val timeZone = TimeZone.currentSystemDefault()
        
        return when (period) {
            TimePeriod.DAILY -> {
                val start = LocalDateTime(current.year, current.month, current.dayOfMonth, 0, 0, 0, 0)
                val end = start.date.plus(1, DateTimeUnit.DAY).atTime(0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
            TimePeriod.WEEKLY -> {
                val startOfWeek = current.date.minus(current.dayOfWeek.ordinal, DateTimeUnit.DAY)
                val start = startOfWeek.atTime(0, 0, 0, 0)
                val end = startOfWeek.plus(7, DateTimeUnit.DAY).atTime(0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
            TimePeriod.MONTHLY -> {
                val start = LocalDateTime(current.year, current.month, 1, 0, 0, 0, 0)
                val end = start.date.plus(1, DateTimeUnit.MONTH).atTime(0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
            TimePeriod.QUARTERLY -> {
                val quarterStart = when (current.month.number) {
                    in 1..3 -> Month.JANUARY
                    in 4..6 -> Month.APRIL
                    in 7..9 -> Month.JULY
                    else -> Month.OCTOBER
                }
                val start = LocalDateTime(current.year, quarterStart, 1, 0, 0, 0, 0)
                val end = start.date.plus(3, DateTimeUnit.MONTH).atTime(0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
            TimePeriod.YEARLY -> {
                val start = LocalDateTime(current.year, Month.JANUARY, 1, 0, 0, 0, 0)
                val end = LocalDateTime(current.year + 1, Month.JANUARY, 1, 0, 0, 0, 0)
                start.toInstant(timeZone) to end.toInstant(timeZone)
            }
        }
    }
    
    private suspend fun generateChartData(
        period: TimePeriod, 
        current: LocalDateTime
    ): Triple<List<ChartData>, List<ChartData>, List<PeriodData>> {
        val expenseData = mutableListOf<ChartData>()
        val incomeData = mutableListOf<ChartData>()
        val periodData = mutableListOf<PeriodData>()
        
        val timeZone = TimeZone.currentSystemDefault()
        
        when (period) {
            TimePeriod.DAILY -> {
                // Last 7 days
                repeat(7) { dayOffset ->
                    val date = current.date.minus(6 - dayOffset, DateTimeUnit.DAY)
                    val start = date.atTime(0, 0, 0, 0).toInstant(timeZone)
                    val end = date.plus(1, DateTimeUnit.DAY).atTime(0, 0, 0, 0).toInstant(timeZone)
                    
                    val dayExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.DEBIT, start, end
                    )
                    val dayIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.CREDIT, start, end
                    )
                    
                    expenseData.add(ChartData("${date.dayOfMonth}/${date.monthNumber}", dayExpenses.toFloat(), date))
                    incomeData.add(ChartData("${date.dayOfMonth}/${date.monthNumber}", dayIncome.toFloat(), date))
                    periodData.add(PeriodData(dayIncome, dayExpenses, "${date.dayOfMonth}/${date.monthNumber}"))
                }
            }
            
            TimePeriod.WEEKLY -> {
                // Last 8 weeks
                repeat(8) { weekOffset ->
                    val weekStartDate = current.date.minus((7 - weekOffset) * 7, DateTimeUnit.DAY)
                    val weekStart = weekStartDate.atTime(0, 0, 0, 0).toInstant(timeZone)
                    val weekEnd = weekStartDate.plus(7, DateTimeUnit.DAY).atTime(0, 0, 0, 0).toInstant(timeZone)
                    
                    val weekExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.DEBIT, weekStart, weekEnd
                    )
                    val weekIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.CREDIT, weekStart, weekEnd
                    )
                    
                    expenseData.add(ChartData("Week ${weekOffset + 1}", weekExpenses.toFloat(), weekStartDate))
                    incomeData.add(ChartData("Week ${weekOffset + 1}", weekIncome.toFloat(), weekStartDate))
                    periodData.add(PeriodData(weekIncome, weekExpenses, "Week ${weekOffset + 1}"))
                }
            }
            
            TimePeriod.MONTHLY -> {
                // Last 6 months
                repeat(6) { monthOffset ->
                    val monthDate = current.date.minus(5 - monthOffset, DateTimeUnit.MONTH)
                    val monthStart = LocalDateTime(monthDate.year, monthDate.month, 1, 0, 0, 0, 0).toInstant(timeZone)
                    val monthEnd = monthStart.plus(1, DateTimeUnit.MONTH, timeZone)
                    
                    val monthExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.DEBIT, monthStart, monthEnd
                    )
                    val monthIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.CREDIT, monthStart, monthEnd
                    )
                    
                    val monthName = monthDate.month.name.take(3)
                    expenseData.add(ChartData(monthName, monthExpenses.toFloat(), monthDate))
                    incomeData.add(ChartData(monthName, monthIncome.toFloat(), monthDate))
                    periodData.add(PeriodData(monthIncome, monthExpenses, monthName))
                }
            }
            
            TimePeriod.QUARTERLY -> {
                // Last 4 quarters
                repeat(4) { quarterOffset ->
                    val quarterStartMonth = when ((current.month.number - 1) / 3) {
                        0 -> Month.JANUARY
                        1 -> Month.APRIL
                        2 -> Month.JULY
                        else -> Month.OCTOBER
                    }
                    
                    val quarterDate = current.date.minus((3 - quarterOffset) * 3, DateTimeUnit.MONTH)
                    val quarterStart = LocalDateTime(quarterDate.year, quarterStartMonth, 1, 0, 0, 0, 0).toInstant(timeZone)
                    val quarterEnd = quarterStart.plus(3, DateTimeUnit.MONTH, timeZone)
                    
                    val quarterExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.DEBIT, quarterStart, quarterEnd
                    )
                    val quarterIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.CREDIT, quarterStart, quarterEnd
                    )
                    
                    expenseData.add(ChartData("Q${quarterOffset + 1}", quarterExpenses.toFloat(), quarterDate))
                    incomeData.add(ChartData("Q${quarterOffset + 1}", quarterIncome.toFloat(), quarterDate))
                    periodData.add(PeriodData(quarterIncome, quarterExpenses, "Q${quarterOffset + 1}"))
                }
            }
            
            TimePeriod.YEARLY -> {
                // Last 3 years
                repeat(3) { yearOffset ->
                    val year = current.year - (2 - yearOffset)
                    val yearStart = LocalDateTime(year, Month.JANUARY, 1, 0, 0, 0, 0).toInstant(timeZone)
                    val yearEnd = LocalDateTime(year + 1, Month.JANUARY, 1, 0, 0, 0, 0).toInstant(timeZone)
                    
                    val yearExpenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.DEBIT, yearStart, yearEnd
                    )
                    val yearIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                        TransactionType.CREDIT, yearStart, yearEnd
                    )
                    
                    expenseData.add(ChartData(year.toString(), yearExpenses.toFloat(), LocalDate(year, 1, 1)))
                    incomeData.add(ChartData(year.toString(), yearIncome.toFloat(), LocalDate(year, 1, 1)))
                    periodData.add(PeriodData(yearIncome, yearExpenses, year.toString()))
                }
            }
        }
        
        return Triple(expenseData, incomeData, periodData)
    }
} 