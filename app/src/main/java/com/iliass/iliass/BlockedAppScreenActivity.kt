package com.iliass.iliass

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockedAppScreenActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var packageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_app_screen)

        val appName = intent.getStringExtra("app_name") ?: "This app"
        val remainingTime = intent.getStringExtra("remaining_time") ?: "Unknown"
        packageName = intent.getStringExtra("package_name")

        findViewById<TextView>(R.id.blockedAppName).text = appName
        findViewById<TextView>(R.id.remainingTimeText).text = "Time remaining: $remainingTime"

        findViewById<Button>(R.id.closeButton).setOnClickListener {
            goToHome()
        }

        // Auto-close after 1.5 seconds
        handler.postDelayed({
            if (!isFinishing) {
                goToHome()
            }
        }, 1500)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Update UI if called again
        val appName = intent.getStringExtra("app_name") ?: "This app"
        val remainingTime = intent.getStringExtra("remaining_time") ?: "Unknown"

        findViewById<TextView>(R.id.blockedAppName)?.text = appName
        findViewById<TextView>(R.id.remainingTimeText)?.text = "Time remaining: $remainingTime"
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent going back to blocked app
        goToHome()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                goToHome()
                true
            }
            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH -> {
                // Let system handle home/recent apps
                super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun goToHome() {
        try {
            // Close any blocked app processes
            packageName?.let {
                val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
                am.killBackgroundProcesses(it)
            }

            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        } catch (e: Exception) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPause() {
        super.onPause()
        // Finish immediately when paused to prevent returning to this screen
        finish()
    }
}