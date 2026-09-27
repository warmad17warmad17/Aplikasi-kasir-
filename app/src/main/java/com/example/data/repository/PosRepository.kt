package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StoreInfoEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import com.example.data.model.CartItem
import com.example.data.model.CartItemSummary
import com.example.data.model.FinancialReport
import com.example.data.model.ReportPeriod
import com.example.data.model.TransactionDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PosRepository(private val db: AppDatabase) {
    private val productDao = db.productDao()
    private val transactionDao = db.transactionDao()
    private val expenseDao = db.expenseDao()
    private val storeInfoDao = db.storeInfoDao()
    private val catalogDao = db.catalogDao()

    // Catalogs
    val allCatalogs: Flow<List<com.example.data.entity.CatalogEntity>> = catalogDao.getAllCatalogs()

    suspend fun saveCatalog(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return -1L
        return catalogDao.insert(com.example.data.entity.CatalogEntity(name = trimmed))
    }

    suspend fun deleteCatalogIfEmpty(catalog: com.example.data.entity.CatalogEntity): Boolean {
        val count = catalogDao.countProductsInCatalog(catalog.name)
        if (count == 0) {
            catalogDao.delete(catalog)
            return true
        }
        return false
    }

    suspend fun countProductsInCatalog(catalogName: String): Int {
        return catalogDao.countProductsInCatalog(catalogName)
    }

    // Products
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode.trim())
    }

    suspend fun saveProduct(product: ProductEntity): Long {
        return if (product.id == 0L) {
            productDao.insert(product)
        } else {
            productDao.update(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.delete(product)
    }

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun getTransactionDetail(txId: Long): TransactionDetail? {
        val tx = transactionDao.getTransactionById(txId) ?: return null
        val items = transactionDao.getItemsForTransactionSync(txId)
        val summaries = items.map {
            CartItemSummary(
                productName = it.productName,
                barcode = it.barcode,
                quantity = it.quantity,
                buyPrice = it.buyPrice,
                sellPrice = it.sellPrice,
                subtotal = it.subtotal
            )
        }
        return TransactionDetail(
            id = tx.id,
            invoiceNumber = tx.invoiceNumber,
            timestamp = tx.timestamp,
            totalAmount = tx.totalAmount,
            cashPaid = tx.cashPaid,
            changeAmount = tx.changeAmount,
            totalCost = tx.totalCost,
            totalProfit = tx.totalProfit,
            itemCount = tx.itemCount,
            paymentMethod = tx.paymentMethod,
            notes = tx.notes,
            items = summaries
        )
    }

    suspend fun processCheckout(
        cartItems: List<CartItem>,
        cashPaid: Double,
        notes: String = ""
    ): TransactionDetail {
        val totalAmount = cartItems.sumOf { it.subtotal }
        val totalCost = cartItems.sumOf { it.totalCost }
        val totalProfit = totalAmount - totalCost
        val changeAmount = (cashPaid - totalAmount).coerceAtLeast(0.0)
        val itemCount = cartItems.sumOf { it.quantity }

        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        val invoice = "TRX-${dateFormat.format(Date())}"

        val tx = TransactionEntity(
            invoiceNumber = invoice,
            timestamp = System.currentTimeMillis(),
            totalAmount = totalAmount,
            cashPaid = cashPaid,
            changeAmount = changeAmount,
            totalCost = totalCost,
            totalProfit = totalProfit,
            itemCount = itemCount,
            paymentMethod = "Tunai",
            notes = notes
        )

        val txId = transactionDao.insertTransaction(tx)

        val txItems = cartItems.map {
            TransactionItemEntity(
                transactionId = txId,
                productId = it.product.id,
                productName = it.product.name,
                barcode = it.product.barcode,
                quantity = it.quantity,
                buyPrice = it.product.buyPrice,
                sellPrice = it.product.sellPrice,
                subtotal = it.subtotal
            )
        }
        transactionDao.insertItems(txItems)

        // Decrease stock for each product
        for (item in cartItems) {
            productDao.decreaseStock(item.product.id, item.quantity)
        }

        val summaries = txItems.map {
            CartItemSummary(
                productName = it.productName,
                barcode = it.barcode,
                quantity = it.quantity,
                buyPrice = it.buyPrice,
                sellPrice = it.sellPrice,
                subtotal = it.subtotal
            )
        }

        return TransactionDetail(
            id = txId,
            invoiceNumber = invoice,
            timestamp = tx.timestamp,
            totalAmount = totalAmount,
            cashPaid = cashPaid,
            changeAmount = changeAmount,
            totalCost = totalCost,
            totalProfit = totalProfit,
            itemCount = itemCount,
            paymentMethod = tx.paymentMethod,
            notes = tx.notes,
            items = summaries
        )
    }

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun saveExpense(expense: ExpenseEntity): Long {
        return if (expense.id == 0L) {
            expenseDao.insert(expense)
        } else {
            expenseDao.update(expense)
            expense.id
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.delete(expense)
    }

    // Store Info & Capital
    val storeInfo: Flow<StoreInfoEntity> = storeInfoDao.getStoreInfo().map {
        it ?: StoreInfoEntity()
    }

    suspend fun updateStoreInfo(info: StoreInfoEntity) {
        storeInfoDao.insertOrUpdate(info)
    }

    // Reporting
    fun getFinancialReport(
        period: ReportPeriod,
        transactions: List<TransactionEntity>,
        expenses: List<ExpenseEntity>
    ): FinancialReport {
        val (startTime, endTime) = getPeriodTimeRange(period)
        val filteredTx = transactions.filter { it.timestamp in startTime..endTime }
        val filteredExp = expenses.filter { it.date in startTime..endTime }

        val grossRevenue = filteredTx.sumOf { it.totalAmount }
        val totalHpp = filteredTx.sumOf { it.totalCost }
        val grossProfit = grossRevenue - totalHpp
        val totalExpenses = filteredExp.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses
        val itemsSold = filteredTx.sumOf { it.itemCount }

        return FinancialReport(
            period = period,
            grossRevenue = grossRevenue,
            totalHpp = totalHpp,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            transactionCount = filteredTx.size,
            itemsSoldCount = itemsSold
        )
    }

    private fun getPeriodTimeRange(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return when (period) {
            ReportPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                Pair(start, now)
            }
            ReportPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                Pair(start, now)
            }
            ReportPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                Pair(start, now)
            }
            ReportPeriod.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
        }
    }

    // ==========================================
    // BACKUP & RESTORE DATA
    // ==========================================

    suspend fun createBackupData(): com.example.data.model.BackupData {
        val storeInfo = storeInfoDao.getStoreInfoSync() ?: StoreInfoEntity()
        val catalogs = catalogDao.getAllCatalogsSync()
        val products = productDao.getAllProductsSync()
        val transactions = transactionDao.getAllTransactionsSync()
        val transactionItems = transactionDao.getAllTransactionItemsSync()
        val expenses = expenseDao.getAllExpensesSync()

        return com.example.data.model.BackupData(
            storeInfo = storeInfo,
            catalogs = catalogs,
            products = products,
            transactions = transactions,
            transactionItems = transactionItems,
            expenses = expenses
        )
    }

    suspend fun restoreBackupData(backupData: com.example.data.model.BackupData) {
        db.withTransaction {
            // 1. Clear existing database records
            productDao.deleteAllProducts()
            transactionDao.deleteAllTransactions()
            transactionDao.deleteAllTransactionItems()
            expenseDao.deleteAllExpenses()
            catalogDao.deleteAllCatalogs()

            // 2. Restore Store Info
            storeInfoDao.insertOrUpdate(backupData.storeInfo)

            // 3. Restore Catalogs
            if (backupData.catalogs.isNotEmpty()) {
                catalogDao.insertAll(backupData.catalogs)
            }

            // 4. Restore Products
            if (backupData.products.isNotEmpty()) {
                productDao.insertAll(backupData.products)
            }

            // 5. Restore Transactions
            if (backupData.transactions.isNotEmpty()) {
                transactionDao.insertTransactions(backupData.transactions)
            }

            // 6. Restore Transaction Items
            if (backupData.transactionItems.isNotEmpty()) {
                transactionDao.insertItems(backupData.transactionItems)
            }

            // 7. Restore Expenses
            if (backupData.expenses.isNotEmpty()) {
                expenseDao.insertAll(backupData.expenses)
            }
        }
    }
}
