package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = false)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String,
    val category: String = "Umum",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Int = 0,
    val minStockAlert: Int = 5,
    val unit: String = "Pcs",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stock <= minStockAlert
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val cashPaid: Double,
    val changeAmount: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val itemCount: Int,
    val paymentMethod: String = "Tunai",
    val notes: String = ""
)

@Entity(
    tableName = "transaction_items",
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val barcode: String,
    val quantity: Int,
    val buyPrice: Double,
    val sellPrice: Double,
    val subtotal: Double
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Operasional",
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "store_info")
data class StoreInfoEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "TOKO MAKMUR",
    val storeAddress: String = "Jl. Kembang Kuning No. 17, Surabaya",
    val storePhone: String = "",
    val initialCapital: Double = 2500000.0,
    val receiptFooter: String = "Terima kasih telah berbelanja di Toko Makmur!"
)

@Entity(
    tableName = "catalogs",
    indices = [Index(value = ["name"], unique = true)]
)
data class CatalogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

