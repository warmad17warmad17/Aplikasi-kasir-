package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.CatalogEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StoreInfoEntity
import com.example.data.entity.TransactionEntity
import com.example.data.model.CartItem
import com.example.data.model.FinancialReport
import com.example.data.model.ReportPeriod
import com.example.data.model.TransactionDetail
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = PosRepository(db)

    // Store Info & Capital
    val storeInfo: StateFlow<StoreInfoEntity> = repository.storeInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreInfoEntity())

    // Products
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Catalogs
    val allCatalogs: StateFlow<List<CatalogEntity>> = repository.allCatalogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product counts per catalog (reactive)
    val catalogProductCounts: StateFlow<Map<String, Int>> = combine(allProducts, allCatalogs) { products, catalogs ->
        val counts = mutableMapOf<String, Int>()
        catalogs.forEach { counts[it.name] = 0 }
        products.forEach { p ->
            val cat = p.category
            counts[cat] = (counts[cat] ?: 0) + 1
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Combined catalog names (including any custom category in products)
    val allCatalogNames: StateFlow<List<String>> = combine(allCatalogs, allProducts) { catalogs, products ->
        val fromDb = catalogs.map { it.name }
        val fromProducts = products.map { it.category }.filter { it.isNotBlank() }
        (fromDb + fromProducts).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product Filter / Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _filterLowStockOnly = MutableStateFlow(false)
    val filterLowStockOnly: StateFlow<Boolean> = _filterLowStockOnly.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategory,
        _filterLowStockOnly
    ) { products, query, cat, lowStockOnly ->
        products.filter { p ->
            val matchQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true)
            val matchCategory = cat == "Semua" || p.category.equals(cat, ignoreCase = true)
            val matchLow = !lowStockOnly || p.isLowStock
            matchQuery && matchCategory && matchLow
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart (Kasir)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartTotal: StateFlow<Double> = _cartItems.combine(_cartItems) { items, _ ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> = _cartItems.combine(_cartItems) { items, _ ->
        items.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Cash Payment
    private val _cashInput = MutableStateFlow("")
    val cashInput: StateFlow<String> = _cashInput.asStateFlow()

    val cashAmount: StateFlow<Double> = _cashInput.combine(_cashInput) { text, _ ->
        text.toDoubleOrNull() ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val changeAmount: StateFlow<Double> = combine(cartTotal, cashAmount) { total, cash ->
        (cash - total).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Quick Nominal presets: 10000, 20000, 30000, 50000, 100000
    val nominalPresets = listOf(10000.0, 20000.0, 30000.0, 50000.0, 100000.0)

    // Transactions
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeReceipt = MutableStateFlow<TransactionDetail?>(null)
    val activeReceipt: StateFlow<TransactionDetail?> = _activeReceipt.asStateFlow()

    // Expenses
    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial Reports
    private val _reportPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    val currentReport: StateFlow<FinancialReport> = combine(
        _reportPeriod,
        allTransactions,
        allExpenses
    ) { period, txs, exps ->
        repository.getFinancialReport(period, txs, exps)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinancialReport(ReportPeriod.TODAY, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0)
    )

    // Overall Cash balance: Modal Awal + Total Penjualan - Total Pengeluaran
    val totalCashBalance: StateFlow<Double> = combine(
        storeInfo,
        allTransactions,
        allExpenses
    ) { info, txs, exps ->
        val totalSales = txs.sumOf { it.totalAmount }
        val totalExpenses = exps.sumOf { it.amount }
        info.initialCapital + totalSales - totalExpenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Inventory Valuation (Berdasarkan Harga Modal Produk)
    val totalInventoryCostValue: StateFlow<Double> = allProducts.map { products ->
        products.sumOf { (it.buyPrice * it.stock).coerceAtLeast(0.0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalInventorySalesValue: StateFlow<Double> = allProducts.map { products ->
        products.sumOf { (it.sellPrice * it.stock).coerceAtLeast(0.0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStockUnits: StateFlow<Int> = allProducts.map { products ->
        products.sumOf { it.stock.coerceAtLeast(0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Total Aset Usaha = Kas Toko + Nilai Modal Stok Produk
    val totalBusinessAsset: StateFlow<Double> = combine(
        totalCashBalance,
        totalInventoryCostValue
    ) { cash, inventoryCost ->
        cash + inventoryCost
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Nilai Modal Stok per Kategori/Katalog
    val inventoryCostByCategory: StateFlow<Map<String, Double>> = allProducts.map { products ->
        val map = mutableMapOf<String, Double>()
        products.forEach { p ->
            val cat = p.category
            val cost = (p.buyPrice * p.stock).coerceAtLeast(0.0)
            map[cat] = (map[cat] ?: 0.0) + cost
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ==========================================
    // CART & CASHIER ACTIONS
    // ==========================================

    fun addToCart(product: ProductEntity, qty: Int = 1) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + qty
            if (newQty <= product.stock) {
                current[index] = existing.copy(quantity = newQty)
            }
        } else {
            if (product.stock >= qty) {
                current.add(CartItem(product = product, quantity = qty))
            }
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: Long, qty: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (qty <= 0) {
                current.removeAt(index)
            } else {
                val item = current[index]
                if (qty <= item.product.stock) {
                    current[index] = item.copy(quantity = qty)
                }
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        val current = _cartItems.value.toMutableList()
        current.removeAll { it.product.id == productId }
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _cashInput.value = ""
    }

    fun scanOrInputBarcode(barcode: String, onResult: (Boolean, String) -> Unit) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) {
            onResult(false, "Kode barcode kosong")
            return
        }
        viewModelScope.launch {
            val product = repository.getProductByBarcode(trimmed)
            if (product != null) {
                if (product.stock <= 0) {
                    onResult(false, "Stok produk ${product.name} telah habis!")
                } else {
                    addToCart(product, 1)
                    onResult(true, "Ditambahkan: ${product.name}")
                }
            } else {
                onResult(false, "Produk dengan kode '$trimmed' tidak ditemukan di stok!")
            }
        }
    }

    fun setNominalPayment(nominal: Double) {
        _cashInput.value = nominal.toLong().toString()
    }

    fun updateCashInput(text: String) {
        // Filter numeric only
        val filtered = text.filter { it.isDigit() }
        _cashInput.value = filtered
    }

    fun setExactCashPayment() {
        _cashInput.value = cartTotal.value.toLong().toString()
    }

    fun checkout(
        notes: String = "",
        onSuccess: (TransactionDetail) -> Unit,
        onError: (String) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            onError("Keranjang belanja masih kosong!")
            return
        }
        val total = cartTotal.value
        val cash = cashAmount.value
        if (cash < total) {
            onError("Nominal pembayaran kurang Rp ${ (total - cash).toLong() }")
            return
        }

        viewModelScope.launch {
            try {
                val detail = repository.processCheckout(items, cash, notes)
                _activeReceipt.value = detail
                clearCart()
                onSuccess(detail)
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan saat memproses transaksi")
            }
        }
    }

    fun dismissReceipt() {
        _activeReceipt.value = null
    }

    fun viewReceipt(txId: Long) {
        viewModelScope.launch {
            val detail = repository.getTransactionDetail(txId)
            _activeReceipt.value = detail
        }
    }

    // ==========================================
    // CATALOG & PRODUCT MANAGEMENT
    // ==========================================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFilterLowStock(filter: Boolean) {
        _filterLowStockOnly.value = filter
    }

    fun saveProduct(product: ProductEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveProduct(product)
            onSaved()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            removeFromCart(product.id)
        }
    }

    // ==========================================
    // CATALOG / CATEGORY MANAGEMENT
    // ==========================================

    fun createCatalog(name: String, onResult: (success: Boolean, message: String) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            onResult(false, "Nama katalog tidak boleh kosong")
            return
        }
        viewModelScope.launch {
            val exists = allCatalogs.value.any { it.name.equals(trimmed, ignoreCase = true) }
            if (exists) {
                onResult(false, "Katalog '$trimmed' sudah ada!")
                return@launch
            }
            val id = repository.saveCatalog(trimmed)
            if (id > 0) {
                onResult(true, "Katalog '$trimmed' berhasil ditambahkan!")
            } else {
                onResult(false, "Gagal menambahkan katalog")
            }
        }
    }

    fun deleteCatalogIfEmpty(catalog: CatalogEntity, onResult: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            val count = repository.countProductsInCatalog(catalog.name)
            if (count > 0) {
                onResult(false, "Tidak dapat menghapus! Katalog '${catalog.name}' masih berisi $count produk.")
            } else {
                val deleted = repository.deleteCatalogIfEmpty(catalog)
                if (deleted) {
                    if (_selectedCategory.value.equals(catalog.name, ignoreCase = true)) {
                        _selectedCategory.value = "Semua"
                    }
                    onResult(true, "Katalog '${catalog.name}' yang kosong berhasil dihapus.")
                } else {
                    onResult(false, "Gagal menghapus katalog.")
                }
            }
        }
    }

    fun deleteCatalogByNameIfEmpty(catalogName: String, onResult: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            val count = repository.countProductsInCatalog(catalogName)
            if (count > 0) {
                onResult(false, "Tidak dapat menghapus! Katalog '$catalogName' masih berisi $count produk.")
            } else {
                val catalog = allCatalogs.value.find { it.name.equals(catalogName, ignoreCase = true) }
                if (catalog != null) {
                    val deleted = repository.deleteCatalogIfEmpty(catalog)
                    if (deleted) {
                        if (_selectedCategory.value.equals(catalogName, ignoreCase = true)) {
                            _selectedCategory.value = "Semua"
                        }
                        onResult(true, "Katalog '$catalogName' yang kosong berhasil dihapus.")
                    } else {
                        onResult(false, "Gagal menghapus katalog.")
                    }
                } else {
                    onResult(false, "Katalog tidak ditemukan.")
                }
            }
        }
    }

    // ==========================================
    // EXPENSE MANAGEMENT
    // ==========================================

    fun saveExpense(expense: ExpenseEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveExpense(expense)
            onSaved()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // ==========================================
    // STORE INFO & CAPITAL MANAGEMENT
    // ==========================================

    fun updateInitialCapital(amount: Double) {
        viewModelScope.launch {
            val current = storeInfo.value
            repository.updateStoreInfo(current.copy(initialCapital = amount))
        }
    }

    fun updateStoreInfo(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        initialCapital: Double,
        receiptFooter: String
    ) {
        viewModelScope.launch {
            val updated = StoreInfoEntity(
                id = 1,
                storeName = storeName,
                storeAddress = storeAddress,
                storePhone = storePhone,
                initialCapital = initialCapital,
                receiptFooter = receiptFooter
            )
            repository.updateStoreInfo(updated)
        }
    }

    // ==========================================
    // REPORT PERIOD SELECTION
    // ==========================================

    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    // ==========================================
    // BACKUP & RESTORE DATA
    // ==========================================

    fun exportBackupToUri(
        uri: android.net.Uri,
        context: android.content.Context,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val backupData = repository.createBackupData()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    com.example.util.BackupManager.writeToOutputStream(backupData, os)
                } ?: throw java.io.IOException("Gagal membuka ruang penyimpanan untuk menulis file cadangan")
                onResult(true, "Cadangan data berhasil disimpan ke penyimpanan lokal!")
            } catch (e: Exception) {
                onResult(false, "Gagal mencadangkan data: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun shareBackupFile(
        context: android.content.Context,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val backupData = repository.createBackupData()
                val file = com.example.util.BackupManager.createCacheBackupFile(context, backupData)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(
                        android.content.Intent.EXTRA_SUBJECT,
                        "Cadangan Data ${storeInfo.value.storeName} - ${com.example.util.Formatters.formatDateOnly(System.currentTimeMillis())}"
                    )
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        "File cadangan data aplikasi ${storeInfo.value.storeName}. Simpan atau kirim file ini ke HP baru untuk memulihkan seluruh data produk, transaksi, dan riwayat."
                    )
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = android.content.Intent.createChooser(shareIntent, "Kirim / Bagikan File Cadangan").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                onResult(true, "Menu kirim file cadangan siap digunakan")
            } catch (e: Exception) {
                onResult(false, "Gagal membagikan cadangan: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun inspectBackupFromUri(
        uri: android.net.Uri,
        context: android.content.Context,
        onResult: (success: Boolean, summary: com.example.data.model.BackupSummary?, message: String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    com.example.util.BackupManager.readFromInputStream(inputStream)
                } ?: throw java.io.IOException("Tidak dapat membaca file cadangan")

                val summary = com.example.util.BackupManager.peekBackupSummary(jsonString)
                onResult(true, summary, "File cadangan valid")
            } catch (e: Exception) {
                onResult(false, null, "File tidak valid atau format rusak: ${e.localizedMessage ?: "Gagal membaca file"}")
            }
        }
    }

    fun restoreBackupFromUri(
        uri: android.net.Uri,
        context: android.content.Context,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    com.example.util.BackupManager.readFromInputStream(inputStream)
                } ?: throw java.io.IOException("Tidak dapat membaca file cadangan")

                val backupData = com.example.util.BackupManager.parseBackupJson(jsonString)
                repository.restoreBackupData(backupData)
                _selectedCategory.value = "Semua"
                onResult(
                    true,
                    "Data berhasil dipulihkan! ${backupData.products.size} produk, ${backupData.transactions.size} transaksi telah dimuat."
                )
            } catch (e: Exception) {
                onResult(false, "Gagal memulihkan data: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }
}
