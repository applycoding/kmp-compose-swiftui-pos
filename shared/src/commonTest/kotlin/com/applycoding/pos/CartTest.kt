package com.applycoding.pos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CartTest {
    private val coffee = Product("COF", "Flat white", 350, "SECOND_REDUCED", "Drinks")
    private val mug = Product("MUG", "Mug", 1200, "STANDARD", "Merch")
    private val book = Product("BOOK", "Cookbook", 2000, "ZERO", "Books")

    @Test fun addingSameSkuMergesLines() {
        val c = Cart().add(coffee).add(coffee, 2)
        assertEquals(1, c.lines.size)
        assertEquals(3, c.itemCount)
    }

    @Test fun vatPerRate() {
        val t = Cart().add(coffee, 2).add(mug).add(book).totals()
        assertEquals(700 + 1200 + 2000, t.net)
        assertEquals(63, t.vatByRate[VatRate.SECOND_REDUCED]) // 9% of 7.00
        assertEquals(276, t.vatByRate[VatRate.STANDARD]) // 23% of 12.00
        assertEquals(0, t.vatByRate[VatRate.ZERO])
        assertEquals(t.net + t.vat, t.gross)
    }

    @Test fun percentDiscountReducesTaxableAmount() {
        val t = Cart().add(mug).withDiscount(Discount.Percent(1000)).totals()
        assertEquals(120, t.discount)
        assertEquals(248, t.vat) // 23% of 10.80
        assertEquals(1200 - 120 + 248, t.gross)
    }

    @Test fun fixedDiscountIsCappedAndAllocated() {
        val c = Cart().add(coffee).add(mug)
        assertEquals(c.totals().net, c.withDiscount(Discount.Fixed(99_999)).totals().discount)
        assertEquals(0, c.withDiscount(Discount.Fixed(99_999)).totals().vat)
    }

    @Test fun allocationAlwaysSumsToAmount() {
        val weights = listOf(333L, 333L, 334L, 1L, 0L)
        for (amount in listOf(0L, 1L, 7L, 100L, 1001L)) assertEquals(amount, Cart.allocate(amount, weights).sum())
        assertEquals(listOf(0L, 0L), Cart.allocate(10, listOf(0, 0)))
    }

    @Test fun quantityUpdatesAndRemoval() {
        val c = Cart().add(coffee).add(mug).setQuantity("COF", 5).remove("MUG")
        assertEquals(listOf(5), c.lines.map { it.quantity })
        assertEquals(0, c.clear().itemCount)
        assertFailsWith<IllegalArgumentException> { c.add(coffee, 0) }
        assertFailsWith<IllegalArgumentException> { c.setQuantity("COF", -1) }
    }

    @Test fun moneyFormatting() {
        assertEquals("€3.50", 350L.format())
        assertEquals("-€0.05", (-5L).format())
        assertEquals("USD 10.00", 1000L.format("USD"))
    }
}
