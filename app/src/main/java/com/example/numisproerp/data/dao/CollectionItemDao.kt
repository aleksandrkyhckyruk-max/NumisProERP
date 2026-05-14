package com.numisproerp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.numisproerp.data.entities.CollectionItem
import kotlinx.coroutines.flow.Flow

/**
 * Колекційна позиція разом з «живими» даними про продажі та
 * реальний залишок.
 *
 * - [soldQuantity] — скільки вже продано з цієї колекційної позиції (сума
 *   `sales.quantity` для `catalogId = collectionId`).
 * - [writtenOffQuantity] — скільки списано (брак, втрата тощо).
 * - [remainingQuantity] = початкова `quantity` − `soldQuantity` − `writtenOffQuantity`.
 *   Не може бути від'ємним — в єдиних бизнес-сценаріях залишок >= 0.
 */
data class CollectionItemWithStock(
    @Embedded val item: CollectionItem,
    val soldQuantity: Int,
    val writtenOffQuantity: Int,
    val remainingQuantity: Int
)

@Dao
interface CollectionItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CollectionItem)

    @Update
    suspend fun update(item: CollectionItem)

    @Delete
    suspend fun delete(item: CollectionItem)

    @Query("SELECT * FROM collection_items ORDER BY dateAdded DESC")
    fun getAll(): Flow<List<CollectionItem>>

    /**
     * Те ж саме, що і [getAll], але для кожної позиції підраховує реальний
     * залишок на складі (початкова кількість − продане − списане). Розрахунок
     * відповідає формулі в [ProductDao.currentStock] для повної узгодженості.
     */
    @Query("""
        SELECT
            c.*,
            COALESCE((
                SELECT SUM(quantity) FROM sales WHERE catalogId = c.collectionId
            ), 0) AS soldQuantity,
            COALESCE((
                SELECT SUM(quantity) FROM writeoffs WHERE catalogId = c.collectionId
            ), 0) AS writtenOffQuantity,
            MAX(
                0,
                c.quantity
                    - COALESCE((SELECT SUM(quantity) FROM sales WHERE catalogId = c.collectionId), 0)
                    - COALESCE((SELECT SUM(quantity) FROM writeoffs WHERE catalogId = c.collectionId), 0)
            ) AS remainingQuantity
        FROM collection_items c
        ORDER BY c.dateAdded DESC
    """)
    fun getAllWithStock(): Flow<List<CollectionItemWithStock>>

    @Query("SELECT * FROM collection_items ORDER BY dateAdded DESC")
    suspend fun getAllSync(): List<CollectionItem>

    @Query("SELECT * FROM collection_items WHERE collectionId = :id")
    suspend fun getById(id: String): CollectionItem?

    @Query("SELECT SUM(estimatedValue * quantity) FROM collection_items")
    suspend fun getTotalEstimatedValue(): Double?

    @Query("SELECT COUNT(*) FROM collection_items")
    suspend fun getCount(): Int

    @Query("DELETE FROM collection_items")
    suspend fun deleteAll()
}
