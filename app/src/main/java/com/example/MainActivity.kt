package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PosViewModel
import com.example.ui.components.ReceiptDialog
import com.example.ui.dialogs.BackupRestoreDialog
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProductCatalogScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen(val title: String, val icon: ImageVector) {
    POS("Kasir", Icons.Default.PointOfSale),
    CATALOG("Katalog & Stok", Icons.Default.Inventory),
    TRANSACTIONS("Riwayat Penjualan", Icons.Default.ReceiptLong),
    REPORTS("Laba & Rugi", Icons.Default.BarChart),
    EXPENSES("Modal & Biaya", Icons.Default.AccountBalanceWallet)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PosViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var currentScreen by remember { mutableStateOf(Screen.POS) }
                var showBackupDialog by remember { mutableStateOf(false) }

                val storeInfo by viewModel.storeInfo.collectAsStateWithLifecycle()
                val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
                val activeReceipt by viewModel.activeReceipt.collectAsStateWithLifecycle()

                // Back navigation handling
                BackHandler(enabled = currentScreen != Screen.POS) {
                    currentScreen = Screen.POS
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = if (currentScreen == Screen.POS) storeInfo.storeName else currentScreen.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showBackupDialog = true },
                                    modifier = Modifier.testTag("action_backup_restore_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Backup,
                                        contentDescription = "Cadangkan & Pulihkan Data",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            Screen.values().forEach { screen ->
                                val isSelected = currentScreen == screen
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        if (screen == Screen.CATALOG && lowStockProducts.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = Color(0xFFD32F2F)) {
                                                        Text("${lowStockProducts.size}")
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.POS -> PosScreen(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                onNavigateToCatalog = { currentScreen = Screen.CATALOG }
                            )
                            Screen.CATALOG -> ProductCatalogScreen(viewModel = viewModel)
                            Screen.TRANSACTIONS -> TransactionsScreen(viewModel = viewModel)
                            Screen.REPORTS -> ReportsScreen(viewModel = viewModel)
                            Screen.EXPENSES -> ExpensesScreen(viewModel = viewModel)
                        }

                        // Receipt Dialog for completed transactions or viewing from history
                        activeReceipt?.let { receipt ->
                            ReceiptDialog(
                                transaction = receipt,
                                storeInfo = storeInfo,
                                onDismiss = { viewModel.dismissReceipt() }
                            )
                        }

                        // Backup & Restore Dialog
                        if (showBackupDialog) {
                            BackupRestoreDialog(
                                viewModel = viewModel,
                                onDismiss = { showBackupDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
