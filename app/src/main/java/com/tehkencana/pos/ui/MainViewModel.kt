package com.tehkencana.pos.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tehkencana.pos.data.*
import com.tehkencana.pos.utils.PrinterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appDao()
    private val storeManager = StoreManager(application)
    
    val printerManager = PrinterManager(application)
    
    // --- STATE ---
    val categories = dao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val savedPrinterMac = storeManager.printerMacFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Cart
    var cart = mutableStateMapOf<Product, Int>() 
        private set

    // --- ACTIONS ---

    // Get products untuk kategori tertentu
    fun getProductsForCategory(categoryId: Int): Flow<List<Product>> {
        return dao.getProductsByCategory(categoryId)
    }

    // Cart Operations
    fun updateCartQty(product: Product, delta: Int) {
        val currentQty = cart[product] ?: 0
        val newQty = currentQty + delta

        if (newQty <= 0) {
            cart.remove(product)
        } else {
            cart[product] = newQty
        }
    }
    
    fun clearCart() { 
        cart.clear() 
    }

    // Category Operations
    fun addCategory(name: String, imageUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxPosition = categories.value.maxOfOrNull { it.position } ?: -1
            dao.insertCategory(
                Category(name = name, imageUri = imageUri, position = maxPosition + 1)
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCategory(category)
            // Hapus juga dari cart jika ada produk dari kategori ini
            val productsInCategory = dao.getProductsByCategory(category.id).first()
            productsInCategory.forEach { product ->
                cart.remove(product)
            }
        }
    }

    fun reorderCategories(newList: List<Category>) {
        viewModelScope.launch(Dispatchers.IO) {
            newList.forEachIndexed { index, category ->
                dao.updateCategory(category.copy(position = index))
            }
        }
    }

    // Product Operations
    fun addProduct(name: String, price: String, categoryId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingProducts = dao.getProductsByCategory(categoryId).first()
            val maxPosition = existingProducts.maxOfOrNull { it.position } ?: -1
            
            val p = Product(
                name = name, 
                price = price.toDoubleOrNull() ?: 0.0, 
                categoryId = categoryId,
                position = maxPosition + 1
            )
            dao.insertProduct(p)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateProduct(product)
            // Update di cart jika ada
            if (cart.containsKey(product)) {
                val qty = cart[product]!!
                cart.remove(product)
                cart[product] = qty
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteProduct(product)
            // Hapus dari cart jika ada
            cart.remove(product)
        }
    }

    fun reorderProducts(newList: List<Product>) {
        viewModelScope.launch(Dispatchers.IO) {
            newList.forEachIndexed { index, product ->
                dao.updateProduct(product.copy(position = index))
            }
        }
    }
    
    // Printer Operations
    fun savePrinter(mac: String) {
        viewModelScope.launch { 
            storeManager.savePrinterMac(mac) 
        }
    }

    // Print Receipt
    fun printStruk(total: Double) {
        val mac = savedPrinterMac.value
        if (mac.isNullOrEmpty()) {
            return
        }
        
        val sb = StringBuilder()
        
        val ESC = "\u001B"
        val CENTER = "$ESC${'a'}1"
        val LEFT = "$ESC${'a'}0"
        val BOLD_ON = "$ESC${'E'}1"
        val BOLD_OFF = "$ESC${'E'}0"

        sb.append(CENTER).append(BOLD_ON)
        sb.append("TEH KENCANA POS\n")
        sb.append(BOLD_OFF)
        sb.append("Indonesia & Eatery\n")
        sb.append("================================\n")
        sb.append(LEFT)

        cart.forEach { (p, qty) ->
            val name = p.name.take(16).padEnd(16)
            val qStr = "${qty}x".padStart(3)
            val subtotal = (p.price * qty).toInt().toString().padStart(9)
            sb.append("$name $qStr $subtotal\n")
        }
        
        sb.append("================================\n")
        sb.append(CENTER).append(BOLD_ON)
        sb.append("TOTAL: Rp ${total.toInt()}\n")
        sb.append(BOLD_OFF)
        sb.append("\n")
        sb.append("Terima Kasih!\n")
        sb.append("\n\n\n")

        printerManager.printReceipt(mac, sb.toString())
        clearCart()
    }
}