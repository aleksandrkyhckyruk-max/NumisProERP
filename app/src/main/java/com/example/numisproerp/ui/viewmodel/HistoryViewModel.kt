package com.numisproerp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numisproerp.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryEntryType(val label: String) {
    PURCHASE("Закупівля"),
    SALE("Продаж"),
    WRITEOFF("Списання"),
    EXPENSE("Витрата")
}

enum class HistorySort {
    DATE_DESC,
    DATE_ASC
}

/**
 * Уніфікований запис історії. `sign` показує, як ця операція впливає на
 * грошовий потік: +1 для надходжень, -1 для витрат/списань.
 */
data class HistoryEntry(
    val id: String,
    val type: HistoryEntryType,
    val date: Long,
    val productName: String,
    val counterparty: String,
    val quantity: Int?,
    val amount: Double,
    val sign: Int,
    val comment: String,
    /**
     * Дочірні записи, якщо ця HistoryEntry — це синтетична «згрупована чека»
     * по постачальнику + день. У звичайних рядках лишається порожнім.
     * Завдяки цьому UI може спокійно показувати один рядок, а при тапі
     * розкривати список позицій.
     */
    val subEntries: List<HistoryEntry> = emptyList()
)

/**
 * Текущий вибраний фільтр історії. `null` = «Всі», тобто без фільтрації.
 * Користувач натискає одну кнопку (радіо-стиль), а не комбінує кілька
 * категорій — щоб «Закупівля» дійсно показувала тільки закупівлі.
 */
data class HistoryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val selectedType: HistoryEntryType? = null,
    val sort: HistorySort = HistorySort.DATE_DESC
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val entries = mutableListOf<HistoryEntry>()

            repository.getAllPurchasesWithDetails().forEach { p ->
                entries.add(
                    HistoryEntry(
                        id = p.purchaseId,
                        type = HistoryEntryType.PURCHASE,
                        date = p.date,
                        productName = p.productName,
                        counterparty = p.supplierName,
                        quantity = null,
                        amount = p.totalAmount,
                        sign = -1,
                        comment = ""
                    )
                )
            }
            repository.getAllSalesWithDetails().forEach { s ->
                entries.add(
                    HistoryEntry(
                        id = s.saleId,
                        type = HistoryEntryType.SALE,
                        date = s.date,
                        productName = s.productName,
                        counterparty = s.clientName,
                        quantity = null,
                        amount = s.totalAmount,
                        sign = +1,
                        comment = ""
                    )
                )
            }
            repository.getAllWriteoffsWithProductName().forEach { w ->
                entries.add(
                    HistoryEntry(
                        id = w.writeoffId,
                        type = HistoryEntryType.WRITEOFF,
                        date = w.date,
                        productName = w.productName,
                        counterparty = w.reason,
                        quantity = w.quantity,
                        amount = w.totalAmount,
                        sign = -1,
                        comment = w.comment
                    )
                )
            }
            repository.getAllExpensesSync().forEach { e ->
                entries.add(
                    HistoryEntry(
                        id = e.expenseId,
                        type = HistoryEntryType.EXPENSE,
                        date = e.date,
                        productName = "",
                        counterparty = e.category,
                        quantity = null,
                        amount = e.amount,
                        sign = -1,
                        comment = e.comment
                    )
                )
            }

            _uiState.value = _uiState.value.copy(
                entries = entries,
                isLoading = false
            )
        }
    }

    /**
     * Вибір одного типу. Якщо натиснули вже активний тип — переходимо на «Всі».
     */
    fun selectType(type: HistoryEntryType?) {
        val next = if (_uiState.value.selectedType == type) null else type
        _uiState.value = _uiState.value.copy(selectedType = next)
    }

    fun setSort(sort: HistorySort) {
        _uiState.value = _uiState.value.copy(sort = sort)
    }

    fun visibleEntries(): List<HistoryEntry> {
        val state = _uiState.value
        val filtered = if (state.selectedType == null) {
            state.entries
        } else {
            state.entries.filter { it.type == state.selectedType }
        }
        val grouped = groupPurchasesBySupplierDay(filtered)
        return when (state.sort) {
            HistorySort.DATE_DESC -> grouped.sortedByDescending { it.date }
            HistorySort.DATE_ASC -> grouped.sortedBy { it.date }
        }
    }

    /**
     * Збираємо в одну синтетичну «чеку» закупівлі, що мають того самого
     * постачальника та припадають на той самий календарний день. Кінцевий
     * рядок показує сумарну вартість і кількість позицій, а оригінальні
     * рядки лишаються у `subEntries`, щоб UI міг їх розкрити при тапі.
     * Усі інші типи (Продаж/Списання/Витрата) проходять без змін.
     */
    private fun groupPurchasesBySupplierDay(entries: List<HistoryEntry>): List<HistoryEntry> {
        if (entries.isEmpty()) return entries
        val dayMs = 24L * 60L * 60L * 1000L
        val purchases = entries.filter { it.type == HistoryEntryType.PURCHASE }
        val others = entries.filter { it.type != HistoryEntryType.PURCHASE }
        if (purchases.isEmpty()) return entries

        val grouped = mutableListOf<HistoryEntry>()
        purchases
            .groupBy { Pair(it.counterparty.trim(), it.date / dayMs) }
            .forEach { (_, items) ->
                if (items.size <= 1) {
                    grouped.add(items.first())
                } else {
                    val sorted = items.sortedByDescending { it.date }
                    val total = sorted.sumOf { it.amount }
                    val first = sorted.first()
                    grouped.add(
                        HistoryEntry(
                            id = "purchase-group-" + first.counterparty + "-" + (first.date / dayMs),
                            type = HistoryEntryType.PURCHASE,
                            date = first.date,
                            productName = "",
                            counterparty = first.counterparty,
                            quantity = sorted.size,
                            amount = total,
                            sign = -1,
                            comment = "",
                            subEntries = sorted
                        )
                    )
                }
            }
        return grouped + others
    }
}
