package com.iliass.iliass

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.model.Debt
import com.iliass.iliass.model.DebtTransaction
import com.iliass.iliass.model.DebtType
import com.iliass.iliass.model.TransactionType
import com.iliass.iliass.repository.DebtDatabase
import com.iliass.iliass.ui.adapter.TransactionAdapter
import com.iliass.iliass.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

class DebtDetailActivity : AppCompatActivity() {

    private lateinit var personNameText: TextView
    private lateinit var reasonText: TextView
    private lateinit var initialAmountText: TextView
    private lateinit var remainingAmountText: TextView
    private lateinit var dateCreatedText: TextView
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var emptyTransactionsText: TextView
    private lateinit var addPaymentButton: Button
    private lateinit var addLoanButton: Button
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var pageInfoText: TextView

    private val debtDatabase by lazy { DebtDatabase.getInstance(this) }
    private lateinit var transactionAdapter: TransactionAdapter
    private var debt: Debt? = null

    // Pagination
    private var currentPage = 0
    private val itemsPerPage = 10
    private var allTransactions: List<DebtTransaction> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debt_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupRecyclerView()
        loadDebt()
        setupButtons()
    }

    private fun initViews() {
        personNameText = findViewById(R.id.personNameText)
        reasonText = findViewById(R.id.reasonText)
        initialAmountText = findViewById(R.id.initialAmountText)
        remainingAmountText = findViewById(R.id.remainingAmountText)
        dateCreatedText = findViewById(R.id.dateCreatedText)
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView)
        emptyTransactionsText = findViewById(R.id.emptyTransactionsText)
        addPaymentButton = findViewById(R.id.addPaymentButton)
        addLoanButton = findViewById(R.id.addLoanButton)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        pageInfoText = findViewById(R.id.pageInfoText)
    }

    private fun setupRecyclerView() {
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(
            transactions = emptyList(),
            currency = debt?.currency ?: com.iliass.iliass.model.Currency.KMF,
            onTransactionLongClick = { transaction ->
                showDeleteTransactionDialog(transaction)
            }
        )
        transactionsRecyclerView.adapter = transactionAdapter
    }

    private fun loadDebt() {
        val debtId = intent.getStringExtra("DEBT_ID") ?: return
        debt = debtDatabase.getDebtById(debtId)

        debt?.let { d ->
            supportActionBar?.title = d.getDisplayName()
            personNameText.text = d.personName
            reasonText.text = "📌 ${d.reason}"
            initialAmountText.text = "Initial: ${CurrencyUtils.formatCurrency(d.initialAmount, d.currency)}"

            val remaining = d.getRemainingAmount()
            remainingAmountText.text = "Remaining: ${CurrencyUtils.formatCurrency(remaining, d.currency)}"

            if (remaining <= 0) {
                remainingAmountText.setTextColor(0xFF43A047.toInt())
                remainingAmountText.text = "✓ PAID IN FULL"
            } else if (d.type == DebtType.I_OWE) {
                remainingAmountText.setTextColor(0xFFE53935.toInt())
            } else {
                remainingAmountText.setTextColor(0xFF43A047.toInt())
            }

            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            dateCreatedText.text = "📅 ${dateFormat.format(Date(d.dateCreated))}"

            // Store all transactions sorted by date (newest first)
            allTransactions = d.transactions.sortedByDescending { it.date }

            updateTransactionDisplay()

            if (d.type == DebtType.I_OWE) {
                addPaymentButton.text = "💳 Make Payment"
                addLoanButton.text = "➕ Add to Debt"
            } else {
                addPaymentButton.text = "💸 Receive Payment"
                addLoanButton.text = "➕ Lend More"
            }
        }
    }

    private fun updateTransactionDisplay() {
        if (allTransactions.isEmpty()) {
            emptyTransactionsText.visibility = View.VISIBLE
            transactionsRecyclerView.visibility = View.GONE
            findViewById<LinearLayout>(R.id.paginationControls).visibility = View.GONE
        } else {
            emptyTransactionsText.visibility = View.GONE
            transactionsRecyclerView.visibility = View.VISIBLE

            val totalPages = (allTransactions.size + itemsPerPage - 1) / itemsPerPage

            // Show pagination controls only if more than one page
            if (totalPages > 1) {
                findViewById<LinearLayout>(R.id.paginationControls).visibility = View.VISIBLE

                // Update page info
                pageInfoText.text = "Page ${currentPage + 1} of $totalPages"

                // Enable/disable buttons based on current page
                previousButton.isEnabled = currentPage > 0
                previousButton.alpha = if (currentPage > 0) 1.0f else 0.5f

                nextButton.isEnabled = currentPage < totalPages - 1
                nextButton.alpha = if (currentPage < totalPages - 1) 1.0f else 0.5f
            } else {
                findViewById<LinearLayout>(R.id.paginationControls).visibility = View.GONE
            }

            // Get transactions for current page
            val startIndex = currentPage * itemsPerPage
            val endIndex = minOf(startIndex + itemsPerPage, allTransactions.size)
            val pageTransactions = allTransactions.subList(startIndex, endIndex)

            transactionAdapter.updateTransactions(pageTransactions, debt?.currency ?: com.iliass.iliass.model.Currency.KMF)
        }
    }

    private fun setupButtons() {
        addPaymentButton.setOnClickListener {
            showAddTransactionDialog(TransactionType.PAYMENT)
        }

        addLoanButton.setOnClickListener {
            showAddTransactionDialog(TransactionType.ADDITIONAL_LOAN)
        }

        previousButton.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                updateTransactionDisplay()
            }
        }

        nextButton.setOnClickListener {
            val totalPages = (allTransactions.size + itemsPerPage - 1) / itemsPerPage
            if (currentPage < totalPages - 1) {
                currentPage++
                updateTransactionDisplay()
            }
        }
    }

    private fun showAddTransactionDialog(type: TransactionType) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.transactionAmountInput)
        val noteInput = dialogView.findViewById<EditText>(R.id.transactionNoteInput)

        val title = when (type) {
            TransactionType.PAYMENT -> if (debt?.type == DebtType.I_OWE) "Make Payment" else "Receive Payment"
            TransactionType.ADDITIONAL_LOAN -> if (debt?.type == DebtType.I_OWE) "Add to Debt" else "Lend More"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amountStr = amountInput.text.toString().trim()
                val note = noteInput.text.toString().trim()

                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    val transaction = DebtTransaction(
                        type = type,
                        amount = amount,
                        note = note
                    )
                    debt?.id?.let { debtId ->
                        debtDatabase.addTransaction(debtId, transaction)
                        currentPage = 0 // Reset to first page when adding new transaction
                        loadDebt()
                    }
                } else {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteTransactionDialog(transaction: DebtTransaction) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                showPasswordDialogForTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPasswordDialogForTransaction(transaction: DebtTransaction) {
        val passwordInput = EditText(this).apply {
            hint = "Enter PIN to delete"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Confirm Deletion")
            .setMessage("Enter the PIN to confirm deletion of this transaction.")
            .setView(passwordInput)
            .setPositiveButton("Confirm") { _, _ ->
                val enteredPassword = passwordInput.text.toString().trim()
                if (enteredPassword == "12345") {
                    deleteTransaction(transaction)
                } else {
                    Toast.makeText(this, "❌ Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transaction: DebtTransaction) {
        debt?.let { currentDebt ->
            currentDebt.transactions.removeIf { it.id == transaction.id }

            // Recalculate if debt should be marked as paid
            val remaining = currentDebt.getRemainingAmount()
            val updatedDebt = if (remaining <= 0 && currentDebt.transactions.isNotEmpty()) {
                currentDebt.copy(isPaid = true)
            } else {
                currentDebt.copy(isPaid = false)
            }

            debtDatabase.updateDebt(updatedDebt)
            Toast.makeText(this, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()

            // Adjust current page if needed after deletion
            val totalPages = (currentDebt.transactions.size + itemsPerPage - 1) / itemsPerPage
            if (currentPage >= totalPages && currentPage > 0) {
                currentPage = totalPages - 1
            }

            loadDebt()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_debt_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showPasswordDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPasswordDialog() {
        val passwordInput = EditText(this).apply {
            hint = "Enter password to delete"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Delete Debt")
            .setMessage("Enter the password to confirm deletion of this debt record.")
            .setView(passwordInput)
            .setPositiveButton("Delete") { _, _ ->
                val enteredPassword = passwordInput.text.toString().trim()
                if (enteredPassword == "12345") {
                    debt?.id?.let { debtId ->
                        debtDatabase.deleteDebt(debtId)
                        Toast.makeText(this, "Debt deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "❌ Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}