package com.applycoding.pos.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.applycoding.pos.Discount
import com.applycoding.pos.Product
import com.applycoding.pos.android.PosState
import com.applycoding.pos.format

/** Stateless POS screen: product grid on the left, basket and totals on the right. */
@Composable
fun PosScreen(
    state: PosState,
    onAdd: (Product) -> Unit,
    onQuantity: (String, Int) -> Unit,
    onDiscount: (Int?) -> Unit,
    onPay: () -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyVerticalGrid(GridCells.Adaptive(140.dp), Modifier.weight(1.4f).fillMaxHeight().testTag("catalog"),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.products, key = { it.sku }) { p ->
                Card(Modifier.clickable { onAdd(p) }.testTag("product-${p.sku}")) {
                    Column(Modifier.padding(12.dp)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text(p.category, style = MaterialTheme.typography.bodySmall)
                        Text(p.priceCents.format(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text("Basket (${state.cart.itemCount})", style = MaterialTheme.typography.titleLarge)
            LazyColumn(Modifier.weight(1f).testTag("basket")) {
                items(state.cart.lines, key = { it.product.sku }) { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("line-${line.product.sku}")) {
                        Text("${line.quantity} x ${line.product.name}", Modifier.weight(1f))
                        TextButton({ onQuantity(line.product.sku, line.quantity - 1) }, Modifier.testTag("dec-${line.product.sku}")) { Text("-") }
                        Text(line.net.format())
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, 10, 20).forEach { pct ->
                    val selected = (state.cart.discount as? Discount.Percent)?.basisPoints == pct?.times(100) || (pct == null && state.cart.discount == null)
                    FilterChip(selected, { onDiscount(pct) }, label = { Text(pct?.let { "$it% off" } ?: "No discount") }, modifier = Modifier.testTag("discount-${pct ?: 0}"))
                }
            }
            HorizontalDivider()
            val t = state.cart.totals()
            TotalRow("Net", t.net.format())
            if (t.discount > 0) TotalRow("Discount", "-" + t.discount.format())
            TotalRow("VAT", t.vat.format())
            TotalRow("Total", t.gross.format(), Modifier.testTag("total"))
            Spacer(Modifier.height(8.dp))
            Button(onPay, Modifier.fillMaxWidth().testTag("pay"), enabled = state.cart.lines.isNotEmpty()) { Text("Pay ${t.gross.format()}") }
            state.message?.let { Text(it, Modifier.testTag("message")) }
            if (state.pendingSync > 0) Text("${state.pendingSync} order(s) waiting to sync", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f))
        Text(value)
    }
}
