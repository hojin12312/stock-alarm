package com.example.playground.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val name: String,
    val type: String,       // "MA" or "RSI"
    val status: String,     // "BUY" or "SELL"
    val market: String = "", // "US" or "KR"
    val detail: String,
    val createdAt: Long,
    val read: Boolean = false,
)
