import SwiftUI
import Shared

/// Small SwiftUI client reusing the Kotlin Multiplatform `Cart` (pricing, discounts and VAT)
/// from the Shared framework, so Android and iOS compute identical totals.
final class PosModel: ObservableObject {
    @Published private(set) var cart = Cart(lines: [], discount: nil)
    let products: [Product] = [
        Product(sku: "COF-FW", name: "Flat white", priceCents: 350, vat: "SECOND_REDUCED", category: "Drinks"),
        Product(sku: "BAK-SC", name: "Scone", priceCents: 280, vat: "REDUCED", category: "Bakery"),
        Product(sku: "MER-MG", name: "Mug", priceCents: 1200, vat: "STANDARD", category: "Merch"),
    ]

    func add(_ p: Product) { cart = cart.add(p: p, qty: 1) }
    func clear() { cart = cart.clear() }
    var totals: Totals { cart.totals() }
}

struct ContentView: View {
    @StateObject private var model = PosModel()

    var body: some View {
        NavigationStack {
            List {
                Section("Products") {
                    ForEach(model.products, id: \.sku) { p in
                        Button { model.add(p) } label: {
                            HStack { Text(p.name); Spacer(); Text(MoneyKt.format(p.priceCents, currency: "EUR")) }
                        }
                    }
                }
                Section("Basket (\(model.cart.itemCount))") {
                    ForEach(model.cart.lines, id: \.product.sku) { line in
                        HStack { Text("\(line.quantity) x \(line.product.name)"); Spacer(); Text(MoneyKt.format(line.net, currency: "EUR")) }
                    }
                    HStack { Text("VAT"); Spacer(); Text(MoneyKt.format(model.totals.vat, currency: "EUR")) }
                    HStack { Text("Total").bold(); Spacer(); Text(MoneyKt.format(model.totals.gross, currency: "EUR")).bold() }
                }
            }
            .navigationTitle("Demo POS")
            .toolbar { Button("Clear") { model.clear() } }
        }
    }
}
