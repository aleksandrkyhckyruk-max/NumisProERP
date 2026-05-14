package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "other_expenses")
data class OtherExpense(
    @PrimaryKey
    val expenseId: String,
    val date: Long,
    val category: String,
    val amount: Double,
    val comment: String = ""
)