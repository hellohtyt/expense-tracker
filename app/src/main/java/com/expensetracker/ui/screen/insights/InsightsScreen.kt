package com.expensetracker.ui.screen.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.data.dao.CategoryAmount
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = { viewModel.refreshInsights() }
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Time Period Filter
        item {
            TimePeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = viewModel::selectTimePeriod
            )
        }
        
        when {
            uiState.isLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analyzing your finances...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            uiState.errorMessage != null -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Error: ${uiState.errorMessage}",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            else -> {
                // Financial Health Score
                item {
                    FinancialHealthCard(
                        income = uiState.monthlyIncome,
                        expenses = uiState.monthlyExpenses,
                        period = uiState.selectedPeriod
                    )
                }
                
                // Period Summary with Comparison
                item {
                    EnhancedPeriodSummaryCard(
                        income = uiState.monthlyIncome,
                        expenses = uiState.monthlyExpenses,
                        balance = uiState.monthlyIncome - uiState.monthlyExpenses,
                        period = uiState.selectedPeriod,
                        previousPeriodData = uiState.previousPeriodComparison
                    )
                }
                
                // Enhanced Chart with Trends
                if (uiState.expenseChartData.isNotEmpty()) {
                    item {
                        EnhancedExpenseChart(
                            expenseData = uiState.expenseChartData,
                            incomeData = uiState.incomeChartData,
                            period = uiState.selectedPeriod
                        )
                    }
                }
                
                // Spending Patterns
                item {
                    SpendingPatternsCard(
                        dayOfWeekSpending = uiState.dayOfWeekSpending,
                        timeOfDaySpending = uiState.timeOfDaySpending
                    )
                }
                
                // Top Categories with Progress
                if (uiState.categoryExpenses.isNotEmpty()) {
                    item {
                        Text(
                            text = "Top Spending Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    item {
                        CategoryExpenseBreakdown(
                            categories = uiState.categoryExpenses.take(5),
                            totalExpenses = uiState.monthlyExpenses
                        )
                    }
                }
                
                // Financial Insights & Tips
                item {
                    FinancialInsightsCard(
                        insights = uiState.financialInsights
                    )
                }
                
                // Enhanced Quick Stats
                item {
                    EnhancedQuickStatsCard(
                        totalTransactions = uiState.totalTransactions,
                        avgTransactionAmount = uiState.avgTransactionAmount,
                        largestExpense = uiState.largestExpense,
                        savingsRate = uiState.savingsRate,
                        expenseFrequency = uiState.expenseFrequency
                    )
                }
                
                // Expense Goals & Budgets
                if (uiState.budgetStatus.isNotEmpty()) {
                    item {
                        BudgetStatusCard(budgetStatus = uiState.budgetStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialHealthCard(
    income: Double,
    expenses: Double,
    period: TimePeriod
) {
    val healthScore = calculateFinancialHealth(income, expenses)
    val healthColor = when {
        healthScore >= 80 -> Color(0xFF4CAF50)
        healthScore >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFE57373)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = healthColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Financial Health Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getHealthDescription(healthScore),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(healthColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${healthScore.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = healthScore / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = healthColor,
                trackColor = healthColor.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun EnhancedPeriodSummaryCard(
    income: Double,
    expenses: Double,
    balance: Double,
    period: TimePeriod,
    previousPeriodData: PreviousPeriodComparison?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "This ${period.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinancialMetricCard(
                    title = "Income",
                    amount = income,
                    icon = Icons.Filled.TrendingUp,
                    color = Color(0xFF4CAF50),
                    changePercentage = previousPeriodData?.incomeChange
                )
                
                FinancialMetricCard(
                    title = "Expenses",
                    amount = expenses,
                    icon = Icons.Filled.TrendingDown,
                    color = Color(0xFFE57373),
                    changePercentage = previousPeriodData?.expenseChange
                )
                
                FinancialMetricCard(
                    title = "Balance",
                    amount = abs(balance),
                    icon = if (balance >= 0) Icons.Filled.AccountBalance else Icons.Filled.Warning,
                    color = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFE57373),
                    changePercentage = previousPeriodData?.balanceChange
                )
            }
        }
    }
}

@Composable
private fun FinancialMetricCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    changePercentage: Double?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
        
        Text(
            text = "₹${formatAmount(amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        changePercentage?.let { change ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (change >= 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFE57373),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${abs(change).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFE57373)
                )
            }
        }
    }
}

