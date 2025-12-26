package com.iliass.iliass

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var selectAllButton: Button
    private lateinit var continueButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView

    private val allApps = mutableListOf<SelectableApp>()
    private val filteredApps = mutableListOf<SelectableApp>()
    private lateinit var adapter: AppSelectionAdapter
    private val TAG = "AppSelectionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        initViews()
        loadApps()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.appsRecyclerView)
        searchBar = findViewById(R.id.searchBar)
        selectAllButton = findViewById(R.id.selectAllButton)
        continueButton = findViewById(R.id.continueButton)
        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppSelectionAdapter(filteredApps) { updateContinueButton() }
        recyclerView.adapter = adapter

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        selectAllButton.setOnClickListener {
            toggleSelectAll()
        }

        continueButton.setOnClickListener {
            showBlockSettingsDialog()
        }

        updateContinueButton()
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        searchBar.visibility = View.GONE
        selectAllButton.visibility = View.GONE

        Thread {
            try {
                val pm = packageManager
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                val apps = mutableListOf<SelectableApp>()

                for (appInfo in installedApps) {
                    try {
                        // Skip this app
                        if (appInfo.packageName == packageName) continue

                        // Only include apps with launcher intent
                        val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                        if (launchIntent != null) {
                            apps.add(SelectableApp(
                                packageName = appInfo.packageName,
                                name = pm.getApplicationLabel(appInfo).toString(),
                                icon = pm.getApplicationIcon(appInfo),
                                isSelected = false
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing: ${appInfo.packageName}", e)
                    }
                }

                apps.sortBy { it.name.lowercase() }

                runOnUiThread {
                    allApps.clear()
                    allApps.addAll(apps)
                    filteredApps.clear()
                    filteredApps.addAll(apps)

                    adapter.notifyDataSetChanged()

                    progressBar.visibility = View.GONE
                    loadingText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    searchBar.visibility = View.VISIBLE
                    selectAllButton.visibility = View.VISIBLE

                    Log.d(TAG, "Loaded ${apps.size} apps")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps", e)
                runOnUiThread {
                    Toast.makeText(this, "Error loading apps", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.start()
    }

    private fun filterApps(query: String) {
        filteredApps.clear()

        if (query.isEmpty()) {
            filteredApps.addAll(allApps)
        } else {
            val lowerQuery = query.lowercase()
            filteredApps.addAll(allApps.filter {
                it.name.lowercase().contains(lowerQuery)
            })
        }

        adapter.notifyDataSetChanged()
    }

    private fun toggleSelectAll() {
        val allSelected = filteredApps.all { it.isSelected }

        filteredApps.forEach { it.isSelected = !allSelected }
        allApps.forEach { app ->
            val filtered = filteredApps.find { it.packageName == app.packageName }
            if (filtered != null) {
                app.isSelected = filtered.isSelected
            }
        }

        adapter.notifyDataSetChanged()
        updateContinueButton()
    }

    private fun updateContinueButton() {
        val selectedCount = allApps.count { it.isSelected }

        if (selectedCount > 0) {
            continueButton.isEnabled = true
            continueButton.text = "Continue ($selectedCount selected)"
        } else {
            continueButton.isEnabled = false
            continueButton.text = "Select apps to continue"
        }
    }

    private fun showBlockSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_block_settings, null)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val enableTimer = dialogView.findViewById<CheckBox>(R.id.enableTimerCheckbox)
        val permanentBlock = dialogView.findViewById<CheckBox>(R.id.permanentBlockCheckbox)
        val timerLayout = dialogView.findViewById<LinearLayout>(R.id.timerLayout)

        timePicker.setIs24HourView(true)
        timePicker.hour = 1
        timePicker.minute = 0

        permanentBlock.isChecked = true

        enableTimer.setOnCheckedChangeListener { _, isChecked ->
            timerLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) permanentBlock.isChecked = false
        }

        permanentBlock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableTimer.isChecked = false
                timerLayout.visibility = View.GONE
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Block Settings")
            .setView(dialogView)
            .setPositiveButton("Block Apps") { _, _ ->
                val durationMinutes = if (enableTimer.isChecked) {
                    timePicker.hour * 60 + timePicker.minute
                } else {
                    -1
                }

                val selectedPackages = allApps.filter { it.isSelected }
                    .map { it.packageName }

                val resultIntent = Intent().apply {
                    putStringArrayListExtra("selected_packages", ArrayList(selectedPackages))
                    putExtra("duration_minutes", durationMinutes)
                }

                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

data class SelectableApp(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    var isSelected: Boolean
)

class AppSelectionAdapter(
    private val apps: List<SelectableApp>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val checkBox: CheckBox = view.findViewById(R.id.appCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selectable_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]

        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.checkBox.isChecked = app.isSelected

        holder.itemView.setOnClickListener {
            app.isSelected = !app.isSelected
            holder.checkBox.isChecked = app.isSelected
            onSelectionChanged()
        }

        holder.checkBox.setOnClickListener {
            app.isSelected = holder.checkBox.isChecked
            onSelectionChanged()
        }
    }

    override fun getItemCount() = apps.size
}