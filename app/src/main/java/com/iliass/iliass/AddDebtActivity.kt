package com.iliass.iliass

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.iliass.iliass.model.Currency
import com.iliass.iliass.model.Debt
import com.iliass.iliass.model.DebtType
import com.iliass.iliass.repository.DebtDatabase

class AddDebtActivity : AppCompatActivity() {

    private lateinit var personNameInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var reasonInput: EditText
    private lateinit var currencyRadioGroup: RadioGroup
    private lateinit var saveButton: Button

    private val debtDatabase by lazy { DebtDatabase.getInstance(this) }
    private var debtType: DebtType = DebtType.I_OWE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_debt)

        val typeString = intent.getStringExtra("DEBT_TYPE") ?: "I_OWE"
        debtType = if (typeString == "I_OWE") DebtType.I_OWE else DebtType.OWED_TO_ME

        supportActionBar?.title = if (debtType == DebtType.I_OWE) {
            "Add Debt I Owe"
        } else {
            "Add Debt Owed to Me"
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        personNameInput = findViewById(R.id.personNameInput)
        amountInput = findViewById(R.id.amountInput)
        reasonInput = findViewById(R.id.reasonInput)
        currencyRadioGroup = findViewById(R.id.currencyRadioGroup)
        saveButton = findViewById(R.id.saveButton)

        personNameInput.hint = if (debtType == DebtType.I_OWE) {
            "Person you owe"
        } else {
            "Person who owes you"
        }
    }

    private fun setupListeners() {
        saveButton.setOnClickListener {
            saveDebt()
        }
    }

    private fun saveDebt() {
        val personName = personNameInput.text.toString().trim()
        val amountStr = amountInput.text.toString().trim()
        val reason = reasonInput.text.toString().trim()

        if (personName.isEmpty()) {
            Toast.makeText(this, "Please enter person's name", Toast.LENGTH_SHORT).show()
            return
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (reason.isEmpty()) {
            Toast.makeText(this, "Please enter reason", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected currency
        val selectedCurrency = when (currencyRadioGroup.checkedRadioButtonId) {
            R.id.radioUGX -> Currency.UGX
            else -> Currency.KMF
        }

        val debt = Debt(
            personName = personName,
            type = debtType,
            initialAmount = amount,
            reason = reason,
            currency = selectedCurrency
        )

        debtDatabase.addDebt(debt)

        Toast.makeText(this, "Debt added successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}