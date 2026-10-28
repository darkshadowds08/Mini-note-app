package com.example.model

import com.example.data.local.TransactionEntity
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class MonthlyReportData(
    val year: Int,
    val month: Int, // 0-based Calendar month (0 = January)
    val monthNameArabic: String,
    val totalRevenues: Double,       // جملة الإيرادات
    val totalDeductions: Double,     // الخصومات
    val remainingBalance: Double,    // المتبقي بعد الخصومات
    val totalTransactionsCount: Int,
    val additionCount: Int,
    val deductionCount: Int,
    val transactions: List<TransactionEntity>,
    val additionTransactions: List<TransactionEntity>,
    val deductionTransactions: List<TransactionEntity>,
    val topAddition: TransactionEntity?,
    val topDeduction: TransactionEntity?,
    val deductionRatePercent: Int, // % of revenue spent
    val isSurplus: Boolean
) {
    fun getShareableReportText(currency: String): String {
        val numberFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }

        return buildString {
            appendLine("📊 تقرير شهر $monthNameArabic")
            appendLine("══════════════════════════")
            appendLine("🟢 جملة الإيرادات: +${numberFormatter.format(totalRevenues)} $currency ($additionCount عملية)")
            appendLine("🔴 جملة الخصومات: -${numberFormatter.format(totalDeductions)} $currency ($deductionCount عملية)")
            val status = if (remainingBalance >= 0) "فائض مالي ممتاز 🟢" else "عجز مالي يجب ترشيده 🔴"
            appendLine("💎 المتبقي بعد الخصومات: ${numberFormatter.format(remainingBalance)} $currency ($status)")
            appendLine("──────────────────────────")
            appendLine("📋 إجمالي العمليات: $totalTransactionsCount حركة")
            if (topAddition != null) {
                appendLine("⭐ أعلى إيراد: ${topAddition.title} (+${numberFormatter.format(topAddition.amount)} $currency)")
            }
            if (topDeduction != null) {
                appendLine("⚠️ أعلى خصم: ${topDeduction.title} (-${numberFormatter.format(topDeduction.amount)} $currency)")
            }
            if (transactions.isNotEmpty()) {
                appendLine("\n📝 مراجعة المعاملات:")
                transactions.take(20).forEach { tx ->
                    val sign = if (tx.isAddition) "(+)" else "(-)"
                    appendLine("• ${tx.title}: $sign${numberFormatter.format(tx.amount)} $currency")
                }
                if (transactions.size > 20) {
                    appendLine("... وغيرها ${transactions.size - 20} حركة")
                }
            }
            appendLine("══════════════════════════")
            appendLine("تم إعداد هذا التقرير عبر مصغرة النوتة المالية")
        }
    }

    companion object {
        private val ARABIC_MONTH_NAMES = arrayOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )

        fun getMonthNameArabic(year: Int, month: Int): String {
            val safeMonth = month.coerceIn(0, 11)
            return "${ARABIC_MONTH_NAMES[safeMonth]} $year"
        }

        fun calculate(
            allTransactions: List<TransactionEntity>,
            year: Int,
            month: Int
        ): MonthlyReportData {
            val cal = Calendar.getInstance()
            
            // Filter transactions that fall strictly within the given year and month
            val monthTransactions = allTransactions.filter { tx ->
                cal.timeInMillis = tx.timestamp
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }.sortedByDescending { it.timestamp }

            val additions = monthTransactions.filter { it.isAddition }
            val deductions = monthTransactions.filter { !it.isAddition }

            val totalRevenues = additions.sumOf { it.amount }
            val totalDeductions = deductions.sumOf { it.amount }
            val remainingBalance = totalRevenues - totalDeductions

            val topAddition = additions.maxByOrNull { it.amount }
            val topDeduction = deductions.maxByOrNull { it.amount }

            val deductionRate = if (totalRevenues > 0) {
                ((totalDeductions / totalRevenues) * 100).toInt().coerceIn(0, 999)
            } else if (totalDeductions > 0) {
                100
            } else {
                0
            }

            return MonthlyReportData(
                year = year,
                month = month,
                monthNameArabic = getMonthNameArabic(year, month),
                totalRevenues = totalRevenues,
                totalDeductions = totalDeductions,
                remainingBalance = remainingBalance,
                totalTransactionsCount = monthTransactions.size,
                additionCount = additions.size,
                deductionCount = deductions.size,
                transactions = monthTransactions,
                additionTransactions = additions,
                deductionTransactions = deductions,
                topAddition = topAddition,
                topDeduction = topDeduction,
                deductionRatePercent = deductionRate,
                isSurplus = remainingBalance >= 0
            )
        }
    }
}
