package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.DataExportManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportDataDialog(
    viewModel: FinanceViewModel,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
    }

    // Local Restore state
    var parsedBackup by remember { mutableStateOf<DataExportManager.BackupData?>(null) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }
    var restoreSuccessMessage by remember { mutableStateOf<String?>(null) }
    var replaceExisting by remember { mutableStateOf(false) } // false = merge, true = replace all
    var isRestoring by remember { mutableStateOf(false) }
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }

    val restoreDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            restoreErrorMessage = null
            restoreSuccessMessage = null
            val result = viewModel.readBackupFromUri(context, uri)
            result.onSuccess { data ->
                parsedBackup = data
            }.onFailure { err ->
                parsedBackup = null
                restoreErrorMessage = err.message ?: "تعذر قراءة ملف النسخة الاحتياطية"
            }
        }
    }

    // SharedPreferences for Telegram Bot credentials
    val prefs = remember { context.getSharedPreferences("telegram_backup_prefs", Context.MODE_PRIVATE) }
    var botToken by remember { mutableStateOf(prefs.getString("bot_token", "").orEmpty()) }
    var chatId by remember { mutableStateOf(prefs.getString("chat_id", "").orEmpty()) }
    var showHelpGuide by remember { mutableStateOf(false) }

    var isUploadingBot by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var isUploadSuccess by remember { mutableStateOf(false) }

    // Pending content for SAF document creation
    var pendingExportType by remember { mutableStateOf<String?>(null) } // "json" or "csv"

    val dateStamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) }

    // SAF Document Creators
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val json = viewModel.getExportJson()
            val success = viewModel.writeExportToUri(context, uri, json)
            if (success) {
                Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "فشل حفظ الملف", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val csv = viewModel.getExportCsv()
            val success = viewModel.writeExportToUri(context, uri, csv)
            if (success) {
                Toast.makeText(context, "تم حفظ ملف CSV بنجاح!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "فشل حفظ الملف", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "النسخ الاحتياطي والاسترداد",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تصدير، استرداد محلي، أو أونلاين عبر تليغرام",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs: Local Export vs Local Restore vs Telegram
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير محلي", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استرداد محلي", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("عبر تليغرام", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                if (selectedTab == 0) {
                    // =================== LOCAL EXPORT TAB ===================
                    Text(
                        text = "خيارات التصدير المحلي:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 1: Full JSON Backup
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "نسخة احتياطية كاملة (ملف JSON)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تتضمن الحركات، الواجهات، الإيرادات والعملة (ج.س). صالحة للحفظ والأرشفة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        createJsonLauncher.launch("mini_note_finance_backup_$dateStamp.json")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ بالجهاز", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val json = viewModel.getExportJson()
                                        val file = DataExportManager.saveContentToFile(
                                            context,
                                            json,
                                            "mini_note_backup_$dateStamp.json"
                                        )
                                        val uri = DataExportManager.getShareableUri(context, file)
                                        val summary = viewModel.getTelegramSummary()
                                        DataExportManager.shareViaSystemChooser(
                                            context = context,
                                            fileUri = uri,
                                            mimeType = "application/json",
                                            title = "مشاركة نسخة احتياطية للنوتة المالية",
                                            summaryText = summary
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مشاركة", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: Excel / CSV Export
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جدول إكسل للحركات (ملف CSV)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يفتح في برامج Excel و Sheets مباشرة مع ترميز UTF-8 سليم للغة العربية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        createCsvLauncher.launch("mini_note_transactions_$dateStamp.csv")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ كجدول", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val csv = viewModel.getExportCsv()
                                        val file = DataExportManager.saveContentToFile(
                                            context,
                                            csv,
                                            "mini_note_transactions_$dateStamp.csv"
                                        )
                                        val uri = DataExportManager.getShareableUri(context, file)
                                        val summary = viewModel.getTelegramSummary()
                                        DataExportManager.shareViaSystemChooser(
                                            context = context,
                                            fileUri = uri,
                                            mimeType = "text/csv",
                                            title = "مشاركة جدول العمليات المالية",
                                            summaryText = summary
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مشاركة", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                } else if (selectedTab == 1) {
                    // =================== LOCAL RESTORE TAB ===================
                    Text(
                        text = "استرداد البيانات من نسخة محلية:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Step 1: Pick Backup File
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اختيار ملف النسخة الاحتياطية (JSON)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "اختر ملف النسخة الاحتياطية (.json) الذي قمت بحفظه مسبقاً على هاتفك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    restoreDocLauncher.launch(
                                        arrayOf(
                                            "application/json",
                                            "text/plain",
                                            "application/octet-stream",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تصفح واختيار ملف النسخة من الهاتف")
                            }
                        }
                    }

                    // Error feedback
                    if (restoreErrorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = restoreErrorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Success feedback
                    if (restoreSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = restoreSuccessMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Step 2: Show parsed details & restore strategy
                    if (parsedBackup != null) {
                        val backup = parsedBackup!!
                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تم فحص النسخة بنجاح",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                    Text(
                                        text = backup.currency,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                if (backup.exportDateArabic.isNotBlank() || backup.exportDate.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "تاريخ النسخة: ${backup.exportDateArabic.ifBlank { backup.exportDate }}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Counts & Statistics
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "الحركات المسجلة:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${backup.totalTransactionsCount} عملية",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "الواجهات (النوتات):",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${backup.workspaces.size} واجهات",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "مصادر الدخل:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${backup.incomeSources.size} مصادر",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "جملة الإيرادات: +${numberFormat.format(backup.totalIncome)} ${backup.currency}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "الخصومات: -${numberFormat.format(backup.totalExpense)} ${backup.currency}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "طريقة الاسترداد المطلوبة:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Strategy 1: Merge
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { replaceExisting = false },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!replaceExisting) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (!replaceExisting) 2.dp else 1.dp,
                                        if (!replaceExisting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = !replaceExisting,
                                            onClick = { replaceExisting = false }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "دمج وإضافة (موصى به)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "إضافة الحركات من النسخة دون حذف بياناتك الحالية",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Strategy 2: Replace All
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { replaceExisting = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (replaceExisting) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (replaceExisting) 2.dp else 1.dp,
                                        if (replaceExisting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = replaceExisting,
                                            onClick = { replaceExisting = true },
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "استبدال كامل لكافة البيانات",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (replaceExisting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Text(
                                                text = "مسح النوتة الحالية وتطبيق النسخة المستردة تماماً",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (replaceExisting) {
                                            showReplaceConfirmDialog = true
                                        } else {
                                            isRestoring = true
                                            viewModel.restoreBackup(backup, false) { success, msg ->
                                                isRestoring = false
                                                if (success) {
                                                    restoreSuccessMessage = msg
                                                    parsedBackup = null
                                                } else {
                                                    restoreErrorMessage = msg
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isRestoring,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = if (replaceExisting) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isRestoring) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جارٍ استرداد البيانات...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (replaceExisting) "تأكيد واستبدال كافة البيانات" else "تأكيد وبدء الدمج الآن",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // =================== TELEGRAM EXPORT TAB ===================
                    val telegramBlue = Color(0xFF229ED9)

                    Text(
                        text = "خيارات التصدير عبر تليغرام:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = telegramBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Telegram App Direct Share
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = telegramBlue.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, telegramBlue.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(telegramBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "إرسال مباشر إلى تطبيق تليغرام",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "مشاركة الملف والملخص إلى محادثاتك أو الرسائل المحفوظة",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val json = viewModel.getExportJson()
                                    val file = DataExportManager.saveContentToFile(
                                        context,
                                        json,
                                        "mini_note_backup_$dateStamp.json"
                                    )
                                    val uri = DataExportManager.getShareableUri(context, file)
                                    val summary = viewModel.getTelegramSummary()

                                    val opened = DataExportManager.shareToTelegramApp(
                                        context = context,
                                        fileUri = uri,
                                        mimeType = "application/json",
                                        captionText = summary
                                    )
                                    if (!opened) {
                                        // Fallback to general share sheet if Telegram app isn't installed
                                        Toast.makeText(
                                            context,
                                            "لم يتم العثور على تطبيق تليغرام، يتم الفتح عبر خيارات المشاركة",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        DataExportManager.shareViaSystemChooser(
                                            context = context,
                                            fileUri = uri,
                                            mimeType = "application/json",
                                            title = "إرسال إلى تليغرام",
                                            summaryText = summary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = telegramBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إرسال إلى تطبيق تليغرام الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Telegram Bot API Cloud Upload
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "الرفع أونلاين عبر بوت تليغرام",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                TextButton(
                                    onClick = { showHelpGuide = !showHelpGuide },
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showHelpGuide) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("شرح البوت", fontSize = 11.sp)
                                }
                            }

                            AnimatedVisibility(visible = showHelpGuide) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "💡 خطوات سريعة للنسخ الاحتياطي عبر البوت:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "1. أنشئ بوتاً في تليغرام عبر @BotFather واحصل على Token الخاص به.\n" +
                                                "2. أرسل رسالة للبوت ثم احصل على Chat ID الخاص بك عبر @userinfobot.\n" +
                                                "3. ضع الـ Token و Chat ID هنا لحفظ نسختك أونلاين مباشرة بضغطة زر في أي وقت.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = botToken,
                                onValueChange = {
                                    botToken = it
                                    prefs.edit().putString("bot_token", it).apply()
                                },
                                label = { Text("رمز البوت (Bot Token)") },
                                placeholder = { Text("مثال: 123456:ABC-DEF1234ghIkl-zyx57W2v") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = chatId,
                                onValueChange = {
                                    chatId = it
                                    prefs.edit().putString("chat_id", it).apply()
                                },
                                label = { Text("معرف المحادثة (Chat ID)") },
                                placeholder = { Text("مثال: 987654321 أو -100xxxx للقنوات") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (uploadStatusMessage != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isUploadSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isUploadSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isUploadSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uploadStatusMessage!!,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = if (isUploadSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Button(
                                onClick = {
                                    isUploadingBot = true
                                    uploadStatusMessage = null
                                    viewModel.uploadBackupToTelegramBot(
                                        context = context,
                                        botToken = botToken,
                                        chatId = chatId
                                    ) { success, message ->
                                        isUploadingBot = false
                                        isUploadSuccess = success
                                        uploadStatusMessage = message
                                    }
                                },
                                enabled = !isUploadingBot && botToken.isNotBlank() && chatId.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isUploadingBot) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جارٍ الرفع إلى تليغرام...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("رفع النسخة أونلاين عبر البوت")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                // Footer close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showReplaceConfirmDialog && parsedBackup != null) {
        val backupToRestore = parsedBackup!!
        AlertDialog(
            onDismissRequest = { showReplaceConfirmDialog = false },
            title = { Text("تأكيد استبدال كافة البيانات") },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في استبدال كافة السجلات الحالية بملف النسخة الاحتياطية (${backupToRestore.totalTransactionsCount} عملية)؟\n\n" +
                            "⚠️ سيتم مسح العمليات الحالية واستبدالها بالكامل ببيانات ملف النسخة."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReplaceConfirmDialog = false
                        isRestoring = true
                        viewModel.restoreBackup(backupToRestore, true) { success, msg ->
                            isRestoring = false
                            if (success) {
                                restoreSuccessMessage = msg
                                parsedBackup = null
                            } else {
                                restoreErrorMessage = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، استبدل الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

