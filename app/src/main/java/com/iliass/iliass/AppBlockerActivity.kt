package com.iliass.iliass

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.ui.adapter.BlockedAppsAdapter
import com.iliass.iliass.util.BatteryOptimizationHelper
import com.iliass.iliass.util.ServiceStatusManager

class AppBlockerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BlockedAppsAdapter
    private lateinit var addAppButton: Button
    private lateinit var emptyView: LinearLayout
    private val blockedApps = mutableListOf<BlockedApp>()
    private val prefs by lazy { getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE) }
    private val TAG = "AppBlockerActivity"

    companion object {
        const val REQUEST_SELECT_APPS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_blocker)

        initViews()
        checkPermissions()

        // Check battery optimization on first launch
        checkBatteryOptimization()
    }

    override fun onResume() {
        super.onResume()
        loadBlockedApps()
        updateUI()

        // Check if service is running, if not start it
        if (checkUsageStatsPermission()) {
            startBlockerService()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_app_blocker, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_service_status -> {
                showServiceStatus()
                true
            }
            R.id.action_battery_settings -> {
                showBatteryOptimizationInstructions()
                true
            }
            R.id.action_restart_service -> {
                restartServiceWithConfirmation()
                true
            }
            R.id.action_help -> {
                showHelpDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun restartServiceWithConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Restart Service")
            .setMessage("This will restart the App Blocker service. Do you want to continue?")
            .setPositiveButton("Restart") { dialog, _ ->
                if (ServiceStatusManager.restartService(this)) {
                    Toast.makeText(this, "Service restarted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to restart service", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHelpDialog() {
        val helpText = """
            📱 App Blocker Help
            
            ━━━━━━━━━━━━━━━━━━━━━━
            
            🔒 How it works:
            • Monitors foreground apps
            • Blocks access to selected apps
            • Shows block screen when triggered
            
            ⚙️ Setup Requirements:
            ✓ Usage Access permission
            ✓ Display over other apps
            ✓ Battery optimization disabled
            
            🔋 Battery Optimization:
            Go to Settings → Battery Settings
            Follow manufacturer instructions
            
            🛠️ Troubleshooting:
            
            Service not working?
            • Check Service Status menu
            • Restart the service
            • Verify all permissions granted
            • Disable battery optimization
            
            Apps not blocking?
            • Ensure blocks are active (toggle on)
            • Check block hasn't expired
            • Verify Usage Access enabled
            • Try restarting service
            
            Service keeps stopping?
            • Disable battery optimization
            • Follow manufacturer instructions
            • Lock app in recent apps
            • Enable "Autostart" (if available)
            
            📱 Manufacturer Settings:
            
            ${BatteryOptimizationHelper.getManufacturerSpecificInstructions()}
            
            ━━━━━━━━━━━━━━━━━━━━━━
            
            Still having issues?
            Check Service Status for details.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Help & Troubleshooting")
            .setMessage(helpText)
            .setPositiveButton("Check Status") { dialog, _ ->
                dialog.dismiss()
                showServiceStatus()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun checkBatteryOptimization() {
        // Only check on first run or periodically
        val hasChecked = prefs.getBoolean("battery_optimization_checked", false)

        if (!hasChecked && !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            // Delay the dialog slightly so it doesn't appear immediately on app launch
            window.decorView.postDelayed({
                BatteryOptimizationHelper.showBatteryOptimizationDialog(this) {
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
                }
                prefs.edit().putBoolean("battery_optimization_checked", true).apply()
            }, 1000)
        }
    }

    private fun showBatteryOptimizationInstructions() {
        val isOptimized = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)

        val message = if (isOptimized) {
            "✓ Battery optimization is disabled.\n\n" +
                    "The App Blocker service can run without restrictions.\n\n" +
                    "If you're still experiencing issues, check manufacturer-specific settings:\n\n" +
                    BatteryOptimizationHelper.getManufacturerSpecificInstructions()
        } else {
            "⚠ Battery optimization is enabled.\n\n" +
                    "This may cause the App Blocker service to be killed by the system.\n\n" +
                    "For best performance:\n\n" +
                    BatteryOptimizationHelper.getManufacturerSpecificInstructions()
        }

        AlertDialog.Builder(this)
            .setTitle("Battery Optimization")
            .setMessage(message)
            .setPositiveButton("Open Settings") { dialog, _ ->
                if (isOptimized) {
                    BatteryOptimizationHelper.openBatteryOptimizationSettings(this)
                } else {
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showServiceStatus() {
        val statusMessage = ServiceStatusManager.getStatusMessage(this)
        val status = ServiceStatusManager.getServiceStatus(this)

        AlertDialog.Builder(this)
            .setTitle("Service Status")
            .setMessage(statusMessage)
            .setPositiveButton("Restart Service") { dialog, _ ->
                ServiceStatusManager.restartService(this)
                Toast.makeText(this, "Service restarted", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Show updated status after a delay
                window.decorView.postDelayed({
                    showServiceStatus()
                }, 1000)
            }
            .setNeutralButton(if (status.isBatteryOptimized) "Fix Battery" else "Settings") { dialog, _ ->
                if (status.isBatteryOptimized) {
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
                } else {
                    showBatteryOptimizationInstructions()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.blockedAppsRecyclerView)
        addAppButton = findViewById(R.id.addAppButton)
        emptyView = findViewById(R.id.emptyView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BlockedAppsAdapter(
            blockedApps,
            onEdit = { app, position -> editBlockSettings(app, position) },
            onDelete = { position -> deleteBlockedApp(position) },
            onToggle = { app, position, isActive -> toggleBlock(app, position, isActive) }
        )
        recyclerView.adapter = adapter

        addAppButton.setOnClickListener {
            if (checkUsageStatsPermission()) {
                openAppSelectionActivity()
            }
        }
    }

    private fun checkPermissions() {
        if (!checkUsageStatsPermission()) {
            showPermissionDialog()
        }

        if (!checkOverlayPermission()) {
            showOverlayPermissionDialog()
        }
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Usage Access Required")
            .setMessage("App Blocker needs Usage Access permission to monitor apps.\n\nPlease enable it in the next screen.")
            .setPositiveButton("Grant Permission") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "Please enable Usage Access in Settings", Toast.LENGTH_LONG).show()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Display Over Other Apps")
            .setMessage("App Blocker needs permission to display over other apps to block them effectively.")
            .setPositiveButton("Grant Permission") { _, _ ->
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Please enable overlay permission in Settings", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun openAppSelectionActivity() {
        val intent = Intent(this, AppSelectionActivity::class.java)
        startActivityForResult(intent, REQUEST_SELECT_APPS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SELECT_APPS && resultCode == RESULT_OK) {
            val selectedPackages = data?.getStringArrayListExtra("selected_packages") ?: return
            val durationMinutes = data.getIntExtra("duration_minutes", -1)

            addBlockedApps(selectedPackages, durationMinutes)
        }
    }

    private fun addBlockedApps(packageNames: List<String>, durationMinutes: Int) {
        val pm = packageManager
        var addedCount = 0

        for (packageName in packageNames) {
            try {
                // Check if already blocked
                val existingIndex = blockedApps.indexOfFirst { it.packageName == packageName }

                val appInfo = pm.getApplicationInfo(packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()

                val blockedApp = BlockedApp(
                    packageName = packageName,
                    appName = appName,
                    blockedAt = System.currentTimeMillis(),
                    durationMinutes = durationMinutes,
                    isActive = true
                )

                if (existingIndex != -1) {
                    blockedApps[existingIndex] = blockedApp
                } else {
                    blockedApps.add(blockedApp)
                    addedCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding app: $packageName", e)
            }
        }

        saveBlockedApps()
        adapter.notifyDataSetChanged()
        updateUI()

        startBlockerService()

        Toast.makeText(this, "$addedCount app(s) blocked", Toast.LENGTH_SHORT).show()
    }

    private fun editBlockSettings(app: BlockedApp, position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_block_settings, null)
        val timePicker = dialogView.findViewById<android.widget.TimePicker>(R.id.timePicker)
        val enableTimer = dialogView.findViewById<android.widget.CheckBox>(R.id.enableTimerCheckbox)
        val permanentBlock = dialogView.findViewById<android.widget.CheckBox>(R.id.permanentBlockCheckbox)
        val timerLayout = dialogView.findViewById<LinearLayout>(R.id.timerLayout)

        timePicker.setIs24HourView(true)

        if (app.durationMinutes > 0) {
            enableTimer.isChecked = true
            timerLayout.visibility = View.VISIBLE
            timePicker.hour = app.durationMinutes / 60
            timePicker.minute = app.durationMinutes % 60
        } else {
            permanentBlock.isChecked = true
        }

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
            .setTitle("Edit: ${app.appName}")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newDuration = if (enableTimer.isChecked) {
                    timePicker.hour * 60 + timePicker.minute
                } else {
                    -1
                }

                blockedApps[position] = app.copy(
                    durationMinutes = newDuration,
                    blockedAt = System.currentTimeMillis()
                )

                saveBlockedApps()
                adapter.notifyItemChanged(position)
                Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteBlockedApp(position: Int) {
        val app = blockedApps[position]

        AlertDialog.Builder(this)
            .setTitle("Unblock App")
            .setMessage("Remove ${app.appName} from blocked apps?")
            .setPositiveButton("Remove") { _, _ ->
                blockedApps.removeAt(position)
                saveBlockedApps()
                adapter.notifyItemRemoved(position)
                updateUI()
                Toast.makeText(this, "${app.appName} unblocked", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleBlock(app: BlockedApp, position: Int, isActive: Boolean) {
        blockedApps[position] = app.copy(isActive = isActive)
        saveBlockedApps()
        adapter.notifyItemChanged(position)
    }

    private fun saveBlockedApps() {
        try {
            val json = Gson().toJson(blockedApps)
            prefs.edit().putString("blocked_apps_list", json).apply()
            Log.d(TAG, "Saved ${blockedApps.size} apps")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving", e)
        }
    }

    private fun loadBlockedApps() {
        try {
            val json = prefs.getString("blocked_apps_list", null)
            if (json != null) {
                val type = object : TypeToken<MutableList<BlockedApp>>() {}.type
                val loaded = Gson().fromJson<MutableList<BlockedApp>>(json, type)
                blockedApps.clear()
                blockedApps.addAll(loaded)
                Log.d(TAG, "Loaded ${blockedApps.size} apps")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading", e)
        }
    }

    private fun updateUI() {
        if (blockedApps.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun startBlockerService() {
        if (!ServiceStatusManager.startService(this)) {
            Toast.makeText(this, "Failed to start blocker service", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Service started")

        // Show a subtle reminder about battery optimization if not already disabled
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            val lastReminder = prefs.getLong("last_battery_reminder", 0)
            val daysSinceReminder = (System.currentTimeMillis() - lastReminder) / (1000 * 60 * 60 * 24)

            // Remind every 7 days
            if (daysSinceReminder >= 7) {
                Toast.makeText(
                    this,
                    "Tip: Disable battery optimization for better reliability",
                    Toast.LENGTH_LONG
                ).show()
                prefs.edit().putLong("last_battery_reminder", System.currentTimeMillis()).apply()
            }
        }
    }
}

data class BlockedApp(
    val packageName: String,
    val appName: String,
    val blockedAt: Long,
    val durationMinutes: Int, // -1 for permanent
    val isActive: Boolean = true
) {
    fun isExpired(): Boolean {
        if (durationMinutes < 0) return false
        val expiryTime = blockedAt + (durationMinutes * 60 * 1000L)
        return System.currentTimeMillis() > expiryTime
    }

    fun getRemainingTime(): String {
        if (durationMinutes < 0) return "Permanent"
        if (isExpired()) return "Expired"

        val expiryTime = blockedAt + (durationMinutes * 60 * 1000L)
        val remaining = expiryTime - System.currentTimeMillis()
        val hours = remaining / (1000 * 60 * 60)
        val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}