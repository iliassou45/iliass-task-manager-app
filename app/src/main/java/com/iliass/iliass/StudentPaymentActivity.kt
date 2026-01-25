package com.iliass.iliass

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.iliass.iliass.model.Student
import com.iliass.iliass.repository.StudentDatabase
import com.iliass.iliass.ui.adapter.StudentAdapter
import com.iliass.iliass.util.StudentDataExportManager
import java.io.File
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
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchButton: ImageButton

    private val studentDatabase by lazy { StudentDatabase.getInstance(this) }
    private var currentTab = 0
    private var pendingImportMergeMode = false
    private var currentSearchQuery = ""
    private var allStudentsList: List<Student> = emptyList()

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            performImport(it, pendingImportMergeMode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_payment)

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupSearch()
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
        searchEditText = findViewById(R.id.searchEditText)
        clearSearchButton = findViewById(R.id.clearSearchButton)
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString() ?: ""
                clearSearchButton.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                filterStudents()
            }
        })

        clearSearchButton.setOnClickListener {
            searchEditText.text.clear()
            currentSearchQuery = ""
            filterStudents()
        }
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
            },
            onWhatsAppClick = { student ->
                openWhatsApp(student)
            }
        )
        recyclerView.adapter = studentAdapter
    }

    private fun openWhatsApp(student: Student) {
        if (student.phone.isEmpty()) {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show()
            return
        }

        // Clean the phone number - remove spaces, dashes, etc.
        val phoneNumber = student.phone.replace(Regex("[^0-9+]"), "")

        try {
            // Try to open WhatsApp directly with the phone number
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFab() {
        fabAddStudent.setOnClickListener {
            val intent = Intent(this, AddEditStudentActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadStudents() {
        allStudentsList = if (currentTab == 0) {
            studentDatabase.getActiveStudents()
        } else {
            studentDatabase.getInactiveStudents()
        }

        filterStudents()
        updateStatistics()
    }

    private fun filterStudents() {
        val allPayments = studentDatabase.getAllPayments()

        val filteredStudents = if (currentSearchQuery.isEmpty()) {
            allStudentsList
        } else {
            allStudentsList.filter { student ->
                student.name.contains(currentSearchQuery, ignoreCase = true) ||
                student.phone.contains(currentSearchQuery, ignoreCase = true) ||
                student.email.contains(currentSearchQuery, ignoreCase = true) ||
                student.location.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        if (filteredStudents.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyView.text = if (currentSearchQuery.isNotEmpty()) {
                "No students found matching \"$currentSearchQuery\""
            } else if (currentTab == 0) {
                "No active students.\nTap + to add one."
            } else {
                "No inactive students."
            }
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        studentAdapter.updateData(filteredStudents, allPayments)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_student_payment, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_all_json -> {
                exportAllToJson()
                true
            }
            R.id.action_export_students_csv -> {
                exportStudentsToCsv()
                true
            }
            R.id.action_export_payments_csv -> {
                exportPaymentsToCsv()
                true
            }
            R.id.action_import_json -> {
                showImportConfirmation(false)
                true
            }
            R.id.action_import_merge_json -> {
                showImportConfirmation(true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportAllToJson() {
        when (val result = StudentDataExportManager.exportToJson(this)) {
            is StudentDataExportManager.ExportResult.Success -> {
                showExportSuccessDialog(result.filePath, result.format)
            }
            is StudentDataExportManager.ExportResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportStudentsToCsv() {
        when (val result = StudentDataExportManager.exportStudentsToCsv(this)) {
            is StudentDataExportManager.ExportResult.Success -> {
                showExportSuccessDialog(result.filePath, result.format)
            }
            is StudentDataExportManager.ExportResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportPaymentsToCsv() {
        when (val result = StudentDataExportManager.exportPaymentsToCsv(this)) {
            is StudentDataExportManager.ExportResult.Success -> {
                showExportSuccessDialog(result.filePath, result.format)
            }
            is StudentDataExportManager.ExportResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showExportSuccessDialog(filePath: String, format: String) {
        AlertDialog.Builder(this)
            .setTitle("Export Successful")
            .setMessage("Data exported to $format file:\n\n$filePath")
            .setPositiveButton("Share") { _, _ ->
                shareExportedFile(filePath)
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun shareExportedFile(filePath: String) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (filePath.endsWith(".json")) "application/json" else "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Export File"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImportConfirmation(mergeMode: Boolean) {
        val title = if (mergeMode) "Import & Merge Data" else "Import & Replace Data"
        val message = if (mergeMode) {
            "This will add new data from the backup file without replacing existing data.\n\n${StudentDataExportManager.getDataSummary(this)}\n\nContinue?"
        } else {
            "WARNING: This will REPLACE all existing data with the backup file.\n\n${StudentDataExportManager.getDataSummary(this)}\n\nContinue?"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Import") { _, _ ->
                pendingImportMergeMode = mergeMode
                importLauncher.launch("application/json")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performImport(uri: android.net.Uri, mergeMode: Boolean) {
        when (val result = StudentDataExportManager.importFromJson(this, uri, mergeMode)) {
            is StudentDataExportManager.ImportResult.Success -> {
                AlertDialog.Builder(this)
                    .setTitle("Import Successful")
                    .setMessage("Imported:\n- ${result.studentsCount} students\n- ${result.paymentsCount} payments\n- ${result.classesCount} classes\n- ${result.lessonsCount} lessons")
                    .setPositiveButton("OK") { _, _ ->
                        loadStudents()
                    }
                    .show()
            }
            is StudentDataExportManager.ImportResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
