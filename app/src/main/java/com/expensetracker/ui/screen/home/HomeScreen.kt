package com.expensetracker.ui.screen.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.toLocalDateTime
import com.expensetracker.ui.component.*
import com.expensetracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasSmsPermission: Boolean,
    onRequestPermissions: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var selectedTransactionForDetails by remember { mutableStateOf(null as com.expensetracker.data.entity.Transaction?) }
    
    // Clean background
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Simple Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Expense Tracker",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track your finances with ease",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
        
        if (!hasSmsPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "SMS Permission Required",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Grant SMS permission to automatically track your bank transactions. Your data stays completely private on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Permission")
                        }
                    }
                }
            }
        } else {
            // Financial Overview Cards
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income and Expenses Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CleanOverviewCard(
                            title = "Income",
                            subtitle = "This Month",
                            amount = uiState.monthlyIncome,
                            icon = Icons.Filled.TrendingUp,
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        CleanOverviewCard(
                            title = "Expenses", 
                            subtitle = "This Month",
                            amount = uiState.monthlyExpenses,
                            icon = Icons.Filled.TrendingDown,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Net Balance Card
                    val netBalance = uiState.monthlyIncome - uiState.monthlyExpenses
                    val isPositive = netBalance >= 0
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Net Balance",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Income - Expenses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${if (isPositive) "+" else "-"}₹${String.format("%,.0f", kotlin.math.abs(netBalance))}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) SuccessGreen else ErrorRed
                                    )
                                )
                            }
                            
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = (if (isPositive) SuccessGreen else ErrorRed).copy(alpha = 0.15f)
                            ) {
                                Icon(
                                    Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    tint = if (isPositive) SuccessGreen else ErrorRed
                                )
                            }
                        }
                    }
                }
            }
            
            // Quick Stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Quick Stats",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = "Transactions",
                                value = uiState.totalTransactions.toString(),
                                icon = Icons.Filled.Receipt
                            )
                            
                            Divider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp)
                            )
                            
                            StatItem(
                                label = "Avg Expense",
                                value = "₹${String.format("%.0f", uiState.averageExpense)}",
                                icon = Icons.Filled.TrendingDown
                            )
                        }
                    }
                }
            }
            
            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        FilledTonalButton(
                            onClick = { viewModel.syncSmsTransactions() }
                        ) {
                            Icon(
                                Icons.Filled.Sync, 
                                contentDescription = "Sync",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync SMS")
                        }
                    }
                }
            }
            
            if (uiState.recentTransactions.isEmpty() && !uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Filled.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap 'Sync SMS' to import your bank transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentTransactions.size) { index ->
                    CleanTransactionCard(
                        transaction = uiState.recentTransactions[index],
                        onClick = { selectedTransactionForDetails = uiState.recentTransactions[index] }
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
    
    // Transaction Details Dialog
    selectedTransactionForDetails?.let { transaction ->
        TransactionDetailsDialog(
            transaction = transaction,
            categories = categories,
            onDismiss = { selectedTransactionForDetails = null },
            onCategoryUpdate = { newCategory ->
                viewModel.updateTransactionCategory(transaction, newCategory)
            }
        )
    }
}

@Composable
private fun CleanOverviewCard(
    title: String,
    subtitle: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            }
            
            // Amount
            Text(
                text = "₹${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CleanTransactionCard(
    transaction: com.expensetracker.data.entity.Transaction,
    onClick: () -> Unit
) {
    val isCredit = transaction.type == com.expensetracker.data.entity.TransactionType.CREDIT
    val amountColor = if (isCredit) SuccessGreen else ErrorRed
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Transaction type icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = amountColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(10.dp)
                )
            }
            
            // Transaction details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.payee ?: "Transaction",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.category ?: "Uncategorized",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    val localDateTime = transaction.timestamp.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    Text(
                        text = "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Amount
            Text(
                text = "${if (isCredit) "+" else "-"}₹${String.format("%.0f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = amountColor
            )
        }
    }
} 