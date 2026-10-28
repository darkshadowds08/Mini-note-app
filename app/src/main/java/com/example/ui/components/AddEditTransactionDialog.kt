package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.TransactionEntity
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    initialIsAddition: Boolean = true,
    transactionToEdit: TransactionEntity? = null,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, isAddition: Boolean, timestamp: Long, imageUris: String) -> Unit
) {
    val context = LocalContext.current

    var isAddition by remember {
        mutableStateOf(transactionToEdit?.isAddition ?: initialIsAddition)
    }
    var title by remember {
        mutableStateOf(transactionToEdit?.title ?: "")
    }
    var amountText by remember {
        mutableStateOf(
            transactionToEdit?.let {
                if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString()
            } ?: ""
        )
    }

    // Date selection state
    var selectedTimestamp by remember {
        mutableLongStateOf(transactionToEdit?.timestamp ?: System.currentTimeMillis())
    }
    var isCustomDateChosen by remember {
        mutableStateOf(transactionToEdit != null)
    }
    var showDatePickerModal by remember { mutableStateOf(false) }

    // Attached images state
    var attachedImages by remember {
        mutableStateOf(transactionToEdit?.getImageList() ?: emptyList())
    }

    val dateFormatter = remember {
        SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
    }

    // Photo picker launcher for multiple images
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val savedFiles = uris.mapNotNull { uri ->
                saveUriToInternalStorage(context, uri)
            }
            attachedImages = attachedImages + savedFiles
        }
    }

    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (transactionToEdit == null) {
                            if (isAddition) "إضافة مبلغ (+)" else "خصم مبلغ (-)"
                        } else {
                            "تعديل الحركة المالية"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Addition / Deduction Tabs
                TabRow(
                    selectedTabIndex = if (isAddition) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Tab(
                        selected = isAddition,
                        onClick = { isAddition = true },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isAddition) FinanceGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إيداع (+)",
                                    fontWeight = if (isAddition) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAddition) FinanceGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Tab(
                        selected = !isAddition,
                        onClick = { isAddition = false },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = if (!isAddition) FinanceRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "خصم (-)",
                                    fontWeight = if (!isAddition) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isAddition) FinanceRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Amount Input Field (المبلغ)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (it.toDoubleOrNull() != null && it.toDouble() > 0) amountError = false
                    },
                    label = { Text("المبلغ ($currency)") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Paid,
                            contentDescription = null,
                            tint = if (isAddition) FinanceGreen else FinanceRed
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = {
                        if (amountError) Text("يرجى إدخال مبلغ صحيح أكبر من 0", color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Description / Note Field (وصف المبلغ والبيان)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("وصف المبلغ / البيان") },
                    placeholder = { Text(if (isAddition) "مثلاً: راتب، تحويل، أرباح، هدية..." else "مثلاً: فاتورة كهرباء، بقالة، مشتريات...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isError = titleError,
                    supportingText = {
                        if (titleError) Text("يرجى إدخال وصف المبلغ", color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Date Selection Section (التاريخ)
                Text(
                    text = "التاريخ:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCustomDateChosen,
                        onClick = {
                            isCustomDateChosen = false
                            selectedTimestamp = System.currentTimeMillis()
                        },
                        label = { Text("تاريخ اليوم") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    FilterChip(
                        selected = isCustomDateChosen && isYesterday(selectedTimestamp),
                        onClick = {
                            isCustomDateChosen = true
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                            selectedTimestamp = cal.timeInMillis
                        },
                        label = { Text("أمس") }
                    )

                    FilterChip(
                        selected = isCustomDateChosen && !isYesterday(selectedTimestamp),
                        onClick = {
                            isCustomDateChosen = true
                            showDatePickerModal = true
                        },
                        label = { Text("تحديد تاريخ...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display selected date preview card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePickerModal = true },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateFormatter.format(Date(selectedTimestamp)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { showDatePickerModal = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "تغيير التاريخ",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Attach Photos Section (إرفاق صورة أو عدة صور)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الصور المرفقة (${attachedImages.size}):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (attachedImages.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة المزيد", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (attachedImages.isEmpty()) {
                    // Empty state button to attach photos
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرفاق صورة أو عدة صور (فاتورة، إيصال...)",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    // Horizontal scroll of thumbnail cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for ((index, path) in attachedImages.withIndex()) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "صورة مرفقة",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Remove badge button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable {
                                            attachedImages = attachedImages.filterIndexed { i, _ -> i != index }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف الصورة",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            var hasError = false
                            if (title.isBlank()) {
                                titleError = true
                                hasError = true
                            }
                            if (amount <= 0.0) {
                                amountError = true
                                hasError = true
                            }
                            if (!hasError) {
                                onSave(
                                    title.trim(),
                                    amount,
                                    isAddition,
                                    selectedTimestamp,
                                    attachedImages.joinToString(",")
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAddition) FinanceGreen else FinanceRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (transactionToEdit == null) "حفظ الحركة" else "تحديث التعديل",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Modal Dialog
    if (showDatePickerModal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerModal = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedMillis ->
                            val pickedCal = Calendar.getInstance().apply { timeInMillis = pickedMillis }
                            val currentCal = Calendar.getInstance()
                            pickedCal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                            pickedCal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                            selectedTimestamp = pickedCal.timeInMillis
                            isCustomDateChosen = true
                        }
                        showDatePickerModal = false
                    }
                ) {
                    Text("تأكيد التاريخ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerModal = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun isYesterday(timestamp: Long): Boolean {
    val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }
    return calYesterday.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            calYesterday.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
}

private fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val imagesDir = File(context.filesDir, "transaction_images").apply { mkdirs() }
        val file = File(imagesDir, "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
