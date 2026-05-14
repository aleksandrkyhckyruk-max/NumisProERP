package com.numisproerp.data.dao

data class ProductStockInfo(
    val catalogId: String,
    val name: String,
    val currentStock: Int,
    val avgPurchasePrice: Double
)