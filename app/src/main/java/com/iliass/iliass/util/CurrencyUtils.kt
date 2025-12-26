package com.iliass.iliass.util


object CurrencyUtils {

    fun formatCurrency(amount: Double): String {
        val formatted = String.format("%,.0f", amount)
        return "$formatted KMF"
    }

    fun formatCurrencyWithDecimals(amount: Double): String {
        val formatted = String.format("%,.2f", amount)
        return "$formatted KMF"
    }
}