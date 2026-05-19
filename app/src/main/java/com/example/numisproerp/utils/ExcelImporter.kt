package com.numisproerp.utils

import android.content.Context
import android.net.Uri
import com.numisproerp.data.database.AppDatabase
import com.numisproerp.data.entities.Client
import com.numisproerp.data.entities.CollectionItem
import com.numisproerp.data.entities.OtherExpense
import com.numisproerp.data.entities.Product
import com.numisproerp.data.entities.Purchase
import com.numisproerp.data.entities.Sale
import com.numisproerp.data.entities.Supplier
import com.numisproerp.data.entities.Writeoff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipInputStream

class ExcelImporter(
    private val database: AppDatabase
) {

    companion object {
        /**
         * Генерує детермінований CatalogID для рядків імпорту без власного ID.
         * Формат: "AUTO_<16-симв. hex>", де hex — перші 64 біти MD5 від об’єднаних
         * нормалізованих полів. Через детермінованість повторний імпорт того ж рядка
         * дає той самий ID, тому upsert (PK REPLACE) працює, а не дублює.
         */
        internal fun generateAutoCatalogId(
            name: String,
            series: String,
            nominal: String,
            material: String,
            category: String,
            issueDate: String,
            quality: String,
            diameter: String = "",
            weight: String = "",
            mintageAnnounced: String = "",
            artist: String = "",
            sculptor: String = ""
        ): String {
            val signature = listOf(
                name, series, nominal, material, category, issueDate, quality,
                diameter, weight, mintageAnnounced, artist, sculptor
            ).joinToString(separator = "\u0001") { it.trim().lowercase(Locale.ROOT) }
            val digest = MessageDigest.getInstance("MD5").digest(signature.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString(separator = "") { "%02x".format(it) }
            return "AUTO_${hex.substring(0, 16)}"
        }
    }

    data class ImportResult(
        val success: Boolean,
        val message: String,
        val productsCount: Int = 0,
        val productsAutoIdCount: Int = 0,
        val productsSkippedCount: Int = 0,
        val clientsCount: Int = 0,
        val suppliersCount: Int = 0,
        val purchasesCount: Int = 0,
        val salesCount: Int = 0,
        val expensesCount: Int = 0,
        val writeoffsCount: Int = 0,
        val collectionCount: Int = 0
    )

    /**
     * @param productsOnly якщо true — імпортується лише аркуш «Каталог Товарів»,
     *                     решта аркушів (клієнти, постачальники, закупівлі, продажі,
     *                     витрати, списання, колекція) ігнорується.
     */
    suspend fun importFromUri(context: Context, uri: Uri, productsOnly: Boolean = false): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                // Зчитуємо весь файл у пам'ять. І ZIP-бекап (PK… з database.xlsx
                // всередині), і чистий .xlsx починаються з ZIP-магічних байтів,
                // тому однопрохідний ZipInputStream не дозволяв надійно
                // відрізнити одне від другого без повторного відкриття стріму.
                // Тримаємо байти в ByteArray, щоб можна було спробувати обидва
                // шляхи без втрати даних. Стандартні бекапи на пристрої — це
                // десятки МБ максимум, тому in-memory варіант безпечний.
                val bytes = context.contentResolver.openInputStream(uri)?.use { rawStream ->
                    rawStream.readBytesSafely()
                } ?: return@withContext ImportResult(false, "Не вдалося відкрити файл")

                val isZipMagic = bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

                if (isZipMagic) {
                    // Спочатку пробуємо як ZIP-бекап: шукаємо `database.xlsx`
                    // всередині архіву. Якщо знайдено — імпортуємо саме його
                    // байти, попередньо розпакувавши фото в `imported_photos/`.
                    val extracted = ByteArrayInputStream(bytes).use { zipBytes ->
                        tryExtractZipBackup(context, zipBytes)
                    }
                    if (extracted != null) {
                        return@withContext ByteArrayInputStream(extracted.workbookBytes).use { wbStream ->
                            importFromInputStream(wbStream, productsOnly, extracted.photoPathRemap)
                        }
                    }
                }

                // Або це чистий .xlsx (теж починається з PK…, але без
                // `database.xlsx` всередині), або взагалі не ZIP — у будь-якому
                // разі віддаємо повні байти у WorkbookFactory зі свіжого стріму.
                return@withContext ByteArrayInputStream(bytes).use { wbStream ->
                    importFromInputStream(wbStream, productsOnly, emptyMap())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ImportResult(false, "Помилка імпорту: ${e.message}")
            }
        }
    }

    /**
     * Безпечне читання всього стріму в `ByteArray`. Використовується замість
     * стандартного `readBytes()` явно через сумісність з мінімальним Android API
     * та щоб збирати буфер вручну (деякі content-provider стріми не повертають
     * коректну довжину через `available()`).
     */
    private fun InputStream.readBytesSafely(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (true) {
            val n = this.read(chunk)
            if (n <= 0) break
            buffer.write(chunk, 0, n)
        }
        return buffer.toByteArray()
    }

    /**
     * Результат розпакування ZIP-бекапу:
     *  - `workbookBytes` — байти `database.xlsx`
     *  - `photoPathRemap` — мапа `photos/<safe>` -> абсолютний шлях, куди розпаковано
     *    фото на цьому пристрої (в `filesDir/imported_photos/`).
     */
    private data class ZipExtractResult(
        val workbookBytes: ByteArray,
        val photoPathRemap: Map<String, String>
    )

    /**
     * Розпаковує ZIP-бекап. Повертає `null`, якщо в ньому нема `database.xlsx`
     * (тобто це просто .xlsx з ZIP-магічними байтами — обробимо його в звичайній гілці).
     */
    private fun tryExtractZipBackup(context: Context, stream: InputStream): ZipExtractResult? {
        val photosDir = File(context.filesDir, "imported_photos").apply { mkdirs() }
        val remap = mutableMapOf<String, String>()
        var workbookBytes: ByteArray? = null

        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && name == "database.xlsx") {
                    workbookBytes = zip.readBytes()
                } else if (!entry.isDirectory && name.startsWith("photos/") && name.length > "photos/".length) {
                    val safeName = name.substringAfter("photos/").substringAfterLast('/')
                    val outFile = File(photosDir, safeName)
                    FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                    remap[name] = outFile.absolutePath
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val wb = workbookBytes ?: return null
        return ZipExtractResult(wb, remap)
    }

    /**
     * Якщо значення комірки виглядає як відносний шлях у ZIP-бекапі (`photos/...`)
     * і у нас є мапа розпакованих фото — повертаємо абсолютний шлях нового місця.
     * Якщо мапи нема або ключ не знайдено — повертаємо вхідне значення без змін
     * (це або абсолютний шлях зі старого .xlsx-бекапу, або просто відсутнє фото).
     */
    private fun remapPhoto(value: String, remap: Map<String, String>): String {
        if (remap.isEmpty()) return value
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return value
        return remap[trimmed] ?: value
    }

    private suspend fun importFromInputStream(
        inputStream: InputStream,
        productsOnly: Boolean = false,
        photoPathRemap: Map<String, String> = emptyMap()
    ): ImportResult {
        var productsCount = 0
        var productsAutoIdCount = 0
        var productsSkippedCount = 0
        var clientsCount = 0
        var suppliersCount = 0
        var purchasesCount = 0
        var salesCount = 0
        var expensesCount = 0
        var writeoffsCount = 0
        var collectionCount = 0

        try {
            val workbook = WorkbookFactory.create(inputStream)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            // 1. Імпорт товарів (аркуш "Каталог Товарів")
            val productSheet = workbook.getSheet("Каталог Товарів")
            if (productSheet != null) {
                // Визначаємо формат по заголовку:
                //  - "PhotoPath" у колонці 8 → новий експорт NumisProERP
                //  - інакше — стара 13-колонкова структура (legacy)
                val headerRow = productSheet.getRow(0)
                val isNewFormat = (headerRow?.getCell(8)?.toString() ?: "")
                    .equals("PhotoPath", ignoreCase = true)

                val products = mutableListOf<Product>()
                for (rowIndex in 1 until productSheet.physicalNumberOfRows) {
                    val row = productSheet.getRow(rowIndex) ?: continue
                    val rawCatalogId = row.getCell(0)?.toString()?.trim() ?: ""
                    val nameValue = row.getCell(1)?.toString()?.trim() ?: ""

                    // Якщо порожній і ID, і назва — рядок повністю порожній, пропускаємо
                    if (rawCatalogId.isBlank() && nameValue.isBlank()) {
                        productsSkippedCount++
                        continue
                    }

                    val product = if (isNewFormat) {
                        val series = row.getCell(2)?.toString() ?: ""
                        val material = row.getCell(3)?.toString() ?: ""
                        val nominal = row.getCell(4)?.toString() ?: ""
                        val category = row.getCell(5)?.toString() ?: ""
                        val issueDate = row.getCell(6)?.toString() ?: ""
                        val quality = row.getCell(7)?.toString() ?: ""
                        val photoPath = remapPhoto(row.getCell(8)?.toString() ?: "", photoPathRemap)
                        // Розширений експорт (колонки 9-18) додано для підтримки
                        // ручно доданих товарів. Старі експорти не мають цих
                        // колонок — fall-back на порожній рядок / 0.0 / false.
                        val photoPathBack = remapPhoto(row.getCell(9)?.toString() ?: "", photoPathRemap)
                        val diameter = row.getCell(10)?.toString() ?: ""
                        val weight = row.getCell(11)?.toString() ?: ""
                        val mintageAnnounced = row.getCell(12)?.toString() ?: ""
                        val mintageActual = row.getCell(13)?.toString() ?: ""
                        val artist = row.getCell(14)?.toString() ?: ""
                        val sculptor = row.getCell(15)?.toString() ?: ""
                        val description = row.getCell(16)?.toString() ?: ""
                        val estimatedValue = (row.getCell(17)?.toString() ?: "")
                            .replace(',', '.').toDoubleOrNull() ?: 0.0
                        val isManualStr = row.getCell(18)?.toString()?.trim() ?: ""
                        val isManual = isManualStr == "1" || isManualStr.equals("true", ignoreCase = true)
                        val catalogId = if (rawCatalogId.isBlank()) {
                            productsAutoIdCount++
                            // Детермінований ID: MD5-хеш від контенту рядка. Повторний імпорт
                            // того ж файлу дасть той самий ID → upsert замість дублікату.
                            generateAutoCatalogId(nameValue, series, nominal, material, category, issueDate, quality)
                        } else {
                            rawCatalogId
                        }
                        Product(
                            catalogId = catalogId,
                            name = nameValue,
                            series = series,
                            material = material,
                            nominal = nominal,
                            category = category,
                            issueDate = issueDate,
                            quality = quality,
                            diameter = diameter,
                            weight = weight,
                            mintageAnnounced = mintageAnnounced,
                            mintageActual = mintageActual,
                            artist = artist,
                            sculptor = sculptor,
                            photoPath = photoPath,
                            photoPathBack = photoPathBack,
                            description = description,
                            estimatedValue = estimatedValue,
                            isManual = isManual
                        )
                    } else {
                        val series = row.getCell(2)?.toString() ?: ""
                        val issueDate = row.getCell(3)?.toString() ?: ""
                        val material = row.getCell(4)?.toString() ?: ""
                        val nominal = row.getCell(5)?.toString() ?: ""
                        val diameter = row.getCell(6)?.toString() ?: ""
                        val weight = row.getCell(7)?.toString() ?: ""
                        val mintageAnnounced = row.getCell(8)?.toString() ?: ""
                        val category = row.getCell(9)?.toString() ?: ""
                        val quality = row.getCell(10)?.toString() ?: ""
                        val artist = row.getCell(11)?.toString() ?: ""
                        val sculptor = row.getCell(12)?.toString() ?: ""
                        val catalogId = if (rawCatalogId.isBlank()) {
                            productsAutoIdCount++
                            // Для legacy-формату (13 колонок) включаємо diameter, weight,
                            // mintageAnnounced, artist, sculptor у хеш, щоб рядки, які відрізняються
                            // лише цими полями, не колідували.
                            generateAutoCatalogId(
                                nameValue, series, nominal, material, category, issueDate, quality,
                                diameter, weight, mintageAnnounced, artist, sculptor
                            )
                        } else {
                            rawCatalogId
                        }
                        Product(
                            catalogId = catalogId,
                            name = nameValue,
                            series = series,
                            issueDate = issueDate,
                            material = material,
                            nominal = nominal,
                            diameter = diameter,
                            weight = weight,
                            mintageAnnounced = mintageAnnounced,
                            category = category,
                            quality = quality,
                            artist = artist,
                            sculptor = sculptor,
                            photoPath = ""
                        )
                    }
                    products.add(product)
                }
                if (products.isNotEmpty()) {
                    // Upsert: існуючі товари оновлюються по catalogId (PK),
                    // нові — додаються. Попередня БД НЕ зноситься.
                    database.productDao().insertAll(products)
                    productsCount = products.size
                }
            }

            if (productsOnly) {
                workbook.close()
                return ImportResult(
                    success = true,
                    message = "Імпорт товарів завершено успішно",
                    productsCount = productsCount,
                    productsAutoIdCount = productsAutoIdCount,
                    productsSkippedCount = productsSkippedCount
                )
            }

            // 2. Імпорт клієнтів (аркуш "Клієнти")
            val clientsSheet = workbook.getSheet("Клієнти")
            if (clientsSheet != null) {
                val clients = mutableListOf<Client>()
                for (rowIndex in 1 until clientsSheet.physicalNumberOfRows) {
                    val row = clientsSheet.getRow(rowIndex) ?: continue
                    val clientId = row.getCell(0)?.toString() ?: continue
                    if (clientId.isBlank()) continue

                    val client = Client(
                        clientId = clientId,
                        name = row.getCell(1)?.toString() ?: "",
                        phone = row.getCell(2)?.toString() ?: "",
                        telegram = row.getCell(3)?.toString() ?: "",
                        city = row.getCell(4)?.toString() ?: "",
                        notes = row.getCell(5)?.toString() ?: ""
                    )
                    clients.add(client)
                }
                if (clients.isNotEmpty()) {
                    // Upsert (PK = clientId)
                    database.clientDao().insertAll(clients)
                    clientsCount = clients.size
                }
            }

            // 3. Імпорт постачальників (аркуш "Постачальники")
            val suppliersSheet = workbook.getSheet("Постачальники")
            if (suppliersSheet != null) {
                val suppliers = mutableListOf<Supplier>()
                for (rowIndex in 1 until suppliersSheet.physicalNumberOfRows) {
                    val row = suppliersSheet.getRow(rowIndex) ?: continue
                    val supplierId = row.getCell(0)?.toString() ?: continue
                    if (supplierId.isBlank()) continue

                    val supplier = Supplier(
                        supplierId = supplierId,
                        name = row.getCell(1)?.toString() ?: "",
                        contact = row.getCell(2)?.toString() ?: "",
                        type = row.getCell(3)?.toString() ?: "",
                        comment = row.getCell(4)?.toString() ?: ""
                    )
                    suppliers.add(supplier)
                }
                if (suppliers.isNotEmpty()) {
                    // Upsert (PK = supplierId)
                    database.supplierDao().insertAll(suppliers)
                    suppliersCount = suppliers.size
                }
            }

            // 4. Імпорт закупівель (аркуш "Закупівлі")
            val purchasesSheet = workbook.getSheet("Закупівлі")
            if (purchasesSheet != null) {
                val purchases = mutableListOf<Purchase>()
                for (rowIndex in 1 until purchasesSheet.physicalNumberOfRows) {
                    val row = purchasesSheet.getRow(rowIndex) ?: continue
                    val purchaseId = row.getCell(0)?.toString() ?: continue
                    if (purchaseId.isBlank()) continue

                    val dateStr = row.getCell(1)?.toString() ?: ""
                    val date = try {
                        dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val purchase = Purchase(
                        purchaseId = purchaseId,
                        date = date,
                        catalogId = row.getCell(2)?.toString() ?: "",
                        supplierId = row.getCell(3)?.toString() ?: "",
                        // POI записує Int як NUMERIC з .0 ("5.0"). `.toIntOrNull()` на "5.0" дає null,
                        // треба спочатку в Double, потім в Int.
                        quantity = row.getCell(4)?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
                        pricePerUnit = row.getCell(5)?.toString()?.toDoubleOrNull() ?: 0.0,
                        additionalCosts = row.getCell(6)?.toString()?.toDoubleOrNull() ?: 0.0,
                        totalAmount = row.getCell(7)?.toString()?.toDoubleOrNull() ?: 0.0,
                        // ID на кшталт `P_BUNDLE_<bundleId>` — внутрішня операція збірки лоту.
                        // Зберігаємо як службову, щоб не з'являлась у звичайних історіях/звітах.
                        isBundleOp = purchaseId.startsWith("P_BUNDLE_")
                    )
                    purchases.add(purchase)
                }
                if (purchases.isNotEmpty()) {
                    // Upsert (PK = purchaseId)
                    purchases.forEach { database.purchaseDao().insert(it) }
                    purchasesCount = purchases.size
                }
            }

            // 5. Імпорт продажів (аркуш "Продажі")
            val salesSheet = workbook.getSheet("Продажі")
            if (salesSheet != null) {
                val sales = mutableListOf<Sale>()
                for (rowIndex in 1 until salesSheet.physicalNumberOfRows) {
                    val row = salesSheet.getRow(rowIndex) ?: continue
                    val saleId = row.getCell(0)?.toString() ?: continue
                    if (saleId.isBlank()) continue

                    val dateStr = row.getCell(1)?.toString() ?: ""
                    val date = try {
                        dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val sale = Sale(
                        saleId = saleId,
                        date = date,
                        catalogId = row.getCell(2)?.toString() ?: "",
                        clientId = row.getCell(3)?.toString() ?: "",
                        // POI записує Int як NUMERIC з .0 ("5.0"). `.toIntOrNull()` на "5.0" дає null,
                        // треба спочатку в Double, потім в Int.
                        quantity = row.getCell(4)?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
                        pricePerUnit = row.getCell(5)?.toString()?.toDoubleOrNull() ?: 0.0,
                        additionalCosts = row.getCell(6)?.toString()?.toDoubleOrNull() ?: 0.0,
                        netProfit = row.getCell(7)?.toString()?.toDoubleOrNull() ?: 0.0,
                        totalAmount = row.getCell(8)?.toString()?.toDoubleOrNull() ?: 0.0
                    )
                    sales.add(sale)
                }
                if (sales.isNotEmpty()) {
                    // Upsert (PK = saleId)
                    sales.forEach { database.saleDao().insert(it) }
                    salesCount = sales.size
                }
            }

            // 6. Імпорт витрат (аркуш "Інші Витрати")
            val expensesSheet = workbook.getSheet("Інші Витрати")
            if (expensesSheet != null) {
                val expenses = mutableListOf<OtherExpense>()
                for (rowIndex in 1 until expensesSheet.physicalNumberOfRows) {
                    val row = expensesSheet.getRow(rowIndex) ?: continue
                    val expenseId = row.getCell(0)?.toString() ?: continue
                    if (expenseId.isBlank()) continue

                    val dateStr = row.getCell(1)?.toString() ?: ""
                    val date = try {
                        dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val expense = OtherExpense(
                        expenseId = expenseId,
                        date = date,
                        category = row.getCell(2)?.toString() ?: "",
                        amount = row.getCell(3)?.toString()?.toDoubleOrNull() ?: 0.0,
                        comment = row.getCell(4)?.toString() ?: ""
                    )
                    expenses.add(expense)
                }
                if (expenses.isNotEmpty()) {
                    // Upsert (PK = expenseId)
                    expenses.forEach { database.otherExpenseDao().insert(it) }
                    expensesCount = expenses.size
                }
            }

            // 7. Імпорт списань (аркуш "Списання")
            val writeoffsSheet = workbook.getSheet("Списання")
            if (writeoffsSheet != null) {
                val writeoffs = mutableListOf<Writeoff>()
                for (rowIndex in 1 until writeoffsSheet.physicalNumberOfRows) {
                    val row = writeoffsSheet.getRow(rowIndex) ?: continue
                    val writeoffId = row.getCell(0)?.toString() ?: continue
                    if (writeoffId.isBlank()) continue

                    val dateStr = row.getCell(1)?.toString() ?: ""
                    val date = try {
                        dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val writeoff = Writeoff(
                        writeoffId = writeoffId,
                        date = date,
                        catalogId = row.getCell(2)?.toString() ?: "",
                        quantity = row.getCell(3)?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
                        pricePerUnit = row.getCell(4)?.toString()?.toDoubleOrNull() ?: 0.0,
                        totalAmount = row.getCell(5)?.toString()?.toDoubleOrNull() ?: 0.0,
                        reason = row.getCell(6)?.toString() ?: "",
                        comment = row.getCell(7)?.toString() ?: "",
                        // ID на кшталт `WO_BUNDLE_<bundleId>_<componentId>` — внутрішнє
                        // списання компонента при збірці. Не показуємо в історії списань.
                        isBundleOp = writeoffId.startsWith("WO_BUNDLE_")
                    )
                    writeoffs.add(writeoff)
                }
                if (writeoffs.isNotEmpty()) {
                    // Upsert (PK = writeoffId)
                    writeoffs.forEach { database.writeoffDao().insert(it) }
                    writeoffsCount = writeoffs.size
                }
            }

            // 8. Імпорт колекції (аркуш "Моя колекція")
            val collectionSheet = workbook.getSheet("Моя колекція")
            if (collectionSheet != null) {
                val items = mutableListOf<CollectionItem>()
                for (rowIndex in 1 until collectionSheet.physicalNumberOfRows) {
                    val row = collectionSheet.getRow(rowIndex) ?: continue
                    val collectionId = row.getCell(0)?.toString() ?: continue
                    if (collectionId.isBlank()) continue

                    val dateStr = row.getCell(9)?.toString() ?: ""
                    val dateAdded = try {
                        dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val item = CollectionItem(
                        collectionId = collectionId,
                        name = row.getCell(1)?.toString() ?: "",
                        series = row.getCell(2)?.toString() ?: "",
                        category = row.getCell(3)?.toString() ?: "",
                        material = row.getCell(4)?.toString() ?: "",
                        nominal = row.getCell(5)?.toString() ?: "",
                        quality = row.getCell(6)?.toString() ?: "",
                        quantity = row.getCell(7)?.toString()?.toDoubleOrNull()?.toInt() ?: 1,
                        estimatedValue = row.getCell(8)?.toString()?.toDoubleOrNull() ?: 0.0,
                        dateAdded = dateAdded,
                        description = row.getCell(10)?.toString() ?: "",
                        photoPath = remapPhoto(row.getCell(11)?.toString() ?: "", photoPathRemap)
                    )
                    items.add(item)
                }
                if (items.isNotEmpty()) {
                    // Upsert (PK = collectionId)
                    items.forEach { database.collectionItemDao().insert(it) }
                    collectionCount = items.size
                }
            }

            workbook.close()

            // Після імпорту products повторно синхронізуємо «дзеркальні» Product-и
            // для всіх товарів з «Моєї колекції». Записуємо з актуальним `photoPath`
            // із колекції (upsert не зачепить інших товарів — оновить лише ті, що
            // мають той самий catalogId, що й collectionId).
            try {
                val collectionItems = database.collectionItemDao().getAllSync()
                if (collectionItems.isNotEmpty()) {
                    val collectionProducts = collectionItems.map { ci ->
                        Product(
                            catalogId = ci.collectionId,
                            name = ci.name,
                            series = ci.series,
                            material = ci.material,
                            nominal = ci.nominal,
                            category = ci.category,
                            quality = ci.quality,
                            photoPath = ci.photoPath
                        )
                    }
                    database.productDao().insertAll(collectionProducts)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return ImportResult(
                success = true,
                message = "Імпорт завершено успішно",
                productsCount = productsCount,
                productsAutoIdCount = productsAutoIdCount,
                productsSkippedCount = productsSkippedCount,
                clientsCount = clientsCount,
                suppliersCount = suppliersCount,
                purchasesCount = purchasesCount,
                salesCount = salesCount,
                expensesCount = expensesCount,
                writeoffsCount = writeoffsCount,
                collectionCount = collectionCount
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return ImportResult(false, "Помилка імпорту: ${e.message}")
        }
    }
}