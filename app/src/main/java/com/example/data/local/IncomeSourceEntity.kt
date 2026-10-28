package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_sources")
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val targetMonthlyAmount: Double = 0.0,
    val expectedDay: Int = 1,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
