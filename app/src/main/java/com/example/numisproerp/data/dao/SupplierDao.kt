package com.numisproerp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.numisproerp.data.entities.Supplier
import kotlinx.coroutines.flow.Flow

data class SupplierForSelection(
    val supplierId: String,
    val name: String
)

data class SupplierWithBalance(
    val supplierId: String,
    val name: String,
    val contact: String,
    val type: String,
    val comment: String,
    val totalSpent: Double
)

@Dao
interface SupplierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: Supplier)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<Supplier>)

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)

    @Query("DELETE FROM suppliers WHERE supplierId = :supplierId")
    suspend fun deleteById(supplierId: String)

    @Query("SELECT * FROM suppliers ORDER BY name")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers ORDER BY name")
    suspend fun getAllSuppliersSync(): List<Supplier>

    @Query("SELECT supplierId, name FROM suppliers ORDER BY name")
    suspend fun getSuppliersForSelection(): List<SupplierForSelection>

    @Query("""
        SELECT 
            s.supplierId,
            s.name,
            s.contact,
            s.type,
            s.comment,
            COALESCE((
                SELECT SUM(totalAmount) FROM purchases WHERE supplierId = s.supplierId
            ), 0.0) as totalSpent
        FROM suppliers s
        ORDER BY s.name
    """)
    fun getSuppliersWithBalance(): Flow<List<SupplierWithBalance>>

    @Query("SELECT * FROM suppliers WHERE supplierId = :supplierId")
    suspend fun getSupplierById(supplierId: String): Supplier?

    @Query("DELETE FROM suppliers")
    suspend fun deleteAll()
}