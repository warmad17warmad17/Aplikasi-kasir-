package com.example.data.model

import com.example.data.entity.ProductEntity

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = product.sellPrice * quantity

    val totalCost: Double
        get() = product.buyPrice * quantity

    val profit: Double
        get() = subtotal - totalCost
}

data class TransactionDetail(
    val id: Long,
    val invoiceNumber: String,
    val timestamp: Long,
    val totalAmount: Double,
    val cashPaid: Double,
    val changeAmount: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val itemCount: Int,
    val paymentMethod: String,
    val notes: String,
    val items: List<CartItemSummary>
)

data class CartItemSummary(
    val productName: String,
    val barcode: String,
    val quantity: Int,
    val buyPrice: Double,
    val sellPrice: Double,
    val subtotal: Double
)

enum class ReportPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME
}

data class FinancialReport(
    val period: ReportPeriod,
    val grossRevenue: Double,    // Total Penjualan (Pendapatan Kotor)
    val totalHpp: Double,        // Harga Pokok Penjualan (Modal Barang Terjual)
    val grossProfit: Double,     // grossRevenue - totalHpp
    val totalExpenses: Double,   // Total Pengeluaran Toko
    val netProfit: Double,       // grossProfit - totalExpenses (Pendapatan Bersih)
    val transactionCount: Int,
    val itemsSoldCount: Int
)
