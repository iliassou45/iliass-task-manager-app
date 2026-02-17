package com.iliass.iliass.repository


import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.model.Currency
import com.iliass.iliass.model.Debt
import com.iliass.iliass.model.DebtTransaction
import com.iliass.iliass.model.DebtType

class DebtDatabase private constructor(context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("debt_manager_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val debts = mutableListOf<Debt>()

    init {
        loadDebtsFromStorage()
    }

    companion object {
        @Volatile
        private var instance: DebtDatabase? = null

        fun getInstance(context: Context): DebtDatabase {
            return instance ?: synchronized(this) {
                instance ?: DebtDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun loadDebtsFromStorage() {
        val json = sharedPrefs.getString("debts", null)
        if (json != null) {
            val type = object : TypeToken<List<Debt>>() {}.type
            val loadedDebts: List<Debt> = gson.fromJson(json, type)
            debts.clear()
            debts.addAll(loadedDebts.map { sanitizeDebt(it) })
        }
    }

    /**
     * Gson bypasses Kotlin default parameter values during deserialization.
     * Old debt records saved before the currency field was added will have null
     * currency at the JVM level, causing NPE when copy() is called.
     * This method fixes those null fields.
     */
    @Suppress("SENSELESS_COMPARISON")
    private fun sanitizeDebt(debt: Debt): Debt {
        return if (debt.currency == null) {
            Debt(
                id = debt.id,
                personName = debt.personName,
                type = debt.type,
                initialAmount = debt.initialAmount,
                reason = debt.reason,
                currency = Currency.KMF,
                dateCreated = debt.dateCreated,
                transactions = debt.transactions,
                isPaid = debt.isPaid
            )
        } else {
            debt
        }
    }

    private fun saveDebtsToStorage() {
        val json = gson.toJson(debts)
        sharedPrefs.edit().putString("debts", json).apply()
    }

    fun addDebt(debt: Debt) {
        debts.add(debt)
        saveDebtsToStorage()
    }

    fun updateDebt(updatedDebt: Debt) {
        val index = debts.indexOfFirst { it.id == updatedDebt.id }
        if (index != -1) {
            debts[index] = updatedDebt
            saveDebtsToStorage()
        }
    }

    fun deleteDebt(debtId: String) {
        debts.removeIf { it.id == debtId }
        saveDebtsToStorage()
    }

    fun getDebtById(id: String): Debt? {
        return debts.find { it.id == id }
    }

    fun getDebtsByType(type: DebtType): List<Debt> {
        return debts.filter { it.type == type && !it.isPaid }
            .sortedByDescending { it.dateCreated }
    }

    fun getAllDebts(): List<Debt> {
        return debts.toList()
    }

    fun addTransaction(debtId: String, transaction: DebtTransaction) {
        val debt = getDebtById(debtId) ?: return
        debt.transactions.add(transaction)

        // Check if debt is fully paid
        val remaining = debt.getRemainingAmount()
        if (remaining <= 0) {
            updateDebt(debt.copy(isPaid = true))
        } else {
            updateDebt(debt)
        }
    }
}