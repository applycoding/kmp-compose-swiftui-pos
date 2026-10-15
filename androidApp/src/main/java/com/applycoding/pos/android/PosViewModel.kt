package com.applycoding.pos.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.applycoding.pos.Cart
import com.applycoding.pos.Discount
import com.applycoding.pos.PosApi
import com.applycoding.pos.Product
import com.applycoding.pos.android.data.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PosState(
    val products: List<Product> = SampleCatalog.products,
    val cart: Cart = Cart(),
    val message: String? = null,
    val pendingSync: Int = 0,
)

object SampleCatalog {
    val products = listOf(
        Product("COF-FW", "Flat white", 350, "SECOND_REDUCED", "Drinks"),
        Product("COF-AM", "Americano", 300, "SECOND_REDUCED", "Drinks"),
        Product("TEA-BR", "Breakfast tea", 250, "SECOND_REDUCED", "Drinks"),
        Product("BAK-SC", "Scone", 280, "REDUCED", "Bakery"),
        Product("BAK-CR", "Croissant", 320, "REDUCED", "Bakery"),
        Product("MER-MG", "Mug", 1200, "STANDARD", "Merch"),
        Product("BOK-CB", "Cookbook", 2000, "ZERO", "Books"),
    )
}

class PosViewModel(private val repo: OrderRepository, private val api: PosApi?) : ViewModel() {
    private val _state = MutableStateFlow(PosState())
    val state: StateFlow<PosState> = _state.asStateFlow()

    init {
        // Use the server catalogue when reachable, otherwise keep the bundled one (offline first).
        viewModelScope.launch {
            api?.let { a -> runCatching { a.products() }.onSuccess { p -> if (p.isNotEmpty()) _state.update { it.copy(products = p) } } }
        }
    }

    fun add(p: Product) = _state.update { it.copy(cart = it.cart.add(p), message = null) }
    fun setQuantity(sku: String, q: Int) = _state.update { it.copy(cart = it.cart.setQuantity(sku, q)) }
    fun applyDiscount(percent: Int?) = _state.update { it.copy(cart = it.cart.withDiscount(percent?.let { p -> Discount.Percent(p * 100) })) }

    fun pay() = viewModelScope.launch {
        val cart = _state.value.cart
        if (cart.lines.isEmpty()) return@launch
        val order = repo.checkout(cart)
        val synced = repo.sync()
        _state.update { it.copy(cart = Cart(), message = "Order ${order.clientOrderId.take(8)} saved", pendingSync = if (synced > 0) 0 else it.pendingSync + 1) }
    }
}
