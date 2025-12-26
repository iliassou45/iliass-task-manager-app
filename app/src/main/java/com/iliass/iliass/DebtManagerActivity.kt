package com.iliass.iliass

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.iliass.iliass.model.Debt
import com.iliass.iliass.model.DebtType
import com.iliass.iliass.repository.DebtDatabase
import com.iliass.iliass.ui.adapter.DebtAdapter
import com.iliass.iliass.util.CurrencyUtils


class DebtManagerActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var debtAdapter: DebtAdapter
    private lateinit var fabAddDebt: FloatingActionButton
    private lateinit var emptyView: TextView
    private lateinit var totalOwedText: TextView
    private lateinit var totalOwingText: TextView

    private val debtDatabase by lazy { DebtDatabase.getInstance(this) }
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debt_manager)

        supportActionBar?.title = "💰 Debt Manager"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupTabs()
        setupRecyclerView()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        loadDebts()
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.debtsRecyclerView)
        fabAddDebt = findViewById(R.id.fabAddDebt)
        emptyView = findViewById(R.id.emptyView)
        totalOwedText = findViewById(R.id.totalOwedText)
        totalOwingText = findViewById(R.id.totalOwingText)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("I Owe"))
        tabLayout.addTab(tabLayout.newTab().setText("Owed to Me"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                loadDebts()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        debtAdapter = DebtAdapter(
            debts = emptyList(),
            onDebtClick = { debt ->
                val intent = Intent(this, DebtDetailActivity::class.java)
                intent.putExtra("DEBT_ID", debt.id)
                startActivity(intent)
            },
            onDebtLongClick = { debt ->
                showDeleteDebtDialog(debt)
            }
        )
        recyclerView.adapter = debtAdapter
    }

    private fun setupFab() {
        fabAddDebt.setOnClickListener {
            val intent = Intent(this, AddDebtActivity::class.java)
            intent.putExtra("DEBT_TYPE", if (currentTab == 0) "I_OWE" else "OWED_TO_ME")
            startActivity(intent)
        }
    }

    private fun loadDebts() {
        val type = if (currentTab == 0) DebtType.I_OWE else DebtType.OWED_TO_ME
        val debts = debtDatabase.getDebtsByType(type)

        if (debts.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyView.text = if (currentTab == 0) {
                "No debts you owe.\nTap + to add one."
            } else {
                "No one owes you money.\nTap + to add one."
            }
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        debtAdapter.updateDebts(debts)
        updateTotals()
    }

    private fun updateTotals() {
        // Calculate totals in KMF (base currency)
        val totalOwingKMF = debtDatabase.getDebtsByType(DebtType.I_OWE)
            .sumOf { CurrencyUtils.toKMF(it.getRemainingAmount(), it.currency) }
        val totalOwedKMF = debtDatabase.getDebtsByType(DebtType.OWED_TO_ME)
            .sumOf { CurrencyUtils.toKMF(it.getRemainingAmount(), it.currency) }

        // Calculate totals in UGX
        val totalOwingUGX = debtDatabase.getDebtsByType(DebtType.I_OWE)
            .sumOf { CurrencyUtils.toUGX(it.getRemainingAmount(), it.currency) }
        val totalOwedUGX = debtDatabase.getDebtsByType(DebtType.OWED_TO_ME)
            .sumOf { CurrencyUtils.toUGX(it.getRemainingAmount(), it.currency) }

        // Display totals in both currencies
        totalOwingText.text = "I Owe: ${CurrencyUtils.formatCurrency(totalOwingKMF, com.iliass.iliass.model.Currency.KMF)} / ${CurrencyUtils.formatCurrency(totalOwingUGX, com.iliass.iliass.model.Currency.UGX)}"
        totalOwedText.text = "Owed to Me: ${CurrencyUtils.formatCurrency(totalOwedKMF, com.iliass.iliass.model.Currency.KMF)} / ${CurrencyUtils.formatCurrency(totalOwedUGX, com.iliass.iliass.model.Currency.UGX)}"
    }

    private fun showDeleteDebtDialog(debt: Debt) {
        val transactionCount = debt.transactions.size
        val message = if (transactionCount > 0) {
            "Are you sure you want to delete this debt?\n\nThis will also delete $transactionCount transaction(s) associated with it."
        } else {
            "Are you sure you want to delete this debt?"
        }

        AlertDialog.Builder(this)
            .setTitle("🗑️ Delete Debt")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                showPasswordDialogForDebt(debt)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPasswordDialogForDebt(debt: Debt) {
        val passwordInput = EditText(this).apply {
            hint = "Enter PIN to delete"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Confirm Deletion")
            .setMessage("Enter the PIN to confirm deletion of this debt and all its transactions.")
            .setView(passwordInput)
            .setPositiveButton("Confirm") { _, _ ->
                val enteredPassword = passwordInput.text.toString().trim()
                if (enteredPassword == "12345") {
                    deleteDebt(debt)
                } else {
                    Toast.makeText(this, "❌ Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDebt(debt: Debt) {
        debtDatabase.deleteDebt(debt.id)
        Toast.makeText(this, "Debt and all transactions deleted successfully", Toast.LENGTH_SHORT).show()
        loadDebts()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}