package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ProductEntity
import com.example.ui.components.BarcodeVisual
import com.example.util.Formatters
import kotlin.random.Random

@Composable
fun EditProductDialog(
    initialProduct: ProductEntity? = null,
    availableCategories: List<String> = emptyList(),
    onSave: (ProductEntity) -> Unit,
    onDelete: ((ProductEntity) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var barcode by remember {
        mutableStateOf(initialProduct?.barcode ?: generateRandomBarcode())
    }
    var category by remember {
        mutableStateOf(initialProduct?.category ?: if (availableCategories.isNotEmpty()) availableCategories.first() else "Sembako")
    }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "Pcs") }
    var buyPriceText by remember {
        mutableStateOf(initialProduct?.buyPrice?.toLong()?.toString() ?: "")
    }
    var sellPriceText by remember {
        mutableStateOf(initialProduct?.sellPrice?.toLong()?.toString() ?: "")
    }
    var stockText by remember {
        mutableStateOf(initialProduct?.stock?.toString() ?: "10")
    }
    var minStockAlertText by remember {
        mutableStateOf(initialProduct?.minStockAlert?.toString() ?: "5")
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0
    val sellPrice = sellPriceText.toDoubleOrNull() ?: 0.0
    val marginProfit = sellPrice - buyPrice

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialProduct == null) "Tambah Produk Baru" else "Edit Produk & Stok",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Barcode Preview Box
                    if (barcode.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BarcodeVisual(barcode = barcode, showText = true)
                            }
                        }
                    }

                    // Product Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input"),
                        label = { Text("Nama Produk / Barang *") },
                        placeholder = { Text("Contoh: Minyak Goreng 2L") },
                        singleLine = true
                    )

                    // Barcode with regenerate button
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_barcode_input"),
                        label = { Text("Kode Barcode / QR *") },
                        placeholder = { Text("Contoh: 8998866100012") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { barcode = generateRandomBarcode() }) {
                                Icon(Icons.Default.Autorenew, contentDescription = "Acak Barcode")
                            }
                        }
                    )

                    // Category & Unit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("product_category_input"),
                            label = { Text("Kategori") },
                            placeholder = { Text("Sembako / Minuman") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("product_unit_input"),
                            label = { Text("Satuan") },
                            placeholder = { Text("Pcs/Bks") },
                            singleLine = true
                        )
                    }

                    if (availableCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableCategories.size) { index ->
                                val catName = availableCategories[index]
                                val isSelected = category.equals(catName, ignoreCase = true)
                                androidx.compose.material3.FilterChip(
                                    selected = isSelected,
                                    onClick = { category = catName },
                                    label = { Text(catName, fontSize = 11.sp) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // Stock & Low Stock Alert
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_stock_input"),
                            label = { Text("Jumlah Stok *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minStockAlertText,
                            onValueChange = { minStockAlertText = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_alert_input"),
                            label = { Text("Batas Peringatan *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    // Buy Price & Sell Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = buyPriceText,
                            onValueChange = { buyPriceText = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_buy_price_input"),
                            label = { Text("Harga Beli (Modal) *") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sellPriceText,
                            onValueChange = { sellPriceText = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_sell_price_input"),
                            label = { Text("Harga Jual *") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    // Profit Preview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (marginProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimasi Laba per Satuan:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Text(
                                text = Formatters.formatRupiah(marginProfit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (marginProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Delete (if editing), Save, Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (initialProduct != null && onDelete != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_product_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus Produk",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama produk wajib diisi"
                                return@Button
                            }
                            if (barcode.isBlank()) {
                                errorMessage = "Barcode / QR wajib diisi"
                                return@Button
                            }
                            val stock = stockText.toIntOrNull() ?: 0
                            val minAlert = minStockAlertText.toIntOrNull() ?: 5

                            val product = ProductEntity(
                                id = initialProduct?.id ?: 0L,
                                name = name.trim(),
                                barcode = barcode.trim(),
                                category = category.trim().ifEmpty { "Umum" },
                                unit = unit.trim().ifEmpty { "Pcs" },
                                stock = stock,
                                minStockAlert = minAlert,
                                buyPrice = buyPrice,
                                sellPrice = sellPrice,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(product)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("save_product_button")
                    ) {
                        Text("Simpan Produk")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && initialProduct != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Produk?") },
            text = { Text("Apakah Anda yakin ingin menghapus '${initialProduct.name}' dari katalog?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initialProduct)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun generateRandomBarcode(): String {
    val prefix = "899"
    val randomPart = (1000000000L + Random.nextLong(8999999999L)).toString()
    return prefix + randomPart.take(10)
}
