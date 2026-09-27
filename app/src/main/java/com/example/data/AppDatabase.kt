package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CatalogDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ProductDao
import com.example.data.dao.StoreInfoDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.CatalogEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.StoreInfoEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.TransactionItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        ExpenseEntity::class,
        StoreInfoEntity::class,
        CatalogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun storeInfoDao(): StoreInfoDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalogs` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_catalogs_name` ON `catalogs` (`name`)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "retail_pos_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val current = database.storeInfoDao().getStoreInfoSync()
                    if (current == null || current.storeName.contains("Retail", ignoreCase = true) || current.storeAddress.contains("Sudirman", ignoreCase = true) || current.storeAddress.contains("Pasar Baru", ignoreCase = true)) {
                        database.storeInfoDao().insertOrUpdate(
                            StoreInfoEntity(
                                id = 1,
                                storeName = "TOKO MAKMUR",
                                storeAddress = "Jl. Kembang Kuning No. 17, Surabaya",
                                storePhone = "",
                                initialCapital = current?.initialCapital ?: 2500000.0,
                                receiptFooter = current?.receiptFooter ?: "Terima kasih telah berbelanja di Toko Makmur!"
                            )
                        )
                    }

                    // Seed default and existing categories into catalogs
                    val catalogDao = database.catalogDao()
                    val defaultCatalogs = listOf("Sembako", "Minuman", "Makanan", "Bumbu Dapur", "Kebutuhan Rumah", "Snack", "Lainnya")
                    defaultCatalogs.forEach { cat ->
                        catalogDao.insert(CatalogEntity(name = cat))
                    }
                    val existingCats = database.productDao().getDistinctCategories()
                    existingCats.forEach { cat ->
                        if (cat.isNotBlank()) {
                            catalogDao.insert(CatalogEntity(name = cat.trim()))
                        }
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val productDao = database.productDao()
            val storeInfoDao = database.storeInfoDao()
            val expenseDao = database.expenseDao()
            val transactionDao = database.transactionDao()
            val catalogDao = database.catalogDao()

            // Seed default catalogs
            val defaultCatalogs = listOf("Sembako", "Minuman", "Makanan", "Bumbu Dapur", "Kebutuhan Rumah", "Snack", "Lainnya")
            defaultCatalogs.forEach { cat ->
                catalogDao.insert(CatalogEntity(name = cat))
            }

            // 1. Initial Store Info & Capital
            storeInfoDao.insertOrUpdate(
                StoreInfoEntity(
                    id = 1,
                    storeName = "TOKO MAKMUR",
                    storeAddress = "Jl. Kembang Kuning No. 17, Surabaya",
                    storePhone = "",
                    initialCapital = 2500000.0,
                    receiptFooter = "Terima kasih telah berbelanja di Toko Makmur!"
                )
            )

            // 2. Pre-seeded Retail Products with QR / Barcodes
            val initialProducts = listOf(
                ProductEntity(
                    name = "Beras Premium 5kg",
                    barcode = "8992753123456",
                    category = "Sembako",
                    buyPrice = 62000.0,
                    sellPrice = 72000.0,
                    stock = 18,
                    minStockAlert = 5,
                    unit = "Sak"
                ),
                ProductEntity(
                    name = "Minyak Goreng 2L",
                    barcode = "8998866100012",
                    category = "Sembako",
                    buyPrice = 31000.0,
                    sellPrice = 36500.0,
                    stock = 12,
                    minStockAlert = 5,
                    unit = "Pouch"
                ),
                ProductEntity(
                    name = "Gula Pasir Kristal 1kg",
                    barcode = "8991002334411",
                    category = "Sembako",
                    buyPrice = 14500.0,
                    sellPrice = 17500.0,
                    stock = 3, // Low stock!
                    minStockAlert = 5,
                    unit = "Bks"
                ),
                ProductEntity(
                    name = "Kopi Hitam Mantap 150g",
                    barcode = "8992345600021",
                    category = "Minuman",
                    buyPrice = 9000.0,
                    sellPrice = 12500.0,
                    stock = 25,
                    minStockAlert = 5,
                    unit = "Bks"
                ),
                ProductEntity(
                    name = "Teh Celup Melati 25s",
                    barcode = "8995544332211",
                    category = "Minuman",
                    buyPrice = 5500.0,
                    sellPrice = 7500.0,
                    stock = 2, // Low stock!
                    minStockAlert = 5,
                    unit = "Kotak"
                ),
                ProductEntity(
                    name = "Mie Instan Kuah Soto",
                    barcode = "8998866200034",
                    category = "Makanan",
                    buyPrice = 2800.0,
                    sellPrice = 3500.0,
                    stock = 45,
                    minStockAlert = 10,
                    unit = "Bks"
                ),
                ProductEntity(
                    name = "Sabun Cair Cuci Piring 750ml",
                    barcode = "8997788990012",
                    category = "Kebersihan",
                    buyPrice = 13000.0,
                    sellPrice = 16500.0,
                    stock = 14,
                    minStockAlert = 4,
                    unit = "Pouch"
                ),
                ProductEntity(
                    name = "Air Mineral Botol 600ml",
                    barcode = "8991234567890",
                    category = "Minuman",
                    buyPrice = 2500.0,
                    sellPrice = 4000.0,
                    stock = 30,
                    minStockAlert = 6,
                    unit = "Botol"
                ),
                ProductEntity(
                    name = "Susu UHT Cokelat 1L",
                    barcode = "8994455667788",
                    category = "Minuman",
                    buyPrice = 16500.0,
                    sellPrice = 21000.0,
                    stock = 4, // Low stock!
                    minStockAlert = 5,
                    unit = "Kotak"
                ),
                ProductEntity(
                    name = "Biskuit Gandum Segar 200g",
                    barcode = "8993322114455",
                    category = "Makanan",
                    buyPrice = 8500.0,
                    sellPrice = 11500.0,
                    stock = 20,
                    minStockAlert = 4,
                    unit = "Bks"
                )
            )
            productDao.insertAll(initialProducts)

            // 3. Pre-seed Sample Expenses
            expenseDao.insert(
                ExpenseEntity(
                    title = "Beli Plastik Kresek Toko",
                    category = "Operasional",
                    amount = 45000.0,
                    date = System.currentTimeMillis() - 86400000L * 2,
                    note = "Beli kantong ukuran sedang & besar"
                )
            )
            expenseDao.insert(
                ExpenseEntity(
                    title = "Token Listrik Toko",
                    category = "Utilitas",
                    amount = 100000.0,
                    date = System.currentTimeMillis() - 86400000L,
                    note = "Isi ulang token PLN toko"
                )
            )

            // 4. Sample Completed Transaction
            val txTime = System.currentTimeMillis() - 3600000L * 3
            val txId = transactionDao.insertTransaction(
                TransactionEntity(
                    invoiceNumber = "TRX-SAMPLE-001",
                    timestamp = txTime,
                    totalAmount = 50000.0,
                    cashPaid = 50000.0,
                    changeAmount = 0.0,
                    totalCost = 39000.0,
                    totalProfit = 11000.0,
                    itemCount = 3,
                    paymentMethod = "Tunai",
                    notes = "Contoh transaksi lunas"
                )
            )

            transactionDao.insertItems(
                listOf(
                    TransactionItemEntity(
                        transactionId = txId,
                        productId = 1,
                        productName = "Minyak Goreng 2L",
                        barcode = "8998866100012",
                        quantity = 1,
                        buyPrice = 31000.0,
                        sellPrice = 36500.0,
                        subtotal = 36500.0
                    ),
                    TransactionItemEntity(
                        transactionId = txId,
                        productId = 2,
                        productName = "Kopi Hitam Mantap 150g",
                        barcode = "8992345600021",
                        quantity = 1,
                        buyPrice = 9000.0,
                        sellPrice = 12500.0,
                        subtotal = 12500.0
                    ),
                    TransactionItemEntity(
                        transactionId = txId,
                        productId = 3,
                        productName = "Air Mineral Botol 600ml",
                        barcode = "8991234567890",
                        quantity = 1,
                        buyPrice = 2500.0,
                        sellPrice = 1000.0, // Discounted sample
                        subtotal = 1000.0
                    )
                )
            )
        }
    }
}
