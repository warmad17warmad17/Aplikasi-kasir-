package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ProductEntity
import com.example.data.model.CartItem
import com.example.ui.PosViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToCatalog: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val cartItemCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val cashInput by viewModel.cashInput.collectAsStateWithLifecycle()
    val cashAmount by viewModel.cashAmount.collectAsStateWithLifecycle()
    val changeAmount by viewModel.changeAmount.collectAsStateWithLifecycle()

    var barcodeInput by remember { mutableStateOf("") }
    var showScannerDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }

    val allCatalogNames by viewModel.allCatalogNames.collectAsStateWithLifecycle()
    val categories = remember(allCatalogNames) {
        listOf("Semua") + allCatalogNames
    }

    val displayProducts = remember(allProducts, selectedCategoryFilter) {
        if (selectedCategoryFilter == "Semua") {
            allProducts
        } else {
            allProducts.filter { it.category == selectedCategoryFilter }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Stock Warning Banner if any products are running low
        if (lowStockProducts.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCatalog() },
                color = Color(0xFFFFF3E0),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Peringatan: ${lowStockProducts.size} produk stoknya hampir habis! Ketuk untuk cek.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFBF360C),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Top Scanner & Quick Barcode Input Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_barcode_input"),
                    placeholder = { Text("Scan / Ketik Barcode atau QR...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (barcodeInput.isNotBlank()) {
                                viewModel.scanOrInputBarcode(barcodeInput) { success, msg ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                    if (success) barcodeInput = ""
                                }
                            }
                        }
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Cari")
                    },
                    trailingIcon = {
                        if (barcodeInput.isNotEmpty()) {
                            IconButton(onClick = { barcodeInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showScannerDialog = true },
                    modifier = Modifier.testTag("open_scanner_dialog_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Pindai Barcode/QR",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan QR")
                }
            }
        }

        // Main POS Screen content (Catalog Chips & Cart / Checkout)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quick Catalog selection header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Cepat Produk",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${displayProducts.size} Produk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category Chips Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Horizontal scrollable product cards for instant tap to cart
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayProducts) { prod ->
                        ElevatedCard(
                            onClick = {
                                if (prod.stock > 0) {
                                    viewModel.addToCart(prod, 1)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Stok '${prod.name}' sudah habis!")
                                    }
                                }
                            },
                            modifier = Modifier
                                .width(140.dp)
                                .testTag("quick_product_${prod.id}"),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (prod.stock <= 0) Color(0xFFEEEEEE) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = prod.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (prod.isLowStock) {
                                        Text(
                                            text = "Stok: ${prod.stock}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD32F2F)
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = "${prod.stock} ${prod.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prod.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatRupiah(prod.sellPrice),
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

            // Cart Items Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Keranjang",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Keranjang Kasir ($cartItemCount Barang)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (cartItems.isNotEmpty()) {
                        TextButtonRow(
                            text = "Kosongkan",
                            onClick = { viewModel.clearCart() }
                        )
                    }
                }
            }

            // Empty cart placeholder
            if (cartItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Keranjang Belanja Masih Kosong",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Scan barcode atau klik produk di atas untuk memasukkan barang",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                // Cart Items List
                items(cartItems, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { viewModel.addToCart(item.product, 1) },
                        onDecrease = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                        onRemove = { viewModel.removeFromCart(item.product.id) }
                    )
                }
            }

            // Payment and Checkout Panel
            if (cartItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Subtotal & Total Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL BELANJA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = Formatters.formatRupiah(cartTotal),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Nominal Cepat: 10000, 20000, 30000, 50000, 100000 & Uang Pas
                            Text(
                                text = "PILIHAN NOMINAL BAYAR CEPAT:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Exact amount button
                                OutlinedButton(
                                    onClick = { viewModel.setExactCashPayment() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (cashAmount == cartTotal && cartTotal > 0)
                                            MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                    modifier = Modifier.testTag("nominal_exact_button")
                                ) {
                                    Text("Uang Pas")
                                }

                                // 10.000, 20.000, 30.000, 50.000, 100.000
                                viewModel.nominalPresets.forEach { preset ->
                                    val isSelected = cashAmount == preset
                                    OutlinedButton(
                                        onClick = { viewModel.setNominalPayment(preset) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        modifier = Modifier.testTag("nominal_${preset.toLong()}_button")
                                    ) {
                                        Text(Formatters.formatNumber(preset))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Editable Nominal Input Field (User can edit the exact number!)
                            OutlinedTextField(
                                value = cashInput,
                                onValueChange = { viewModel.updateCashInput(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cash_paid_input"),
                                label = { Text("Nominal Tunai yang Dibayarkan (Bisa Diedit)") },
                                placeholder = { Text("Ketik atau pilih nominal di atas") },
                                prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                trailingIcon = {
                                    if (cashInput.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateCashInput("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Change Amount Box (Kembalian)
                            val isSufficient = cashAmount >= cartTotal && cartTotal > 0
                            val deficiency = cartTotal - cashAmount

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSufficient) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isSufficient) "UANG KEMBALIAN:" else "KURANG BAYAR:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSufficient) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                    )
                                    Text(
                                        text = if (isSufficient) Formatters.formatRupiah(changeAmount)
                                        else Formatters.formatRupiah(deficiency),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSufficient) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Process Checkout & Print Receipt Button
                            Button(
                                onClick = {
                                    viewModel.checkout(
                                        onSuccess = {
                                            // Dialog will be shown via ViewModel's activeReceipt
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(error)
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("process_checkout_button"),
                                enabled = isSufficient,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1565C0)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(
                                    text = "Bayar & Cetak Struk Belanja",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Barcode Scanner Dialog
    if (showScannerDialog) {
        BarcodeScannerDialog(
            products = allProducts,
            onBarcodeScanned = { barcode ->
                showScannerDialog = false
                viewModel.scanOrInputBarcode(barcode) { success, msg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            },
            onDismiss = { showScannerDialog = false }
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${Formatters.formatRupiah(item.product.sellPrice)} / ${item.product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                Text(
                    text = "Subtotal: ${Formatters.formatRupiah(item.subtotal)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                )
            }

            // Quantity controls (+, -, Qty)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Kurang",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onIncrease,
                    enabled = item.quantity < item.product.stock,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.quantity < item.product.stock)
                                MaterialTheme.colorScheme.primaryContainer
                            else Color.LightGray
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Tambah",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete button for cancelled order item
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .testTag("remove_item_${item.product.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus Pesanan",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TextButtonRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    )
}
