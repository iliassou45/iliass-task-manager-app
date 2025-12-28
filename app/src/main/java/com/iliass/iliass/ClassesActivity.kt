package com.iliass.iliass

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.iliass.iliass.adapter.ClassAdapter
import com.iliass.iliass.model.StudentClass
import com.iliass.iliass.repository.StudentDatabase
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
}
