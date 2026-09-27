package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ProductEntity
import com.example.ui.PosViewModel
import com.example.ui.components.BarcodeVisual
import com.example.ui.dialogs.CatalogManagementDialog
import com.example.ui.dialogs.EditProductDialog
import com.example.ui.dialogs.QuickCreateCatalogDialog
import com.example.util.Formatters

@Composable
fun ProductCatalogScreen(viewModel: PosViewModel) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val filterLowStockOnly by viewModel.filterLowStockOnly.collectAsStateWithLifecycle()

    val allCatalogNames by viewModel.allCatalogNames.collectAsStateWithLifecycle()
    val productCounts by viewModel.catalogProductCounts.collectAsStateWithLifecycle()

    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCatalogManagerDialog by remember { mutableStateOf(false) }
    var showQuickCreateCatalogDialog by remember { mutableStateOf(false) }
    var emptyCatalogToDelete by remember { mutableStateOf<String?>(null) }

    val displayCategories = remember(allCatalogNames) {
        listOf("Semua") + allCatalogNames
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Filter Controls
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalog_search_input"),
                        placeholder = { Text("Cari nama produk atau kode barcode...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Cari")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category selection chips with Catalog manager and Add buttons
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Kelola Katalog" button
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { showCatalogManagerDialog = true },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    label = { Text("Kelola", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("btn_open_catalog_manager")
                                )
                            }

                            // "+ Buat Katalog" quick button
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { showQuickCreateCatalogDialog = true },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text("Katalog Baru", fontSize = 11.sp) },
                                    modifier = Modifier.testTag("btn_quick_add_catalog")
                                )
                            }

                            items(displayCategories) { cat ->
                                val count = if (cat == "Semua") allProducts.size else (productCounts[cat] ?: 0)
                                val isEmpty = cat != "Semua" && count == 0
                                val isSelected = selectedCategory == cat

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectCategory(cat) },
                                    label = {
                                        Text(
                                            text = "$cat ($count)",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isEmpty) Color(0xFFF57C00) else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (isEmpty) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Low Stock Warning Filter Pill Button
                        FilterChip(
                            selected = filterLowStockOnly,
                            onClick = { viewModel.toggleFilterLowStock(!filterLowStockOnly) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (filterLowStockOnly) Color.White else Color(0xFFD32F2F)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Stok Tipis (${lowStockProducts.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD32F2F),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_low_stock_button")
                        )
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                val isSelectedCatalogEmpty = selectedCategory != "Semua" && (productCounts[selectedCategory] ?: 0) == 0

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (isSelectedCatalogEmpty) Icons.Default.FolderOpen else Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = if (isSelectedCatalogEmpty) Color(0xFFF57C00) else Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isSelectedCatalogEmpty) {
                            Text(
                                text = "Katalog '$selectedCategory' masih kosong!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tidak ada produk di dalam katalog ini. Anda dapat menambahkan produk atau menghapus katalog ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showAddDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah Produk", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { emptyCatalogToDelete = selectedCategory },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_delete_empty_catalog_screen")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus Katalog Kosong Ini", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = if (filterLowStockOnly) "Tidak ada produk dengan stok menipis!"
                                else "Tidak ada produk yang cocok dengan pencarian",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onEdit = { productToEdit = product }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Product Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_product_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        EditProductDialog(
            initialProduct = null,
            availableCategories = allCatalogNames,
            onSave = { newProduct ->
                viewModel.saveProduct(newProduct) {
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit Product Dialog
    productToEdit?.let { prod ->
        EditProductDialog(
            initialProduct = prod,
            availableCategories = allCatalogNames,
            onSave = { updatedProduct ->
                viewModel.saveProduct(updatedProduct) {
                    productToEdit = null
                }
            },
            onDelete = { toDelete ->
                viewModel.deleteProduct(toDelete)
                productToEdit = null
            },
            onDismiss = { productToEdit = null }
        )
    }

    // Catalog Management Dialog
    if (showCatalogManagerDialog) {
        CatalogManagementDialog(
            viewModel = viewModel,
            onDismiss = { showCatalogManagerDialog = false }
        )
    }

    // Quick Create Catalog Dialog
    if (showQuickCreateCatalogDialog) {
        QuickCreateCatalogDialog(
            onConfirm = { newName ->
                viewModel.createCatalog(newName) { success, _ ->
                    if (success) {
                        viewModel.selectCategory(newName)
                        showQuickCreateCatalogDialog = false
                    }
                }
            },
            onDismiss = { showQuickCreateCatalogDialog = false }
        )
    }

    // Delete empty catalog confirmation
    emptyCatalogToDelete?.let { catName ->
        AlertDialog(
            onDismissRequest = { emptyCatalogToDelete = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Hapus Katalog Kosong?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("Katalog '$catName' tidak memiliki produk di dalamnya dan akan dihapus.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = emptyCatalogToDelete
                        emptyCatalogToDelete = null
                        if (target != null) {
                            viewModel.deleteCatalogByNameIfEmpty(target) { _, _ -> }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Ya, Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { emptyCatalogToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    onEdit: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Category, Low stock alert banner/badge, Edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                if (product.isLowStock) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Stok Menipis!",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_product_${product.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Produk",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Product Name & Stock count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (product.isLowStock) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Stok: ${product.stock} ${product.unit}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (product.isLowStock) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Barcode snippet visual
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = product.barcode,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color.DarkGray
                            )
                        )
                    }

                    BarcodeVisual(
                        barcode = product.barcode,
                        modifier = Modifier.height(18.dp),
                        showText = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Buy price, Sell price & Profit margin
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Harga Beli (Modal)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = Formatters.formatRupiah(product.buyPrice),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Column {
                        Text(text = "Harga Jual", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = Formatters.formatRupiah(product.sellPrice),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Margin Laba", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        val margin = product.sellPrice - product.buyPrice
                        Text(
                            text = Formatters.formatRupiah(margin),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        )
                    }
                }
            }
        }
    }
}
