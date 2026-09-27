package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ReportPeriod
import com.example.ui.PosViewModel
import com.example.util.Formatters

@Composable
fun ReportsScreen(viewModel: PosViewModel) {
    val report by viewModel.currentReport.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.reportPeriod.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Period Tabs Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Laporan Laba & Rugi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Period Filter: Hari Ini, Minggu Ini, Bulan Ini, Semua
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodFilterButton(
                        title = "Hari Ini",
                        isSelected = selectedPeriod == ReportPeriod.TODAY,
                        onClick = { viewModel.setReportPeriod(ReportPeriod.TODAY) },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodFilterButton(
                        title = "Minggu Ini",
                        isSelected = selectedPeriod == ReportPeriod.THIS_WEEK,
                        onClick = { viewModel.setReportPeriod(ReportPeriod.THIS_WEEK) },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodFilterButton(
                        title = "Bulan Ini",
                        isSelected = selectedPeriod == ReportPeriod.THIS_MONTH,
                        onClick = { viewModel.setReportPeriod(ReportPeriod.THIS_MONTH) },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodFilterButton(
                        title = "Semua",
                        isSelected = selectedPeriod == ReportPeriod.ALL_TIME,
                        onClick = { viewModel.setReportPeriod(ReportPeriod.ALL_TIME) },
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }

        // Scrollable Report Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Net Profit Hero Banner (Pendapatan Bersih)
            val isNetProfitPositive = report.netProfit >= 0
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_net_profit_card"),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isNetProfitPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PENDAPATAN BERSIH (NET PROFIT)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetProfitPositive) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            )
                            Text(
                                text = "Laba Kotor dikurangi Biaya Pengeluaran",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                        }

                        Icon(
                            imageVector = if (isNetProfitPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isNetProfitPositive) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = Formatters.formatRupiah(report.netProfit),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isNetProfitPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    )
                }
            }

            // Detailed Financial Breakdown Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rincian Pendapatan & Biaya",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Pendapatan Kotor (Gross Revenue)
                    MetricItem(
                        icon = Icons.Default.MonetizationOn,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "1. Pendapatan Kotor (Omzet)",
                        subtitle = "Total penjualan yang berhasil dibayar",
                        value = Formatters.formatRupiah(report.grossRevenue),
                        valueColor = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // 2. Harga Pokok Penjualan (HPP / Modal Beli Barang Terjual)
                    MetricItem(
                        icon = Icons.Default.ShoppingBag,
                        iconTint = Color(0xFFE65100),
                        title = "2. Modal Barang Terjual (HPP)",
                        subtitle = "Total harga beli produk yang laku",
                        value = Formatters.formatRupiah(report.totalHpp),
                        valueColor = Color(0xFFBF360C)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // 3. Laba Kotor Penjualan (Gross Profit)
                    MetricItem(
                        icon = Icons.Default.TrendingUp,
                        iconTint = Color(0xFF00897B),
                        title = "3. Laba Kotor Penjualan",
                        subtitle = "Pendapatan Kotor - Modal Barang",
                        value = Formatters.formatRupiah(report.grossProfit),
                        valueColor = Color(0xFF00695C)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // 4. Total Pengeluaran Toko (Expenses)
                    MetricItem(
                        icon = Icons.Default.TrendingDown,
                        iconTint = Color(0xFFD32F2F),
                        title = "4. Total Pengeluaran Toko",
                        subtitle = "Biaya operasional toko pada periode ini",
                        value = "- ${Formatters.formatRupiah(report.totalExpenses)}",
                        valueColor = Color(0xFFD32F2F)
                    )
                }
            }

            // Summary Stats (Transaction count and total items sold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transaksi Berhasil",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${report.transactionCount} Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color(0xFF00897B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Barang Terjual",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${report.itemsSoldCount} Item",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PeriodFilterButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        )
    }
}
