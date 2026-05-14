package com.numisproerp.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.numisproerp.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Генератор звітних PDF на основі вмісту бази даних.
 *
 * Використовує `android.graphics.pdf.PdfDocument` (вбудований у Android),
 * який підтримує кирилицю через системні шрифти Canvas. Завдяки цьому не
 * потрібно поставляти TTF-файли разом з додатком.
 *
 * Підтримує:
 *   - повний звіт по операціях ([generateOperationsReport])
 *   - звіт по продажам клієнтам за діапазоном дат
 *     ([generateSalesReport])
 *   - звіт по закупівлям у постачальників за діапазоном дат
 *     ([generatePurchasesReport])
 */
class PdfReportGenerator(
    private val database: AppDatabase
) {

    data class Result(
        val success: Boolean,
        val message: String,
        val filePath: String = ""
    )

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    // ============================================================
    // Загальні утиліти для роботи з PdfDocument
    // ============================================================

    private class PageState(
        var page: PdfDocument.Page,
        var pageNumber: Int,
        var y: Float
    )

    private inner class PdfWriter(
        val document: PdfDocument,
        val pageWidth: Int = 595,  // A4 portrait, 72 DPI ~= 595x842
        val pageHeight: Int = 842,
        val margin: Float = 36f
    ) {
        val usableWidth: Float = pageWidth - margin * 2

        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = 0xFF000000.toInt()
        }
        val sectionPaint = Paint().apply {
            textSize = 13f
            isFakeBoldText = true
            color = 0xFF000000.toInt()
            typeface = Typeface.DEFAULT_BOLD
        }
        val bodyPaint = Paint().apply {
            textSize = 10f
            color = 0xFF222222.toInt()
        }
        val mutedPaint = Paint().apply {
            textSize = 9f
            color = 0xFF666666.toInt()
        }

        private val firstInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        private var state: PageState = PageState(document.startPage(firstInfo), 1, margin)

        private fun newPage() {
            document.finishPage(state.page)
            val nextNumber = state.pageNumber + 1
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, nextNumber).create()
            state = PageState(document.startPage(info), nextNumber, margin)
        }

        private fun ensureSpace(needed: Float) {
            if (state.y + needed > pageHeight - margin) newPage()
        }

        fun drawText(text: String, paint: Paint = bodyPaint, indent: Float = 0f) {
            ensureSpace(paint.textSize + 4f)
            // Простий перенос: ділимо рядок, якщо він не вміщається в ширину сторінки.
            val available = usableWidth - indent
            val lines = wrapText(text, paint, available)
            for (line in lines) {
                ensureSpace(paint.textSize + 4f)
                state.page.canvas.drawText(line, margin + indent, state.y + paint.textSize, paint)
                state.y += paint.textSize + 4f
            }
        }

        fun drawDivider() {
            ensureSpace(8f)
            state.page.canvas.drawLine(
                margin, state.y + 4f,
                margin + usableWidth, state.y + 4f,
                mutedPaint
            )
            state.y += 8f
        }

        fun drawSection(title: String) {
            state.y += 6f
            drawText(title, sectionPaint)
            drawDivider()
        }

        fun finish() {
            document.finishPage(state.page)
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (paint.measureText(text) <= maxWidth) return listOf(text)
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var current = StringBuilder()
            for (w in words) {
                val candidate = if (current.isEmpty()) w else "$current $w"
                if (paint.measureText(candidate) <= maxWidth) {
                    if (current.isEmpty()) current.append(w) else { current.append(' '); current.append(w) }
                } else {
                    if (current.isNotEmpty()) {
                        lines.add(current.toString())
                        current = StringBuilder(w)
                    } else {
                        // одне слово ширше за рядок — обрізаємо побуквенно.
                        var chunk = StringBuilder()
                        for (ch in w) {
                            if (paint.measureText(chunk.toString() + ch) <= maxWidth) {
                                chunk.append(ch)
                            } else {
                                lines.add(chunk.toString())
                                chunk = StringBuilder().append(ch)
                            }
                        }
                        if (chunk.isNotEmpty()) current = chunk
                    }
                }
            }
            if (current.isNotEmpty()) lines.add(current.toString())
            return lines
        }
    }

    private fun saveDocument(document: PdfDocument, fileNamePrefix: String): Result {
        val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.pdf"
        val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!folder.exists()) folder.mkdirs()
        val file = File(folder, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return Result(true, "PDF збережено", file.absolutePath)
    }

    private fun saveDocumentTo(document: PdfDocument, target: File): Result {
        target.parentFile?.let { if (!it.exists()) it.mkdirs() }
        FileOutputStream(target).use { document.writeTo(it) }
        document.close()
        return Result(true, "PDF збережено", target.absolutePath)
    }

    // ============================================================
    // Повний звіт по операціях
    // ============================================================

    suspend fun generateOperationsReport(context: Context): Result = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val w = PdfWriter(document)

            w.drawText("NumisProERP — Звіт по операціях", w.titlePaint)
            w.drawText("Сформовано: ${dateTimeFormat.format(Date())}", w.mutedPaint)
            w.drawDivider()

            val purchases = database.purchaseDao().getAllPurchases()
            val totalPurchases = purchases.sumOf { it.totalAmount }
            w.drawSection("Закупівлі (${purchases.size}) — Сума: ${"%,.2f".format(totalPurchases)} ₴")
            for (p in purchases) {
                val productName = database.productDao().getProductById(p.catalogId)?.name ?: p.catalogId
                w.drawText("${dateFormat.format(Date(p.date))}  •  $productName  •  ${p.quantity} шт. × ${"%,.2f".format(p.pricePerUnit)} = ${"%,.2f".format(p.totalAmount)} ₴")
            }

            val sales = database.saleDao().getAllSales()
            val totalSales = sales.sumOf { it.totalAmount }
            w.drawSection("Продажі (${sales.size}) — Сума: ${"%,.2f".format(totalSales)} ₴")
            for (s in sales) {
                val productName = database.productDao().getProductById(s.catalogId)?.name ?: s.catalogId
                w.drawText("${dateFormat.format(Date(s.date))}  •  $productName  •  ${s.quantity} шт. × ${"%,.2f".format(s.pricePerUnit)} = ${"%,.2f".format(s.totalAmount)} ₴")
            }

            val writeoffs = database.writeoffDao().getAll()
            val totalWriteoffs = writeoffs.sumOf { it.totalAmount }
            w.drawSection("Списання (${writeoffs.size}) — Сума: ${"%,.2f".format(totalWriteoffs)} ₴")
            for (wo in writeoffs) {
                val productName = database.productDao().getProductById(wo.catalogId)?.name ?: wo.catalogId
                w.drawText("${dateFormat.format(Date(wo.date))}  •  $productName  •  ${wo.quantity} шт. ×  ${"%,.2f".format(wo.pricePerUnit)} = ${"%,.2f".format(wo.totalAmount)} ₴  •  ${wo.reason}")
            }

            val expenses = database.otherExpenseDao().getAllExpensesSync()
            val totalExpenses = expenses.sumOf { it.amount }
            w.drawSection("Витрати (${expenses.size}) — Сума: ${"%,.2f".format(totalExpenses)} ₴")
            for (e in expenses) {
                w.drawText("${dateFormat.format(Date(e.date))}  •  ${e.category}  •  ${"%,.2f".format(e.amount)} ₴  •  ${e.comment}")
            }

            val collection = database.collectionItemDao().getAllSync()
            val totalEstimated = collection.sumOf { it.estimatedValue * it.quantity }
            w.drawSection("Моя колекція (${collection.size}) — Оціночна вартість: ${"%,.2f".format(totalEstimated)} ₴")
            for (c in collection) {
                w.drawText("${c.name}  •  ${c.quantity} шт.  •  ${"%,.2f".format(c.estimatedValue)} ₴/шт.")
            }

            w.drawSection("Підсумки")
            w.drawText("Дохід (продажі): ${"%,.2f".format(totalSales)} ₴")
            w.drawText("Витрати на закупівлі: ${"%,.2f".format(totalPurchases)} ₴")
            w.drawText("Інші витрати: ${"%,.2f".format(totalExpenses)} ₴")
            w.drawText("Списання: ${"%,.2f".format(totalWriteoffs)} ₴")
            val netProfit = totalSales - totalPurchases - totalExpenses - totalWriteoffs
            w.drawText("Чистий прибуток: ${"%,.2f".format(netProfit)} ₴", w.sectionPaint)

            w.finish()
            saveDocument(document, "NumisProERP_Report")
        } catch (e: Exception) {
            e.printStackTrace()
            Result(false, "Помилка генерації PDF: ${e.message}")
        }
    }

    // ============================================================
    // Продажі по клієнтах за діапазоном дат
    // ============================================================

    /**
     * Якщо `clientId == null` — звіт по всіх клієнтах.
     * `endDate` включно (час 23:59:59 додається при перевірці).
     */
    suspend fun generateSalesReport(
        context: Context,
        startDate: Long,
        endDate: Long,
        clientId: String?
    ): Result = withContext(Dispatchers.IO) {
        try {
            val endInclusive = endDate + 24 * 60 * 60 * 1000L - 1
            val allSales = database.saleDao().getAllSales().filter {
                it.date in startDate..endInclusive
            }
            val sales = if (clientId != null) allSales.filter { it.clientId == clientId } else allSales

            val document = PdfDocument()
            val w = PdfWriter(document)

            w.drawText("NumisProERP — Звіт продажів", w.titlePaint)
            w.drawText("Період: ${dateFormat.format(Date(startDate))} – ${dateFormat.format(Date(endDate))}", w.mutedPaint)
            if (clientId != null) {
                val client = database.clientDao().getClientById(clientId)
                w.drawText("Клієнт: ${client?.name ?: clientId}", w.mutedPaint)
            } else {
                w.drawText("Клієнт: усі", w.mutedPaint)
            }
            w.drawText("Сформовано: ${dateTimeFormat.format(Date())}", w.mutedPaint)
            w.drawDivider()

            // Group by client when reporting "all clients"
            if (clientId == null) {
                val byClient = sales.groupBy { it.clientId }
                for ((cid, group) in byClient) {
                    val client = database.clientDao().getClientById(cid)
                    val clientName = client?.name ?: cid
                    val clientTotal = group.sumOf { it.totalAmount }
                    w.drawSection("Клієнт: $clientName — ${"%,.2f".format(clientTotal)} ₴ (${group.size} операцій)")
                    for (s in group.sortedBy { it.date }) {
                        val productName = database.productDao().getProductById(s.catalogId)?.name ?: s.catalogId
                        w.drawText("${dateFormat.format(Date(s.date))}  •  $productName  •  ${s.quantity} шт. × ${"%,.2f".format(s.pricePerUnit)}  •  дод. ${"%,.2f".format(s.additionalCosts)} ₴  •  Σ ${"%,.2f".format(s.totalAmount)} ₴")
                    }
                }
            } else {
                w.drawSection("Продажі (${sales.size})")
                for (s in sales.sortedBy { it.date }) {
                    val productName = database.productDao().getProductById(s.catalogId)?.name ?: s.catalogId
                    w.drawText("${dateFormat.format(Date(s.date))}  •  $productName  •  ${s.quantity} шт. × ${"%,.2f".format(s.pricePerUnit)}  •  дод. ${"%,.2f".format(s.additionalCosts)} ₴  •  Σ ${"%,.2f".format(s.totalAmount)} ₴")
                }
            }

            val total = sales.sumOf { it.totalAmount }
            val totalProfit = sales.sumOf { it.netProfit }
            w.drawSection("Підсумки за період")
            w.drawText("Кількість операцій: ${sales.size}")
            w.drawText("Загальна сума: ${"%,.2f".format(total)} ₴")
            w.drawText("Чистий прибуток: ${"%,.2f".format(totalProfit)} ₴", w.sectionPaint)

            w.finish()
            saveDocument(document, "NumisProERP_Sales")
        } catch (e: Exception) {
            e.printStackTrace()
            Result(false, "Помилка генерації PDF: ${e.message}")
        }
    }

    // ============================================================
    // Закупівлі по постачальниках за діапазоном дат
    // ============================================================

    suspend fun generatePurchasesReport(
        context: Context,
        startDate: Long,
        endDate: Long,
        supplierId: String?
    ): Result = withContext(Dispatchers.IO) {
        try {
            val endInclusive = endDate + 24 * 60 * 60 * 1000L - 1
            val all = database.purchaseDao().getAllPurchases().filter {
                it.date in startDate..endInclusive
            }
            val items = if (supplierId != null) all.filter { it.supplierId == supplierId } else all

            val document = PdfDocument()
            val w = PdfWriter(document)

            w.drawText("NumisProERP — Звіт закупівель", w.titlePaint)
            w.drawText("Період: ${dateFormat.format(Date(startDate))} – ${dateFormat.format(Date(endDate))}", w.mutedPaint)
            if (supplierId != null) {
                val s = database.supplierDao().getSupplierById(supplierId)
                w.drawText("Постачальник: ${s?.name ?: supplierId}", w.mutedPaint)
            } else {
                w.drawText("Постачальник: усі", w.mutedPaint)
            }
            w.drawText("Сформовано: ${dateTimeFormat.format(Date())}", w.mutedPaint)
            w.drawDivider()

            if (supplierId == null) {
                val bySupplier = items.groupBy { it.supplierId }
                for ((sid, group) in bySupplier) {
                    val supp = database.supplierDao().getSupplierById(sid)
                    val name = supp?.name ?: sid
                    val total = group.sumOf { it.totalAmount }
                    w.drawSection("Постачальник: $name — ${"%,.2f".format(total)} ₴ (${group.size} операцій)")
                    for (p in group.sortedBy { it.date }) {
                        val productName = database.productDao().getProductById(p.catalogId)?.name ?: p.catalogId
                        w.drawText("${dateFormat.format(Date(p.date))}  •  $productName  •  ${p.quantity} шт. × ${"%,.2f".format(p.pricePerUnit)}  •  дод. ${"%,.2f".format(p.additionalCosts)} ₴  •  Σ ${"%,.2f".format(p.totalAmount)} ₴")
                    }
                }
            } else {
                w.drawSection("Закупівлі (${items.size})")
                for (p in items.sortedBy { it.date }) {
                    val productName = database.productDao().getProductById(p.catalogId)?.name ?: p.catalogId
                    w.drawText("${dateFormat.format(Date(p.date))}  •  $productName  •  ${p.quantity} шт. × ${"%,.2f".format(p.pricePerUnit)}  •  дод. ${"%,.2f".format(p.additionalCosts)} ₴  •  Σ ${"%,.2f".format(p.totalAmount)} ₴")
                }
            }

            val total = items.sumOf { it.totalAmount }
            w.drawSection("Підсумки за період")
            w.drawText("Кількість операцій: ${items.size}")
            w.drawText("Загальна сума: ${"%,.2f".format(total)} ₴", w.sectionPaint)

            w.finish()
            saveDocument(document, "NumisProERP_Purchases")
        } catch (e: Exception) {
            e.printStackTrace()
            Result(false, "Помилка генерації PDF: ${e.message}")
        }
    }

    // ============================================================
    // PDF-чек одного продажу (всі позиції в кошику)
    // ============================================================

    data class ReceiptItem(
        val productName: String,
        val quantity: Int,
        val pricePerUnit: Double,
        val additionalCosts: Double,
        val totalAmount: Double
    )

    /**
     * Генерує PDF-чек з результатами щойно проведеного продажу.
     * Зберігає у внутрішньому кеш-каталозі (cacheDir/receipts), щоб
     * можна було поділитися через FileProvider/Intent.ACTION_SEND.
     */
    suspend fun generateSaleReceipt(
        context: Context,
        clientName: String,
        date: Long,
        items: List<ReceiptItem>
    ): Result = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val w = PdfWriter(document)

            w.drawText("NumisProERP — Чек продажу", w.titlePaint)
            w.drawText("Дата: ${dateTimeFormat.format(Date(date))}", w.mutedPaint)
            w.drawText("Клієнт: $clientName", w.mutedPaint)
            w.drawDivider()

            w.drawSection("Позиції")
            for ((index, it) in items.withIndex()) {
                w.drawText("${index + 1}. ${it.productName}", w.sectionPaint)
                w.drawText("    кількість: ${it.quantity} шт.", indent = 8f)
                w.drawText("    ціна за шт: ${"%,.2f".format(it.pricePerUnit)} ₴", indent = 8f)
                if (it.additionalCosts > 0.0) {
                    w.drawText("    дод. витрати: ${"%,.2f".format(it.additionalCosts)} ₴", indent = 8f)
                }
                w.drawText("    разом: ${"%,.2f".format(it.totalAmount)} ₴", indent = 8f)
            }

            w.drawDivider()

            val totalQuantity = items.sumOf { it.quantity }
            val totalAdditional = items.sumOf { it.additionalCosts }
            val grandTotal = items.sumOf { it.totalAmount }

            w.drawSection("Загалом")
            w.drawText("Кількість позицій: ${items.size}")
            w.drawText("Загальна кількість: $totalQuantity шт.")
            if (totalAdditional > 0.0) {
                w.drawText("Дод. витрати: ${"%,.2f".format(totalAdditional)} ₴")
            }
            w.drawText("Разом до сплати: ${"%,.2f".format(grandTotal)} ₴", w.sectionPaint)

            w.finish()

            val folder = File(context.cacheDir, "receipts")
            val file = File(folder, "Receipt_${date}.pdf")
            saveDocumentTo(document, file)
        } catch (e: Exception) {
            e.printStackTrace()
            Result(false, "Помилка генерації чеку: ${e.message}")
        }
    }
}
