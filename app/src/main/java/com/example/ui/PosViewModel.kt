package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
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
}
