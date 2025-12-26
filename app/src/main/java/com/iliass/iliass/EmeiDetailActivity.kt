package com.iliass.iliass

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.Phone
import com.iliass.iliass.util.PhoneStorageManager
import com.iliass.iliass.util.TimeUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EmeiDetailActivity : AppCompatActivity() {

    private lateinit var phoneImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var phoneNameInput: TextInputEditText
    private lateinit var imeiInput: TextInputEditText
    private lateinit var shopNameInput: AutoCompleteTextView
    private lateinit var shopAddressInput: AutoCompleteTextView
    private lateinit var notesInput: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button
    private lateinit var headerTitle: TextView
    private lateinit var registrationInfoCard: View
    private lateinit var registrationTimeText: TextView
    private lateinit var registrationDateText: TextView
    private lateinit var warrantyStatusText: TextView
    private lateinit var changeDateButton: Button

    private lateinit var storageManager: PhoneStorageManager
    private var selectedImageBitmap: Bitmap? = null
    private var currentPhone: Phone? = null
    private var isEditMode = false
    private var customRegistrationDate: Long? = null

    // Store shop data for autocomplete
    private val shopDataMap = mutableMapOf<String, MutableSet<String>>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                loadImageFromUri(uri)
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                selectedImageBitmap = it
                phoneImageView.setImageBitmap(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emei_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        storageManager = PhoneStorageManager(this)

        setupViews()
        loadShopData()
        setupAutoComplete()
        checkEditMode()
        setupListeners()
    }

    private fun setupViews() {
        phoneImageView = findViewById(R.id.phoneImageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        phoneNameInput = findViewById(R.id.phoneNameInput)
        imeiInput = findViewById(R.id.imeiInput)
        shopNameInput = findViewById(R.id.shopNameInput)
        shopAddressInput = findViewById(R.id.shopAddressInput)
        notesInput = findViewById(R.id.notesInput)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)
        headerTitle = findViewById(R.id.headerTitle)
        registrationInfoCard = findViewById(R.id.registrationInfoCard)
        registrationTimeText = findViewById(R.id.registrationTimeText)
        registrationDateText = findViewById(R.id.registrationDateText)
        warrantyStatusText = findViewById(R.id.warrantyStatusText)
        changeDateButton = findViewById(R.id.changeDateButton)
    }

    private fun loadShopData() {
        val phones = storageManager.getAllPhones()

        phones.forEach { phone ->
            if (shopDataMap.containsKey(phone.shopName)) {
                shopDataMap[phone.shopName]?.add(phone.shopAddress)
            } else {
                shopDataMap[phone.shopName] = mutableSetOf(phone.shopAddress)
            }
        }
    }

    private fun setupAutoComplete() {
        val shopNames = shopDataMap.keys.toList().sorted()
        val shopNameAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            shopNames
        )
        shopNameInput.setAdapter(shopNameAdapter)
        shopNameInput.threshold = 1

        shopNameInput.setOnItemClickListener { _, _, position, _ ->
            val selectedShop = shopNameAdapter.getItem(position)
            selectedShop?.let { shopName ->
                updateAddressSuggestions(shopName)
                val addresses = shopDataMap[shopName]?.toList() ?: emptyList()
                if (addresses.size == 1) {
                    shopAddressInput.setText(addresses[0])
                }
            }
        }

        updateAddressSuggestions(null)
    }

    private fun updateAddressSuggestions(shopName: String?) {
        val addresses = if (shopName != null) {
            shopDataMap[shopName]?.toList()?.sorted() ?: emptyList()
        } else {
            shopDataMap.values.flatten().distinct().sorted()
        }

        val addressAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            addresses
        )
        shopAddressInput.setAdapter(addressAdapter)
        shopAddressInput.threshold = 1
    }

    private fun checkEditMode() {
        currentPhone = intent.getSerializableExtra("phone") as? Phone

        if (currentPhone != null) {
            isEditMode = true
            headerTitle.text = "✏️ Edit Phone"
            saveButton.text = "💾 Update Phone"
            deleteButton.visibility = Button.VISIBLE
            registrationInfoCard.visibility = View.VISIBLE

            currentPhone?.let { phone ->
                phoneNameInput.setText(phone.name)
                imeiInput.setText(phone.imei)
                shopNameInput.setText(phone.shopName)
                shopAddressInput.setText(phone.shopAddress)
                notesInput.setText(phone.notes ?: "")

                // Store the current registration date
                customRegistrationDate = phone.dateRegistered

                // Display registration time info
                updateRegistrationInfo(phone.dateRegistered)

                phone.imagePath?.let { path ->
                    val imageFile = File(path)
                    if (imageFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(path)
                        phoneImageView.setImageBitmap(bitmap)
                        selectedImageBitmap = bitmap
                    }
                }
            }
        } else {
            // New phone mode - show card but with "Set Custom Date" option
            registrationInfoCard.visibility = View.VISIBLE
            headerTitle.text = "📱 Add New Phone"
            saveButton.text = "💾 Save Phone"

            // Show default "will be added now" message
            registrationTimeText.text = "📅 Will be added now"
            registrationDateText.text = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                .format(Date(System.currentTimeMillis()))
            warrantyStatusText.text = "✅ 12 months warranty will start from registration"
            warrantyStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
            changeDateButton.text = "📆 Set Custom Registration Date"
        }
    }

    private fun updateRegistrationInfo(registrationDate: Long) {
        // Relative time
        val relativeTime = TimeUtils.getRelativeTimeString(registrationDate)
        registrationTimeText.text = "📅 Added $relativeTime"

        // Exact date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        registrationDateText.text = dateFormat.format(Date(registrationDate))

        // Warranty status (assuming 12 months warranty)
        val warrantyStatus = TimeUtils.getWarrantyStatus(registrationDate, 12)

        if (warrantyStatus.isActive) {
            warrantyStatusText.text = "✅ Warranty: ${warrantyStatus.message}"
            warrantyStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            warrantyStatusText.text = "⚠️ Warranty: ${warrantyStatus.message}"
            warrantyStatusText.setTextColor(getColor(android.R.color.holo_orange_dark))
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        // If there's a custom date set, use it; otherwise use current phone's date
        val initialDate = customRegistrationDate ?: currentPhone?.dateRegistered ?: System.currentTimeMillis()
        calendar.timeInMillis = initialDate

        // Show Date Picker
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // After date is selected, show Time Picker
                android.app.TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        // Save the selected date/time
                        customRegistrationDate = calendar.timeInMillis

                        // Update the display
                        updateRegistrationInfo(customRegistrationDate!!)

                        Toast.makeText(
                            this,
                            "Registration date updated. Click Save to apply changes.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false // 12-hour format
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set max date to today (can't register a phone in the future)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun setupListeners() {
        selectImageButton.setOnClickListener {
            showImagePickerDialog()
        }

        phoneImageView.setOnClickListener {
            showFullSizeImage()
        }

        saveButton.setOnClickListener {
            savePhone()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        changeDateButton.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun showFullSizeImage() {
        selectedImageBitmap?.let { bitmap ->
            val imageView = ImageView(this).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(16, 16, 16, 16)
            }

            AlertDialog.Builder(this)
                .setView(imageView)
                .setPositiveButton("Close", null)
                .create()
                .show()
        } ?: run {
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        AlertDialog.Builder(this)
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (checkCameraPermission()) {
                            takePicture()
                        } else {
                            requestCameraPermission()
                        }
                    }
                    1 -> pickImageFromGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            selectedImageBitmap = bitmap
            phoneImageView.setImageBitmap(bitmap)
            inputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePhone() {
        val name = phoneNameInput.text.toString().trim()
        val imei = imeiInput.text.toString().trim()
        val shopName = shopNameInput.text.toString().trim()
        val shopAddress = shopAddressInput.text.toString().trim()
        val notes = notesInput.text.toString().trim()

        if (name.isEmpty()) {
            phoneNameInput.error = "Phone name is required"
            return
        }

        if (imei.isEmpty()) {
            imeiInput.error = "IMEI is required"
            return
        }

        if (shopName.isEmpty()) {
            shopNameInput.error = "Shop name is required"
            return
        }

        if (shopAddress.isEmpty()) {
            shopAddressInput.error = "Shop address is required"
            return
        }

        val phoneIdToExclude = if (isEditMode) currentPhone?.id else null
        if (storageManager.isImeiExists(imei, phoneIdToExclude)) {
            imeiInput.error = "This IMEI already exists in inventory"
            AlertDialog.Builder(this)
                .setTitle("⚠️ Duplicate IMEI")
                .setMessage("A phone with IMEI \"$imei\" already exists in your inventory. Each phone must have a unique IMEI number.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        var imagePath: String? = currentPhone?.imagePath
        selectedImageBitmap?.let { bitmap ->
            val phoneId = currentPhone?.id ?: System.currentTimeMillis().toString()
            imagePath = storageManager.saveImage(bitmap, phoneId)
        }

        val phone = if (isEditMode && currentPhone != null) {
            currentPhone!!.copy(
                name = name,
                imei = imei,
                shopName = shopName,
                shopAddress = shopAddress,
                notes = notes.ifEmpty { null },
                imagePath = imagePath,
                dateRegistered = customRegistrationDate ?: currentPhone!!.dateRegistered
            )
        } else {
            Phone(
                name = name,
                imei = imei,
                shopName = shopName,
                shopAddress = shopAddress,
                notes = notes.ifEmpty { null },
                imagePath = imagePath
            )
        }

        val success = if (isEditMode) {
            storageManager.updatePhone(phone)
        } else {
            storageManager.savePhone(phone)
        }

        if (success) {
            Toast.makeText(
                this,
                if (isEditMode) "Phone updated successfully" else "Phone added successfully",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } else {
            Toast.makeText(
                this,
                "Failed to save phone. IMEI might already exist.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showDeleteConfirmation() {
        val input = TextInputEditText(this).apply {
            hint = "Enter password (1-5)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val container = android.widget.FrameLayout(this).apply {
            setPadding(50, 20, 50, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Delete Phone")
            .setMessage("To delete this phone entry, please enter the password (a number from 1 to 5):")
            .setView(container)
            .setPositiveButton("Delete") { _, _ ->
                val password = input.text.toString().trim()

                if (password.isEmpty()) {
                    Toast.makeText(this, "❌ Password cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val passwordNumber = password.toIntOrNull()

                if (passwordNumber == null || passwordNumber !in 1..5) {
                    Toast.makeText(this, "❌ Invalid password! Must be a number between 1 and 5", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                // Password is valid, proceed with deletion
                deletePhone()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePhone() {
        currentPhone?.let { phone ->
            val success = storageManager.deletePhone(phone.id)
            if (success) {
                Toast.makeText(this, "Phone deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to delete phone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }
}