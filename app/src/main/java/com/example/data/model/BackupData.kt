package com.example.data.model

import com.example.data.entity.CatalogEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StoreInfoEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity

data class BackupData(
    val formatVersion: Int = 1,
    val appIdentifier: String = "TOKO_MAKMUR_POS",
    val backupTimestamp: Long = System.currentTimeMillis(),
    val storeInfo: StoreInfoEntity,
    val catalogs: List<CatalogEntity>,
    val products: List<ProductEntity>,
    val transactions: List<TransactionEntity>,
    val transactionItems: List<TransactionItemEntity>,
    val expenses: List<ExpenseEntity>
)

data class BackupSummary(
    val formatVersion: Int,
    val appIdentifier: String,
    val backupTimestamp: Long,
    val storeName: String,
    val storeAddress: String,
    val productCount: Int,
    val catalogCount: Int,
    val transactionCount: Int,
    val expenseCount: Int
)
