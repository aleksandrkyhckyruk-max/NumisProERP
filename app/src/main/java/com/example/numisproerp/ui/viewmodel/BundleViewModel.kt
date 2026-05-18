package com.numisproerp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numisproerp.data.dao.BundleDao
import com.numisproerp.data.dao.BundleWithSales
import com.numisproerp.data.dao.ProductInStock
import com.numisproerp.data.entities.Bundle
import com.numisproerp.data.entities.BundleComponent
import com.numisproerp.data.entities.Product
import com.numisproerp.data.entities.Purchase
import com.numisproerp.data.entities.Writeoff
import com.numisproerp.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Один компонент у формі створення збірки: який товар, скільки штук, чи
 * достатньо на складі. `unitCost` — кешована середня закупочна ціна.
 */
data class BundleComponentDraft(
    val catalogId: String,
    val name: String,
    val quantity: Int,
    val unitCost: Double,
    val availableInStock: Int
) {
    val lineTotal: Double get() = quantity * unitCost
}

data class BundleUiState(
    val bundles: List<BundleWithSales> = emptyList(),
    val productsInStock: List<ProductInStock> = emptyList(),
    val isLoading: Boolean = false,
    val showCreator: Boolean = false,
    val draftName: String = "",
    val draftComponents: List<BundleComponentDraft> = emptyList(),
    val draftSuggestedPrice: String = "",
    val draftComment: String = "",
    val errorMessage: String = ""
) {
    val draftTotalCost: Double get() = draftComponents.sumOf { it.lineTotal }
}

/**
 * ViewModel для «Моя збірка». Створює збірку як комбінацію існуючих товарів зі
 * складу: списує компоненти через [Writeoff], реєструє нову готову позицію як
 * [Product] категорії «Збірка» та вставляє [Purchase] на 1 шт. з ціною, що
 * дорівнює сумі закупочних цін компонентів.
 *
 * Подальший продаж збірки виконується через звичайний механізм Sale; прибуток
 * автоматично рахується як `(sale.totalAmount × кількість) − bundle.totalCost`.
 */
