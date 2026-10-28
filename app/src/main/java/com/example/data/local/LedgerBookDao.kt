package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerBookDao {
    @Query("SELECT * FROM ledger_books ORDER BY id ASC")
    fun getAllBooks(): Flow<List<LedgerBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: LedgerBookEntity): Long

    @Update
    suspend fun updateBook(book: LedgerBookEntity)

    @Delete
    suspend fun deleteBook(book: LedgerBookEntity)

    @Query("DELETE FROM transactions WHERE bookId = :bookId")
    suspend fun deleteTransactionsForBook(bookId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<LedgerBookEntity>)

    @Query("DELETE FROM ledger_books")
    suspend fun deleteAllBooks()
}
