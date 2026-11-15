package com.bayzed.scanner.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bayzed.scanner.data.AppDatabase
import com.bayzed.scanner.data.ScanItem
import com.bayzed.scanner.data.ScanRepository
import com.bayzed.scanner.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    HISTORY, SCANNER, GENERATOR
}

enum class ScanFilter {
    ALL, TEXT, NUMERIC, MIXED
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val database by lazy { AppDatabase.getDatabase(application) }
    private val repository by lazy { ScanRepository(database.scanDao()) }

    val allScans: StateFlow<List<ScanItem>> by lazy {
        repository.allScans
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val _currentScreen = MutableStateFlow(AppScreen.HISTORY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ScanFilter.ALL)
    val selectedFilter: StateFlow<ScanFilter> = _selectedFilter.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _selectedScanForSheet = MutableStateFlow<ScanItem?>(null)
    val selectedScanForSheet: StateFlow<ScanItem?> = _selectedScanForSheet.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedScanIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedScanIds: StateFlow<Set<Long>> = _selectedScanIds.asStateFlow()

    val filteredScans: StateFlow<List<ScanItem>> = combine(
        allScans,
        _searchQuery,
        _selectedFilter
    ) { scans, query, filter ->
        scans.filter { scan ->
            val matchesQuery = query.isBlank() || 
                scan.content.contains(query, ignoreCase = true) ||
                scan.format.contains(query, ignoreCase = true) ||
                scan.parsedType.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                ScanFilter.ALL -> true
                ScanFilter.TEXT -> scan.content.isNotBlank() && scan.content.all { it.isLetter() || it.isWhitespace() }
                ScanFilter.NUMERIC -> scan.content.isNotBlank() && scan.content.all { it.isDigit() }
                ScanFilter.MIXED -> {
                    val isText = scan.content.isNotBlank() && scan.content.all { it.isLetter() || it.isWhitespace() }
                    val isNum = scan.content.isNotBlank() && scan.content.all { it.isDigit() }
                    !isText && !isNum
                }
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen != AppScreen.HISTORY) {
            clearMultiSelect()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ScanFilter) {
        _selectedFilter.value = filter
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setSelectedScanForSheet(scan: ScanItem?) {
        _selectedScanForSheet.value = scan
    }

    fun toggleMultiSelectMode(enabled: Boolean) {
        _isMultiSelectMode.value = enabled
        if (!enabled) {
            _selectedScanIds.value = emptySet()
        }
    }

    fun toggleScanSelection(id: Long) {
        val current = _selectedScanIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedScanIds.value = current
    }

    fun selectAllScans(allIds: List<Long>) {
        _selectedScanIds.value = allIds.toSet()
    }

    fun clearMultiSelect() {
        _isMultiSelectMode.value = false
        _selectedScanIds.value = emptySet()
    }

    fun deleteSelectedScans() {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = _selectedScanIds.value
            for (id in ids) {
                repository.deleteById(id)
            }
            clearMultiSelect()
        }
    }

    fun addScan(format: String, content: String): ScanItem {
        val parsedType = parseContentType(content)
        val item = ScanItem(
            format = format,
            content = content,
            parsedType = parsedType
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(item)
        }
        return item
    }

    fun restoreScan(scan: ScanItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(scan)
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            clearMultiSelect()
        }
    }

    private fun parseContentType(content: String): String {
        val lower = content.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") -> "URL"
            lower.startsWith("WIFI:") -> "WIFI"
            lower.startsWith("mailto:") || (lower.contains("@") && !lower.contains(" ")) -> "EMAIL"
            lower.startsWith("tel:") -> "PHONE"
            lower.startsWith("BEGIN:VCARD") -> "CONTACT"
            else -> "TEXT"
        }
    }
}
