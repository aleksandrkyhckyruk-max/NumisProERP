package com.numisproerp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.numisproerp.data.entities.Client
import kotlinx.coroutines.flow.Flow

data class ClientForSelection(
    val clientId: String,
    val name: String
)

data class ClientWithBalance(
    val clientId: String,
    val name: String,
    val phone: String,
    val telegram: String,
    val city: String,
    val notes: String,
    val totalSpent: Double
)

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<Client>)

    @Update
    suspend fun update(client: Client)

    @Delete
    suspend fun delete(client: Client)

    @Query("DELETE FROM clients WHERE clientId = :clientId")
    suspend fun deleteById(clientId: String)

    @Query("SELECT * FROM clients ORDER BY name")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name")
    suspend fun getAllClientsSync(): List<Client>

    @Query("SELECT clientId, name FROM clients ORDER BY name")
    suspend fun getClientsForSelection(): List<ClientForSelection>

    @Query("""
        SELECT 
            c.clientId,
            c.name,
            c.phone,
            c.telegram,
            c.city,
            c.notes,
            COALESCE((
                SELECT SUM(totalAmount) FROM sales WHERE clientId = c.clientId
            ), 0.0) as totalSpent
        FROM clients c
        ORDER BY c.name
    """)
    fun getClientsWithBalance(): Flow<List<ClientWithBalance>>

    @Query("SELECT * FROM clients WHERE clientId = :clientId")
    suspend fun getClientById(clientId: String): Client?

    @Query("DELETE FROM clients")
    suspend fun deleteAll()
}