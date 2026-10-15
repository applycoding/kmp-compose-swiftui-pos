package com.applycoding.pos.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applycoding.pos.Cart
import com.applycoding.pos.Discount
import com.applycoding.pos.android.ui.PosScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi screenshot tests (JVM, Robolectric native graphics).
 * `./gradlew :androidApp:recordRoborazziDebug` writes PNGs to androidApp/build/outputs/roborazzi;
 * `verifyRoborazziDebug` compares against committed baselines.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-xhdpi")
class PosScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String, state: PosState) {
        compose.setContent { MaterialTheme { PosScreen(state, {}, { _, _ -> }, {}, {}) } }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun emptyBasket() = shot("pos_empty", PosState())

    @Test fun fullBasketWithDiscount() {
        val p = SampleCatalog.products
        val cart = Cart().add(p[0], 2).add(p[3]).add(p[5]).add(p[6]).withDiscount(Discount.Percent(1000))
        shot("pos_basket_discount", PosState(cart = cart, message = "Order 1a2b3c4d saved", pendingSync = 1))
    }
}
