package com.numisproerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.numisproerp.data.entities.Bundle
import com.numisproerp.data.entities.BundleComponent
import kotlinx.coroutines.flow.Flow

/**
 * Сума продажів та прибуток по збірці.
 *
 * `soldCount` — скільки одиниць збірки вже продано (sales з catalogId =
 * "BUNDLE_<bundleId>"); `totalRevenue` — сума всіх продажів; `profit` =
 * `totalRevenue − (soldCount × totalCost)`.
 */
data class BundleWithSales(
    val bundleId: String,
    val name: String,
    val assembledDate: Long,
    val totalCost: Double,
    val suggestedPrice: Double,
    val photoPath: String,
    val comment: String,
    val soldCount: Int,
    val totalRevenue: Double
)

@Dao
interface BundleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundle(bundle: Bundle)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<BundleComponent>)

    @Query("SELECT * FROM bundles WHERE bundleId = :id")
    suspend fun getById(id: String): Bundle?

    @Query("SELECT * FROM bundle_components WHERE bundleId = :bundleId")
    suspend fun getComponentsForBundle(bundleId: String): List<BundleComponent>

    @Query("DELETE FROM bundles WHERE bundleId = :id")
    suspend fun deleteBundle(id: String)

    @Query("DELETE FROM bundles")
    suspend fun deleteAllBundles()

    @Query("DELETE FROM bundle_components")
    suspend fun deleteAllComponents()

    /**
     * Список збірок із агрегованими даними по продажах. Кількість одиниць
     * збірки на складі = (1 закупівля на момент створення) − sales − writeoffs.
     */
    @Transaction
    @Query("""
        SELECT
            b.bundleId,
            b.name,
            b.assembledDate,
            b.totalCost,
            b.suggestedPrice,
            b.photoPath,
            b.comment,
            COALESCE((
                SELECT SUM(quantity)
                FROM sales
                WHERE catalogId = 'BUNDLE_' || b.bundleId
            ), 0) as soldCount,
            COALESCE((
                SELECT SUM(totalAmount)
                FROM sales
                WHERE catalogId = 'BUNDLE_' || b.bundleId
            ), 0.0) as totalRevenue
        FROM bundles b
        ORDER BY b.assembledDate DESC
    """)
    fun getAllWithSales(): Flow<List<BundleWithSales>>
}
