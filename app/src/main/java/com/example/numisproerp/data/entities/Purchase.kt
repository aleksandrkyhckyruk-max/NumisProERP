package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Закупівля товару.
 *
 * `isBundleOp` = true позначає внутрішню операцію складу, створену при
 * збірці лоту в «Моя збірка» (закупівля = поява готової збірки на складі).
 * Така закупівля впливає на залишок (Product.currentStock рахує всі
 * purchases), але **виключається** з історії та звітів закупівель — це не
 * нова покупка з постачальника, а перетворення існуючих компонентів у
 * готовий лот. Див. також `Writeoff.isBundleOp`.
 */
@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey
    val purchaseId: String,
    val date: Long,
    val catalogId: String,
    val supplierId: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val additionalCosts: Double,
    val totalAmount: Double,
    val isBundleOp: Boolean = false
)