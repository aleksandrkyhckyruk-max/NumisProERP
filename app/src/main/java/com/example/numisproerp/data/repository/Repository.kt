package com.numisproerp.data.repository

import com.numisproerp.data.dao.ClientForSelection
import com.numisproerp.data.dao.ClientWithBalance
import com.numisproerp.data.dao.CollectionItemWithStock
import com.numisproerp.data.dao.ProductForSelection
import com.numisproerp.data.dao.ProductInStock
import com.numisproerp.data.dao.ProductStockInfo
import com.numisproerp.data.dao.ProductWithStock
import com.numisproerp.data.dao.PurchaseWithProductName
import com.numisproerp.data.dao.SaleWithClientName
import com.numisproerp.data.dao.SaleWithProductName
import com.numisproerp.data.dao.SupplierForSelection
import com.numisproerp.data.dao.SupplierWithBalance
import com.numisproerp.data.dao.WriteoffWithProductName
import com.numisproerp.data.database.AppDatabase
import com.numisproerp.data.entities.Client
import com.numisproerp.data.entities.OtherExpense
import com.numisproerp.data.entities.Product
import com.numisproerp.data.entities.Purchase
import com.numisproerp.data.entities.Sale
import com.numisproerp.data.entities.Supplier
import com.numisproerp.data.entities.Writeoff
import com.numisproerp.data.entities.Bundle
import com.numisproerp.data.entities.BundleComponent
import com.numisproerp.data.entities.CollectionItem
import com.numisproerp.data.entities.Note
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class TransactionSummary(
    val id: String,
    val type: String,
    val amount: Double,
    val date: Long,
    val productName: String,
    val counterpartyName: String
)

