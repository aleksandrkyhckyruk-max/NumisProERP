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
    val photoPath: String = ""  // НОВЕ ПОЛЕ: шлях до фото
)