package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Запис про списання товару зі складу (некондиція, брак, інше).
 *
 * `pricePerUnit` — це **закупочна** ціна на момент списання (середня по
 * залишку), а `totalAmount = quantity * pricePerUnit`. Списання впливає на
 * залишки на складі (мінусує кількість) та враховується як витрата у звітах.
 *
 * `isBundleOp` = true означає, що це **внутрішнє** списання компонента при
 * збірці лоту в «Моя збірка», а не реальна втрата товару. Такий запис
 * впливає на залишок (компонент пропадає зі складу), але **виключається** з
 * історії списань та зі звітів про витрати — це не збиток, а перетворення
 * у готовий лот.
 */
@Entity(tableName = "writeoffs")
data class Writeoff(
    @PrimaryKey
    val writeoffId: String,
    val date: Long,
    val catalogId: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val totalAmount: Double,
    val reason: String,
    val comment: String,
    val isBundleOp: Boolean = false
)
