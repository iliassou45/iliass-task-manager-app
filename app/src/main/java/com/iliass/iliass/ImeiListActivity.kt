package com.iliass.iliass

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.Phone
import com.iliass.iliass.ui.adapter.PhoneAdapter
import com.iliass.iliass.util.PhoneStorageManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class ImeiListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var phoneAdapter: PhoneAdapter
    private lateinit var storageManager: PhoneStorageManager
    private lateinit var importExportManager: ImportExportManager
    private lateinit var searchEditText: TextInputEditText
    private lateinit var emptyStateText: TextView
    private lateinit var addPhoneFab: FloatingActionButton
    private lateinit var menuFab: FloatingActionButton

    private val createJsonFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportToUri(it, ExportFormat.JSON) }
    }

    private val createCsvFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { exportToUri(it, ExportFormat.CSV) }
    }

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imei_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        storageManager = PhoneStorageManager(this)
        importExportManager = ImportExportManager(this)

        setupViews()
        setupRecyclerView()
        setupSearch()
        loadPhones()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.phonesRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        emptyStateText = findViewById(R.id.emptyStateText)
        addPhoneFab = findViewById(R.id.addPhoneFab)
        menuFab = findViewById(R.id.menuFab)

        addPhoneFab.setOnClickListener {
            val intent = Intent(this, EmeiDetailActivity::class.java)
            startActivity(intent)
        }

        menuFab.setOnClickListener {
            showImportExportMenu()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        phoneAdapter = PhoneAdapter(emptyList()) { phone ->
            openPhoneDetails(phone)
        }
        recyclerView.adapter = phoneAdapter
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                val filteredPhones = storageManager.searchPhones(query)
                phoneAdapter.updatePhones(filteredPhones)
                updateEmptyState(filteredPhones.isEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadPhones() {
        val phones = storageManager.getAllPhones()
        phoneAdapter.updatePhones(phones)
        updateEmptyState(phones.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun openPhoneDetails(phone: Phone) {
        val intent = Intent(this, EmeiDetailActivity::class.java)
        intent.putExtra("phone", phone)
        startActivity(intent)
    }

    private fun showImportExportMenu() {
        val options = arrayOf(
            "📤 Export to JSON (with images)",
            "📤 Export to CSV (without images)",
            "📥 Import from JSON"
        )

        AlertDialog.Builder(this)
            .setTitle("Import/Export")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportJson()
                    1 -> exportCsv()
                    2 -> importJson()
                }
            }
            .show()
    }

    private fun exportJson() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "phone_inventory_$timestamp.json"
        createJsonFileLauncher.launch(fileName)
    }

    private fun exportCsv() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "phone_inventory_$timestamp.csv"
        createCsvFileLauncher.launch(fileName)
    }

    private fun importJson() {
        AlertDialog.Builder(this)
            .setTitle("Import Data")
            .setMessage("This will add phones from the file to your existing inventory. Existing phones won't be affected.\n\nContinue?")
            .setPositiveButton("Import") { _, _ ->
                importFileLauncher.launch(arrayOf("application/json", "text/plain"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportToUri(uri: Uri, format: ExportFormat) {
        try {
            val content = when (format) {
                ExportFormat.JSON -> importExportManager.exportToJson()
                ExportFormat.CSV -> importExportManager.exportToCsv()
            }

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }

            val formatName = when (format) {
                ExportFormat.JSON -> "JSON"
                ExportFormat.CSV -> "CSV"
            }

            Toast.makeText(
                this,
                "✅ Successfully exported to $formatName",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "❌ Export failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun importFromUri(uri: Uri) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: ""

            val result = importExportManager.importFromJson(content)

            if (result.success) {
                AlertDialog.Builder(this)
                    .setTitle("✅ Import Successful")
                    .setMessage(result.getMessage())
                    .setPositiveButton("OK") { _, _ ->
                        loadPhones()
                    }
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("❌ Import Failed")
                    .setMessage(result.getMessage())
                    .setPositiveButton("OK", null)
                    .show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("❌ Import Failed")
                .setMessage("Error reading file: ${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPhones()
    }

    private enum class ExportFormat {
        JSON, CSV
    }
}