@Singleton
class Repository @Inject constructor(
    private val database: AppDatabase
) {

    // ==================== PRODUCTS ====================

    fun getProductsWithStock(searchQuery: String): Flow<List<ProductWithStock>> {
        return database.productDao().getProductsWithStock(searchQuery)
    }

    fun getProductsWithStockByCategory(category: String): Flow<List<ProductWithStock>> {
        return database.productDao().getProductsWithStockByCategory(category)
    }

    suspend fun getDistinctCategories(): List<String> {
        return withContext(Dispatchers.IO) {
            database.productDao().getDistinctCategories()
        }
    }

    suspend fun getDistinctMaterials(): List<String> {
        return withContext(Dispatchers.IO) {
            database.productDao().getDistinctMaterials()
        }
    }

    suspend fun insertProduct(product: Product) {
        return withContext(Dispatchers.IO) {
            database.productDao().insert(product)
        }
    }

    suspend fun getProductsForSelection(): List<ProductForSelection> {
        return withContext(Dispatchers.IO) {
            database.productDao().getProductsForSelection()
        }
    }

    fun getProductsInStock(): Flow<List<ProductInStock>> {
        return database.productDao().getProductsInStock()
    }

    fun getAllProducts(): Flow<List<Product>> {
        return database.productDao().getAllProducts()
    }

    suspend fun getCatalogImageMap(): Map<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val catalogItems = database.catalogDao().getAllItemsSync()
                val byId = catalogItems
                    .filter { it.imageUrlFront.isNotEmpty() }
                    .associate { it.id to it.imageUrlFront }
                val byName = catalogItems
                    .filter { it.imageUrlFront.isNotEmpty() }
                    .associate { it.name to it.imageUrlFront }
                byId + byName
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun getCatalogImagePairMap(): Map<String, Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val catalogItems = database.catalogDao().getAllItemsSync()
                val byId = catalogItems
                    .filter { it.imageUrlFront.isNotEmpty() || it.imageUrlBack.isNotEmpty() }
                    .associate { it.id to Pair(it.imageUrlFront, it.imageUrlBack) }
                val byName = catalogItems
                    .filter { it.imageUrlFront.isNotEmpty() || it.imageUrlBack.isNotEmpty() }
                    .associate { it.name to Pair(it.imageUrlFront, it.imageUrlBack) }
                byId + byName
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun getProductById(catalogId: String): Product? {
        return withContext(Dispatchers.IO) {
            database.productDao().getProductById(catalogId)
        }
    }

    // ==================== CLIENTS ====================

    fun getClientsWithBalance(): Flow<List<ClientWithBalance>> {
        return database.clientDao().getClientsWithBalance()
    }

    suspend fun getClientsForSelection(): List<ClientForSelection> {
        return withContext(Dispatchers.IO) {
            database.clientDao().getClientsForSelection()
        }
    }

    suspend fun insertClient(client: Client) {
        return withContext(Dispatchers.IO) {
            database.clientDao().insert(client)
        }
    }

    suspend fun updateClient(client: Client) {
        return withContext(Dispatchers.IO) {
            database.clientDao().update(client)
        }
    }

    suspend fun deleteClient(clientId: String) {
        return withContext(Dispatchers.IO) {
            database.clientDao().deleteById(clientId)
        }
    }

    suspend fun getSalesHistoryForClient(clientId: String): List<SaleWithProductName> {
        return withContext(Dispatchers.IO) {
            database.saleDao().getSalesByClient(clientId)
        }
    }

    // ==================== SUPPLIERS ====================

    fun getSuppliersWithBalance(): Flow<List<SupplierWithBalance>> {
        return database.supplierDao().getSuppliersWithBalance()
    }

    suspend fun getSuppliersForSelection(): List<SupplierForSelection> {
        return withContext(Dispatchers.IO) {
            database.supplierDao().getSuppliersForSelection()
        }
    }

    suspend fun insertSupplier(supplier: Supplier) {
        return withContext(Dispatchers.IO) {
            database.supplierDao().insert(supplier)
        }
    }

    suspend fun updateSupplier(supplier: Supplier) {
        return withContext(Dispatchers.IO) {
            database.supplierDao().update(supplier)
        }
    }

    suspend fun deleteSupplier(supplierId: String) {
        return withContext(Dispatchers.IO) {
            database.supplierDao().deleteById(supplierId)
        }
    }

    // ==================== PURCHASES ====================

    suspend fun insertPurchase(purchase: Purchase) {
        return withContext(Dispatchers.IO) {
            database.purchaseDao().insert(purchase)
        }
    }

    suspend fun getTotalPurchasesSum(): Double = withContext(Dispatchers.IO) {
        database.purchaseDao().getTotalSum() ?: 0.0
    }

    suspend fun getPurchasesSumByDateRange(startDate: Long, endDate: Long): Double = withContext(Dispatchers.IO) {
        database.purchaseDao().getSumByDateRange(startDate, endDate) ?: 0.0
    }

    suspend fun getPurchaseHistoryForSupplier(supplierId: String): List<PurchaseWithProductName> {
        return withContext(Dispatchers.IO) {
            database.purchaseDao().getPurchasesBySupplier(supplierId)
        }
    }

    // ==================== SALES ====================

    suspend fun insertSale(sale: Sale) {
        return withContext(Dispatchers.IO) {
            database.saleDao().insert(sale)
        }
    }

    suspend fun getTotalSalesSum(): Double = withContext(Dispatchers.IO) {
        database.saleDao().getTotalSum() ?: 0.0
    }

    suspend fun getSalesSumByDateRange(startDate: Long, endDate: Long): Double = withContext(Dispatchers.IO) {
        database.saleDao().getSumByDateRange(startDate, endDate) ?: 0.0
    }

    suspend fun getProductStockInfo(catalogId: String): ProductStockInfo? {
        return withContext(Dispatchers.IO) {
            database.saleDao().getProductStockInfo(catalogId)
        }
    }

    suspend fun getAllSales(): List<Sale> {
        return withContext(Dispatchers.IO) {
            database.saleDao().getAllSales()
        }
    }

    // ==================== OTHER EXPENSES ====================

    fun getAllExpenses(): Flow<List<OtherExpense>> {
        return database.otherExpenseDao().getAllExpenses()
    }

    suspend fun getTotalOtherExpensesSum(): Double = withContext(Dispatchers.IO) {
        database.otherExpenseDao().getTotalSum() ?: 0.0
    }

    /**
     * Сума інших витрат за вибраний діапазон дат. Потрібно для віджета
     * календаря у шапці дашборда — він обчислює баланс «продажі − закупки
     * − інші витрати» за день/місяць.
     */
    suspend fun getOtherExpensesSumByDateRange(startDate: Long, endDate: Long): Double =
        withContext(Dispatchers.IO) {
            database.otherExpenseDao().getSumByDateRange(startDate, endDate) ?: 0.0
        }

    suspend fun insertOtherExpense(expense: OtherExpense) {
        return withContext(Dispatchers.IO) {
            database.otherExpenseDao().insert(expense)
        }
    }

    suspend fun getRecentExpenses(limit: Int): List<OtherExpense> {
        return withContext(Dispatchers.IO) {
            database.otherExpenseDao().getRecentExpenses(limit)
        }
    }

    suspend fun getAllExpensesSync(): List<OtherExpense> {
        return withContext(Dispatchers.IO) {
            database.otherExpenseDao().getAllExpensesSync()
        }
    }

    // ==================== WRITEOFFS ====================

    suspend fun insertWriteoff(writeoff: Writeoff) {
        return withContext(Dispatchers.IO) {
            database.writeoffDao().insert(writeoff)
        }
    }

    suspend fun getAllWriteoffs(): List<Writeoff> {
        return withContext(Dispatchers.IO) {
            database.writeoffDao().getAll()
        }
    }

    suspend fun getAllWriteoffsWithProductName(): List<WriteoffWithProductName> {
        return withContext(Dispatchers.IO) {
            database.writeoffDao().getAllWithProductName()
        }
    }

    suspend fun getTotalWriteoffsSum(): Double = withContext(Dispatchers.IO) {
        database.writeoffDao().getTotalSum() ?: 0.0
    }

    suspend fun getWriteoffsSumByDateRange(startDate: Long, endDate: Long): Double =
        withContext(Dispatchers.IO) {
            database.writeoffDao().getSumByDateRange(startDate, endDate) ?: 0.0
        }

    // ==================== COLLECTION (МОЯ КОЛЕКЦІЯ) ====================

    /**
     * Додає товар до «Моєї колекції» (п. 12-13 ТЗ). Створюється:
     *  - запис у `collection_items` (з фото, описом, оціночною вартістю);
     *  - дублюючий `Product` з тим самим ID, щоб товар з'являвся у каталозі та
     *    був доступний у вибірках Sale/Stock.
     * Не створюється запис у `purchases`, тому грошовий баланс не зменшується,
     * а середня закупочна ціна = 0 → весь дохід при продажі = чистий прибуток.
     */
    suspend fun addCollectionItem(item: CollectionItem) {
        withContext(Dispatchers.IO) {
            val productEquivalent = Product(
                catalogId = item.collectionId,
                name = item.name,
                series = item.series,
                material = item.material,
                nominal = item.nominal,
                category = item.category,
                quality = item.quality,
                photoPath = item.photoPath
            )
            database.productDao().insert(productEquivalent)
            database.collectionItemDao().insert(item)
        }
    }

    suspend fun updateCollectionItem(item: CollectionItem) {
        withContext(Dispatchers.IO) {
            database.collectionItemDao().update(item)
            // Підтримуємо синхронну метадату на стороні products (назва, фото, серія, тощо).
            val product = database.productDao().getProductById(item.collectionId)
            if (product != null) {
                database.productDao().insert(
                    product.copy(
                        name = item.name,
                        series = item.series,
                        material = item.material,
                        nominal = item.nominal,
                        category = item.category,
                        quality = item.quality,
                        photoPath = item.photoPath
                    )
                )
            }
        }
    }

    suspend fun deleteCollectionItem(item: CollectionItem) {
        withContext(Dispatchers.IO) {
            database.collectionItemDao().delete(item)
            // Окремо не видаляємо Product, бо може бути пов'язана історія
            // продажів (FOREIGN KEY-подібні залежності) — товар просто
            // перестане числитися у складі (collection_items.quantity = 0).
        }
    }

    /**
     * Додає існуючий товар до колекції БЕЗ перезапису Product-запису.
     * Повертає false якщо товар вже в колекції.
     */
    suspend fun addExistingProductToCollection(item: CollectionItem): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = database.collectionItemDao().getById(item.collectionId)
            if (existing != null) return@withContext false
            database.collectionItemDao().insert(item)
            true
        }
    }

    fun getAllCollectionItems(): Flow<List<CollectionItem>> =
        database.collectionItemDao().getAll()

    /**
     * Повертає колекційні позиції разом з полями `soldQuantity` /
     * `writtenOffQuantity` / `remainingQuantity`. Використовується на екрані
     * «Моя колекція», щоб показувати РЕАЛЬНИЙ залишок (початкова кількість −
     * продане − списане), а не статичне введене `quantity`.
     */
    fun getAllCollectionItemsWithStock(): Flow<List<CollectionItemWithStock>> =
        database.collectionItemDao().getAllWithStock()

    suspend fun getCollectionItemById(id: String): CollectionItem? =
        withContext(Dispatchers.IO) {
            database.collectionItemDao().getById(id)
        }

    suspend fun getCollectionTotalEstimatedValue(): Double = withContext(Dispatchers.IO) {
        database.collectionItemDao().getTotalEstimatedValue() ?: 0.0
    }

    suspend fun getCollectionCount(): Int = withContext(Dispatchers.IO) {
        database.collectionItemDao().getCount()
    }

    // ==================== RECENT TRANSACTIONS ====================

    suspend fun getAllPurchasesWithDetails() = withContext(Dispatchers.IO) {
        database.purchaseDao().getRecentWithDetails(Int.MAX_VALUE)
    }

    suspend fun getAllSalesWithDetails() = withContext(Dispatchers.IO) {
        database.saleDao().getRecentWithDetails(Int.MAX_VALUE)
    }

    /**
     * Стрічка «Останні операції» на дашборді. Користувач у фідбеку попросив
     * групувати позиції одного постачальника за один календарний день у
     * ОДИН рядок (а не показувати кожен товар окремо). Аналогічно групуються
     * продажі одного клієнта за день.
     *
     * Алгоритм:
     *  1) Витягуємо «з запасом» (limit * 5) сирих операцій з БД — так після
     *     злипання дублікатів ми все ще маємо достатньо рядків, щоб віддати
     *     запрошений limit.
     *  2) Ключ групи = (counterpartyName, localDayStartMillis), де
     *     localDayStartMillis — початок календарного дня у системній TZ
     *     (так, як це бачить користувач у Києві). UTC-кей дав би неправильні
     *     стики на ~опівночі.
     *  3) У підсумковому TransactionSummary:
     *      - amount = сума totalAmount у групі;
     *      - date = дата найновішої операції у групі (щоб сортування за датою
     *        було стабільне);
     *      - productName: якщо в групі один товар — його назва; якщо більше —
     *        «<перший товар> +N» (де N — кількість додаткових позицій).
     *      - id = стабільний "<type>-group-<counterparty>-<dayKey>".
     *  4) Сортуємо все за датою спадно й беремо limit.
     */
    suspend fun getRecentTransactions(limit: Int): List<TransactionSummary> = withContext(Dispatchers.IO) {
        // Зчитуємо «з запасом», щоб після злипання все одно вистачило на limit.
        val rawLimit = (limit * 5).coerceAtLeast(limit)
        val recentPurchases = database.purchaseDao().getRecentWithDetails(rawLimit)
        val recentSales = database.saleDao().getRecentWithDetails(rawLimit)

        val purchaseSummaries = groupTransactionsByCounterpartyDay(
            type = "Покупка",
            entries = recentPurchases.map { p ->
                RawTxn(
                    id = p.purchaseId,
                    date = p.date,
                    amount = p.totalAmount,
                    productName = p.productName,
                    counterpartyName = p.supplierName
                )
            }
        )
        val saleSummaries = groupTransactionsByCounterpartyDay(
            type = "Продаж",
            entries = recentSales.map { s ->
                RawTxn(
                    id = s.saleId,
                    date = s.date,
                    amount = s.totalAmount,
                    productName = s.productName,
                    counterpartyName = s.clientName
                )
            }
        )

        (purchaseSummaries + saleSummaries)
            .sortedByDescending { it.date }
            .take(limit)
    }

    private data class RawTxn(
        val id: String,
        val date: Long,
        val amount: Double,
        val productName: String,
        val counterpartyName: String
    )

    private fun groupTransactionsByCounterpartyDay(
        type: String,
        entries: List<RawTxn>
    ): List<TransactionSummary> {
        if (entries.isEmpty()) return emptyList()
        // Локальний календарний день у TZ користувача — як і в History.
        val cal = java.util.Calendar.getInstance()
        fun localDayKey(millis: Long): Long {
            cal.timeInMillis = millis
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // Зберігаємо порядок надходження — entries вже відсортовані за date DESC у DAO,
        // тож перший елемент у групі — найсвіжіший.
        val grouped = linkedMapOf<Pair<String, Long>, MutableList<RawTxn>>()
        entries.forEach { e ->
            val key = e.counterpartyName to localDayKey(e.date)
            grouped.getOrPut(key) { mutableListOf() }.add(e)
        }
        return grouped.map { (key, list) ->
            val first = list.first()
            val productName = if (list.size == 1) {
                first.productName
            } else {
                "${first.productName} +${list.size - 1}"
            }
            TransactionSummary(
                id = "$type-group-${key.first}-${key.second}",
                type = type,
                amount = list.sumOf { it.amount },
                date = first.date,
                productName = productName,
                counterpartyName = key.first
            )
        }
    }

    /**
     * Повне очищення всіх таблиць (товари, контрагенти, операції, колекція).
     * Каталог НБУ не очищується — він імпортується окремою кнопкою з Excel
     * і не вважається "робочими даними". Викликається з Налаштувань після
     * подвійного підтвердження.
     *
     * Атомарність: усі 8 DELETE виконуються всередині `database.withTransaction`,
     * тобто або всі викочуються, або жоден. `NonCancellable` гарантує, що навіть
     * якщо користувач вийде зі Settings (composition-scope cancel) — операція
     * добіжить до кінця, а не лишить БД у частково очищеному стані.
     */
    // ==================== NOTES (МОЇ ЗАМІТКИ) ====================

    fun getAllNotes(): Flow<List<Note>> = database.noteDao().getAll()

    suspend fun insertNote(note: Note) {
        withContext(Dispatchers.IO) { database.noteDao().insert(note) }
    }

    suspend fun updateNote(note: Note) {
        withContext(Dispatchers.IO) { database.noteDao().update(note) }
    }

    suspend fun deleteNote(note: Note) {
        withContext(Dispatchers.IO) { database.noteDao().delete(note) }
    }

    suspend fun getDueReminders(timestamp: Long): List<Note> {
        return withContext(Dispatchers.IO) { database.noteDao().getDueReminders(timestamp) }
    }

    // ==================== SALES HISTORY (ALL) ====================

    suspend fun getAllSalesWithClientName(): List<SaleWithClientName> {
        return withContext(Dispatchers.IO) {
            database.saleDao().getAllSalesWithClientName()
        }
    }

    // ==================== CLEAR ALL DATA ====================

    suspend fun clearAllData() = withContext(NonCancellable + Dispatchers.IO) {
        database.withTransaction {
            // Спочатку дочірні таблиці (operations), потім довідники
            database.bundleDao().deleteAllComponents()
            database.bundleDao().deleteAllBundles()
            database.writeoffDao().deleteAll()
            database.saleDao().deleteAll()
            database.purchaseDao().deleteAll()
            database.otherExpenseDao().deleteAll()
            database.noteDao().deleteAll()
            database.collectionItemDao().deleteAll()
            database.productDao().deleteAll()
            database.clientDao().deleteAll()
            database.supplierDao().deleteAll()
        }
    }

    // ==================== BUNDLES ====================

    /**
     * Атомарне створення збірки: всі операції (списання компонентів,
     * реєстрація Product, закупівля, вставка Bundle + BundleComponent) виконуються
     * в єдиній Room-транзакції під [NonCancellable] — їх неможливо розірвати
     * каскадним припиненням корутини або вбивством застосунку.
     *
     * Списання компонентів і закупівля самої збірки створюються з прапорцем
     * `isBundleOp = true` — вони впливають на залишок, але не з'являються в
     * історії закупівель/списань і в звітах. Це не нова покупка з
     * постачальника і не реальна втрата, а перетворення компонентів у
     * готовий лот.
     */
    suspend fun createBundleAtomically(
        writeoffs: List<Writeoff>,
        product: Product,
        purchase: Purchase,
        bundle: Bundle,
        components: List<BundleComponent>
    ) = withContext(NonCancellable + Dispatchers.IO) {
        database.withTransaction {
            writeoffs.forEach { database.writeoffDao().insert(it.copy(isBundleOp = true)) }
            database.productDao().insert(product)
            database.purchaseDao().insert(purchase.copy(isBundleOp = true))
            database.bundleDao().insertBundle(bundle)
            database.bundleDao().insertComponents(components)
        }
    }

    /**
     * Результат спроби розібрати збірку назад на компоненти.
     */
    sealed class DisassembleResult {
        /** Розібрано успішно — компоненти повернулися на склад. */
        object Success : DisassembleResult()
        /** Збірка вже частково чи повністю продана — розбирати не можна. */
        object AlreadySold : DisassembleResult()
        /** Збірки з таким ID не знайдено в БД. */
        object NotFound : DisassembleResult()
    }

    /**
     * Атомарне розбирання збірки назад на компоненти.
     *
     * Логіка: фактично видаляє всі сліди створення цієї збірки —
     * `Purchase` (`P_BUNDLE_<id>`), всі `Writeoff` (`WO_BUNDLE_<id>_*`),
     * саму `Bundle` (cascade-чистить `bundle_components`) і `Product` із
     * `catalogId = "BUNDLE_<id>"`. Після цього SQL-обчислення `currentStock`
     * автоматично «поверне» компоненти на склад (бо їх більше не списано), а
     * сама збірка зникне зі складу та з каталогу.
     *
     * Захист: якщо для збірки вже існує хоча б один `Sale`, повертаємо
     * [DisassembleResult.AlreadySold] і нічого не змінюємо — інакше залишок
     * компонентів виявиться неправильним (вже не на складі), а Sale залишився
     * без батьківського Product.
     */
    suspend fun disassembleBundleAtomically(bundleId: String): DisassembleResult =
        withContext(NonCancellable + Dispatchers.IO) {
            val existing = database.bundleDao().getById(bundleId)
                ?: return@withContext DisassembleResult.NotFound
            val bundleCatalogId = "BUNDLE_${existing.bundleId}"
            val sold = database.saleDao().getTotalQuantitySold(bundleCatalogId)
            if (sold > 0) {
                return@withContext DisassembleResult.AlreadySold
            }
            database.withTransaction {
                database.purchaseDao().deleteById("P_BUNDLE_${existing.bundleId}")
                database.writeoffDao().deleteByIdLike("WO_BUNDLE_${existing.bundleId}_%")
                database.bundleDao().deleteBundle(existing.bundleId)
                database.productDao().deleteByCatalogId(bundleCatalogId)
            }
            DisassembleResult.Success
        }

    // ==================== REPAIR ====================

    /**
     * Виправляє нульову кількість у Purchase/Sale-записах, які залишилися після
     * багу імпорту Excel (старі версії читали NUMERIC cell як "5.0", і
     * `toIntOrNull()` повертав null → qty=0).
     *
     * Для кожного запису з qty=0, totalAmount>0 і pricePerUnit>0:
     *   qty = round((totalAmount − additionalCosts) / pricePerUnit)
     *
     * Якщо обчислена qty виходить ≤ 0 — лишаємо без змін (можливо, легітимний
     * нульовий запис).
     *
     * Повертає кількість виправлених записів окремо для закупівель і продажів.
     */
    suspend fun repairZeroQuantities(): RepairResult = withContext(Dispatchers.IO) {
        var fixedPurchases = 0
        var fixedSales = 0

        database.withTransaction {
            val purchases = database.purchaseDao().getAllPurchases()
            for (p in purchases) {
                if (p.quantity != 0) continue
                if (p.totalAmount <= 0.0 || p.pricePerUnit <= 0.0) continue
                val net = (p.totalAmount - p.additionalCosts).coerceAtLeast(0.0)
                val qty = (net / p.pricePerUnit).let {
                    if (it.isFinite()) kotlin.math.round(it).toInt() else 0
                }
                if (qty > 0) {
                    database.purchaseDao().insert(p.copy(quantity = qty))
                    fixedPurchases += 1
                }
            }

            val sales = database.saleDao().getAllSales()
            for (s in sales) {
                if (s.quantity != 0) continue
                if (s.totalAmount <= 0.0 || s.pricePerUnit <= 0.0) continue
                val net = (s.totalAmount - s.additionalCosts).coerceAtLeast(0.0)
                val qty = (net / s.pricePerUnit).let {
                    if (it.isFinite()) kotlin.math.round(it).toInt() else 0
                }
                if (qty > 0) {
                    database.saleDao().insert(s.copy(quantity = qty))
                    fixedSales += 1
                }
            }
        }

        RepairResult(fixedPurchases = fixedPurchases, fixedSales = fixedSales)
    }

    data class RepairResult(val fixedPurchases: Int, val fixedSales: Int)
}
