package com.bayzed.scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.bayzed.scanner.ui.components.FloatingNavHub
import com.bayzed.scanner.ui.components.ScanResultBottomSheet
import com.bayzed.scanner.ui.generator.GeneratorScreen
import com.bayzed.scanner.ui.history.HistoryScreen
import com.bayzed.scanner.ui.scanner.ScannerScreen
import com.bayzed.scanner.ui.theme.ScannerTheme
import com.bayzed.scanner.ui.viewmodel.AppScreen
import com.bayzed.scanner.ui.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val selectedScan by viewModel.selectedScanForSheet.collectAsState()
            val haptic = LocalHapticFeedback.current

            ScannerTheme(themeMode = themeMode) {
                BackHandler(enabled = currentScreen != AppScreen.HISTORY || selectedScan != null) {
                    if (selectedScan != null) {
                        viewModel.setSelectedScanForSheet(null)
                    } else if (currentScreen != AppScreen.HISTORY) {
                        viewModel.setScreen(AppScreen.HISTORY)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.HISTORY -> HistoryScreen(
                                viewModel = viewModel,
                                onScanClick = { scan -> viewModel.setSelectedScanForSheet(scan) }
                            )
                            AppScreen.SCANNER -> ScannerScreen(
                                onSaveAndNavigateToHistory = { format, content ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addScan(format, content)
                                    viewModel.setScreen(AppScreen.HISTORY)
                                }
                            )
                            AppScreen.GENERATOR -> GeneratorScreen()
                        }

                        FloatingNavHub(
                            currentScreen = currentScreen,
                            onScreenSelected = { screen -> viewModel.setScreen(screen) }
                        )

                        if (selectedScan != null) {
                            ScanResultBottomSheet(
                                scan = selectedScan!!,
                                onDismiss = { viewModel.setSelectedScanForSheet(null) },
                                onDelete = { id -> viewModel.deleteScan(id) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("OPEN_SCANNER", false) == true) {
            viewModel.setScreen(AppScreen.SCANNER)
        }
    }
}
