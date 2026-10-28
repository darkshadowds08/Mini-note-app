package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val isAddition: Boolean,
    val category: String = "GENERAL",
    val timestamp: Long,
    val note: String = "",
    val sourceId: Long? = null,
    val imageUris: String = "",
    val bookId: Long = 1L
) {
    fun getImageList(): List<String> {
        return if (imageUris.isBlank()) emptyList()
        else imageUris.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