@Composable
private fun EnhancedExpenseChart(
    expenseData: List<ChartData>,
    incomeData: List<ChartData>,
    period: TimePeriod
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Income vs Expenses Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ChartLegendItem("Expenses", Color(0xFFE57373))
                ChartLegendItem("Income", Color(0xFF4CAF50))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Enhanced bar chart with both income and expenses
            val maxValue = maxOf(
                expenseData.maxOfOrNull { it.value } ?: 1f,
                incomeData.maxOfOrNull { it.value } ?: 1f
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                expenseData.forEachIndexed { index, expenseItem ->
                    val incomeItem = incomeData.getOrNull(index)
                    
                    Column {
                        Text(
                            text = expenseItem.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // Expense bar
                                LinearProgressIndicator(
                                    progress = if (maxValue > 0) expenseItem.value / maxValue else 0f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFE57373),
                                    trackColor = Color(0xFFE57373).copy(alpha = 0.2f)
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Income bar
                                incomeItem?.let {
                                    LinearProgressIndicator(
                                        progress = if (maxValue > 0) it.value / maxValue else 0f,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFF4CAF50),
                                        trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "₹${formatAmount(expenseItem.value.toDouble())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE57373)
                                )
                                incomeItem?.let {
                                    Text(
                                        text = "₹${formatAmount(it.value.toDouble())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SpendingPatternsCard(
    dayOfWeekSpending: Map<String, Double>,
    timeOfDaySpending: Map<String, Double>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Spending Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Day of week pattern
            Text(
                text = "Busiest Spending Days",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val maxDaySpending = dayOfWeekSpending.values.maxOrNull() ?: 1.0
            dayOfWeekSpending.forEach { (day, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(60.dp)
                    )
                    
                    LinearProgressIndicator(
                        progress = (amount / maxDaySpending).toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                    
                    Text(
                        text = "₹${formatAmount(amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            if (timeOfDaySpending.isNotEmpty() && timeOfDaySpending.values.any { it > 0 }) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Time of Day Spending",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val maxTimeSpending = timeOfDaySpending.values.maxOrNull() ?: 1.0
                timeOfDaySpending.forEach { (timeSlot, amount) ->
                    if (amount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeSlot,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(120.dp)
                            )
                            
                            LinearProgressIndicator(
                                progress = (amount / maxTimeSpending).toFloat(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                            )
                            
                            Text(
                                text = "₹${formatAmount(amount)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(80.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseBreakdown(
    categories: List<CategoryAmount>,
    totalExpenses: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            categories.forEach { categoryAmount ->
                val percentage = if (totalExpenses > 0) (categoryAmount.total / totalExpenses) * 100 else 0.0
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = categoryAmount.category ?: "Uncategorized",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "₹${formatAmount(categoryAmount.total)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE57373)
                            )
                            Text(
                                text = "${percentage.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = (percentage / 100).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = getRandomCategoryColor(categoryAmount.category ?: "Uncategorized"),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FinancialInsightsCard(
    insights: List<FinancialInsight>
) {
    if (insights.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Financial Insights & Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            insights.forEach { insight ->
                FinancialInsightItem(insight = insight)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FinancialInsightItem(
    insight: FinancialInsight
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = insight.icon,
            contentDescription = null,
            tint = insight.color,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnhancedQuickStatsCard(
    totalTransactions: Int,
    avgTransactionAmount: Double,
    largestExpense: Double,
    savingsRate: Double,
    expenseFrequency: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // First row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickStatItem(
                    title = "Transactions",
                    value = totalTransactions.toString(),
                    icon = Icons.Filled.Receipt,
                    color = MaterialTheme.colorScheme.primary
                )
                
                QuickStatItem(
                    title = "Avg. Amount",
                    value = "₹${formatAmount(avgTransactionAmount)}",
                    icon = Icons.Filled.Calculate,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                QuickStatItem(
                    title = "Largest",
                    value = "₹${formatAmount(largestExpense)}",
                    icon = Icons.Filled.TrendingUp,
                    color = Color(0xFFE57373)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Second row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickStatItem(
                    title = "Savings Rate",
                    value = "${savingsRate.toInt()}%",
                    icon = Icons.Filled.Savings,
                    color = Color(0xFF4CAF50)
                )
                
                QuickStatItem(
                    title = "Daily Spending",
                    value = "₹${formatAmount(expenseFrequency)}",
                    icon = Icons.Filled.CalendarToday,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                QuickStatItem(
                    title = "Health Score",
                    value = "${calculateFinancialHealth(0.0, 0.0).toInt()}",
                    icon = Icons.Filled.HealthAndSafety,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun QuickStatItem(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BudgetStatusCard(
    budgetStatus: List<BudgetStatus>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Budget Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            budgetStatus.forEach { budget ->
                BudgetStatusItem(budget = budget)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun BudgetStatusItem(
    budget: BudgetStatus
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = budget.category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "₹${formatAmount(budget.spent)} / ₹${formatAmount(budget.budget)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = (budget.spent / budget.budget).toFloat().coerceAtMost(1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (budget.spent > budget.budget) Color(0xFFE57373) else Color(0xFF4CAF50),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Time Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimePeriod.values()) { period ->
                    FilterChip(
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        selected = selectedPeriod == period
                    )
                }
            }
        }
    }
}

// Helper functions
private fun calculateFinancialHealth(income: Double, expenses: Double): Float {
    if (income <= 0) return 0f
    val savingsRate = ((income - expenses) / income) * 100
    return when {
        savingsRate >= 20 -> 90f
        savingsRate >= 10 -> 75f
        savingsRate >= 5 -> 60f
        savingsRate >= 0 -> 40f
        else -> 20f
    }
}

private fun getHealthDescription(score: Float): String {
    return when {
        score >= 80 -> "Excellent financial health!"
        score >= 60 -> "Good financial management"
        score >= 40 -> "Room for improvement"
        else -> "Needs attention"
    }
}

private fun getRandomCategoryColor(category: String): Color {
    val colors = listOf(
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFF9C27B0), Color(0xFFE91E63), Color(0xFF00BCD4),
        Color(0xFF795548), Color(0xFF607D8B), Color(0xFFFF5722)
    )
    return colors[category.hashCode().mod(colors.size)]
}

private fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

// Data classes for enhanced insights
data class PreviousPeriodComparison(
    val incomeChange: Double,
    val expenseChange: Double,
    val balanceChange: Double
)

data class FinancialInsight(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

data class BudgetStatus(
    val category: String,
    val budget: Double,
    val spent: Double
) 