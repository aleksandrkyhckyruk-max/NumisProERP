package com.numisproerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.numisproerp.data.entities.OtherExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface OtherExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: OtherExpense)

    @Query("SELECT SUM(amount) FROM other_expenses")
    suspend fun getTotalSum(): Double?

    @Query("SELECT SUM(amount) FROM other_expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getSumByDateRange(startDate: Long, endDate: Long): Double?

    @Query("SELECT * FROM other_expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<OtherExpense>>

    @Query("SELECT * FROM other_expenses ORDER BY date DESC")
    suspend fun getAllExpensesSync(): List<OtherExpense>

    @Query("SELECT * FROM other_expenses ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentExpenses(limit: Int): List<OtherExpense>

    @Query("DELETE FROM other_expenses")
    suspend fun deleteAll()
}