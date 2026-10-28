package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.IncomeSourceEntity
import com.example.data.local.LedgerBookEntity
import com.example.data.local.TransactionEntity
import com.example.data.repository.FinanceRepository
import com.example.model.MonthlyReportData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FilterType {
    ALL,
    INCOME_ONLY,
    EXPENSE_ONLY
}

enum class DateFilter(val title: String) {
    ALL_TIME("كل الأوقات"),
    THIS_MONTH("هذا الشهر"),
    THIS_WEEK("هذا الأسبوع"),
    TODAY("اليوم")
}

enum class AppNavTab(val titleArabic: String) {
    MAIN_LEDGER("النوتة المالية"),
    REPORTS("واجهة التقارير")
}

data class FilterConfig(
    val query: String = "",
    val filterType: FilterType = FilterType.ALL,
    val dateFilter: DateFilter = DateFilter.ALL_TIME,
    val currency: String = "ج.س",
    val showMiniMap: Boolean = true,
    val currentTab: AppNavTab = AppNavTab.MAIN_LEDGER,
    val selectedSourceDetailId: Long? = null,
    val activeBookId: Long = 1L,
    val reportYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val reportMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val reportScopeAllBooks: Boolean = false,
    val isMonthStartBannerDismissed: Boolean = false,
    val showMonthStartReviewModal: Boolean = false
)

data class LedgerBookWithStats(
    val book: LedgerBookEntity,
    val balance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val transactionCount: Int
)

