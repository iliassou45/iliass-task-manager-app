package com.iliass.iliass

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.iliass.iliass.model.Student
import com.iliass.iliass.repository.StudentDatabase
import com.iliass.iliass.ui.adapter.StudentAdapter
import java.text.NumberFormat
import java.util.*

class StudentPaymentActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var fabAddStudent: FloatingActionButton
    private lateinit var emptyView: TextView
    private lateinit var activeStudentsText: TextView
    private lateinit var lostStudentsText: TextView
    private lateinit var totalThisMonthText: TextView
    private lateinit var totalThisYearText: TextView

    private val studentDatabase by lazy { StudentDatabase.getInstance(this) }
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_payment)

        supportActionBar?.title = "Student Payments"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupTabs()
        setupRecyclerView()
        setupFab()
    }

    override fun onResume() {
        super.onResume()
        loadStudents()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.studentsRecyclerView)
        fabAddStudent = findViewById(R.id.fabAddStudent)
        emptyView = findViewById(R.id.emptyView)
        activeStudentsText = findViewById(R.id.activeStudentsText)
        lostStudentsText = findViewById(R.id.lostStudentsText)
        totalThisMonthText = findViewById(R.id.totalThisMonthText)
        totalThisYearText = findViewById(R.id.totalThisYearText)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Active"))
        tabLayout.addTab(tabLayout.newTab().setText("Inactive"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                loadStudents()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter(
            students = emptyList(),
            allPayments = emptyList(),
            onStudentClick = { student ->
                val intent = Intent(this, StudentDetailActivity::class.java)
                intent.putExtra("STUDENT_ID", student.id)
                startActivity(intent)
            },
            onStudentLongClick = { student ->
                showStudentOptionsDialog(student)
            }
        )
        recyclerView.adapter = studentAdapter
    }

    private fun setupFab() {
        fabAddStudent.setOnClickListener {
            val intent = Intent(this, AddEditStudentActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadStudents() {
        val students = if (currentTab == 0) {
            studentDatabase.getActiveStudents()
        } else {
            studentDatabase.getInactiveStudents()
        }

        val allPayments = studentDatabase.getAllPayments()

        if (students.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyView.text = if (currentTab == 0) {
                "No active students.\nTap + to add one."
            } else {
                "No inactive students."
            }
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        studentAdapter.updateData(students, allPayments)
        updateStatistics()
    }

    private fun updateStatistics() {
        val activeCount = studentDatabase.getActiveStudentCount()
        val inactiveCount = studentDatabase.getInactiveStudentCount()
        val totalThisMonth = studentDatabase.getTotalPaymentsThisMonth()
        val totalThisYear = studentDatabase.getTotalPaymentsThisYear()

        activeStudentsText.text = activeCount.toString()
        lostStudentsText.text = inactiveCount.toString()

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        totalThisMonthText.text = currencyFormat.format(totalThisMonth)
        totalThisYearText.text = currencyFormat.format(totalThisYear)
    }

    private fun showStudentOptionsDialog(student: Student) {
        val options = if (student.isActive) {
            arrayOf("Edit", "Mark as Inactive", "Delete")
        } else {
            arrayOf("Edit", "Mark as Active", "Delete")
        }

        AlertDialog.Builder(this)
            .setTitle(student.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, AddEditStudentActivity::class.java)
                        intent.putExtra("STUDENT_ID", student.id)
                        startActivity(intent)
                    }
                    1 -> {
                        val updatedStudent = student.copy(isActive = !student.isActive)
                        studentDatabase.updateStudent(updatedStudent)
                        loadStudents()
                        Toast.makeText(
                            this,
                            "Student marked as ${if (updatedStudent.isActive) "active" else "inactive"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> showDeleteStudentDialog(student)
                }
            }
            .show()
    }

    private fun showDeleteStudentDialog(student: Student) {
        val paymentCount = studentDatabase.getPaymentsByStudent(student.id).size
        val message = if (paymentCount > 0) {
            "Are you sure you want to delete ${student.name}?\n\nThis will also delete $paymentCount payment(s) associated with this student."
        } else {
            "Are you sure you want to delete ${student.name}?"
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                studentDatabase.deleteStudent(student.id)
                loadStudents()
                Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
