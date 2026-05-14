package com.numisproerp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numisproerp.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class RecentTransaction(
    val id: String,
    val type: String,
    val amount: Double,
    val date: Long,
    val productName: String,
    val counterpartyName: String
)

data class DashboardData(
    val totalBalance: Double = 0.0,
    val monthlyPurchases: Double = 0.0,
    val monthlySales: Double = 0.0,
    val monthlyProfit: Double = 0.0,
    val recentTransactions: List<RecentTransaction> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true

            val currentMonth = getCurrentMonthRange()
            val monthlyPurchases = repository.getPurchasesSumByDateRange(
                currentMonth.first, currentMonth.second
            )
            val monthlySales = repository.getSalesSumByDateRange(
                currentMonth.first, currentMonth.second
            )
            val totalPurchases = repository.getTotalPurchasesSum()
            val totalSales = repository.getTotalSalesSum()
            val totalExpenses = repository.getTotalOtherExpensesSum()

            val totalBalance = totalSales - totalPurchases - totalExpenses
            val monthlyProfit = monthlySales - monthlyPurchases

            val recentTransactions = repository.getRecentTransactions(limit = 5)

            _dashboardData.value = DashboardData(
                totalBalance = totalBalance,
                monthlyPurchases = monthlyPurchases,
                monthlySales = monthlySales,
                monthlyProfit = monthlyProfit,
                recentTransactions = recentTransactions.map { transaction ->
                    RecentTransaction(
                        id = transaction.id,
                        type = transaction.type,
                        amount = transaction.amount,
                        date = transaction.date,
                        productName = transaction.productName,
                        counterpartyName = transaction.counterpartyName
                    )
                }
            )

            _isLoading.value = false
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }
}
