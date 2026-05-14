package com.numisproerp.ui.viewmodel

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numisproerp.data.repository.Repository
import com.numisproerp.data.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class NotificationSeverity { CRITICAL, WARNING }

data class NotificationItem(
    val id: String,
    val titleUa: String,
    val titleEn: String,
    val descriptionUa: String,
    val descriptionEn: String,
    val severity: NotificationSeverity
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: Repository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    /**
     * Реактивний потік сповіщень.
     *
     * Комбінує:
     *  - `Flow<List<ProductWithStock>>` з repository (оновлюється автоматично
     *     при будь-якій зміні складу — закупівля / продаж / списання);
     *  - `Flow<Int>` поточного порогу з SettingsManager (миттєво пересортовує
     *     warning-and-critical розподіл при зміні слайдера у Налаштуваннях).
     *
     * Завдяки `stateIn(WhileSubscribed)` обчислення зупиняється, коли
     * жоден підписник не активний (TopBar з дашборду + NotificationsScreen).
     */
    val notifications: StateFlow<List<NotificationItem>> = combine(
        repository.getProductsWithStock(""),
        snapshotFlow { settingsManager.lowStockThreshold }
    ) { productsWithStock, threshold ->
        val items = mutableListOf<NotificationItem>()

        // Out of stock — товари, які раніше були закуплені, але повністю вичерпані
        productsWithStock
            .filter { it.totalPurchased > 0 && it.currentStock <= 0 }
            .forEach { p ->
                items.add(
                    NotificationItem(
                        id = "out_${p.catalogId}",
                        titleUa = "Закінчився товар: ${p.name}",
                        titleEn = "Out of stock: ${p.name}",
                        descriptionUa = "Залишок 0. Розгляньте можливість поповнення.",
                        descriptionEn = "Stock is 0. Consider restocking.",
                        severity = NotificationSeverity.CRITICAL
                    )
                )
            }

        // Low stock — тільки якщо threshold > 0
        if (threshold > 0) {
            productsWithStock
                .filter { it.currentStock in 1..threshold }
                .forEach { p ->
                    items.add(
                        NotificationItem(
                            id = "low_${p.catalogId}",
                            titleUa = "Низький залишок: ${p.name}",
                            titleEn = "Low stock: ${p.name}",
                            descriptionUa = "Залишилось ${p.currentStock} шт.",
                            descriptionEn = "Only ${p.currentStock} pcs left.",
                            severity = NotificationSeverity.WARNING
                        )
                    )
                }
        }
        items
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}
