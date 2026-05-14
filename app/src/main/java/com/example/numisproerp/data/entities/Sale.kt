package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey
    val saleId: String,
    val date: Long,
    val catalogId: String,
    val clientId: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val additionalCosts: Double,
    val netProfit: Double,
    val totalAmount: Double
)