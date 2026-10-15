package com.applycoding.pos

import kotlinx.serialization.Serializable

/** Irish VAT categories used by a small cafe or retail POS (illustrative rates). */
enum class VatRate(val basisPoints: Int) { STANDARD(2300), REDUCED(1350), SECOND_REDUCED(900), ZERO(0) }

@Serializable
data class Product(val sku: String, val name: String, val priceCents: Long, val vat: String = "STANDARD", val category: String = "General") {
    val vatRate: VatRate get() = VatRate.valueOf(vat)
}

data class CartLine(val product: Product, val quantity: Int) {
    val net: Cents get() = product.priceCents * quantity
}

data class Totals(val net: Cents, val discount: Cents, val vat: Cents, val gross: Cents, val vatByRate: Map<VatRate, Cents>)

sealed interface Discount {
    /** Percentage off the whole basket, in basis points. */
    data class Percent(val basisPoints: Int) : Discount
    /** Fixed amount off, capped at the basket net. */
    data class Fixed(val cents: Cents) : Discount
}

/**
 * Immutable cart: every operation returns a new cart, which makes it trivially safe to hold
 * in a Compose StateFlow or a SwiftUI @Published property.
 *
 * Prices are VAT exclusive. A basket discount is allocated across lines in proportion to
 * their net value (largest remainder), so VAT is charged on the discounted amount per rate.
 */
data class Cart(val lines: List<CartLine> = emptyList(), val discount: Discount? = null) {
    fun add(p: Product, qty: Int = 1): Cart {
        require(qty > 0) { "quantity must be positive" }
        val existing = lines.indexOfFirst { it.product.sku == p.sku }
        return if (existing >= 0) copy(lines = lines.mapIndexed { i, l -> if (i == existing) l.copy(quantity = l.quantity + qty) else l })
        else copy(lines = lines + CartLine(p, qty))
    }

    fun setQuantity(sku: String, qty: Int): Cart {
        require(qty >= 0) { "quantity must not be negative" }
        return copy(lines = if (qty == 0) lines.filterNot { it.product.sku == sku } else lines.map { if (it.product.sku == sku) it.copy(quantity = qty) else it })
    }

    fun remove(sku: String): Cart = setQuantity(sku, 0)
    fun withDiscount(d: Discount?): Cart = copy(discount = d)
    fun clear(): Cart = Cart()
    val itemCount: Int get() = lines.sumOf { it.quantity }

    fun totals(): Totals {
        val net = lines.sumOf { it.net }
        val discountTotal = when (val d = discount) {
            null -> 0L
            is Discount.Percent -> applyRate(net, d.basisPoints.coerceIn(0, 10_000))
            is Discount.Fixed -> d.cents.coerceIn(0, net)
        }
        val allocated = allocate(discountTotal, lines.map { it.net })
        val vatByRate = mutableMapOf<VatRate, Cents>()
        lines.forEachIndexed { i, l ->
            val taxable = l.net - allocated[i]
            vatByRate[l.product.vatRate] = (vatByRate[l.product.vatRate] ?: 0) + applyRate(taxable, l.product.vatRate.basisPoints)
        }
        val vat = vatByRate.values.sum()
        return Totals(net, discountTotal, vat, net - discountTotal + vat, vatByRate)
    }

    companion object {
        /** Splits [amount] across [weights] proportionally; the parts always sum to [amount]. */
        fun allocate(amount: Cents, weights: List<Cents>): List<Cents> {
            val total = weights.sum()
            if (amount == 0L || total == 0L) return weights.map { 0L }
            val raw = weights.map { it * amount }
            val base = raw.map { it / total }.toMutableList()
            var left = amount - base.sum()
            raw.indices.sortedByDescending { raw[it] % total }.forEach { if (left > 0) { base[it] += 1; left-- } }
            return base
        }
    }
}
