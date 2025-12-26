package com.iliass.iliass.model

import java.util.*

enum class DebtType {
    I_OWE,
    OWED_TO_ME
}

enum class Currency {
    KMF,
    UGX
}

data class Debt(
    val id: String = UUID.randomUUID().toString(),
    val personName: String,
    val type: DebtType,
    val initialAmount: Double,
    val reason: String,
    val currency: Currency = Currency.KMF,
    val dateCreated: Long = System.currentTimeMillis(),
    val transactions: MutableList<DebtTransaction> = mutableListOf(),
    val isPaid: Boolean = false
) {
    fun getRemainingAmount(): Double {
        val totalPaid = transactions.filter { it.type == TransactionType.PAYMENT }
            .sumOf { it.amount }
        val totalAdded = transactions.filter { it.type == TransactionType.ADDITIONAL_LOAN }
            .sumOf { it.amount }
        return initialAmount + totalAdded - totalPaid
    }

    fun getDisplayName(): String {
        return if (type == DebtType.I_OWE) {
            "To: $personName"
        } else {
            "From: $personName"
        }
    }
}

enum class TransactionType {
    PAYMENT,
    ADDITIONAL_LOAN
}

data class DebtTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)