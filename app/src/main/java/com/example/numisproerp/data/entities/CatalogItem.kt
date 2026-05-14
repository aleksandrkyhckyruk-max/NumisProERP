package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_items")
data class CatalogItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val series: String,
    val dateIntroduced: String,
    val material: String,
    val denomination: String,
    val diameter: String,
    val weight: String,
    val mintage: String,
    val category: String,
    val quality: String,
    val artist: String,
    val sculptor: String,
    val websiteUrl: String,
    val imageUrlFront: String,
    val imageUrlBack: String
)