@HiltViewModel
class BundleViewModel @Inject constructor(
    private val repository: Repository,
    private val bundleDao: BundleDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BundleUiState())
    val uiState: StateFlow<BundleUiState> = _uiState.asStateFlow()

    init {
        observeBundles()
        observeProductsInStock()
    }

    private fun observeBundles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            bundleDao.getAllWithSales().collectLatest { list ->
                _uiState.value = _uiState.value.copy(
                    bundles = list,
                    isLoading = false
                )
            }
        }
    }

    private fun observeProductsInStock() {
        viewModelScope.launch {
            repository.getProductsInStock().collectLatest { list ->
                // Виключаємо вже створені збірки зі списку доступних компонентів —
                // не дозволяємо складати збірку зі збірок (зайва рекурсія).
                _uiState.value = _uiState.value.copy(
                    productsInStock = list.filterNot {
                        it.catalogId.startsWith("BUNDLE_") || it.category == "Збірка"
                    }
                )
            }
        }
    }

    fun openCreator() {
        _uiState.value = _uiState.value.copy(
            showCreator = true,
            draftName = "",
            draftComponents = emptyList(),
            draftSuggestedPrice = "",
            draftComment = "",
            errorMessage = ""
        )
    }

    fun closeCreator() {
        _uiState.value = _uiState.value.copy(showCreator = false, errorMessage = "")
    }

    fun setDraftName(value: String) {
        _uiState.value = _uiState.value.copy(draftName = value)
    }

    fun setDraftSuggestedPrice(value: String) {
        _uiState.value = _uiState.value.copy(draftSuggestedPrice = value)
    }

    fun setDraftComment(value: String) {
        _uiState.value = _uiState.value.copy(draftComment = value)
    }

    fun addComponent(product: ProductInStock) {
        val existing = _uiState.value.draftComponents.firstOrNull { it.catalogId == product.catalogId }
        if (existing != null) {
            // Не додаємо вдруге — підвищуємо кількість.
            setComponentQuantity(product.catalogId, existing.quantity + 1)
            return
        }
        val draft = BundleComponentDraft(
            catalogId = product.catalogId,
            name = product.name.ifBlank { product.catalogId },
            quantity = 1,
            unitCost = product.avgPurchasePrice,
            availableInStock = product.currentStock
        )
        _uiState.value = _uiState.value.copy(
            draftComponents = _uiState.value.draftComponents + draft
        )
    }

    fun setComponentQuantity(catalogId: String, quantity: Int) {
        if (quantity <= 0) {
            removeComponent(catalogId)
            return
        }
        _uiState.value = _uiState.value.copy(
            draftComponents = _uiState.value.draftComponents.map {
                if (it.catalogId == catalogId) it.copy(quantity = quantity) else it
            }
        )
    }

    fun removeComponent(catalogId: String) {
        _uiState.value = _uiState.value.copy(
            draftComponents = _uiState.value.draftComponents.filterNot { it.catalogId == catalogId }
        )
    }

    /**
     * Створює збірку:
     *  1. Валідує (назва, ≥1 компонент, кількість у межах залишку).
     *  2. Списує всі компоненти через `Writeoff` (reason = «Збірка: <name>»).
     *  3. Створює `Product` (catalogId = `BUNDLE_<bundleId>`, category = «Збірка»).
     *  4. Створює `Purchase` (qty=1, price = totalCost) — збірка з'являється на складі.
     *  5. Зберігає `Bundle` + `BundleComponent`'и для історії компонування.
     */
    fun saveBundle(onCreated: (Bundle) -> Unit = {}) {
        val state = _uiState.value
        val name = state.draftName.trim()
        if (name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Введіть назву збірки")
            return
        }
        if (state.draftComponents.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Додайте хоча б 1 компонент")
            return
        }
        val overdraw = state.draftComponents.firstOrNull { it.quantity > it.availableInStock }
        if (overdraw != null) {
            _uiState.value = state.copy(
                errorMessage = "Недостатньо ${overdraw.name} на складі (потрібно ${overdraw.quantity}, є ${overdraw.availableInStock})"
            )
            return
        }

        viewModelScope.launch {
            val bundleId = UUID.randomUUID().toString().take(8).uppercase()
            val now = System.currentTimeMillis()
            val totalCost = state.draftTotalCost
            val suggestedPrice = state.draftSuggestedPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
            val bundleCatalogId = "BUNDLE_$bundleId"

            // Готуємо всі сутності, які треба записати атомарно.
            val writeoffs = state.draftComponents.map { c ->
                Writeoff(
                    writeoffId = "WO_BUNDLE_${bundleId}_${c.catalogId}",
                    date = now,
                    catalogId = c.catalogId,
                    quantity = c.quantity,
                    pricePerUnit = c.unitCost,
                    totalAmount = c.lineTotal,
                    reason = "Збірка",
                    comment = "Збірка: $name"
                )
            }
            val bundleProduct = Product(
                catalogId = bundleCatalogId,
                name = name,
                category = "Збірка"
            )
            val purchase = Purchase(
                purchaseId = "P_BUNDLE_$bundleId",
                date = now,
                catalogId = bundleCatalogId,
                supplierId = "",
                quantity = 1,
                pricePerUnit = totalCost,
                additionalCosts = 0.0,
                totalAmount = totalCost
            )
            val bundle = Bundle(
                bundleId = bundleId,
                name = name,
                assembledDate = now,
                totalCost = totalCost,
                suggestedPrice = suggestedPrice,
                photoPath = "",
                comment = state.draftComment.trim()
            )
            val components = state.draftComponents.map { c ->
                BundleComponent(
                    bundleComponentId = "BC_${bundleId}_${c.catalogId}",
                    bundleId = bundleId,
                    componentCatalogId = c.catalogId,
                    quantity = c.quantity,
                    unitCost = c.unitCost
                )
            }

            // Усі 5 кроків (списання, продукт, закупівля, збірка, компоненти)
            // виконуються в одній Room-транзакції під NonCancellable — або всі,
            // або жоден, навіть якщо корутину скасують.
            repository.createBundleAtomically(
                writeoffs = writeoffs,
                product = bundleProduct,
                purchase = purchase,
                bundle = bundle,
                components = components
            )

            _uiState.value = _uiState.value.copy(
                showCreator = false,
                draftName = "",
                draftComponents = emptyList(),
                draftSuggestedPrice = "",
                draftComment = "",
                errorMessage = ""
            )
            onCreated(bundle)
        }
    }

    fun deleteBundle(bundleId: String) {
        viewModelScope.launch {
            // Видаляємо саму збірку (CASCADE підчистить компоненти). Залишаємо
            // вже зроблені продажі та закупівлю незмінними — це історія.
            bundleDao.deleteBundle(bundleId)
        }
    }

    /**
     * Повторити існуючу збірку: підвантажує її компоненти + назву/ціну/коментар
     * у форму створення нової збірки. Користувач може ще щось підправити
     * (назву, кількість, додати компоненти) перед збереженням.
     *
     * Якщо хоча б одного з компонентів не вистачає на складі — діалог НЕ
     * відкривається, у [BundleUiState.errorMessage] записується перший
     * товар, якого бракує (`«Не вистачає на складі: <name> <needed>/<available>»`).
     * Якщо самої збірки вже немає або в неї не було компонентів —
     * відповідне повідомлення в errorMessage.
     */
    fun repeatBundle(bundleId: String) {
        viewModelScope.launch {
            val original = bundleDao.getById(bundleId)
            val originalComponents = bundleDao.getComponentsForBundle(bundleId)
            if (original == null || originalComponents.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Не вдалося завантажити збірку для повторення"
                )
                return@launch
            }

            // Поточні залишки на складі для перевірки доступності компонентів.
            // `productsInStock` уже містить наявні товари (без збірок) — беремо їх
            // зі стейту, який оновлюється `observeProductsInStock()`.
            val stockByCatalog = _uiState.value.productsInStock.associateBy { it.catalogId }

            val drafts = mutableListOf<BundleComponentDraft>()
            for (component in originalComponents) {
                val inStock = stockByCatalog[component.componentCatalogId]
                val name = inStock?.name?.ifBlank { component.componentCatalogId }
                    ?: component.componentCatalogId
                val available = inStock?.currentStock ?: 0
                if (available < component.quantity) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Не вистачає на складі: $name ${component.quantity}/$available"
                    )
                    return@launch
                }
                drafts += BundleComponentDraft(
                    catalogId = component.componentCatalogId,
                    name = name,
                    quantity = component.quantity,
                    // Використовуємо ПОТОЧНУ середню закупочну ціну, а не ціну з
                    // моменту створення оригіналу — собівартість/ціна на полиці
                    // могли змінитися відтоді.
                    unitCost = inStock?.avgPurchasePrice ?: component.unitCost,
                    availableInStock = available
                )
            }

            val suggestedPriceText = if (original.suggestedPrice > 0.0) {
                // Прибираємо зайвий ".0", якщо ціна ціла.
                val price = original.suggestedPrice
                if (price % 1.0 == 0.0) price.toLong().toString() else price.toString()
            } else ""

            _uiState.value = _uiState.value.copy(
                showCreator = true,
                draftName = original.name,
                draftComponents = drafts,
                draftSuggestedPrice = suggestedPriceText,
                draftComment = original.comment,
                errorMessage = ""
            )
        }
    }

    fun clearErrorMessage() {
        if (_uiState.value.errorMessage.isNotBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "")
        }
    }

    /**
     * Розібрати збірку назад на компоненти. Атомарно видаляє «слід» збірки в
     * БД (Purchase + всі Writeoff + Bundle + BundleComponent + Product),
     * через що SQL-розрахунок залишку повертає компоненти на склад.
     *
     * Якщо збірка вже частково/повністю продана — повертає
     * [Repository.DisassembleResult.AlreadySold], нічого не змінює.
     */
    suspend fun disassembleBundle(bundleId: String): Repository.DisassembleResult =
        repository.disassembleBundleAtomically(bundleId)
}
