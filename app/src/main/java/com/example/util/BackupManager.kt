package com.example.util

import android.content.Context
import com.example.data.entity.CatalogEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StoreInfoEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import com.example.data.model.BackupData
import com.example.data.model.BackupSummary
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val APP_IDENTIFIER = "TOKO_MAKMUR_POS"
    private const val FORMAT_VERSION = 1

    fun exportToJson(backupData: BackupData): String {
        val root = JSONObject()
        root.put("formatVersion", FORMAT_VERSION)
        root.put("appIdentifier", APP_IDENTIFIER)
        root.put("backupTimestamp", backupData.backupTimestamp)
        root.put("backupDateFormatted", Formatters.formatDateTime(backupData.backupTimestamp))

        // Store Info
        val storeObj = JSONObject()
        storeObj.put("id", backupData.storeInfo.id)
        storeObj.put("storeName", backupData.storeInfo.storeName)
        storeObj.put("storeAddress", backupData.storeInfo.storeAddress)
        storeObj.put("storePhone", backupData.storeInfo.storePhone)
        storeObj.put("initialCapital", backupData.storeInfo.initialCapital)
        storeObj.put("receiptFooter", backupData.storeInfo.receiptFooter)
        root.put("storeInfo", storeObj)

        // Catalogs
        val catalogsArray = JSONArray()
        backupData.catalogs.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("createdAt", cat.createdAt)
            catalogsArray.put(obj)
        }
        root.put("catalogs", catalogsArray)

        // Products
        val productsArray = JSONArray()
        backupData.products.forEach { prod ->
            val obj = JSONObject()
            obj.put("id", prod.id)
            obj.put("name", prod.name)
            obj.put("barcode", prod.barcode)
            obj.put("category", prod.category)
            obj.put("buyPrice", prod.buyPrice)
            obj.put("sellPrice", prod.sellPrice)
            obj.put("stock", prod.stock)
            obj.put("minStockAlert", prod.minStockAlert)
            obj.put("unit", prod.unit)
            obj.put("updatedAt", prod.updatedAt)
            productsArray.put(obj)
        }
        root.put("products", productsArray)

        // Transactions
        val transactionsArray = JSONArray()
        backupData.transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("invoiceNumber", tx.invoiceNumber)
            obj.put("timestamp", tx.timestamp)
            obj.put("totalAmount", tx.totalAmount)
            obj.put("cashPaid", tx.cashPaid)
            obj.put("changeAmount", tx.changeAmount)
            obj.put("totalCost", tx.totalCost)
            obj.put("totalProfit", tx.totalProfit)
            obj.put("itemCount", tx.itemCount)
            obj.put("paymentMethod", tx.paymentMethod)
            obj.put("notes", tx.notes)
            transactionsArray.put(obj)
        }
        root.put("transactions", transactionsArray)

        // Transaction Items
        val itemsArray = JSONArray()
        backupData.transactionItems.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("transactionId", item.transactionId)
            obj.put("productId", item.productId)
            obj.put("productName", item.productName)
            obj.put("barcode", item.barcode)
            obj.put("quantity", item.quantity)
            obj.put("buyPrice", item.buyPrice)
            obj.put("sellPrice", item.sellPrice)
            obj.put("subtotal", item.subtotal)
            itemsArray.put(obj)
        }
        root.put("transactionItems", itemsArray)

        // Expenses
        val expensesArray = JSONArray()
        backupData.expenses.forEach { exp ->
            val obj = JSONObject()
            obj.put("id", exp.id)
            obj.put("title", exp.title)
            obj.put("category", exp.category)
            obj.put("amount", exp.amount)
            obj.put("date", exp.date)
            obj.put("note", exp.note)
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)

        val formatVersion = root.optInt("formatVersion", 1)
        val appIdentifier = root.optString("appIdentifier", "")
        val backupTimestamp = root.optLong("backupTimestamp", System.currentTimeMillis())

        // Check compatibility
        if (appIdentifier.isNotEmpty() && !appIdentifier.contains("POS", ignoreCase = true) && !appIdentifier.contains("TOKO", ignoreCase = true)) {
            throw IllegalArgumentException("File ini bukan file cadangan aplikasi Toko Makmur POS yang valid.")
        }

        // Store Info
        val storeObj = root.optJSONObject("storeInfo")
        val storeInfo = if (storeObj != null) {
            StoreInfoEntity(
                id = storeObj.optInt("id", 1),
                storeName = storeObj.optString("storeName", "TOKO MAKMUR"),
                storeAddress = storeObj.optString("storeAddress", "Jl. Kembang Kuning No. 17, Surabaya"),
                storePhone = storeObj.optString("storePhone", ""),
                initialCapital = storeObj.optDouble("initialCapital", 2500000.0),
                receiptFooter = storeObj.optString("receiptFooter", "Terima kasih telah berbelanja di Toko Makmur!")
            )
        } else {
            StoreInfoEntity()
        }

        // Catalogs
        val catalogs = mutableListOf<CatalogEntity>()
        val catalogsArray = root.optJSONArray("catalogs")
        if (catalogsArray != null) {
            for (i in 0 until catalogsArray.length()) {
                val obj = catalogsArray.getJSONObject(i)
                catalogs.add(
                    CatalogEntity(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", "Umum"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Products
        val products = mutableListOf<ProductEntity>()
        val productsArray = root.optJSONArray("products")
        if (productsArray != null) {
            for (i in 0 until productsArray.length()) {
                val obj = productsArray.getJSONObject(i)
                products.add(
                    ProductEntity(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", "Produk"),
                        barcode = obj.optString("barcode", ""),
                        category = obj.optString("category", "Umum"),
                        buyPrice = obj.optDouble("buyPrice", 0.0),
                        sellPrice = obj.optDouble("sellPrice", 0.0),
                        stock = obj.optInt("stock", 0),
                        minStockAlert = obj.optInt("minStockAlert", 5),
                        unit = obj.optString("unit", "Pcs"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Transactions
        val transactions = mutableListOf<TransactionEntity>()
        val transactionsArray = root.optJSONArray("transactions")
        if (transactionsArray != null) {
            for (i in 0 until transactionsArray.length()) {
                val obj = transactionsArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = obj.optLong("id", 0),
                        invoiceNumber = obj.optString("invoiceNumber", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        totalAmount = obj.optDouble("totalAmount", 0.0),
                        cashPaid = obj.optDouble("cashPaid", 0.0),
                        changeAmount = obj.optDouble("changeAmount", 0.0),
                        totalCost = obj.optDouble("totalCost", 0.0),
                        totalProfit = obj.optDouble("totalProfit", 0.0),
                        itemCount = obj.optInt("itemCount", 0),
                        paymentMethod = obj.optString("paymentMethod", "Tunai"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        // Transaction Items
        val transactionItems = mutableListOf<TransactionItemEntity>()
        val itemsArray = root.optJSONArray("transactionItems")
        if (itemsArray != null) {
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                transactionItems.add(
                    TransactionItemEntity(
                        id = obj.optLong("id", 0),
                        transactionId = obj.optLong("transactionId", 0),
                        productId = obj.optLong("productId", 0),
                        productName = obj.optString("productName", ""),
                        barcode = obj.optString("barcode", ""),
                        quantity = obj.optInt("quantity", 0),
                        buyPrice = obj.optDouble("buyPrice", 0.0),
                        sellPrice = obj.optDouble("sellPrice", 0.0),
                        subtotal = obj.optDouble("subtotal", 0.0)
                    )
                )
            }
        }

        // Expenses
        val expenses = mutableListOf<ExpenseEntity>()
        val expensesArray = root.optJSONArray("expenses")
        if (expensesArray != null) {
            for (i in 0 until expensesArray.length()) {
                val obj = expensesArray.getJSONObject(i)
                expenses.add(
                    ExpenseEntity(
                        id = obj.optLong("id", 0),
                        title = obj.optString("title", ""),
                        category = obj.optString("category", "Operasional"),
                        amount = obj.optDouble("amount", 0.0),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        note = obj.optString("note", "")
                    )
                )
            }
        }

        return BackupData(
            formatVersion = formatVersion,
            appIdentifier = appIdentifier,
            backupTimestamp = backupTimestamp,
            storeInfo = storeInfo,
            catalogs = catalogs,
            products = products,
            transactions = transactions,
            transactionItems = transactionItems,
            expenses = expenses
        )
    }

    fun peekBackupSummary(jsonString: String): BackupSummary {
        val root = JSONObject(jsonString)
        val formatVersion = root.optInt("formatVersion", 1)
        val appIdentifier = root.optString("appIdentifier", "TOKO_MAKMUR_POS")
        val backupTimestamp = root.optLong("backupTimestamp", System.currentTimeMillis())

        val storeObj = root.optJSONObject("storeInfo")
        val storeName = storeObj?.optString("storeName") ?: "Toko Makmur"
        val storeAddress = storeObj?.optString("storeAddress") ?: "-"

        val productCount = root.optJSONArray("products")?.length() ?: 0
        val catalogCount = root.optJSONArray("catalogs")?.length() ?: 0
        val transactionCount = root.optJSONArray("transactions")?.length() ?: 0
        val expenseCount = root.optJSONArray("expenses")?.length() ?: 0

        return BackupSummary(
            formatVersion = formatVersion,
            appIdentifier = appIdentifier,
            backupTimestamp = backupTimestamp,
            storeName = storeName,
            storeAddress = storeAddress,
            productCount = productCount,
            catalogCount = catalogCount,
            transactionCount = transactionCount,
            expenseCount = expenseCount
        )
    }

    fun writeToOutputStream(backupData: BackupData, outputStream: OutputStream) {
        val jsonString = exportToJson(backupData)
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            writer.write(jsonString)
            writer.flush()
        }
    }

    fun readFromInputStream(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    fun generateBackupFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "Backup_TokoMakmur_${sdf.format(Date())}.json"
    }

    fun createCacheBackupFile(context: Context, backupData: BackupData): File {
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val file = File(backupDir, generateBackupFileName())
        file.outputStream().use { os ->
            writeToOutputStream(backupData, os)
        }
        return file
    }
}
