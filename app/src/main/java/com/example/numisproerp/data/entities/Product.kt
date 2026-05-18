package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val catalogId: String,
    val name: String,
    val series: String = "",
    val material: String = "",
    val nominal: String = "",
    val category: String = "",
    val quality: String = "",
    val diameter: String = "",
    val weight: String = "",
    val mintageAnnounced: String = "",
    val mintageActual: String = "",
    val issueDate: String = "",
    val artist: String = "",
    val sculptor: String = "",
    // Основне фото товару (аверс/лицьовий бік) — файл в внутрішній пам'яті або URL.
    val photoPath: String = "",
    // Додаткове фото (реверс/зворот). Опціональне — використовується при ручному додаванні товару в каталог.
    val photoPathBack: String = "",
    // Орієнтовна вартість (для ручно доданих товарів, які ще не мають вхідної ціни).
    val estimatedValue: Double = 0.0,
    // Довільний опис для ручно доданих товарів.
    val description: String = "",
    // Позначає, що товар додано вручну (не імпортовано з Excel-каталогу).
    val isManual: Boolean = false
)