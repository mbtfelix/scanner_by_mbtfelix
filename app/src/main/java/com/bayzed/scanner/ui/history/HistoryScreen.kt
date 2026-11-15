package com.bayzed.scanner.ui.history

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.bayzed.scanner.data.ScanItem
import com.bayzed.scanner.ui.theme.ThemeMode
import com.bayzed.scanner.ui.viewmodel.ScanFilter
import com.bayzed.scanner.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ScannerViewModel,
    onScanClick: (ScanItem) -> Unit
) {
    val scans by viewModel.filteredScans.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedScanIds by viewModel.selectedScanIds.collectAsState()

    BackHandler(enabled = isMultiSelectMode) {
        viewModel.clearMultiSelect()
    }

    var showClearDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text("${selectedScanIds.size} Selected")
                    } else {
                        Text("Scan History")
                    }
                },
                navigationIcon = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = { viewModel.clearMultiSelect() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Multi-Select")
                        }
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            if (selectedScanIds.size == scans.size) {
                                viewModel.clearMultiSelect()
                            } else {
                                viewModel.selectAllScans(scans.map { it.id })
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedScanIds.size == scans.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = "Select All"
                            )
                        }
                        IconButton(onClick = { showBatchDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleMultiSelectMode(true) }) {
                            Icon(Icons.Default.Checklist, contentDescription = "Multi-Select")
                        }

                        Box {
                            IconButton(onClick = { showThemeMenu = true }) {
                                Icon(Icons.Default.Palette, contentDescription = "Theme Mode")
                            }
                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("System Theme") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM); showThemeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Light Mode") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT); showThemeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dark Mode") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.DARK); showThemeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pure AMOLED Black") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.AMOLED); showThemeMenu = false }
                                )
                            }
                        }

                        if (scans.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search scans...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == ScanFilter.ALL,
                    onClick = { viewModel.setFilter(ScanFilter.ALL) },
                    label = { Text("All") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = selectedFilter == ScanFilter.TEXT,
                    onClick = { viewModel.setFilter(ScanFilter.TEXT) },
                    label = { Text("Text") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = selectedFilter == ScanFilter.NUMERIC,
                    onClick = { viewModel.setFilter(ScanFilter.NUMERIC) },
                    label = { Text("Numeric") },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = selectedFilter == ScanFilter.MIXED,
                    onClick = { viewModel.setFilter(ScanFilter.MIXED) },
                    label = { Text("Mixed") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (scans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No scan history found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(scans, key = { it.id }) { scan ->
                        val isSelected = selectedScanIds.contains(scan.id)
                        var copiedRecently by remember { mutableStateOf(false) }

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.deleteScan(scan.id)
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Scan deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreScan(scan)
                                            }
                                        }
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, scan.content)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Scan"))
                                        false
                                    }
                                    SwipeToDismissBoxValue.Settled -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val color = when (direction) {
                                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFD32F2F)
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                }
                                val alignment = when (direction) {
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    else -> Alignment.Center
                                }
                                val icon = when (direction) {
                                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Share
                                    else -> Icons.Default.Info
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(color)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = alignment
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (isMultiSelectMode) {
                                                viewModel.toggleScanSelection(scan.id)
                                            } else {
                                                onScanClick(scan)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isMultiSelectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.toggleMultiSelectMode(true)
                                                viewModel.toggleScanSelection(scan.id)
                                            }
                                        }
                                    ),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 40.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isMultiSelectMode) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { viewModel.toggleScanSelection(scan.id) }
                                                    )
                                                }
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ) {
                                                    Text(
                                                        text = scan.format,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = dateFormat.format(Date(scan.timestamp)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = scan.content,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 2,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = scan.parsedType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            clipboardManager.setText(AnnotatedString(scan.content))
                                            copiedRecently = true
                                            coroutineScope.launch {
                                                delay(2000L)
                                                copiedRecently = false
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy Text",
                                            tint = if (copiedRecently) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to delete all saved scans? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Delete Selected Scans") },
            text = { Text("Are you sure you want to delete ${selectedScanIds.size} selected scans?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedScans()
                        showBatchDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
