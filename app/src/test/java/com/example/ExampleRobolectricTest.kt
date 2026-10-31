package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TransactionEntity
import com.example.model.MonthlyReportData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // fixed: matches the project SDK level
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mini Note Finance", appName)
    }

    @Test
    fun `monthly report calculates total revenues, deductions, and remaining balance accurately`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val timestampInOctober = cal.timeInMillis

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                title = "راتب شهري",
                amount = 15000.0,
                isAddition = true,
                timestamp = timestampInOctober
            ),
            TransactionEntity(
                id = 2,
                title = "مكافأة عمل إضافي",
                amount = 2500.0,
                isAddition = true,
                timestamp = timestampInOctober
            ),
            TransactionEntity(
                id = 3,
                title = "إيجار شقة",
                amount = 4000.0,
                isAddition = false,
                timestamp = timestampInOctober
            ),
            TransactionEntity(
                id = 4,
                title = "فواتير كهرباء وإنترنت",
                amount = 1200.0,
                isAddition = false,
                timestamp = timestampInOctober
            )
        )

        val report = MonthlyReportData.calculate(
            allTransactions = transactions,
            year = 2026,
            month = Calendar.OCTOBER
        )

        // 1. جملة الإيرادات (15000 + 2500 = 17500)
        assertEquals(17500.0, report.totalRevenues, 0.001)
        assertEquals(2, report.additionCount)

        // 2. الخصومات (4000 + 1200 = 5200)
        assertEquals(5200.0, report.totalDeductions, 0.001)
        assertEquals(2, report.deductionCount)

        // 3. المتبقي بعد الخصومات (17500 - 5200 = 12300)
        assertEquals(12300.0, report.remainingBalance, 0.001)
        assertTrue(report.isSurplus)

        // Verify shareable summary text contains key values with Sudanese Pound
        val shareableText = report.getShareableReportText("ج.س")
        assertTrue(shareableText.contains("جملة الإيرادات"))
        assertTrue(shareableText.contains("جملة الخصومات"))
        assertTrue(shareableText.contains("المتبقي بعد الخصومات"))
        assertTrue(shareableText.contains("17,500"))
        assertTrue(shareableText.contains("5,200"))
        assertTrue(shareableText.contains("12,300"))
        assertTrue(shareableText.contains("ج.س"))
    }

    @Test
    fun `monthly report detects deficit when deductions exceed revenues`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 5)
        }
        val timestamp = cal.timeInMillis

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                title = "إيراد جزئي",
                amount = 1000.0,
                isAddition = true,
                timestamp = timestamp
            ),
            TransactionEntity(
                id = 2,
                title = "صيانة طارئة",
                amount = 3500.0,
                isAddition = false,
                timestamp = timestamp
            )
        )

        val report = MonthlyReportData.calculate(
            allTransactions = transactions,
            year = 2026,
            month = Calendar.NOVEMBER
        )

        assertEquals(1000.0, report.totalRevenues, 0.001)
        assertEquals(3500.0, report.totalDeductions, 0.001)
        assertEquals(-2500.0, report.remainingBalance, 0.001)
        assertFalse(report.isSurplus)
    }

    @Test
    fun `default currency is Sudanese Pound and data export serializes correctly`() {
        // 1. Verify default currency in state
        val defaultUiState = com.example.ui.viewmodel.FinanceUiState()
        assertEquals("ج.س", defaultUiState.currency)

        // 2. Verify JSON export contains Sudanese Pound and transaction data
        val book = com.example.data.local.LedgerBookEntity(id = 1, name = "الرئيسية")
        val tx = TransactionEntity(
            id = 1,
            title = "تحويل بنكك",
            amount = 50000.0,
            isAddition = true,
            timestamp = System.currentTimeMillis()
        )

        val jsonStr = com.example.util.DataExportManager.generateJsonBackup(
            books = listOf(book),
            sources = emptyList(),
            transactions = listOf(tx),
            currency = "ج.س"
        )
        val json = org.json.JSONObject(jsonStr)
        assertEquals("ج.س", json.getString("currency"))
        assertEquals("Mini Note Finance", json.getString("appName"))
        assertEquals(50000.0, json.getJSONObject("summary").getDouble("netBalance"), 0.01)

        // 3. Verify CSV export starts with UTF-8 BOM and contains currency & header
        val csv = com.example.util.DataExportManager.generateCsvExport(
            transactions = listOf(tx),
            booksMap = mapOf(1L to "الرئيسية"),
            currency = "ج.س"
        )
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("تحويل بنكك"))
        assertTrue(csv.contains("ج.س"))

        // 4. Verify Telegram summary format
        val telegramText = com.example.util.DataExportManager.generateTelegramSummary(
            transactions = listOf(tx),
            totalBalance = 50000.0,
            totalIncome = 50000.0,
            totalExpense = 0.0,
            currency = "ج.س",
            bookName = "الرئيسية"
        )
        assertTrue(telegramText.contains("ج.س"))
        assertTrue(telegramText.contains("تحويل بنكك"))
        assertTrue(telegramText.contains("الرصيد الصافي"))
        assertTrue(telegramText.contains("50,000"))
    }

    @Test
    fun `local backup parseJsonBackup accurately reconstitutes workspaces, income sources, and transactions`() {
        val book1 = com.example.data.local.LedgerBookEntity(
            id = 10,
            name = "المتجر والنشاط التجاري",
            iconName = "business",
            colorHex = "#2563EB"
        )
        val source1 = com.example.data.local.IncomeSourceEntity(
            id = 5,
            name = "استشارات تقنية",
            type = "FREELANCE",
            targetMonthlyAmount = 75000.0,
            expectedDay = 25,
            notes = "عقود برمجية"
        )
        val tx1 = TransactionEntity(
            id = 101,
            title = "دفعة عقد استشارة",
            amount = 35000.0,
            isAddition = true,
            category = "FREELANCE",
            timestamp = 1700000000000L,
            note = "الدفعة الأولى",
            sourceId = 5L,
            bookId = 10L
        )
        val tx2 = TransactionEntity(
            id = 102,
            title = "إيجار مكتب عمل",
            amount = 12000.0,
            isAddition = false,
            category = "BILLS",
            timestamp = 1700003600000L,
            note = "إيجار شهري",
            bookId = 10L
        )

        // Generate JSON
        val exportedJson = com.example.util.DataExportManager.generateJsonBackup(
            books = listOf(book1),
            sources = listOf(source1),
            transactions = listOf(tx1, tx2),
            currency = "ج.س"
        )

        // Parse JSON
        val parseResult = com.example.util.DataExportManager.parseJsonBackup(exportedJson)
        assertTrue(parseResult.isSuccess)

        val backupData = parseResult.getOrThrow()
        assertEquals("Mini Note Finance", backupData.appName)
        assertEquals("ج.س", backupData.currency)
        assertEquals(1, backupData.workspaces.size)
        assertEquals("المتجر والنشاط التجاري", backupData.workspaces[0].name)
        assertEquals(1, backupData.incomeSources.size)
        assertEquals("استشارات تقنية", backupData.incomeSources[0].name)
        assertEquals(2, backupData.transactions.size)
        assertEquals(35000.0, backupData.totalIncome, 0.001)
        assertEquals(12000.0, backupData.totalExpense, 0.001)
        assertEquals(23000.0, backupData.netBalance, 0.001)
        assertEquals(101L, backupData.transactions[0].id)
        assertEquals(5L, backupData.transactions[0].sourceId)
        assertEquals(10L, backupData.transactions[0].bookId)
    }

    @Test
    fun `parseJsonBackup fails gracefully with Result failure for invalid JSON string`() {
        val invalidJson = "{ this is not a valid json: true "
        val result = com.example.util.DataExportManager.parseJsonBackup(invalidJson)
        assertTrue(result.isFailure)
    }
}
