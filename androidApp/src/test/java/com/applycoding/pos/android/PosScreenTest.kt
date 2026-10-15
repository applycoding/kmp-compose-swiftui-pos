package com.applycoding.pos.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applycoding.pos.Discount
import com.applycoding.pos.android.ui.PosScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Compose UI tests on the JVM via Robolectric (no emulator needed). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-xhdpi")
class PosScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun setUp(onPay: () -> Unit = {}) {
        compose.setContent {
            var state by mutableStateOf(PosState())
            MaterialTheme {
                PosScreen(
                    state,
                    onAdd = { state = state.copy(cart = state.cart.add(it)) },
                    onQuantity = { sku, q -> state = state.copy(cart = state.cart.setQuantity(sku, q)) },
                    onDiscount = { p -> state = state.copy(cart = state.cart.withDiscount(p?.let { Discount.Percent(it * 100) })) },
                    onPay = onPay,
                )
            }
        }
    }

    @Test fun payDisabledWhenBasketEmpty() {
        setUp()
        compose.onNodeWithTag("pay").assertIsNotEnabled()
    }

    @Test fun addingProductsUpdatesTotals() {
        setUp()
        compose.onNodeWithTag("product-COF-FW").performClick()
        compose.onNodeWithTag("product-COF-FW").performClick()
        compose.onNodeWithTag("product-MER-MG").performClick()
        // 2 x 3.50 at 9% = 0.63 VAT, 12.00 at 23% = 2.76 VAT -> 19.00 + 3.39 = 22.39
        compose.onNodeWithTag("total").assertTextContains("€22.39", substring = true)
        compose.onNodeWithTag("line-COF-FW").assertTextContains("2 x Flat white", substring = true)
    }

    @Test fun discountAndQuantityDecrease() {
        setUp()
        compose.onNodeWithTag("product-MER-MG").performClick()
        compose.onNodeWithTag("discount-10").performClick()
        compose.onNodeWithTag("total").assertTextContains("€13.28", substring = true) // 10.80 + 2.48
        compose.onNodeWithTag("dec-MER-MG").performClick()
        compose.onNodeWithTag("pay").assertIsNotEnabled()
    }

    @Test fun payInvokesCallback() {
        var paid = 0
        setUp { paid++ }
        compose.onNodeWithTag("product-BAK-SC").performClick()
        compose.onNodeWithTag("pay").performClick()
        assertEquals(1, paid)
    }
}