data class IncomeSourceWithStats(
    val source: IncomeSourceEntity,
    val totalCollectedThisMonth: Double,
    val totalCollectedAllTime: Double,
    val transactionCount: Int,
    val progressRatio: Float,
    val remainingToTarget: Double,
    val transactions: List<TransactionEntity>
)

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val incomeSources: List<IncomeSourceWithStats> = emptyList(),
    val ledgerBooks: List<LedgerBookWithStats> = emptyList(),
    val activeBook: LedgerBookEntity? = null,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalIncomeThisMonth: Double = 0.0,
    val totalTargetsThisMonth: Double = 0.0,
    val overallIncomeProgress: Float = 0f,
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val dateFilter: DateFilter = DateFilter.ALL_TIME,
    val currency: String = "ج.س",
    val showMiniMap: Boolean = true,
    val currentTab: AppNavTab = AppNavTab.MAIN_LEDGER,
    val selectedSourceDetailId: Long? = null,
    val isLoading: Boolean = false,
    val monthlyReport: MonthlyReportData = MonthlyReportData.calculate(
        emptyList(),
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH)
    ),
    val reportYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val reportMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val reportScopeAllBooks: Boolean = false,
    val isBeginningOfMonth: Boolean = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) <= 7,
    val isMonthStartBannerVisible: Boolean = true,
    val showMonthStartReviewModal: Boolean = false
)

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    private val _filterConfig = MutableStateFlow(FilterConfig())
    private var _lastDeletedItem: TransactionEntity? = null

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.allTransactions,
        repository.allIncomeSources,
        repository.allLedgerBooks,
        _filterConfig
    ) { transactions: List<TransactionEntity>, sources: List<IncomeSourceEntity>, books: List<LedgerBookEntity>, config: FilterConfig ->

        // Determine active book
        val effectiveActiveBook = books.firstOrNull { it.id == config.activeBookId }
            ?: books.firstOrNull()
            ?: LedgerBookEntity(id = 1, name = "النوتة الشخصية")

        // Compute stats for each book workspace
        val booksWithStats = books.map { b ->
            val bTransactions = transactions.filter { it.bookId == b.id }
            val bIncome = bTransactions.filter { it.isAddition }.sumOf { it.amount }
            val bExpense = bTransactions.filter { !it.isAddition }.sumOf { it.amount }
            LedgerBookWithStats(
                book = b,
                balance = bIncome - bExpense,
                totalIncome = bIncome,
                totalExpense = bExpense,
                transactionCount = bTransactions.size
            )
        }

        // Transactions belonging to the currently active book
        val activeBookTransactions = transactions.filter { it.bookId == effectiveActiveBook.id }

        val totalIncome = activeBookTransactions.filter { it.isAddition }.sumOf { it.amount }
        val totalExpense = activeBookTransactions.filter { !it.isAddition }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Compute stats for each income source
        val sourcesWithStats = sources.map { src ->
            val srcTransactions = activeBookTransactions.filter { it.sourceId == src.id }
            val thisMonthTransactions = srcTransactions.filter { it.timestamp >= startOfMonth && it.isAddition }
            val collectedThisMonth = thisMonthTransactions.sumOf { it.amount }
            val collectedAllTime = srcTransactions.filter { it.isAddition }.sumOf { it.amount }
            val ratio = if (src.targetMonthlyAmount > 0) {
                (collectedThisMonth / src.targetMonthlyAmount).toFloat().coerceIn(0f, 2f)
            } else 0f
            val remaining = (src.targetMonthlyAmount - collectedThisMonth).coerceAtLeast(0.0)

            IncomeSourceWithStats(
                source = src,
                totalCollectedThisMonth = collectedThisMonth,
                totalCollectedAllTime = collectedAllTime,
                transactionCount = srcTransactions.size,
                progressRatio = ratio,
                remainingToTarget = remaining,
                transactions = srcTransactions.sortedByDescending { it.timestamp }
            )
        }

        val totalIncomeThisMonth = activeBookTransactions
            .filter { it.isAddition && it.timestamp >= startOfMonth }
            .sumOf { it.amount }

        val totalTargetsThisMonth = sources.sumOf { it.targetMonthlyAmount }
        val overallProgress = if (totalTargetsThisMonth > 0) {
            (totalIncomeThisMonth / totalTargetsThisMonth).toFloat().coerceIn(0f, 2f)
        } else 0f

        val filtered = activeBookTransactions.filter { tx ->
            val matchesType = when (config.filterType) {
                FilterType.ALL -> true
                FilterType.INCOME_ONLY -> tx.isAddition
                FilterType.EXPENSE_ONLY -> !tx.isAddition
            }

            val matchesDate = when (config.dateFilter) {
                DateFilter.ALL_TIME -> true
                DateFilter.THIS_MONTH -> tx.timestamp >= startOfMonth
                DateFilter.THIS_WEEK -> tx.timestamp >= startOfWeek
                DateFilter.TODAY -> tx.timestamp >= startOfToday
            }

            val matchesSearch = if (config.query.isBlank()) {
                true
            } else {
                tx.title.contains(config.query, ignoreCase = true) ||
                        tx.note.contains(config.query, ignoreCase = true) ||
                        tx.amount.toString().contains(config.query)
            }

            matchesType && matchesDate && matchesSearch
        }

        val nowCal = Calendar.getInstance()
        val isBeginningOfMonth = nowCal.get(Calendar.DAY_OF_MONTH) <= 7
        val isBannerVisible = !config.isMonthStartBannerDismissed

        val reportTransactions = if (config.reportScopeAllBooks) {
            transactions
        } else {
            activeBookTransactions
        }

        val monthlyReport = MonthlyReportData.calculate(
            allTransactions = reportTransactions,
            year = config.reportYear,
            month = config.reportMonth
        )

        FinanceUiState(
            transactions = activeBookTransactions,
            filteredTransactions = filtered,
            incomeSources = sourcesWithStats,
            ledgerBooks = booksWithStats,
            activeBook = effectiveActiveBook,
            totalBalance = balance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalIncomeThisMonth = totalIncomeThisMonth,
            totalTargetsThisMonth = totalTargetsThisMonth,
            overallIncomeProgress = overallProgress,
            searchQuery = config.query,
            filterType = config.filterType,
            dateFilter = config.dateFilter,
            currency = "ج.س",
            showMiniMap = config.showMiniMap,
            currentTab = config.currentTab,
            selectedSourceDetailId = config.selectedSourceDetailId,
            isLoading = false,
            monthlyReport = monthlyReport,
            reportYear = config.reportYear,
            reportMonth = config.reportMonth,
            reportScopeAllBooks = config.reportScopeAllBooks,
            isBeginningOfMonth = isBeginningOfMonth,
            isMonthStartBannerVisible = isBannerVisible,
            showMonthStartReviewModal = config.showMonthStartReviewModal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _filterConfig.update { it.copy(query = query) }
    }

    fun onFilterTypeChanged(filter: FilterType) {
        _filterConfig.update { it.copy(filterType = filter) }
    }

    fun onDateFilterChanged(filter: DateFilter) {
        _filterConfig.update { it.copy(dateFilter = filter) }
    }

    fun onCurrencyChanged(newCurrency: String = "ج.س") {
        // Currency is permanently fixed to Sudanese Pound (ج.س) per user requirement
        _filterConfig.update { it.copy(currency = "ج.س") }
    }

    fun toggleMiniMap() {
        _filterConfig.update { it.copy(showMiniMap = !it.showMiniMap) }
    }

    fun setCurrentTab(tab: AppNavTab) {
        _filterConfig.update { it.copy(currentTab = tab, selectedSourceDetailId = null) }
    }

    fun selectSourceDetail(sourceId: Long?) {
        _filterConfig.update { it.copy(selectedSourceDetailId = sourceId) }
    }

    fun setActiveBook(bookId: Long) {
        _filterConfig.update { it.copy(activeBookId = bookId, selectedSourceDetailId = null) }
    }

    fun selectReportMonth(year: Int, month: Int) {
        _filterConfig.update { it.copy(reportYear = year, reportMonth = month) }
    }

    fun goToPreviousReportMonth() {
        _filterConfig.update { current ->
            var newMonth = current.reportMonth - 1
            var newYear = current.reportYear
            if (newMonth < 0) {
                newMonth = 11
                newYear -= 1
            }
            current.copy(reportYear = newYear, reportMonth = newMonth)
        }
    }

    fun goToNextReportMonth() {
        _filterConfig.update { current ->
            var newMonth = current.reportMonth + 1
            var newYear = current.reportYear
            if (newMonth > 11) {
                newMonth = 0
                newYear += 1
            }
            current.copy(reportYear = newYear, reportMonth = newMonth)
        }
    }

    fun goToCurrentReportMonth() {
        val cal = Calendar.getInstance()
        _filterConfig.update {
            it.copy(
                reportYear = cal.get(Calendar.YEAR),
                reportMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun toggleReportScope() {
        _filterConfig.update { it.copy(reportScopeAllBooks = !it.reportScopeAllBooks) }
    }

    fun dismissMonthStartBanner() {
        _filterConfig.update { it.copy(isMonthStartBannerDismissed = true) }
    }

    fun setMonthStartReviewModal(visible: Boolean) {
        _filterConfig.update { it.copy(showMonthStartReviewModal = visible) }
    }

    fun createLedgerBook(name: String, iconName: String = "wallet", colorHex: String = "#0F766E") {
        viewModelScope.launch {
            val book = LedgerBookEntity(
                name = name.trim(),
                iconName = iconName,
                colorHex = colorHex
            )
            val newId = repository.insertLedgerBook(book)
            _filterConfig.update { it.copy(activeBookId = newId) }
        }
    }

    fun deleteLedgerBook(book: LedgerBookEntity) {
        viewModelScope.launch {
            repository.deleteLedgerBook(book)
            // If the deleted book was active, switch to another book
            _filterConfig.update { current ->
                if (current.activeBookId == book.id) {
                    current.copy(activeBookId = 1L)
                } else current
            }
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        isAddition: Boolean,
        category: String = "GENERAL",
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        sourceId: Long? = null,
        imageUris: String = ""
    ) {
        viewModelScope.launch {
            val currentBookId = _filterConfig.value.activeBookId
            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                isAddition = isAddition,
                category = category,
                timestamp = timestamp,
                note = note.trim(),
                sourceId = sourceId,
                imageUris = imageUris,
                bookId = currentBookId
            )
            repository.insert(entity)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        _lastDeletedItem = transaction
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun undoDelete() {
        _lastDeletedItem?.let { item ->
            viewModelScope.launch {
                repository.insert(item.copy(id = 0))
                _lastDeletedItem = null
            }
        }
    }

    fun addIncomeSource(
        name: String,
        type: String,
        targetMonthlyAmount: Double,
        expectedDay: Int,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val entity = IncomeSourceEntity(
                name = name.trim(),
                type = type,
                targetMonthlyAmount = targetMonthlyAmount,
                expectedDay = expectedDay,
                notes = notes.trim()
            )
            repository.insertIncomeSource(entity)
        }
    }

    fun updateIncomeSource(source: IncomeSourceEntity) {
        viewModelScope.launch {
            repository.updateIncomeSource(source)
        }
    }

    fun deleteIncomeSource(source: IncomeSourceEntity) {
        viewModelScope.launch {
            repository.deleteIncomeSource(source)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun getExportJson(): String {
        val state = uiState.value
        val books = state.ledgerBooks.map { it.book }
        val sources = state.incomeSources.map { it.source }
        return com.example.util.DataExportManager.generateJsonBackup(
            books = books,
            sources = sources,
            transactions = state.transactions,
            currency = state.currency
        )
    }

    fun getExportCsv(): String {
        val state = uiState.value
        val booksMap = state.ledgerBooks.associate { it.book.id to it.book.name }
        return com.example.util.DataExportManager.generateCsvExport(
            transactions = state.transactions,
            booksMap = booksMap,
            currency = state.currency
        )
    }

    fun getTelegramSummary(): String {
        val state = uiState.value
        return com.example.util.DataExportManager.generateTelegramSummary(
            transactions = state.filteredTransactions,
            totalBalance = state.totalBalance,
            totalIncome = state.totalIncome,
            totalExpense = state.totalExpense,
            currency = state.currency,
            bookName = state.activeBook?.name
        )
    }

    fun writeExportToUri(context: android.content.Context, uri: android.net.Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun uploadBackupToTelegramBot(
        context: android.content.Context,
        botToken: String,
        chatId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val json = getExportJson()
            val fileName = "mini_note_backup_${System.currentTimeMillis()}.json"
            val file = com.example.util.DataExportManager.saveContentToFile(context, json, fileName)
            val caption = getTelegramSummary()
            val result = com.example.util.DataExportManager.uploadToTelegramBot(
                botToken = botToken,
                chatId = chatId,
                file = file,
                caption = caption
            )
            result.onSuccess { msg ->
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.message ?: "حدث خطأ أثناء الرفع إلى تليغرام")
            }
        }
    }

    fun readBackupFromUri(context: android.content.Context, uri: android.net.Uri): Result<com.example.util.DataExportManager.BackupData> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return Result.failure(Exception("تعذر قراءة محتوى الملف المحدد"))
            com.example.util.DataExportManager.parseJsonBackup(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun restoreBackup(
        backupData: com.example.util.DataExportManager.BackupData,
        replaceAll: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.restoreBackup(backupData, replaceAll)
                val firstBookId = backupData.workspaces.firstOrNull()?.id ?: 1L
                _filterConfig.update { it.copy(activeBookId = firstBookId) }
                val count = backupData.totalTransactionsCount
                val mode = if (replaceAll) "استبدال كامل" else "دمج وإضافة"
                onResult(true, "تم استرداد $count عملية مالية بنجاح ($mode)!")
            } catch (e: Exception) {
                onResult(false, "فشل استرداد البيانات: ${e.message}")
            }
        }
    }


    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(
                        application.applicationContext,
                        kotlinx.coroutines.GlobalScope
                    )
                    val repo = FinanceRepository(
                        db.transactionDao(),
                        db.incomeSourceDao(),
                        db.ledgerBookDao()
                    )
                    return FinanceViewModel(application, repo) as T
                }
            }
    }
}
