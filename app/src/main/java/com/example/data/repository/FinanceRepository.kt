package com.example.data.repository

import com.example.data.local.IncomeSourceDao
import com.example.data.local.IncomeSourceEntity
import com.example.data.local.LedgerBookDao
import com.example.data.local.LedgerBookEntity
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val incomeSourceDao: IncomeSourceDao,
    private val ledgerBookDao: LedgerBookDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allIncomeSources: Flow<List<IncomeSourceEntity>> = incomeSourceDao.getAllIncomeSources()
    val allLedgerBooks: Flow<List<LedgerBookEntity>> = ledgerBookDao.getAllBooks()

    suspend fun insert(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun clearAll() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun insertIncomeSource(source: IncomeSourceEntity): Long {
        return incomeSourceDao.insertIncomeSource(source)
    }

    suspend fun updateIncomeSource(source: IncomeSourceEntity) {
        incomeSourceDao.updateIncomeSource(source)
    }

    suspend fun deleteIncomeSource(source: IncomeSourceEntity) {
        incomeSourceDao.deleteIncomeSource(source)
    }

    suspend fun insertLedgerBook(book: LedgerBookEntity): Long {
        return ledgerBookDao.insertBook(book)
    }

    suspend fun updateLedgerBook(book: LedgerBookEntity) {
        ledgerBookDao.updateBook(book)
    }

    suspend fun deleteLedgerBook(book: LedgerBookEntity) {
        ledgerBookDao.deleteTransactionsForBook(book.id)
        ledgerBookDao.deleteBook(book)
    }

    suspend fun restoreBackup(
        backupData: com.example.util.DataExportManager.BackupData,
        replaceAll: Boolean
    ) {
        if (replaceAll) {
            transactionDao.deleteAllTransactions()
            incomeSourceDao.deleteAllIncomeSources()
            ledgerBookDao.deleteAllBooks()

            val books = if (backupData.workspaces.isNotEmpty()) {
                backupData.workspaces
            } else {
                listOf(LedgerBookEntity(id = 1, name = "النوتة الشخصية"))
            }
            ledgerBookDao.insertBooks(books)

            if (backupData.incomeSources.isNotEmpty()) {
                incomeSourceDao.insertIncomeSources(backupData.incomeSources)
            }
            if (backupData.transactions.isNotEmpty()) {
                transactionDao.insertTransactions(backupData.transactions)
            }
        } else {
            val books = if (backupData.workspaces.isNotEmpty()) {
                backupData.workspaces
            } else {
                listOf(LedgerBookEntity(id = 1, name = "النوتة الشخصية"))
            }
            ledgerBookDao.insertBooks(books)

            if (backupData.incomeSources.isNotEmpty()) {
                incomeSourceDao.insertIncomeSources(backupData.incomeSources)
            }

            // Insert transactions with id = 0 to assign new auto-generated IDs without overwriting existing
            val txToInsert = backupData.transactions.map { tx ->
                tx.copy(id = 0)
            }
            if (txToInsert.isNotEmpty()) {
                transactionDao.insertTransactions(txToInsert)
            }
        }
    }
}

