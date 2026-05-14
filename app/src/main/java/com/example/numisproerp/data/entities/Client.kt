package com.numisproerp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey
    val clientId: String,
    val name: String,
    val phone: String = "",
    val telegram: String = "",
    val city: String = "",
    val notes: String = ""
)