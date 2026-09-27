package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StoreInfoEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stock <= minStockAlert ORDER BY stock ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId")
    suspend fun decreaseStock(productId: Long, quantity: Int)

    @Query("UPDATE products SET stock = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Query("SELECT DISTINCT category FROM products WHERE category IS NOT NULL AND category != ''")
    suspend fun getDistinctCategories(): List<String>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsSync(): List<ProductEntity>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getItemsForTransaction(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransactionSync(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items ORDER BY id DESC")
    fun getAllTransactionItems(): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items ORDER BY id ASC")
    suspend fun getAllTransactionItemsSync(): List<TransactionItemEntity>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllTransactionItems()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY id ASC")
    suspend fun getAllExpensesSync(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}

@Dao
interface StoreInfoDao {
    @Query("SELECT * FROM store_info WHERE id = 1 LIMIT 1")
    fun getStoreInfo(): Flow<StoreInfoEntity?>

    @Query("SELECT * FROM store_info WHERE id = 1 LIMIT 1")
    suspend fun getStoreInfoSync(): StoreInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(storeInfo: StoreInfoEntity)
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalogs ORDER BY name ASC")
    fun getAllCatalogs(): Flow<List<com.example.data.entity.CatalogEntity>>

    @Query("SELECT * FROM catalogs ORDER BY id ASC")
    suspend fun getAllCatalogsSync(): List<com.example.data.entity.CatalogEntity>

    @Query("SELECT * FROM catalogs WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCatalogByName(name: String): com.example.data.entity.CatalogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(catalog: com.example.data.entity.CatalogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(catalogs: List<com.example.data.entity.CatalogEntity>)

    @Delete
    suspend fun delete(catalog: com.example.data.entity.CatalogEntity)

    @Query("DELETE FROM catalogs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM catalogs")
    suspend fun deleteAllCatalogs()

    @Query("SELECT COUNT(*) FROM products WHERE LOWER(category) = LOWER(:catalogName)")
    suspend fun countProductsInCatalog(catalogName: String): Int

    @Query("SELECT COUNT(*) FROM catalogs")
    suspend fun countCatalogs(): Int
}

