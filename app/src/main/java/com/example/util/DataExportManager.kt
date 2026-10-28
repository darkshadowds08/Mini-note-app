package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.IncomeSourceEntity
import com.example.data.local.LedgerBookEntity
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DataExportManager {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    /**
     * Data model holding parsed backup content.
     */
    data class BackupData(
        val appName: String = "Mini Note Finance",
        val appVersion: String = "1.0",
        val exportDate: String = "",
        val exportDateArabic: String = "",
        val currency: String = "ج.س",
        val workspaces: List<LedgerBookEntity> = emptyList(),
        val incomeSources: List<IncomeSourceEntity> = emptyList(),
        val transactions: List<TransactionEntity> = emptyList(),
        val totalTransactionsCount: Int = transactions.size,
        val totalIncome: Double = 0.0,
        val totalExpense: Double = 0.0,
        val netBalance: Double = 0.0
    )

    /**
     * Parses a JSON backup string exported by this app.
     * Returns Result.success(BackupData) if valid, or Result.failure with error message.
     */
    fun parseJsonBackup(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString)

            val appName = root.optString("appName", "Mini Note Finance")
            val appVersion = root.optString("appVersion", "1.0")
            val exportDate = root.optString("exportDate", "")
            val exportDateArabic = root.optString("exportDateArabic", exportDate)
            val currency = root.optString("currency", "ج.س")

            // Workspaces / Books
            val workspaces = mutableListOf<LedgerBookEntity>()
            val booksArray = root.optJSONArray("workspaces")
            if (booksArray != null) {
                for (i in 0 until booksArray.length()) {
                    val bObj = booksArray.getJSONObject(i)
                    workspaces.add(
                        LedgerBookEntity(
                            id = bObj.optLong("id", (i + 1).toLong()),
                            name = bObj.optString("name", "واجهة ${i + 1}"),
                            iconName = bObj.optString("iconName", "wallet"),
                            colorHex = bObj.optString("colorHex", "#0F766E"),
                            createdAt = bObj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
            if (workspaces.isEmpty()) {
                workspaces.add(
                    LedgerBookEntity(
                        id = 1L,
                        name = "النوتة الشخصية",
                        iconName = "wallet",
                        colorHex = "#0F766E"
                    )
                )
            }

            // Income Sources
            val incomeSources = mutableListOf<IncomeSourceEntity>()
            val sourcesArray = root.optJSONArray("incomeSources")
            if (sourcesArray != null) {
                for (i in 0 until sourcesArray.length()) {
                    val sObj = sourcesArray.getJSONObject(i)
                    incomeSources.add(
                        IncomeSourceEntity(
                            id = sObj.optLong("id", (i + 1).toLong()),
                            name = sObj.optString("name", "مصدر ${i + 1}"),
                            type = sObj.optString("type", "OTHER_INCOME"),
                            targetMonthlyAmount = sObj.optDouble("targetMonthlyAmount", 0.0),
                            expectedDay = sObj.optInt("expectedDay", 1),
                            notes = sObj.optString("notes", "")
                        )
                    )
                }
            }

            // Transactions
            val transactions = mutableListOf<TransactionEntity>()
            val txArray = root.optJSONArray("transactions")
            val defaultBookId = workspaces.first().id
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val txObj = txArray.getJSONObject(i)
                    val sourceIdVal = if (txObj.has("sourceId") && !txObj.isNull("sourceId")) {
                        txObj.optLong("sourceId")
                    } else null

                    val bookIdVal = txObj.optLong("bookId", defaultBookId)

                    transactions.add(
                        TransactionEntity(
                            id = txObj.optLong("id", (i + 1).toLong()),
                            title = txObj.optString("title", "عملية بدون اسم"),
                            amount = txObj.optDouble("amount", 0.0),
                            isAddition = txObj.optBoolean("isAddition", true),
                            category = txObj.optString("category", "GENERAL"),
                            timestamp = txObj.optLong("timestamp", System.currentTimeMillis()),
                            note = txObj.optString("note", ""),
                            sourceId = sourceIdVal,
                            imageUris = txObj.optString("imageUris", ""),
                            bookId = bookIdVal
                        )
                    )
                }
            }

            val totalIncome = transactions.filter { it.isAddition }.sumOf { it.amount }
            val totalExpense = transactions.filter { !it.isAddition }.sumOf { it.amount }
            val netBalance = totalIncome - totalExpense

            Result.success(
                BackupData(
                    appName = appName,
                    appVersion = appVersion,
                    exportDate = exportDate,
                    exportDateArabic = exportDateArabic,
                    currency = currency,
                    workspaces = workspaces,
                    incomeSources = incomeSources,
                    transactions = transactions,
                    totalTransactionsCount = transactions.size,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netBalance = netBalance
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("ملف النسخة الاحتياطية غير صالح أو تالف: ${e.message}"))
        }
    }

    /**
     * Generates a complete JSON backup representing all user financial records,
     * books/workspaces, income sources, and transactions.
     */
    fun generateJsonBackup(
        books: List<LedgerBookEntity>,
        sources: List<IncomeSourceEntity>,
        transactions: List<TransactionEntity>,
        currency: String = "ج.س"
    ): String {
        val root = JSONObject()
        root.put("appName", "Mini Note Finance")
        root.put("appVersion", "1.0")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))
        root.put("exportDateArabic", SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date()))
        root.put("currency", currency)

        // Summary
        val summary = JSONObject()
        val totalIncome = transactions.filter { it.isAddition }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isAddition }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        summary.put("totalTransactions", transactions.size)
        summary.put("totalIncome", totalIncome)
        summary.put("totalExpense", totalExpense)
        summary.put("netBalance", netBalance)
        summary.put("workspacesCount", books.size)
        root.put("summary", summary)

        // Books / Workspaces
        val booksArray = JSONArray()
        for (book in books) {
            val bookObj = JSONObject()
            bookObj.put("id", book.id)
            bookObj.put("name", book.name)
            bookObj.put("iconName", book.iconName)
            bookObj.put("colorHex", book.colorHex)
            bookObj.put("createdAt", book.createdAt)
            booksArray.put(bookObj)
        }
        root.put("workspaces", booksArray)

        // Income Sources
        val sourcesArray = JSONArray()
        for (src in sources) {
            val srcObj = JSONObject()
            srcObj.put("id", src.id)
            srcObj.put("name", src.name)
            srcObj.put("type", src.type)
            srcObj.put("targetMonthlyAmount", src.targetMonthlyAmount)
            srcObj.put("expectedDay", src.expectedDay)
            srcObj.put("notes", src.notes)
            sourcesArray.put(srcObj)
        }
        root.put("incomeSources", sourcesArray)

        // Transactions
        val txArray = JSONArray()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (tx in transactions) {
            val txObj = JSONObject()
            txObj.put("id", tx.id)
            txObj.put("title", tx.title)
            txObj.put("amount", tx.amount)
            txObj.put("isAddition", tx.isAddition)
            txObj.put("typeArabic", if (tx.isAddition) "إيداع (+)" else "خصم (-)")
            txObj.put("category", tx.category)
            txObj.put("timestamp", tx.timestamp)
            txObj.put("dateFormatted", dateFormatter.format(Date(tx.timestamp)))
            txObj.put("note", tx.note)
            txObj.put("sourceId", tx.sourceId ?: JSONObject.NULL)
            txObj.put("bookId", tx.bookId)
            txObj.put("attachmentsCount", tx.getImageList().size)
            txArray.put(txObj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    /**
     * Generates a CSV file format with UTF-8 BOM so Excel opens Arabic letters without corruption.
     */
    fun generateCsvExport(
        transactions: List<TransactionEntity>,
        booksMap: Map<Long, String> = emptyMap(),
        currency: String = "ج.س"
    ): String {
        val sb = StringBuilder()
        // UTF-8 Byte Order Mark (BOM)
        sb.append('\uFEFF')
        // CSV Header in Arabic
        sb.append("المعرف,التاريخ والوقت,الواجهة,نوع العملية,البيان,المبلغ,العملة,التصنيف,الملاحظات\n")

        val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        for (tx in transactions) {
            val bookName = booksMap[tx.bookId] ?: "الرئيسية"
            val typeStr = if (tx.isAddition) "إيراد (+)" else "خصم (-)"
            val sign = if (tx.isAddition) "+" else "-"
            val formattedAmount = "$sign${tx.amount}"
            val sanitizedTitle = tx.title.replace("\"", "\"\"")
            val sanitizedNote = tx.note.replace("\"", "\"\"")
            val dateStr = dateFormatter.format(Date(tx.timestamp))

            sb.append("${tx.id},\"$dateStr\",\"$bookName\",\"$typeStr\",\"$sanitizedTitle\",$formattedAmount,\"$currency\",\"${tx.category}\",\"$sanitizedNote\"\n")
        }
        return sb.toString()
    }

    /**
     * Generates an attractive Telegram-formatted summary with Arabic emojis and Markdown.
     */
    fun generateTelegramSummary(
        transactions: List<TransactionEntity>,
        totalBalance: Double,
        totalIncome: Double,
        totalExpense: Double,
        currency: String = "ج.س",
        bookName: String? = null
    ): String {
        val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        val dateNow = dateFormatter.format(Date())

        val balanceSign = if (totalBalance >= 0) "🟢" else "🔴"

        return buildString {
            append("📊 *تقرير مصغرة النوتة المالية*\n")
            append("🗓 *التاريخ:* $dateNow\n")
            if (!bookName.isNullOrBlank()) {
                append("📁 *الواجهة:* $bookName\n")
            }
            append("━━━━━━━━━━━━━━━━━━━\n")
            append("$balanceSign *الرصيد الصافي:* ${numberFormat.format(totalBalance)} $currency\n")
            append("📈 *جملة الإيرادات:* +${numberFormat.format(totalIncome)} $currency\n")
            append("📉 *جملة الخصومات:* -${numberFormat.format(totalExpense)} $currency\n")
            append("🔢 *إجمالي العمليات:* ${transactions.size}\n")
            append("━━━━━━━━━━━━━━━━━━━\n")

            if (transactions.isNotEmpty()) {
                append("📋 *آخر العمليات المسجلة:*\n")
                for (tx in transactions.take(10)) {
                    val sign = if (tx.isAddition) "➕" else "➖"
                    val txDate = dateFormatter.format(Date(tx.timestamp))
                    append("$sign *${tx.title}*: ${numberFormat.format(tx.amount)} $currency ($txDate)\n")
                }
                if (transactions.size > 10) {
                    append("_(وعدد ${transactions.size - 10} عملية أخرى في ملف النسخة الاحتياطية المرفق)_\n")
                }
                append("━━━━━━━━━━━━━━━━━━━\n")
            }
            append("✨ تم التصدير عبر تطبيق مصغرة النوتة المالية (العملة: $currency)")
        }
    }

    /**
     * Saves string content to a cached file and returns the File handle.
     */
    fun saveContentToFile(context: Context, content: String, fileName: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Returns a shareable content:// URI using Android FileProvider.
     */
    fun getShareableUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Attempts direct share to Telegram application. Returns true if Telegram is available,
     * false otherwise.
     */
    fun shareToTelegramApp(
        context: Context,
        fileUri: Uri,
        mimeType: String,
        captionText: String
    ): Boolean {
        val telegramPackages = listOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.plus"
        )

        for (pkg in telegramPackages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, captionText)
                setPackage(pkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {
                // Continue to next package
            }
        }
        return false
    }

    /**
     * Shares file and text via Android standard system chooser.
     */
    fun shareViaSystemChooser(
        context: Context,
        fileUri: Uri,
        mimeType: String,
        title: String,
        summaryText: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, summaryText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Uploads the backup file directly to Telegram via Telegram Bot API (SendDocument).
     * Works without needing Telegram app installed on the device.
     */
    suspend fun uploadToTelegramBot(
        botToken: String,
        chatId: String,
        file: File,
        caption: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sanitizedToken = botToken.trim()
            val sanitizedChatId = chatId.trim()

            if (sanitizedToken.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال رمز البوت (Bot Token)"))
            }
            if (sanitizedChatId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال معرف المحادثة (Chat ID)"))
            }

            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", sanitizedChatId)
                .addFormDataPart("caption", caption.take(1024))
                .addFormDataPart("parse_mode", "Markdown")
                .addFormDataPart("document", file.name, requestFile)
                .build()

            val url = "https://api.telegram.org/bot$sanitizedToken/sendDocument"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                if (json.optBoolean("ok", false)) {
                    Result.success("تم إرسال النسخة الاحتياطية بنجاح إلى تليغرام!")
                } else {
                    val desc = json.optString("description", "فشل غير معروف من تليغرام")
                    Result.failure(Exception("خطأ من تليغرام: $desc"))
                }
            } else {
                val errorMsg = try {
                    JSONObject(responseBody).optString("description", response.message)
                } catch (_: Exception) {
                    response.message
                }
                Result.failure(Exception("فشل الإرسال (كود ${response.code}): $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
