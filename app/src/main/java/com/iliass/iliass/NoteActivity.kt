package com.iliass.iliass

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile
import com.iliass.iliass.model.Note
import com.iliass.iliass.util.NoteManager
import com.iliass.iliass.util.CategoryManager

private const val TAG = "NoteActivity"

class NoteActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var titleInput: EditText
    private lateinit var contentInput: EditText
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button
    private lateinit var exportButton: Button
    private lateinit var viewNotesButton: Button
    private lateinit var statusText: TextView
    private lateinit var colorTextButton: Button
    private lateinit var boldButton: Button
    private lateinit var italicButton: Button
    private lateinit var underlineButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var addCategoryButton: ImageButton

    private var currentNote: Note? = null
    private lateinit var noteManager: NoteManager
    private lateinit var categoryManager: CategoryManager
    private var categories: MutableList<String> = mutableListOf()

    companion object {
        private const val EXPORT_FOLDER_REQUEST_CODE = 42
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        noteManager = NoteManager(this)
        categoryManager = CategoryManager(this)

        initViews()
        setupListeners()
        loadCategories()

        // Check if we're editing an existing note
        currentNote = intent.getSerializableExtra("note") as? Note
        if (currentNote != null) {
            loadNoteData()
        }
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        titleInput = findViewById(R.id.titleInput)
        contentInput = findViewById(R.id.contentInput)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)
        exportButton = findViewById(R.id.exportButton)
        viewNotesButton = findViewById(R.id.viewNotesButton)
        statusText = findViewById(R.id.statusText)
        colorTextButton = findViewById(R.id.colorTextButton)
        boldButton = findViewById(R.id.boldButton)
        italicButton = findViewById(R.id.italicButton)
        underlineButton = findViewById(R.id.underlineButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        addCategoryButton = findViewById(R.id.addCategoryButton)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            saveNote()
        }

        deleteButton.setOnClickListener {
            if (currentNote != null) {
                showDeleteConfirmation()
            } else {
                Toast.makeText(this, "No note to delete", Toast.LENGTH_SHORT).show()
            }
        }

        exportButton.setOnClickListener {
            if (currentNote != null) {
                showExportFolderSelector()
            } else {
                Toast.makeText(this, "Save the note first before exporting", Toast.LENGTH_SHORT).show()
            }
        }

        viewNotesButton.setOnClickListener {
            startActivity(Intent(this, NotesListActivity::class.java))
        }

        colorTextButton.setOnClickListener {
            showColorPicker()
        }

        boldButton.setOnClickListener {
            toggleStyle(Typeface.BOLD)
        }

        italicButton.setOnClickListener {
            toggleStyle(Typeface.ITALIC)
        }

        underlineButton.setOnClickListener {
            toggleUnderline()
        }

        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun loadCategories() {
        categories.clear()
        categories.addAll(categoryManager.getAllCategories())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Set the current category if editing a note
        currentNote?.let { note ->
            val categoryIndex = categories.indexOf(note.category)
            if (categoryIndex >= 0) {
                categorySpinner.setSelection(categoryIndex)
            }
        }
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this)
        input.hint = "Category name"

        AlertDialog.Builder(this)
            .setTitle("Add New Category")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val categoryName = input.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    if (categoryManager.addCategory(categoryName)) {
                        loadCategories()
                        // Select the newly added category
                        val newIndex = categories.indexOf(categoryName)
                        if (newIndex >= 0) {
                            categorySpinner.setSelection(newIndex)
                        }
                        Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportFolderSelector() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, EXPORT_FOLDER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EXPORT_FOLDER_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                exportNoteToUri(uri)
            }
        }
    }

    private fun exportNoteToUri(uri: Uri) {
        try {
            currentNote?.let { note ->
                // Create export data in a structured format
                val exportData = StringBuilder()
                exportData.append("ILIASS_NOTE_FORMAT_v1\n")
                exportData.append("---\n")
                exportData.append("ID:${note.id}\n")
                exportData.append("TITLE:${note.title}\n")
                exportData.append("CREATED_AT:${note.createdAt}\n")
                exportData.append("UPDATED_AT:${note.updatedAt}\n")
                exportData.append("COLOR:${note.color}\n")
                exportData.append("CATEGORY:${note.category}\n")
                exportData.append("---\n")
                exportData.append("CONTENT:\n")
                exportData.append(note.content)
                exportData.append("\n---\n")
                exportData.append("FORMATTED_CONTENT:\n")
                exportData.append(note.contentWithFormatting)
                exportData.append("\n---END---\n")

                // Create file in selected folder
                val fileName = "${note.title.replace(" ", "_")}_${System.currentTimeMillis()}.iliass"
                val documentFile = DocumentFile.fromTreeUri(this, uri)

                if (documentFile != null && documentFile.isDirectory) {
                    val newFile = documentFile.createFile("text/plain", fileName)
                    if (newFile != null) {
                        contentResolver.openOutputStream(newFile.uri)?.use { output ->
                            output.write(exportData.toString().toByteArray())
                            output.flush()
                        }

                        Toast.makeText(
                            this,
                            "Note exported: $fileName",
                            Toast.LENGTH_LONG
                        ).show()

                        statusText.text = "✓ Exported to: ${documentFile.name}/$fileName"
                    } else {
                        Toast.makeText(this, "Failed to create file", Toast.LENGTH_LONG).show()
                        statusText.text = "✗ Export failed"
                    }
                } else {
                    Toast.makeText(this, "Invalid folder selected", Toast.LENGTH_LONG).show()
                    statusText.text = "✗ Export failed"
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            statusText.text = "✗ Export failed"
        }
    }

    private fun showDeleteConfirmation() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirmation, null)
        val pinInput = dialogView.findViewById<EditText>(R.id.pinInput)
        val messageText = dialogView.findViewById<TextView>(R.id.deleteMessage)

        messageText.text = "To confirm deletion, please enter the PIN: 12345"

        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setView(dialogView)
            .setPositiveButton("Delete") { _, _ ->
                val enteredPin = pinInput.text.toString().trim()
                if (enteredPin == "12345") {
                    deleteNote()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showColorPicker() {
        val start = contentInput.selectionStart
        val end = contentInput.selectionEnd

        if (start == end) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val colorPreview = dialogView.findViewById<View>(R.id.colorPreview)
        val redSeekBar = dialogView.findViewById<SeekBar>(R.id.redSeekBar)
        val greenSeekBar = dialogView.findViewById<SeekBar>(R.id.greenSeekBar)
        val blueSeekBar = dialogView.findViewById<SeekBar>(R.id.blueSeekBar)
        val redValue = dialogView.findViewById<TextView>(R.id.redValue)
        val greenValue = dialogView.findViewById<TextView>(R.id.greenValue)
        val blueValue = dialogView.findViewById<TextView>(R.id.blueValue)
        val presetColorsLayout = dialogView.findViewById<LinearLayout>(R.id.presetColors)

        var selectedColor = Color.BLACK

        val updateColor = {
            val r = redSeekBar.progress
            val g = greenSeekBar.progress
            val b = blueSeekBar.progress
            selectedColor = Color.rgb(r, g, b)
            colorPreview.setBackgroundColor(selectedColor)
            redValue.text = r.toString()
            greenValue.text = g.toString()
            blueValue.text = b.toString()
        }

        redSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        greenSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        blueSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val presetColors = listOf(
            Color.RED, Color.BLUE, Color.GREEN,
            0xFFFF9800.toInt(),
            0xFF9C27B0.toInt(),
            0xFFE91E63.toInt(),
            0xFF009688.toInt(),
            0xFF795548.toInt(),
            Color.BLACK,
            0xFF424242.toInt()
        )

        for (color in presetColors) {
            val colorButton = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundColor(color)
                setOnClickListener {
                    redSeekBar.progress = Color.red(color)
                    greenSeekBar.progress = Color.green(color)
                    blueSeekBar.progress = Color.blue(color)
                    updateColor()
                }
            }
            presetColorsLayout.addView(colorButton)
        }

        updateColor()

        AlertDialog.Builder(this)
            .setTitle("Choose Text Color")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                applyColorToSelection(start, end, selectedColor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleStyle(style: Int) {
        val start = contentInput.selectionStart
        val end = contentInput.selectionEnd

        if (start == end) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show()
            return
        }

        val text = contentInput.text
        val spannable = if (text is Spannable) text as Spannable else SpannableString(text)

        val existingSpans = spannable.getSpans(start, end, StyleSpan::class.java)
        var isStyleApplied = false

        for (span in existingSpans) {
            if (span.style == style) {
                spannable.removeSpan(span)
                isStyleApplied = true
            }
        }

        if (!isStyleApplied) {
            spannable.setSpan(
                StyleSpan(style),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        contentInput.setText(spannable, TextView.BufferType.SPANNABLE)
        contentInput.setSelection(end)

        val styleName = if (style == Typeface.BOLD) "Bold" else "Italic"
        val action = if (isStyleApplied) "removed" else "applied"
        Toast.makeText(this, "$styleName $action", Toast.LENGTH_SHORT).show()
    }

    private fun toggleUnderline() {
        val start = contentInput.selectionStart
        val end = contentInput.selectionEnd

        if (start == end) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show()
            return
        }

        val text = contentInput.text
        val spannable = if (text is Spannable) text as Spannable else SpannableString(text)

        val existingSpans = spannable.getSpans(start, end, UnderlineSpan::class.java)
        var isUnderlined = false

        for (span in existingSpans) {
            spannable.removeSpan(span)
            isUnderlined = true
        }

        if (!isUnderlined) {
            spannable.setSpan(
                UnderlineSpan(),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        contentInput.setText(spannable, TextView.BufferType.SPANNABLE)
        contentInput.setSelection(end)

        val action = if (isUnderlined) "removed" else "applied"
        Toast.makeText(this, "Underline $action", Toast.LENGTH_SHORT).show()
    }

    private fun applyColorToSelection(start: Int, end: Int, color: Int) {
        val text = contentInput.text
        val spannable = if (text is Spannable) text as Spannable else SpannableString(text)

        val existingSpans = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
        for (span in existingSpans) {
            spannable.removeSpan(span)
        }

        spannable.setSpan(
            ForegroundColorSpan(color),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        contentInput.setText(spannable, TextView.BufferType.SPANNABLE)
        contentInput.setSelection(end)

        Toast.makeText(this, "Color applied to selected text", Toast.LENGTH_SHORT).show()
    }

    private fun loadNoteData() {
        titleInput.setText(currentNote?.title)

        Log.d(TAG, "========== LOADING NOTE ==========")
        Log.d(TAG, "Title: ${currentNote?.title}")
        Log.d(TAG, "Plain Content: ${currentNote?.content}")
        Log.d(TAG, "Formatted Content (Raw): ${currentNote?.contentWithFormatting}")
        Log.d(TAG, "Formatted Content Length: ${currentNote?.contentWithFormatting?.length}")

        if (!currentNote?.contentWithFormatting.isNullOrEmpty()) {
            try {
                Log.d(TAG, "Attempting to parse HTML to Spannable...")
                val spannable = htmlToSpannable(currentNote!!.contentWithFormatting)
                Log.d(TAG, "Spannable created successfully")
                Log.d(TAG, "Spannable length: ${spannable.length}")
                Log.d(TAG, "Spannable text: '${spannable.toString()}'")

                val colorSpans = spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
                Log.d(TAG, "Color spans found in spannable: ${colorSpans.size}")
                for ((index, span) in colorSpans.withIndex()) {
                    val start = spannable.getSpanStart(span)
                    val end = spannable.getSpanEnd(span)
                    val color = span.foregroundColor
                    Log.d(TAG, "Color span $index: start=$start, end=$end, color=${String.format("%08X", color)}, text='${spannable.substring(start, end)}'")
                }

                val styleSpans = spannable.getSpans(0, spannable.length, StyleSpan::class.java)
                Log.d(TAG, "Style spans found: ${styleSpans.size}")
                for ((index, span) in styleSpans.withIndex()) {
                    val start = spannable.getSpanStart(span)
                    val end = spannable.getSpanEnd(span)
                    Log.d(TAG, "Style span $index: start=$start, end=$end, style=${span.style}")
                }

                val underlineSpans = spannable.getSpans(0, spannable.length, UnderlineSpan::class.java)
                Log.d(TAG, "Underline spans found: ${underlineSpans.size}")

                Log.d(TAG, "Setting spannable to EditText with BufferType.SPANNABLE...")
                contentInput.setText(spannable, TextView.BufferType.SPANNABLE)
                contentInput.setSelection(contentInput.text.length)

                Log.d(TAG, "Text set to EditText successfully")

                // Verify what's in the EditText NOW
                val editTextSpans = contentInput.text.getSpans(0, contentInput.text.length, ForegroundColorSpan::class.java)
                Log.d(TAG, "Color spans in EditText AFTER setText: ${editTextSpans.size}")
                for ((index, span) in editTextSpans.withIndex()) {
                    val start = contentInput.text.getSpanStart(span)
                    val end = contentInput.text.getSpanEnd(span)
                    val color = span.foregroundColor
                    Log.d(TAG, "EditText color span $index: start=$start, end=$end, color=${String.format("%08X", color)}, text='${contentInput.text.substring(start, end)}'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing HTML: ${e.message}", e)
                e.printStackTrace()
                contentInput.setText(currentNote?.content)
            }
        } else {
            Log.d(TAG, "No formatted content available")
            contentInput.setText(currentNote?.content)
        }

        statusText.text = "Editing note"
        deleteButton.isEnabled = true
        exportButton.isEnabled = true
        Log.d(TAG, "========== NOTE LOADED ==========")
    }

    private fun saveNote() {
        val title = titleInput.text.toString().trim()
        val plainContent = contentInput.text.toString()
        val selectedCategory = categorySpinner.selectedItem?.toString() ?: CategoryManager.DEFAULT_CATEGORY

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        if (plainContent.trim().isEmpty()) {
            Toast.makeText(this, "Please enter note content", Toast.LENGTH_SHORT).show()
            return
        }

        val htmlContent = spannableToHtml(contentInput.text as? Spannable)

        Log.d(TAG, "========== SAVING NOTE ==========")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Plain Content: $plainContent")
        Log.d(TAG, "HTML Content: $htmlContent")
        Log.d(TAG, "HTML Content Length: ${htmlContent.length}")
        Log.d(TAG, "Category: $selectedCategory")
        Log.d(TAG, "========== NOTE SAVED ==========")

        val note = if (currentNote != null) {
            currentNote!!.copy(
                title = title,
                content = plainContent,
                contentWithFormatting = htmlContent,
                category = selectedCategory,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Note(
                title = title,
                content = plainContent,
                contentWithFormatting = htmlContent,
                category = selectedCategory
            )
        }

        if (noteManager.saveNote(note)) {
            Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show()
            currentNote = note
            statusText.text = "✓ Note saved"
        } else {
            Toast.makeText(this, "Failed to save note", Toast.LENGTH_LONG).show()
            statusText.text = "✗ Save failed"
        }
    }

    private fun spannableToHtml(spannable: Spannable?): String {
        Log.d(TAG, "========== CONVERTING SPANNABLE TO HTML ==========")
        if (spannable == null) {
            Log.d(TAG, "Spannable is null, returning empty string")
            return ""
        }

        Log.d(TAG, "Spannable text: '${spannable.toString()}'")
        Log.d(TAG, "Spannable length: ${spannable.length}")

        // Check all span types
        val allColorSpans = spannable.getSpans(0, spannable.length, ForegroundColorSpan::class.java)
        Log.d(TAG, "Found ${allColorSpans.size} color spans total")
        for ((idx, span) in allColorSpans.withIndex()) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            val flags = spannable.getSpanFlags(span)
            Log.d(TAG, "Color span $idx: start=$start, end=$end, flags=$flags, color=${String.format("%08X", span.foregroundColor)}, text='${spannable.substring(start, end)}'")
        }

        val allStyleSpans = spannable.getSpans(0, spannable.length, StyleSpan::class.java)
        Log.d(TAG, "Found ${allStyleSpans.size} style spans total")

        val allUnderlineSpans = spannable.getSpans(0, spannable.length, UnderlineSpan::class.java)
        Log.d(TAG, "Found ${allUnderlineSpans.size} underline spans total")

        val html = StringBuilder()
        var i = 0

        while (i < spannable.length) {
            val colorSpans = spannable.getSpans(i, i + 1, ForegroundColorSpan::class.java)
            val styleSpans = spannable.getSpans(i, i + 1, StyleSpan::class.java)
            val underlineSpans = spannable.getSpans(i, i + 1, UnderlineSpan::class.java)

            Log.d(TAG, "Position $i: color=${colorSpans.size}, style=${styleSpans.size}, underline=${underlineSpans.size}")

            if (colorSpans.isEmpty() && styleSpans.isEmpty() && underlineSpans.isEmpty()) {
                val char = spannable[i]
                if (char == '\n') {
                    html.append("<br/>")
                } else {
                    html.append(escapeHtmlChar(char))
                }
                i++
            } else {
                var end = i + 1
                for (span in colorSpans) {
                    end = maxOf(end, spannable.getSpanEnd(span))
                }
                for (span in styleSpans) {
                    end = maxOf(end, spannable.getSpanEnd(span))
                }
                for (span in underlineSpans) {
                    end = maxOf(end, spannable.getSpanEnd(span))
                }
                end = minOf(end, spannable.length)

                // Open style tags
                for (span in styleSpans) {
                    when (span.style) {
                        Typeface.BOLD -> html.append("<b>")
                        Typeface.ITALIC -> html.append("<i>")
                    }
                }
                for (span in underlineSpans) {
                    html.append("<u>")
                }
                for (span in colorSpans) {
                    val colorHex = String.format("%08X", span.foregroundColor)
                    html.append("<span style=\"color:#${colorHex.substring(2)}\">")
                }

                // Content
                for (j in i until end) {
                    val char = spannable[j]
                    if (char == '\n') {
                        html.append("<br/>")
                    } else {
                        html.append(escapeHtmlChar(char))
                    }
                }

                // Close color tags
                for (span in colorSpans) {
                    html.append("</span>")
                }
                for (span in underlineSpans) {
                    html.append("</u>")
                }
                for (span in styleSpans.reversed()) {
                    when (span.style) {
                        Typeface.BOLD -> html.append("</b>")
                        Typeface.ITALIC -> html.append("</i>")
                    }
                }

                i = end
            }
        }

        Log.d(TAG, "========== CONVERSION COMPLETE ==========")
        Log.d(TAG, "Final HTML output:\n$html")
        return html.toString()
    }

    private fun htmlToSpannable(html: String): Spannable {
        Log.d(TAG, "========== PARSING HTML TO SPANNABLE ==========")
        Log.d(TAG, "Input HTML: $html")

        val text = StringBuilder()
        val spans = mutableListOf<SpanInfo>()
        var i = 0
        val openTags = mutableListOf<String>() // Track open tags for closing

        while (i < html.length) {
            if (html[i] == '<') {
                val endTag = html.indexOf('>', i)
                if (endTag != -1) {
                    val tag = html.substring(i + 1, endTag)
                    Log.d(TAG, "Found tag: <$tag>")

                    when {
                        tag == "br/" -> {
                            text.append("\n")
                            Log.d(TAG, "Added newline")
                        }
                        tag.startsWith("span style=\"color:#") -> {
                            val colorHex = tag.substring("span style=\"color:#".length, tag.length - 1)
                            Log.d(TAG, "Color hex: $colorHex")
                            val color = (colorHex.toLongOrNull(16)?.toInt() ?: 0) or 0xFF000000.toInt()
                            Log.d(TAG, "Parsed color: ${String.format("%08X", color)}")
                            spans.add(SpanInfo(text.length, -1, "color", color))
                            openTags.add("span")
                            Log.d(TAG, "Added color span at position ${text.length}")
                        }
                        tag == "/span" -> {
                            if (openTags.isNotEmpty() && openTags.last() == "span") {
                                openTags.removeAt(openTags.size - 1)
                                for (span in spans.reversed()) {
                                    if (span.end == -1 && span.type == "color") {
                                        span.end = text.length
                                        Log.d(TAG, "Closed color span: start=${span.start}, end=${span.end}")
                                        break
                                    }
                                }
                            }
                        }
                        tag == "b" -> {
                            spans.add(SpanInfo(text.length, -1, "bold", 0))
                            openTags.add("b")
                            Log.d(TAG, "Added bold span at position ${text.length}")
                        }
                        tag == "/b" -> {
                            if (openTags.isNotEmpty() && openTags.last() == "b") {
                                openTags.removeAt(openTags.size - 1)
                                for (span in spans.reversed()) {
                                    if (span.end == -1 && span.type == "bold") {
                                        span.end = text.length
                                        Log.d(TAG, "Closed bold span: start=${span.start}, end=${span.end}")
                                        break
                                    }
                                }
                            }
                        }
                        tag == "i" -> {
                            spans.add(SpanInfo(text.length, -1, "italic", 0))
                            openTags.add("i")
                            Log.d(TAG, "Added italic span at position ${text.length}")
                        }
                        tag == "/i" -> {
                            if (openTags.isNotEmpty() && openTags.last() == "i") {
                                openTags.removeAt(openTags.size - 1)
                                for (span in spans.reversed()) {
                                    if (span.end == -1 && span.type == "italic") {
                                        span.end = text.length
                                        Log.d(TAG, "Closed italic span: start=${span.start}, end=${span.end}")
                                        break
                                    }
                                }
                            }
                        }
                        tag == "u" -> {
                            spans.add(SpanInfo(text.length, -1, "underline", 0))
                            openTags.add("u")
                            Log.d(TAG, "Added underline span at position ${text.length}")
                        }
                        tag == "/u" -> {
                            if (openTags.isNotEmpty() && openTags.last() == "u") {
                                openTags.removeAt(openTags.size - 1)
                                for (span in spans.reversed()) {
                                    if (span.end == -1 && span.type == "underline") {
                                        span.end = text.length
                                        Log.d(TAG, "Closed underline span: start=${span.start}, end=${span.end}")
                                        break
                                    }
                                }
                            }
                        }
                    }
                    i = endTag + 1
                } else {
                    text.append(html[i])
                    i++
                }
            } else {
                text.append(html[i])
                i++
            }
        }

        // Close any remaining open spans at end of text
        for (idx in spans.indices.reversed()) {
            if (spans[idx].end == -1) {
                spans[idx].end = text.length
                Log.d(TAG, "Auto-closed span at end: type=${spans[idx].type}, start=${spans[idx].start}, end=${spans[idx].end}")
            }
        }

        Log.d(TAG, "Final text: '${text.toString()}'")
        Log.d(TAG, "Total spans to apply: ${spans.size}")
        for ((idx, span) in spans.withIndex()) {
            Log.d(TAG, "Span $idx: type=${span.type}, start=${span.start}, end=${span.end}, value=${String.format("%08X", span.value)}")
        }

        val spannable = SpannableString(text.toString())
        for (span in spans) {
            if (span.end > 0 && span.start < span.end) {
                when (span.type) {
                    "color" -> {
                        spannable.setSpan(
                            ForegroundColorSpan(span.value),
                            span.start,
                            span.end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        Log.d(TAG, "Applied color span: start=${span.start}, end=${span.end}, color=${String.format("%08X", span.value)}")
                    }
                    "bold" -> {
                        spannable.setSpan(
                            StyleSpan(Typeface.BOLD),
                            span.start,
                            span.end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        Log.d(TAG, "Applied bold span: start=${span.start}, end=${span.end}")
                    }
                    "italic" -> {
                        spannable.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            span.start,
                            span.end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        Log.d(TAG, "Applied italic span: start=${span.start}, end=${span.end}")
                    }
                    "underline" -> {
                        spannable.setSpan(
                            UnderlineSpan(),
                            span.start,
                            span.end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        Log.d(TAG, "Applied underline span: start=${span.start}, end=${span.end}")
                    }
                }
            } else {
                Log.d(TAG, "Skipped invalid span: type=${span.type}, start=${span.start}, end=${span.end}")
            }
        }

        Log.d(TAG, "========== PARSING COMPLETE ==========")
        return spannable
    }

    private fun escapeHtmlChar(char: Char): String {
        return when (char) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '"' -> "&quot;"
            '\'' -> "&#39;"
            else -> char.toString()
        }
    }

    private fun deleteNote() {
        currentNote?.let {
            if (noteManager.deleteNote(it.id)) {
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to delete note", Toast.LENGTH_LONG).show()
            }
        }
    }

    private data class SpanInfo(
        val start: Int,
        var end: Int,
        val type: String,
        val value: Int = 0
    )
}