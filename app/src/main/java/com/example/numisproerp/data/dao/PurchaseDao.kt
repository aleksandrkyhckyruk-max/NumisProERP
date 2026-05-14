package com.numisproerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.numisproerp.data.entities.Purchase

data class PurchaseWithDetails(
    val purchaseId: String,
    val date: Long,
    val totalAmount: Double,
    val productName: String,
    val supplierName: String
)

data class PurchaseWithProductName(
    val purchaseId: String,
    val date: Long,
    val catalogId: String,
    val productName: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val totalAmount: Double
)

@Dao
interface PurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: Purchase)

    // Тільки «реальні» закупівлі — без внутрішніх операцій «Моя збірка».
    @Query("SELECT SUM(totalAmount) FROM purchases WHERE isBundleOp = 0")
    suspend fun getTotalSum(): Double?

    @Query("""
        SELECT SUM(totalAmount) FROM purchases
        WHERE date BETWEEN :startDate AND :endDate
          AND isBundleOp = 0
    """)
    suspend fun getSumByDateRange(startDate: Long, endDate: Long): Double?

    @Query("""
        SELECT p.purchaseId, p.date, p.totalAmount, 
               pr.name as productName, s.name as supplierName
        FROM purchases p
        JOIN products pr ON p.catalogId = pr.catalogId
        JOIN suppliers s ON p.supplierId = s.supplierId
        WHERE p.isBundleOp = 0
        ORDER BY p.date DESC
        LIMIT :limit
    """)
    suspend fun getRecentWithDetails(limit: Int): List<PurchaseWithDetails>

    @Query("""
        SELECT p.purchaseId, p.date, p.catalogId, p.quantity, p.pricePerUnit, p.totalAmount,
               pr.name as productName
        FROM purchases p
        JOIN products pr ON p.catalogId = pr.catalogId
        WHERE p.supplierId = :supplierId
          AND p.isBundleOp = 0
        ORDER BY p.date DESC
    """)
    suspend fun getPurchasesBySupplier(supplierId: String): List<PurchaseWithProductName>

    @Query("SELECT * FROM purchases ORDER BY date DESC")
    suspend fun getAllPurchases(): List<Purchase>

    @Query("SELECT SUM(quantity) FROM purchases WHERE catalogId = :catalogId")
    suspend fun getTotalQuantityByProduct(catalogId: String): Int

    @Query("SELECT SUM(totalAmount) FROM purchases WHERE catalogId = :catalogId")
    suspend fun getTotalAmountByProduct(catalogId: String): Double

    @Query("DELETE FROM purchases WHERE purchaseId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM purchases")
    suspend fun deleteAll()
}