package com.applycoding.pos

import kotlin.math.abs

/** Minor units (cents). Money never touches floating point. */
typealias Cents = Long

fun Cents.format(currency: String = "EUR"): String {
    val sign = if (this < 0) "-" else ""
    val a = abs(this)
    val symbol = if (currency == "EUR") "€" else "$currency "
    return "$sign$symbol${a / 100}.${(a % 100).toString().padStart(2, '0')}"
}

/** Basis points: 23% = 2300. Rounds half up. */
fun applyRate(net: Cents, basisPoints: Int): Cents {
    require(net >= 0) { "net must be non negative" }
    return (net * basisPoints + 5_000) / 10_000
}
