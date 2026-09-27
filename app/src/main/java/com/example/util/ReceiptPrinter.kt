package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.entity.StoreInfoEntity
import com.example.data.model.TransactionDetail

object ReceiptPrinter {

    fun generateReceiptText(tx: TransactionDetail, storeInfo: StoreInfoEntity): String {
        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine(storeInfo.storeName.uppercase())
        sb.appendLine(storeInfo.storeAddress)
        if (storeInfo.storePhone.isNotBlank()) {
            sb.appendLine("Telp: ${storeInfo.storePhone}")
        }
        sb.appendLine("================================")
        sb.appendLine("No. Struk : ${tx.invoiceNumber}")
        sb.appendLine("Waktu     : ${Formatters.formatDateTime(tx.timestamp)}")
        sb.appendLine("Kasir     : Kasir Toko")
        sb.appendLine("Metode    : ${tx.paymentMethod}")
        sb.appendLine("--------------------------------")
        for (item in tx.items) {
            sb.appendLine(item.productName)
            val qtyPrice = "  ${item.quantity} x ${Formatters.formatNumber(item.sellPrice)}"
            val subtotal = Formatters.formatRupiah(item.subtotal)
            val padding = 32 - qtyPrice.length - subtotal.length
            val spaces = " ".repeat(padding.coerceAtLeast(1))
            sb.appendLine("$qtyPrice$spaces$subtotal")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine(formatTwoColumns("TOTAL BELANJA:", Formatters.formatRupiah(tx.totalAmount)))
        sb.appendLine(formatTwoColumns("TUNAI DIBAYAR:", Formatters.formatRupiah(tx.cashPaid)))
        sb.appendLine(formatTwoColumns("KEMBALIAN    :", Formatters.formatRupiah(tx.changeAmount)))
        sb.appendLine("================================")
        sb.appendLine(storeInfo.receiptFooter)
        sb.appendLine("Simpan struk ini sebagai")
        sb.appendLine("bukti pembayaran sah.")
        sb.appendLine("================================")
        return sb.toString()
    }

    private fun formatTwoColumns(col1: String, col2: String): String {
        val padding = 32 - col1.length - col2.length
        val spaces = " ".repeat(padding.coerceAtLeast(1))
        return "$col1$spaces$col2"
    }

    fun shareReceipt(context: Context, tx: TransactionDetail, storeInfo: StoreInfoEntity) {
        val receiptText = generateReceiptText(tx, storeInfo)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Struk Belanja")
        context.startActivity(shareIntent)
    }

    fun printReceipt(context: Context, tx: TransactionDetail, storeInfo: StoreInfoEntity) {
        val itemsHtml = StringBuilder()
        for (item in tx.items) {
            itemsHtml.append(
                """
                <tr>
                    <td colspan="2" style="font-weight:bold; padding-top:4px;">${item.productName}</td>
                </tr>
                <tr>
                    <td style="color:#555;">${item.quantity} x ${Formatters.formatRupiah(item.sellPrice)}</td>
                    <td style="text-align:right;">${Formatters.formatRupiah(item.subtotal)}</td>
                </tr>
                """.trimIndent()
            )
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 13px;
                        margin: 0;
                        padding: 10px;
                        width: 280px;
                        color: #000;
                    }
                    .center { text-align: center; }
                    .bold { font-weight: bold; }
                    .divider { border-top: 1px dashed #000; margin: 8px 0; }
                    .double-divider { border-top: 2px solid #000; margin: 8px 0; }
                    table { width: 100%; border-collapse: collapse; }
                    td { font-size: 12px; }
                    .right { text-align: right; }
                </style>
            </head>
            <body>
                <div class="center bold" style="font-size: 16px;">${storeInfo.storeName}</div>
                <div class="center">${storeInfo.storeAddress}</div>
                ${if (storeInfo.storePhone.isNotBlank()) "<div class=\"center\">Telp: ${storeInfo.storePhone}</div>" else ""}
                <div class="double-divider"></div>
                <div>No: ${tx.invoiceNumber}</div>
                <div>Tgl: ${Formatters.formatDateTime(tx.timestamp)}</div>
                <div>Kasir: Kasir Toko</div>
                <div class="divider"></div>
                <table>
                    $itemsHtml
                </table>
                <div class="divider"></div>
                <table>
                    <tr class="bold">
                        <td>TOTAL</td>
                        <td class="right">${Formatters.formatRupiah(tx.totalAmount)}</td>
                    </tr>
                    <tr>
                        <td>TUNAI</td>
                        <td class="right">${Formatters.formatRupiah(tx.cashPaid)}</td>
                    </tr>
                    <tr class="bold">
                        <td>KEMBALIAN</td>
                        <td class="right">${Formatters.formatRupiah(tx.changeAmount)}</td>
                    </tr>
                </table>
                <div class="double-divider"></div>
                <div class="center">${storeInfo.receiptFooter}</div>
                <div class="center" style="font-size: 10px; margin-top: 5px;">Terima Kasih</div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Struk-${tx.invoiceNumber}")
                printManager?.print(
                    "Struk-${tx.invoiceNumber}",
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
