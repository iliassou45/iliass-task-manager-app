package com.iliass.iliass

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.iliass.iliass.adapter.ClassAdapter
import com.iliass.iliass.model.StudentClass
import com.iliass.iliass.repository.StudentDatabase
import com.iliass.iliass.util.StudentDataExportManager
import java.io.File
import java.text.DecimalFormat

class ClassesActivity : AppCompatActivity() {

    private lateinit var database: StudentDatabase
    private lateinit var classAdapter: ClassAdapter
    private lateinit var classesRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var fabAddClass: FloatingActionButton

    private lateinit var activeClassesText: TextView
    private lateinit var totalStudentsText: TextView
    private lateinit var monthlyRevenueText: TextView
    private lateinit var totalDebtText: TextView

    private val decimalFormat = DecimalFormat("#,##0.00")
    private var pendingImportMergeMode = false

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            performImport(it, pendingImportMergeMode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classes)

        supportActionBar?.title = "Student Groups/Classes"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        database = StudentDatabase.getInstance(this)

        initializeViews()
        setupRecyclerView()
        setupTabLayout()
        setupFAB()
        updateUI()
    }

    private fun initializeViews() {
        classesRecyclerView = findViewById(R.id.classesRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        tabLayout = findViewById(R.id.tabLayout)
        fabAddClass = findViewById(R.id.fabAddClass)

        activeClassesText = findViewById(R.id.activeClassesText)
        totalStudentsText = findViewById(R.id.totalStudentsText)
        monthlyRevenueText = findViewById(R.id.monthlyRevenueText)
        totalDebtText = findViewById(R.id.totalDebtText)
    }

    private fun setupRecyclerView() {
        classAdapter = ClassAdapter(emptyList(), database) { studentClass ->
            openClassDetail(studentClass)
        }
        classesRecyclerView.adapter = classAdapter
        classesRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Active"))
        tabLayout.addTab(tabLayout.newTab().setText("Inactive"))
        tabLayout.addTab(tabLayout.newTab().setText("All"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateClassesList()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFAB() {
        fabAddClass.setOnClickListener {
            val intent = Intent(this, AddEditClassActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateUI() {
        updateStatistics()
        updateClassesList()
    }

    private fun updateStatistics() {
        val allClasses = database.getAllClasses()
        val activeClasses = allClasses.filter { it.isActive }

        activeClassesText.text = activeClasses.size.toString()

        // Total students across all classes
        val totalStudents = allClasses.sumOf { it.getCurrentStudentCount() }
        totalStudentsText.text = totalStudents.toString()

        // Monthly revenue potential
        val monthlyRevenue = allClasses.sumOf { it.getMonthlyRevenuePotential(database) }
        monthlyRevenueText.text = "$${decimalFormat.format(monthlyRevenue)}"

        // Total debt
        val totalDebt = allClasses.sumOf { it.getTotalClassDebt(database) }
        totalDebtText.text = "$${decimalFormat.format(totalDebt)}"
    }

    private fun updateClassesList() {
        val classes = when (tabLayout.selectedTabPosition) {
            0 -> database.getActiveClasses()
            1 -> database.getInactiveClasses()
            else -> database.getAllClasses().sortedBy { it.name }
        }

        classAdapter.updateClasses(classes)

        if (classes.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            classesRecyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            classesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun openClassDetail(studentClass: StudentClass) {
        val intent = Intent(this, ClassDetailActivity::class.java)
        intent.putExtra("CLASS_ID", studentClass.id)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_classes, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_all_json -> {
                exportAllToJson()
                true
            }
            R.id.action_export_classes_csv -> {
                exportClassesToCsv()
                true
            }
            R.id.action_export_lessons_csv -> {
                exportLessonsToCsv()
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

    private fun exportClassesToCsv() {
        when (val result = StudentDataExportManager.exportClassesToCsv(this)) {
            is StudentDataExportManager.ExportResult.Success -> {
                showExportSuccessDialog(result.filePath, result.format)
            }
            is StudentDataExportManager.ExportResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportLessonsToCsv() {
        when (val result = StudentDataExportManager.exportLessonsToCsv(this)) {
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
                        updateUI()
                    }
                    .show()
            }
            is StudentDataExportManager.ImportResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
