package com.iliass.iliass

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.model.Payment
import com.iliass.iliass.model.Student
import com.iliass.iliass.repository.StudentDatabase
import com.iliass.iliass.ui.adapter.PaymentAdapter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class StudentDetailActivity : AppCompatActivity() {

    private lateinit var studentNameText: TextView
    private lateinit var contactInfoText: TextView
    private lateinit var monthlyAmountText: TextView
    private lateinit var currentDebtText: TextView
    private lateinit var paidThisMonthText: TextView
    private lateinit var paidThisYearText: TextView
    private lateinit var totalPaidText: TextView
    private lateinit var enrollmentDateText: TextView
    private lateinit var studentNotesText: TextView
    private lateinit var addPaymentButton: Button
    private lateinit var editStudentButton: Button
    private lateinit var paymentsRecyclerView: RecyclerView
    private lateinit var emptyPaymentsText: TextView

    private lateinit var paymentAdapter: PaymentAdapter
    private val studentDatabase by lazy { StudentDatabase.getInstance(this) }
    private var studentId: String = ""
    private var currentStudent: Student? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_detail)

        supportActionBar?.title = "Student Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        if (studentId.isEmpty()) {
            Toast.makeText(this, "Error: Student not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        loadStudentDetails()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initViews() {
        studentNameText = findViewById(R.id.studentNameText)
        contactInfoText = findViewById(R.id.contactInfoText)
        monthlyAmountText = findViewById(R.id.monthlyAmountText)
        currentDebtText = findViewById(R.id.currentDebtText)
        paidThisMonthText = findViewById(R.id.paidThisMonthText)
        paidThisYearText = findViewById(R.id.paidThisYearText)
        totalPaidText = findViewById(R.id.totalPaidText)
        enrollmentDateText = findViewById(R.id.enrollmentDateText)
        studentNotesText = findViewById(R.id.studentNotesText)
        addPaymentButton = findViewById(R.id.addPaymentButton)
        editStudentButton = findViewById(R.id.editStudentButton)
        paymentsRecyclerView = findViewById(R.id.paymentsRecyclerView)
        emptyPaymentsText = findViewById(R.id.emptyPaymentsText)
    }

    private fun setupRecyclerView() {
        paymentsRecyclerView.layoutManager = LinearLayoutManager(this)
        paymentAdapter = PaymentAdapter(
            payments = emptyList(),
            onPaymentClick = null,
            onPaymentLongClick = { payment ->
                showDeletePaymentDialog(payment)
            }
        )
        paymentsRecyclerView.adapter = paymentAdapter
    }

    private fun setupButtons() {
        addPaymentButton.setOnClickListener {
            val intent = Intent(this, AddPaymentActivity::class.java)
            intent.putExtra("STUDENT_ID", studentId)
            startActivity(intent)
        }

        editStudentButton.setOnClickListener {
            val intent = Intent(this, AddEditStudentActivity::class.java)
            intent.putExtra("STUDENT_ID", studentId)
            startActivity(intent)
        }
    }

    private fun loadStudentDetails() {
        currentStudent = studentDatabase.getStudentById(studentId)
        val student = currentStudent

        if (student == null) {
            Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val payments = studentDatabase.getPaymentsByStudent(studentId)
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        // Display student info
        studentNameText.text = student.name
        supportActionBar?.title = student.name

        val contactInfo = buildString {
            if (student.phone.isNotEmpty()) {
                append("Phone: ${student.phone}")
            }
            if (student.email.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append("Email: ${student.email}")
            }
        }

        if (contactInfo.isNotEmpty()) {
            contactInfoText.text = contactInfo
            contactInfoText.visibility = View.VISIBLE
        } else {
            contactInfoText.visibility = View.GONE
        }

        monthlyAmountText.text = currencyFormat.format(student.monthlyAmount)

        val currentDebt = student.getCurrentDebt(payments)
        currentDebtText.text = currencyFormat.format(currentDebt)
        currentDebtText.setTextColor(if (currentDebt > 0) 0xFFE53935.toInt() else 0xFF43A047.toInt())

        paidThisMonthText.text = currencyFormat.format(student.getTotalPaidThisMonth(payments))
        paidThisYearText.text = currencyFormat.format(student.getTotalPaidThisYear(payments))
        totalPaidText.text = currencyFormat.format(student.getTotalPaid(payments))

        enrollmentDateText.text = "Enrolled: ${dateFormat.format(Date(student.enrollmentDate))}"

        if (student.notes.isNotEmpty()) {
            studentNotesText.text = "Notes: ${student.notes}"
            studentNotesText.visibility = View.VISIBLE
        } else {
            studentNotesText.visibility = View.GONE
        }

        // Display payments
        if (payments.isEmpty()) {
            emptyPaymentsText.visibility = View.VISIBLE
            paymentsRecyclerView.visibility = View.GONE
        } else {
            emptyPaymentsText.visibility = View.GONE
            paymentsRecyclerView.visibility = View.VISIBLE
            paymentAdapter.updatePayments(payments)
        }
    }

    private fun showDeletePaymentDialog(payment: Payment) {
        AlertDialog.Builder(this)
            .setTitle("Delete Payment")
            .setMessage("Are you sure you want to delete this payment?")
            .setPositiveButton("Delete") { _, _ ->
                studentDatabase.deletePayment(payment.id)
                loadStudentDetails()
                Toast.makeText(this, "Payment deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
