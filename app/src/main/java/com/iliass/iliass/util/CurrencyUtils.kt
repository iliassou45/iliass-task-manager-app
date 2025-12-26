package com.iliass.iliass.util

import com.iliass.iliass.model.Currency

object CurrencyUtils {

    // Conversion rate: 8 UGX = 1 KMF
    private const val UGX_TO_KMF_RATE = 0.125 // 1 UGX = 0.125 KMF
    private const val KMF_TO_UGX_RATE = 8.0   // 1 KMF = 8 UGX

    fun formatCurrency(amount: Double, currency: Currency? = Currency.KMF): String {
        val actualCurrency = currency ?: Currency.KMF
        val formatted = String.format("%,.0f", amount)
        return "$formatted ${actualCurrency.name}"
    }

    fun formatCurrencyWithDecimals(amount: Double, currency: Currency? = Currency.KMF): String {
        val actualCurrency = currency ?: Currency.KMF
        val formatted = String.format("%,.2f", amount)
        return "$formatted ${actualCurrency.name}"
    }

    /**
     * Convert amount from one currency to another
     * @param amount The amount to convert
     * @param fromCurrency The source currency (defaults to KMF if null for backward compatibility)
     * @param toCurrency The target currency
     * @return The converted amount
     */
    fun convertCurrency(amount: Double, fromCurrency: Currency?, toCurrency: Currency): Double {
        val sourceCurrency = fromCurrency ?: Currency.KMF
        if (sourceCurrency == toCurrency) return amount

        return when {
            sourceCurrency == Currency.UGX && toCurrency == Currency.KMF -> amount * UGX_TO_KMF_RATE
            sourceCurrency == Currency.KMF && toCurrency == Currency.UGX -> amount * KMF_TO_UGX_RATE
            else -> amount
        }
    }

    /**
     * Convert amount to KMF (base currency for calculations)
     * @param amount The amount to convert
     * @param currency The source currency (defaults to KMF if null for backward compatibility with old data)
     */
    fun toKMF(amount: Double, currency: Currency?): Double {
        return convertCurrency(amount, currency, Currency.KMF)
    }

    /**
     * Convert amount to UGX
     * @param amount The amount to convert
     * @param currency The source currency (defaults to KMF if null for backward compatibility with old data)
     */
    fun toUGX(amount: Double, currency: Currency?): Double {
        return convertCurrency(amount, currency, Currency.UGX)
    }
}