package com.iliass.iliass

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.Payment
import com.iliass.iliass.repository.StudentDatabase
import java.text.SimpleDateFormat
import java.util.*

class AddPaymentActivity : AppCompatActivity() {

    private lateinit var studentNameHeader: TextView
    private lateinit var amountInput: TextInputEditText
    private lateinit var monthForInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var savePaymentButton: Button

    private val studentDatabase by lazy { StudentDatabase.getInstance(this) }
    private var studentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_payment)

        supportActionBar?.title = "Add Payment"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        if (studentId.isEmpty()) {
            Toast.makeText(this, "Error: Student not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadStudentInfo()
        setupSaveButton()
        setDefaultMonthFor()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initViews() {
        studentNameHeader = findViewById(R.id.studentNameHeader)
        amountInput = findViewById(R.id.amountInput)
        monthForInput = findViewById(R.id.monthForInput)
        notesInput = findViewById(R.id.notesInput)
        savePaymentButton = findViewById(R.id.savePaymentButton)
    }

    private fun loadStudentInfo() {
        val student = studentDatabase.getStudentById(studentId)
        if (student == null) {
            Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        studentNameHeader.text = student.name
        amountInput.setText(student.monthlyAmount.toString())
    }

    private fun setDefaultMonthFor() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonth = dateFormat.format(Date())
        monthForInput.setText(currentMonth)
    }

    private fun setupSaveButton() {
        savePaymentButton.setOnClickListener {
            if (validateInputs()) {
                savePayment()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val amountStr = amountInput.text.toString().trim()

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter payment amount", Toast.LENGTH_SHORT).show()
            return false
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun savePayment() {
        val amount = amountInput.text.toString().toDouble()
        val monthFor = monthForInput.text.toString().trim()
        val notes = notesInput.text.toString().trim()

        val payment = Payment(
            studentId = studentId,
            amount = amount,
            monthFor = monthFor,
            notes = notes
        )

        studentDatabase.addPayment(payment)
        Toast.makeText(this, "Payment recorded successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
}
