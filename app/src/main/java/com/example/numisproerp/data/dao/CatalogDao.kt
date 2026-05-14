package com.numisproerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.numisproerp.data.entities.CatalogItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogItem>)

    @Query("DELETE FROM catalog_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM catalog_items ORDER BY name")
    fun getAllItems(): Flow<List<CatalogItem>>

    @Query("SELECT * FROM catalog_items WHERE category = :category ORDER BY name")
    fun getItemsByCategory(category: String): Flow<List<CatalogItem>>

    @Query("SELECT DISTINCT category FROM catalog_items ORDER BY category")
    suspend fun getDistinctCategories(): List<String>

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    suspend fun getItemById(id: String): CatalogItem?

    @Query("SELECT COUNT(*) FROM catalog_items")
    suspend fun getCount(): Int

    @Query("SELECT * FROM catalog_items WHERE name = :name LIMIT 1")
    suspend fun getItemByName(name: String): CatalogItem?

    @Query("SELECT * FROM catalog_items")
    suspend fun getAllItemsSync(): List<CatalogItem>
}