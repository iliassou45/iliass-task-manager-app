package com.iliass.iliass

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.Phone
import com.iliass.iliass.util.PhoneStorageManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PhoneImageViewActivity : AppCompatActivity() {

    private lateinit var fullImageView: ImageView
    private lateinit var downloadButton: MaterialButton
    private lateinit var deleteButton: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var storageManager: PhoneStorageManager

    private var phone: Phone? = null
    private var imagePath: String? = null
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_image_view)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        storageManager = PhoneStorageManager(this)

        setupViews()
        loadImageFromIntent()
        setupListeners()
    }

    private fun setupViews() {
        fullImageView = findViewById(R.id.fullImageView)
        downloadButton = findViewById(R.id.downloadButton)
        deleteButton = findViewById(R.id.deleteButton)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadImageFromIntent() {
        phone = intent.getSerializableExtra("phone") as? Phone
        imagePath = intent.getStringExtra("imagePath") ?: phone?.imagePath

        imagePath?.let { path ->
            val imageFile = File(path)
            if (imageFile.exists()) {
                currentBitmap = BitmapFactory.decodeFile(path)
                currentBitmap?.let {
                    fullImageView.setImageBitmap(it)
                }

                // Update toolbar title with phone name if available
                phone?.let {
                    toolbar.title = it.name
                }
            } else {
                Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Toast.makeText(this, "No image path provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupListeners() {
        downloadButton.setOnClickListener {
            if (checkStoragePermission()) {
                downloadImage()
            } else {
                requestStoragePermission()
            }
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ doesn't need WRITE_EXTERNAL_STORAGE for MediaStore
            true
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    private fun downloadImage() {
        currentBitmap?.let { bitmap ->
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "phone_${phone?.name?.replace(" ", "_") ?: "image"}_$timestamp.jpg"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - Use MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhoneInventory")
                    }

                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        }
                        Toast.makeText(
                            this,
                            "✅ Image downloaded to Pictures/PhoneInventory",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    // Android 9 and below - Use traditional file storage
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val phoneInventoryDir = File(picturesDir, "PhoneInventory")
                    if (!phoneInventoryDir.exists()) {
                        phoneInventoryDir.mkdirs()
                    }

                    val imageFile = File(phoneInventoryDir, fileName)
                    FileOutputStream(imageFile).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    }

                    // Notify gallery about the new image
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    }
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    Toast.makeText(
                        this,
                        "✅ Image downloaded to Pictures/PhoneInventory",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this,
                    "❌ Failed to download image: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } ?: run {
            Toast.makeText(this, "No image to download", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation() {
        val input = TextInputEditText(this).apply {
            hint = "Enter password"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val container = android.widget.FrameLayout(this).apply {
            setPadding(50, 20, 50, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Delete Phone")
            .setMessage("To delete this phone entry, please enter the password (12345):")
            .setView(container)
            .setPositiveButton("Delete") { _, _ ->
                val password = input.text.toString().trim()

                if (password.isEmpty()) {
                    Toast.makeText(this, "❌ Password cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (password != "12345") {
                    Toast.makeText(this, "❌ Invalid password!", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                // Password is correct, proceed with deletion
                deletePhone()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePhone() {
        phone?.let { phoneToDelete ->
            val success = storageManager.deletePhone(phoneToDelete.id)
            if (success) {
                Toast.makeText(this, "✅ Phone deleted successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "❌ Failed to delete phone", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Cannot delete: phone data not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadImage()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 101
    }
}
