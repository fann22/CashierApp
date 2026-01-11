package com.tehkencana.pos.data

import android.content.Context
import androidx.room.*
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. Entity Category
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imageUri: String? = null,
    val position: Int = 0  // Untuk ordering
)

// 2. Entity Product
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val categoryId: Int,
    val position: Int = 0  // Untuk ordering
)

// 3. DAO
@Dao
interface AppDao {
    // --- Category Ops ---
    @Query("SELECT * FROM categories ORDER BY position ASC, id DESC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // --- Product Ops ---
    @Query("SELECT * FROM products WHERE categoryId = :catId ORDER BY position ASC, name ASC")
    fun getProductsByCategory(catId: Int): Flow<List<Product>>

    @Insert
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)
}

// 4. Database Config
@Database(entities = [Category::class, Product::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "pos_db_v3")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

// 5. DataStore (Printer Setting)
val Context.dataStore by preferencesDataStore(name = "settings")

class StoreManager(private val context: Context) {
    companion object {
        val PRINTER_MAC = stringPreferencesKey("printer_mac")
    }

    val printerMacFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PRINTER_MAC] }

    suspend fun savePrinterMac(mac: String) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_MAC] = mac
        }
    }
}