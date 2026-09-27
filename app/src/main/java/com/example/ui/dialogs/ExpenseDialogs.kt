package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.StoreInfoEntity

@Composable
fun EditExpenseDialog(
    initialExpense: ExpenseEntity? = null,
    onSave: (ExpenseEntity) -> Unit,
    onDelete: ((ExpenseEntity) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var category by remember { mutableStateOf(initialExpense?.category ?: "Operasional") }
    var amountText by remember {
        mutableStateOf(initialExpense?.amount?.toLong()?.toString() ?: "")
    }
    var note by remember { mutableStateOf(initialExpense?.note ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialExpense == null) "Catat Pengeluaran Baru" else "Edit Pengeluaran",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input"),
                    label = { Text("Nama Pengeluaran *") },
                    placeholder = { Text("Contoh: Token Listrik, Plastik Toko, Air") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_category_input"),
                    label = { Text("Kategori Pengeluaran") },
                    placeholder = { Text("Operasional / Utilitas / Perlengkapan / Gaji") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    label = { Text("Nominal Pengeluaran (Rp) *") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_note_input"),
                    label = { Text("Catatan / Keterangan (Opsional)") },
                    maxLines = 3
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (initialExpense != null && onDelete != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_expense_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus Pengeluaran",
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
                            if (title.isBlank()) {
                                errorMessage = "Nama pengeluaran tidak boleh kosong"
                                return@Button
                            }
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount <= 0) {
                                errorMessage = "Nominal pengeluaran harus lebih dari 0"
                                return@Button
                            }

                            val exp = ExpenseEntity(
                                id = initialExpense?.id ?: 0L,
                                title = title.trim(),
                                category = category.trim().ifEmpty { "Operasional" },
                                amount = amount,
                                date = initialExpense?.date ?: System.currentTimeMillis(),
                                note = note.trim()
                            )
                            onSave(exp)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("save_expense_button")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && initialExpense != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Pengeluaran?") },
            text = { Text("Apakah Anda yakin ingin menghapus '${initialExpense.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initialExpense)
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

@Composable
fun EditCapitalDialog(
    currentCapital: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var capitalText by remember {
        mutableStateOf(currentCapital.toLong().toString())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Modal Toko",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Masukkan jumlah modal awal atau modal kas toko yang dialokasikan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = capitalText,
                    onValueChange = { capitalText = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capital_input"),
                    label = { Text("Jumlah Modal Kas Toko (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val newCap = capitalText.toDoubleOrNull() ?: 0.0
                            onSave(newCap)
                        },
                        modifier = Modifier.testTag("save_capital_button")
                    ) {
                        Text("Simpan Modal")
                    }
                }
            }
        }
    }
}

@Composable
fun EditStoreProfileDialog(
    currentInfo: StoreInfoEntity,
    onSave: (name: String, address: String, phone: String, footer: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentInfo.storeName) }
    var address by remember { mutableStateOf(currentInfo.storeAddress) }
    var phone by remember { mutableStateOf(currentInfo.storePhone) }
    var footer by remember { mutableStateOf(currentInfo.receiptFooter) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Profil Toko & Struk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_name_input"),
                    label = { Text("Nama Toko") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Alamat Toko") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nomor Telepon Toko") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = footer,
                    onValueChange = { footer = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pesan Kaki Struk (Footer)") },
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            onSave(name.trim(), address.trim(), phone.trim(), footer.trim())
                        },
                        modifier = Modifier.testTag("save_store_profile_button")
                    ) {
                        Text("Simpan Profil")
                    }
                }
            }
        }
    }
}
