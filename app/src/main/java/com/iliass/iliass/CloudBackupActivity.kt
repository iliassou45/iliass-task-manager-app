package com.iliass.iliass

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.util.SupabaseBackupManager
import com.iliass.iliass.util.SupabaseManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CloudBackupActivity : AppCompatActivity() {

    // Auth views
    private lateinit var authCard: CardView
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var btnSignIn: MaterialButton
    private lateinit var btnSignUp: MaterialButton

    // User info views
    private lateinit var userInfoCard: CardView
    private lateinit var userEmailText: TextView
    private lateinit var btnSignOut: MaterialButton

    // Data summary views
    private lateinit var dataSummaryCard: CardView
    private lateinit var dataSummaryText: TextView
    private lateinit var lastSyncText: TextView

    // Cloud backup info views
    private lateinit var cloudBackupInfoCard: CardView
    private lateinit var cloudBackupStatusText: TextView

    // Action views
    private lateinit var backupActionsCard: CardView
    private lateinit var btnCompareWithCloud: MaterialButton
    private lateinit var btnBackupToCloud: MaterialButton
    private lateinit var btnRestoreFromCloud: MaterialButton
    private lateinit var btnMergeFromCloud: MaterialButton

    // Status views
    private lateinit var progressBar: ProgressBar
    private lateinit var statusMessage: TextView

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_backup)

        supportActionBar?.title = "Cloud Backup"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupListeners()
        updateUI()
    }

    private fun initViews() {
        // Auth views
        authCard = findViewById(R.id.authCard)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignUp = findViewById(R.id.btnSignUp)

        // User info views
        userInfoCard = findViewById(R.id.userInfoCard)
        userEmailText = findViewById(R.id.userEmailText)
        btnSignOut = findViewById(R.id.btnSignOut)

        // Data summary views
        dataSummaryCard = findViewById(R.id.dataSummaryCard)
        dataSummaryText = findViewById(R.id.dataSummaryText)
        lastSyncText = findViewById(R.id.lastSyncText)

        // Cloud backup info views
        cloudBackupInfoCard = findViewById(R.id.cloudBackupInfoCard)
        cloudBackupStatusText = findViewById(R.id.cloudBackupStatusText)

        // Action views
        backupActionsCard = findViewById(R.id.backupActionsCard)
        btnCompareWithCloud = findViewById(R.id.btnCompareWithCloud)
        btnBackupToCloud = findViewById(R.id.btnBackupToCloud)
        btnRestoreFromCloud = findViewById(R.id.btnRestoreFromCloud)
        btnMergeFromCloud = findViewById(R.id.btnMergeFromCloud)

        // Status views
        progressBar = findViewById(R.id.progressBar)
        statusMessage = findViewById(R.id.statusMessage)
    }

    private fun setupListeners() {
        btnSignIn.setOnClickListener { performSignIn() }
        btnSignUp.setOnClickListener { performSignUp() }
        btnSignOut.setOnClickListener { performSignOut() }
        btnCompareWithCloud.setOnClickListener { performComparison() }
        btnBackupToCloud.setOnClickListener { showBackupConfirmation() }
        btnRestoreFromCloud.setOnClickListener { showRestoreConfirmation(mergeMode = false) }
        btnMergeFromCloud.setOnClickListener { showRestoreConfirmation(mergeMode = true) }
    }

    private fun updateUI() {
        val isAuthenticated = SupabaseManager.isAuthenticated()

        if (isAuthenticated) {
            authCard.visibility = View.GONE
            userInfoCard.visibility = View.VISIBLE
            dataSummaryCard.visibility = View.VISIBLE
            cloudBackupInfoCard.visibility = View.VISIBLE
            backupActionsCard.visibility = View.VISIBLE

            userEmailText.text = SupabaseManager.getCurrentUserEmail() ?: "Unknown"
            dataSummaryText.text = SupabaseBackupManager.getDataSummary(this)
            lastSyncText.text = SupabaseBackupManager.getFormattedLastSyncTime(this)

            // Check cloud backup status
            checkCloudBackupStatus()
        } else {
            authCard.visibility = View.VISIBLE
            userInfoCard.visibility = View.GONE
            dataSummaryCard.visibility = View.GONE
            cloudBackupInfoCard.visibility = View.GONE
            backupActionsCard.visibility = View.GONE

            // Restore saved email if available
            val savedEmail = SupabaseManager.getSavedUserEmail(this)
            if (savedEmail != null) {
                emailInput.setText(savedEmail)
            }
        }
    }

    private fun checkCloudBackupStatus() {
        cloudBackupStatusText.text = "Checking cloud backup..."

        lifecycleScope.launch {
            try {
                val backupInfo = SupabaseBackupManager.checkBackupExists(this@CloudBackupActivity)

                if (backupInfo != null && backupInfo.exists) {
                    cloudBackupStatusText.text = buildString {
                        appendLine("Backup found in cloud!")
                        appendLine("Backup date: ${dateFormat.format(Date(backupInfo.backupDate))}")
                        appendLine("Items: ${backupInfo.itemCounts.toDisplayString()}")
                    }
                } else {
                    cloudBackupStatusText.text = "No backup found in cloud.\nCreate your first backup now!"
                }
            } catch (e: Exception) {
                cloudBackupStatusText.text = "Could not check cloud backup status.\nError: ${e.message}"
            }
        }
    }

    private fun performSignIn() {
        val email = emailInput.text?.toString()?.trim() ?: ""
        val password = passwordInput.text?.toString() ?: ""

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true, "Signing in...")

        lifecycleScope.launch {
            when (val result = SupabaseManager.signIn(email, password)) {
                is SupabaseManager.AuthResult.Success -> {
                    SupabaseManager.saveUserEmail(this@CloudBackupActivity, email)
                    setLoading(false)
                    showStatus("Signed in successfully!", isSuccess = true)
                    updateUI()
                }
                is SupabaseManager.AuthResult.Error -> {
                    setLoading(false)
                    showStatus("Sign in failed: ${result.message}", isSuccess = false)
                }
            }
        }
    }

    private fun performSignUp() {
        val email = emailInput.text?.toString()?.trim() ?: ""
        val password = passwordInput.text?.toString() ?: ""

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true, "Creating account...")

        lifecycleScope.launch {
            when (val result = SupabaseManager.signUp(email, password)) {
                is SupabaseManager.AuthResult.Success -> {
                    setLoading(false)
                    showSignUpSuccessDialog()
                }
                is SupabaseManager.AuthResult.Error -> {
                    setLoading(false)
                    showStatus("Sign up failed: ${result.message}", isSuccess = false)
                }
            }
        }
    }

    private fun showSignUpSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Account Created")
            .setMessage("Your account has been created successfully!\n\nPlease check your email for a verification link, then sign in.")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun performSignOut() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                lifecycleScope.launch {
                    SupabaseManager.signOut()
                    SupabaseManager.clearSavedData(this@CloudBackupActivity)
                    updateUI()
                    Toast.makeText(this@CloudBackupActivity, "Signed out", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performComparison() {
        setLoading(true, "Comparing with cloud data...")

        lifecycleScope.launch {
            when (val result = SupabaseBackupManager.compareWithCloud(this@CloudBackupActivity)) {
                is SupabaseBackupManager.ComparisonResult.Success -> {
                    setLoading(false)
                    showComparisonDialog(result)
                }
                is SupabaseBackupManager.ComparisonResult.Error -> {
                    setLoading(false)
                    showStatus("Comparison failed: ${result.message}", isSuccess = false)
                }
            }
        }
    }

    private fun showComparisonDialog(result: SupabaseBackupManager.ComparisonResult.Success) {
        val message = buildString {
            appendLine("Cloud Backup Date: ${dateFormat.format(Date(result.cloudBackupDate))}")
            appendLine()
            appendLine("LOCAL DATA:")
            appendLine("  Students: ${result.localCounts.students}")
            appendLine("  Payments: ${result.localCounts.payments}")
            appendLine("  Classes: ${result.localCounts.classes}")
            appendLine("  Lessons: ${result.localCounts.lessons}")
            appendLine("  Notes: ${result.localCounts.notes}")
            appendLine("  Debts: ${result.localCounts.debts}")
            appendLine()
            appendLine("CLOUD DATA:")
            appendLine("  Students: ${result.cloudCounts.students}")
            appendLine("  Payments: ${result.cloudCounts.payments}")
            appendLine("  Classes: ${result.cloudCounts.classes}")
            appendLine("  Lessons: ${result.cloudCounts.lessons}")
            appendLine("  Notes: ${result.cloudCounts.notes}")
            appendLine("  Debts: ${result.cloudCounts.debts}")
            appendLine()
            appendLine("DIFFERENCES:")
            append(result.differences.toSummaryString())
        }

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Cloud vs Local Comparison")
            .setMessage(message)
            .setNegativeButton("Close", null)

        if (result.differences.hasDifferences()) {
            // Show options if there are differences
            if (result.differences.newStudentsInCloud.isNotEmpty() ||
                result.differences.newClassesInCloud.isNotEmpty() ||
                result.differences.newNotesInCloud.isNotEmpty() ||
                result.differences.newDebtsInCloud.isNotEmpty() ||
                result.differences.newPaymentsCount > 0 ||
                result.differences.newLessonsCount > 0) {
                dialogBuilder.setPositiveButton("Merge Cloud Data") { _, _ ->
                    performRestore(mergeMode = true)
                }
            }
            if (result.differences.missingStudentsInCloud.isNotEmpty() ||
                result.differences.missingClassesInCloud.isNotEmpty() ||
                result.differences.missingNotesInCloud.isNotEmpty() ||
                result.differences.missingDebtsInCloud.isNotEmpty() ||
                result.differences.missingPaymentsCount > 0 ||
                result.differences.missingLessonsCount > 0) {
                dialogBuilder.setNeutralButton("Backup to Cloud") { _, _ ->
                    performBackup()
                }
            }
        }

        dialogBuilder.show()
    }

    private fun showBackupConfirmation() {
        setLoading(true, "Comparing with cloud data...")

        lifecycleScope.launch {
            when (val result = SupabaseBackupManager.compareWithCloud(this@CloudBackupActivity)) {
                is SupabaseBackupManager.ComparisonResult.Success -> {
                    setLoading(false)
                    showBackupConfirmationDialog(result)
                }
                is SupabaseBackupManager.ComparisonResult.Error -> {
                    setLoading(false)
                    // No backup exists yet, show simple confirmation
                    if (result.message.contains("No backup found")) {
                        showFirstBackupConfirmation()
                    } else {
                        showStatus("Could not compare: ${result.message}", isSuccess = false)
                    }
                }
            }
        }
    }

    private fun showFirstBackupConfirmation() {
        val localSummary = SupabaseBackupManager.getDataSummary(this)

        AlertDialog.Builder(this)
            .setTitle("Create First Backup")
            .setMessage("No backup exists in cloud yet.\n\nThis will upload your local data:\n\n$localSummary\nDo you want to create your first backup?")
            .setPositiveButton("Backup Now") { _, _ ->
                performBackup()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBackupConfirmationDialog(comparison: SupabaseBackupManager.ComparisonResult.Success) {
        val message = buildString {
            appendLine("WARNING: This will REPLACE the cloud backup with your local data.")
            appendLine()
            appendLine("CURRENT CLOUD BACKUP (will be replaced):")
            appendLine("  Backup date: ${dateFormat.format(Date(comparison.cloudBackupDate))}")
            appendLine("  Students: ${comparison.cloudCounts.students}")
            appendLine("  Payments: ${comparison.cloudCounts.payments}")
            appendLine("  Classes: ${comparison.cloudCounts.classes}")
            appendLine("  Lessons: ${comparison.cloudCounts.lessons}")
            appendLine("  Notes: ${comparison.cloudCounts.notes}")
            appendLine("  Debts: ${comparison.cloudCounts.debts}")
            appendLine()
            appendLine("YOUR LOCAL DATA (will be uploaded):")
            appendLine("  Students: ${comparison.localCounts.students}")
            appendLine("  Payments: ${comparison.localCounts.payments}")
            appendLine("  Classes: ${comparison.localCounts.classes}")
            appendLine("  Lessons: ${comparison.localCounts.lessons}")
            appendLine("  Notes: ${comparison.localCounts.notes}")
            appendLine("  Debts: ${comparison.localCounts.debts}")

            // Show warnings if cloud has more data
            val warnings = mutableListOf<String>()
            if (comparison.cloudCounts.students > comparison.localCounts.students) {
                warnings.add("Cloud has ${comparison.cloudCounts.students - comparison.localCounts.students} more student(s)")
            }
            if (comparison.cloudCounts.payments > comparison.localCounts.payments) {
                warnings.add("Cloud has ${comparison.cloudCounts.payments - comparison.localCounts.payments} more payment(s)")
            }
            if (comparison.cloudCounts.classes > comparison.localCounts.classes) {
                warnings.add("Cloud has ${comparison.cloudCounts.classes - comparison.localCounts.classes} more class(es)")
            }
            if (comparison.cloudCounts.lessons > comparison.localCounts.lessons) {
                warnings.add("Cloud has ${comparison.cloudCounts.lessons - comparison.localCounts.lessons} more lesson(s)")
            }
            if (comparison.cloudCounts.notes > comparison.localCounts.notes) {
                warnings.add("Cloud has ${comparison.cloudCounts.notes - comparison.localCounts.notes} more note(s)")
            }
            if (comparison.cloudCounts.debts > comparison.localCounts.debts) {
                warnings.add("Cloud has ${comparison.cloudCounts.debts - comparison.localCounts.debts} more debt(s)")
            }

            if (warnings.isNotEmpty()) {
                appendLine()
                appendLine("ATTENTION - You may lose data:")
                warnings.forEach { appendLine("  - $it") }
            }

            if (comparison.differences.newStudentsInCloud.isNotEmpty() ||
                comparison.differences.newClassesInCloud.isNotEmpty() ||
                comparison.differences.newNotesInCloud.isNotEmpty() ||
                comparison.differences.newDebtsInCloud.isNotEmpty()) {
                appendLine()
                appendLine("Items in cloud that will be LOST:")
                comparison.differences.newStudentsInCloud.forEach { appendLine("  - Student: ${it.name}") }
                comparison.differences.newClassesInCloud.forEach { appendLine("  - Class: ${it.name}") }
                comparison.differences.newNotesInCloud.forEach { appendLine("  - Note: ${it.title}") }
                comparison.differences.newDebtsInCloud.forEach { appendLine("  - Debt: ${it.personName}") }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Backup to Cloud")
            .setMessage(message)
            .setPositiveButton("Backup Now") { _, _ ->
                performBackup()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Merge First") { _, _ ->
                // First merge cloud data, then backup
                performRestore(mergeMode = true)
            }
            .show()
    }

    private fun performBackup() {
        setLoading(true, "Uploading backup to cloud...")

        lifecycleScope.launch {
            when (val result = SupabaseBackupManager.uploadBackup(this@CloudBackupActivity)) {
                is SupabaseBackupManager.BackupResult.Success -> {
                    setLoading(false)
                    showStatus("Backup successful!\n${result.itemCounts.toDisplayString()}", isSuccess = true)
                    lastSyncText.text = dateFormat.format(Date(result.timestamp))
                    checkCloudBackupStatus()
                }
                is SupabaseBackupManager.BackupResult.Error -> {
                    setLoading(false)
                    showStatus("Backup failed: ${result.message}", isSuccess = false)
                }
            }
        }
    }

    private fun showRestoreConfirmation(mergeMode: Boolean) {
        val title = if (mergeMode) "Merge from Cloud" else "Restore from Cloud"
        val message = if (mergeMode) {
            "This will add cloud data to your local data.\n\nExisting items will NOT be replaced.\n\nContinue?"
        } else {
            "WARNING: This will REPLACE all your local data with the cloud backup.\n\nAny local data not in the cloud will be lost.\n\nContinue?"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(if (mergeMode) "Merge" else "Restore") { _, _ ->
                performRestore(mergeMode)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRestore(mergeMode: Boolean) {
        val actionText = if (mergeMode) "Merging" else "Restoring"
        setLoading(true, "$actionText from cloud...")

        lifecycleScope.launch {
            when (val result = SupabaseBackupManager.downloadBackup(this@CloudBackupActivity, mergeMode)) {
                is SupabaseBackupManager.RestoreResult.Success -> {
                    setLoading(false)
                    showStatus("${result.message}\n${result.itemCounts.toDisplayString()}", isSuccess = true)
                    lastSyncText.text = dateFormat.format(Date(result.timestamp))
                    dataSummaryText.text = SupabaseBackupManager.getDataSummary(this@CloudBackupActivity)
                }
                is SupabaseBackupManager.RestoreResult.Error -> {
                    setLoading(false)
                    showStatus("Restore failed: ${result.message}", isSuccess = false)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean, message: String = "") {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        statusMessage.visibility = if (loading && message.isNotEmpty()) View.VISIBLE else View.GONE
        statusMessage.text = message
        statusMessage.setTextColor(resources.getColor(android.R.color.darker_gray, null))

        // Disable buttons while loading
        btnSignIn.isEnabled = !loading
        btnSignUp.isEnabled = !loading
        btnSignOut.isEnabled = !loading
        btnCompareWithCloud.isEnabled = !loading
        btnBackupToCloud.isEnabled = !loading
        btnRestoreFromCloud.isEnabled = !loading
        btnMergeFromCloud.isEnabled = !loading
    }

    private fun showStatus(message: String, isSuccess: Boolean) {
        statusMessage.visibility = View.VISIBLE
        statusMessage.text = message
        statusMessage.setTextColor(
            if (isSuccess) resources.getColor(android.R.color.holo_green_dark, null)
            else resources.getColor(android.R.color.holo_red_dark, null)
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
