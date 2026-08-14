package com.example.tiendita.util

import java.text.NumberFormat
import java.util.Locale

/**
 * RNF3: Manejo de cifras en pesos mexicanos, con 2 dígitos decimales.
 */
object MoneyUtil {
    private val formatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("es").setRegion("MX").build()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    fun format(amount: Double): String = formatter.format(amount)
}
