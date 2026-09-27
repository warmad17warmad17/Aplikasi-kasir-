package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(indonesianLocale)
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    fun formatNumber(amount: Double): String {
        val format = NumberFormat.getNumberInstance(indonesianLocale)
        format.maximumFractionDigits = 0
        return format.format(amount)
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", indonesianLocale)
        return sdf.format(Date(timestamp))
    }
}
