package com.applycoding.pos.android.data

import com.applycoding.pos.Cart
import com.applycoding.pos.OrderLineInput
import com.applycoding.pos.PosApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class OrderRepository(private val dao: OrderDao, private val api: PosApi?, private val now: () -> Long = System::currentTimeMillis) {
    val orders = dao.observeAll()

    /** Saves the order locally with a client generated id (idempotency key for the server). */
    suspend fun checkout(cart: Cart): OrderEntity {
        require(cart.lines.isNotEmpty()) { "cart is empty" }
        val t = cart.totals()
        val lines = cart.lines.map { OrderLineInput(it.product.sku, it.quantity) }
        val order = OrderEntity(UUID.randomUUID().toString(), now(), Json.encodeToString(lines), t.net - t.discount, t.vat, t.gross)
        dao.insert(order)
        return order
    }

    /** Pushes pending orders; failures leave them pending for the next attempt. Returns how many synced. */
    suspend fun sync(): Int {
        val client = api ?: return 0
        var synced = 0
        for (o in dao.pending()) {
            val result = runCatching { client.submitOrder(o.clientOrderId, Json.decodeFromString<List<OrderLineInput>>(o.linesJson)) }
            result.onSuccess { dao.markSynced(o.clientOrderId, it.id); synced++ }
            if (result.isFailure) break
        }
        return synced
    }

    suspend fun takings(): Long = dao.takings()
}
