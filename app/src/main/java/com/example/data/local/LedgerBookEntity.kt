package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledger_books")
data class LedgerBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "wallet",
    val colorHex: String = "#0F766E",
    val createdAt: Long = System.currentTimeMillis()
)
