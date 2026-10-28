package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.ui.components.AddEditTransactionDialog
import com.example.ui.components.BalanceCard
import com.example.ui.components.ExportDataDialog
import com.example.ui.components.MonthStartReviewDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.WorkspaceManagerDialog
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.DateFilter
import com.example.ui.viewmodel.FilterType
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceMiniMapScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog & Navigation states
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogIsAddition by remember { mutableStateOf(true) }
    var addDialogDefaultSourceId by remember { mutableStateOf<Long?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionDetailView by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showWorkspaceDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportDialogInitialTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Ensure Arabic Right-to-Left layout direction for natural RTL experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "مصغرة النوتة المالية",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = uiState.activeBook?.name ?: "الواجهة الرئيسية",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "بحث في النوتة"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "تصدير البيانات محلياً أو عبر تليغرام"
                            )
                        }
                        IconButton(onClick = { showWorkspaceDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "الواجهات والنوتات"
                            )
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                if (uiState.currentTab == AppNavTab.MAIN_LEDGER) {
                    FloatingActionButton(
                        onClick = {
                            transactionToEdit = null
                            addDialogIsAddition = true
                            addDialogDefaultSourceId = null
                            showAddDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة حركة مالية جديدة")
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.currentTab == AppNavTab.MAIN_LEDGER,
                        onClick = { viewModel.setCurrentTab(AppNavTab.MAIN_LEDGER) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "النوتة والعمليات"
                            )
                        },
                        label = {
                            Text(
                                text = "النوتة والعمليات",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_main_ledger")
                    )
                    NavigationBarItem(
                        selected = uiState.currentTab == AppNavTab.REPORTS,
                        onClick = { viewModel.setCurrentTab(AppNavTab.REPORTS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "واجهة التقارير"
                            )
                        },
                        label = {
                            Text(
                                text = "واجهة التقارير",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_reports")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.currentTab == AppNavTab.REPORTS) {
                    ReportsScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Month-Start Review Alert Banner
                        if (uiState.isMonthStartBannerVisible) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setMonthStartReviewModal(true) }
                                        .testTag("month_start_banner"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Assessment,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "تقرير مراجعة بداية الشهر 🔔",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "راجع إجمالي الإيرادات والخصومات والمتبقي لشهر ${uiState.monthlyReport.monthNameArabic}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Button(
                                                onClick = { viewModel.setMonthStartReviewModal(true) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "مراجعة",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.dismissMonthStartBanner() },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "إغلاق التنبيه",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                                // Search Field (if open)
                                if (showSearchBar) {
                                    item {
                                        OutlinedTextField(
                                            value = uiState.searchQuery,
                                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                                            placeholder = { Text("ابحث بالبيان، المبلغ، أو الملاحظة...") },
                                            leadingIcon = {
                                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                            },
                                            trailingIcon = {
                                                if (uiState.searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            singleLine = true
                                        )
                                    }
                                }

                                // Active Workspace / Interface Selector Card
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showWorkspaceDialog = true },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Layers,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "الواجهة الحالية:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = uiState.activeBook?.name ?: "النوتة المالية",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { showWorkspaceDialog = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "واجهة أخرى",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Balance Card
                                item {
                                    BalanceCard(
                                        totalBalance = uiState.totalBalance,
                                        totalIncome = uiState.totalIncome,
                                        totalExpense = uiState.totalExpense,
                                        currency = uiState.currency,
                                        onAddClick = {
                                            transactionToEdit = null
                                            addDialogIsAddition = true
                                            addDialogDefaultSourceId = null
                                            showAddDialog = true
                                        },
                                        onDeductClick = {
                                            transactionToEdit = null
                                            addDialogIsAddition = false
                                            addDialogDefaultSourceId = null
                                            showAddDialog = true
                                        }
                                    )
                                }

                                // Filters Section
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = uiState.filterType == FilterType.ALL,
                                                onClick = { viewModel.onFilterTypeChanged(FilterType.ALL) },
                                                label = { Text("الكل (${uiState.transactions.size})") }
                                            )
                                            FilterChip(
                                                selected = uiState.filterType == FilterType.INCOME_ONLY,
                                                onClick = { viewModel.onFilterTypeChanged(FilterType.INCOME_ONLY) },
                                                label = { Text("إيداع (+)") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = FinanceGreen.copy(alpha = 0.2f),
                                                    selectedLabelColor = FinanceGreen
                                                )
                                            )
                                            FilterChip(
                                                selected = uiState.filterType == FilterType.EXPENSE_ONLY,
                                                onClick = { viewModel.onFilterTypeChanged(FilterType.EXPENSE_ONLY) },
                                                label = { Text("خصم (-)") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = FinanceRed.copy(alpha = 0.2f),
                                                    selectedLabelColor = FinanceRed
                                                )
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (dateFilter in DateFilter.entries) {
                                                FilterChip(
                                                    selected = uiState.dateFilter == dateFilter,
                                                    onClick = { viewModel.onDateFilterChanged(dateFilter) },
                                                    label = { Text(dateFilter.title, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Header for Transactions List
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "آخر الحركات المالية:",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${uiState.filteredTransactions.size} حركة",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Transactions List
                                if (uiState.filteredTransactions.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.ReceiptLong,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(44.dp)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = if (uiState.searchQuery.isNotEmpty())
                                                        "لا توجد نتائج تطابق بحثك '${uiState.searchQuery}'"
                                                    else
                                                        "لا توجد حركات مسجلة بعد، اضغط إضافة أو خصم للبدء",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(
                                        items = uiState.filteredTransactions,
                                        key = { it.id }
                                    ) { item ->
                                        TransactionItemCard(
                                            transaction = item,
                                            currency = uiState.currency,
                                            onClick = { transactionDetailView = item },
                                            onEditClick = {
                                                transactionToEdit = item
                                                addDialogIsAddition = item.isAddition
                                                addDialogDefaultSourceId = item.sourceId
                                                showAddDialog = true
                                            },
                                            onDeleteClick = {
                                                transactionToDelete = item
                                            }
                                        )
                                    }
                                }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

        // Add or Edit Transaction Dialog
        if (showAddDialog) {
            AddEditTransactionDialog(
                initialIsAddition = addDialogIsAddition,
                transactionToEdit = transactionToEdit,
                currency = uiState.currency,
                onDismiss = {
                    showAddDialog = false
                    transactionToEdit = null
                    addDialogDefaultSourceId = null
                },
                onSave = { title, amount, isAdd, timestamp, imageUris ->
                    if (transactionToEdit == null) {
                        viewModel.addTransaction(
                            title = title,
                            amount = amount,
                            isAddition = isAdd,
                            category = if (isAdd) "OTHER_INCOME" else "OTHER_EXPENSE",
                            note = "",
                            timestamp = timestamp,
                            sourceId = addDialogDefaultSourceId,
                            imageUris = imageUris
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (isAdd) "تمت إضافة مبلغ $amount ${uiState.currency} بنجاح" else "تم خصم مبلغ $amount ${uiState.currency} بنجاح",
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else {
                        viewModel.updateTransaction(
                            transactionToEdit!!.copy(
                                title = title,
                                amount = amount,
                                isAddition = isAdd,
                                timestamp = timestamp,
                                imageUris = imageUris
                            )
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "تم تعديل الحركة المالية بنجاح",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                    showAddDialog = false
                    transactionToEdit = null
                    addDialogDefaultSourceId = null
                }
            )
        }

        // Transaction Details Dialog
        transactionDetailView?.let { tx ->
            TransactionDetailDialog(
                transaction = tx,
                currency = uiState.currency,
                onDismiss = { transactionDetailView = null },
                onEdit = {
                    transactionToEdit = tx
                    addDialogIsAddition = tx.isAddition
                    addDialogDefaultSourceId = tx.sourceId
                    transactionDetailView = null
                    showAddDialog = true
                },
                onDelete = {
                    transactionToDelete = tx
                    transactionDetailView = null
                }
            )
        }

        // Delete Confirmation Dialog
        transactionToDelete?.let { tx ->
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("حذف الحركة المالية") },
                text = { Text("هل تريد بالتأكيد حذف '${tx.title}' بمبلغ ${tx.amount} ${uiState.currency}؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(tx)
                            transactionToDelete = null
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "تم حذف الحركة المالية",
                                    actionLabel = "تراجع",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoDelete()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                currency = uiState.currency,
                transactions = uiState.transactions,
                totalBalance = uiState.totalBalance,
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
                onClearAllData = {
                    viewModel.clearAllData()
                    scope.launch {
                        snackbarHostState.showSnackbar("تم مسح كافة البيانات المالية")
                    }
                },
                onOpenExportDialog = {
                    exportDialogInitialTab = 0
                    showExportDialog = true
                },
                onOpenRestoreDialog = {
                    exportDialogInitialTab = 1
                    showExportDialog = true
                },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Export Data Dialog (Local & Telegram)
        if (showExportDialog) {
            ExportDataDialog(
                viewModel = viewModel,
                initialTab = exportDialogInitialTab,
                onDismiss = {
                    showExportDialog = false
                    exportDialogInitialTab = 0
                }
            )
        }

        // Workspace / Interface Manager Dialog
        if (showWorkspaceDialog) {
            WorkspaceManagerDialog(
                books = uiState.ledgerBooks,
                activeBookId = uiState.activeBook?.id ?: 1L,
                currency = uiState.currency,
                onSelectBook = { bookId ->
                    viewModel.setActiveBook(bookId)
                    showWorkspaceDialog = false
                },
                onCreateBook = { name, iconName, colorHex ->
                    viewModel.createLedgerBook(name, iconName, colorHex)
                    scope.launch {
                        snackbarHostState.showSnackbar("تم إنشاء واجهة \"$name\" بنجاح")
                    }
                },
                onDeleteBook = { book ->
                    viewModel.deleteLedgerBook(book)
                    scope.launch {
                        snackbarHostState.showSnackbar("تم حذف واجهة \"${book.name}\" وحركاتها المالية")
                    }
                },
                onDismiss = { showWorkspaceDialog = false }
            )
        }

        // Month Start Review Dialog
        if (uiState.showMonthStartReviewModal) {
            MonthStartReviewDialog(
                report = uiState.monthlyReport,
                currency = uiState.currency,
                onDismiss = { viewModel.setMonthStartReviewModal(false) },
                onTransactionClick = { tx ->
                    transactionDetailView = tx
                }
            )
        }
    }
}
