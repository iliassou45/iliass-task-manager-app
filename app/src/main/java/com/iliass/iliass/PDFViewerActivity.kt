package com.iliass.iliass

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.iliass.iliass.util.PDFManager
import java.io.File

class PDFViewerActivity : AppCompatActivity() {

    private lateinit var pdfTitleText: TextView
    private lateinit var pdfInfoText: TextView
    private lateinit var pdfWebView: WebView
    private lateinit var pdfMessageText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "PDF Viewer"

        initializeViews()
        loadPDF()
    }

    private fun initializeViews() {
        pdfTitleText = findViewById(R.id.pdfTitleText)
        pdfInfoText = findViewById(R.id.pdfInfoText)
        pdfWebView = findViewById(R.id.pdfWebView)
        pdfMessageText = findViewById(R.id.pdfMessageText)
    }

    private fun loadPDF() {
        val pdfPath = intent.getStringExtra("PDF_PATH")
        val lessonTitle = intent.getStringExtra("LESSON_TITLE") ?: "Lesson PDF"

        if (pdfPath == null || !PDFManager.pdfExists(pdfPath)) {
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        pdfTitleText.text = lessonTitle
        pdfInfoText.text = "File size: ${PDFManager.getFileSize(pdfPath)}"

        // Display PDF using WebView
        displayPDFInWebView(pdfPath)
    }

    private fun displayPDFInWebView(pdfPath: String) {
        try {
            pdfWebView.settings.apply {
                javaScriptEnabled = true
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            // Use Google Docs Viewer as a fallback for PDF rendering
            val pdfFile = File(pdfPath)
            val encodedPath = android.util.Base64.encodeToString(
                pdfFile.readBytes(),
                android.util.Base64.NO_WRAP
            )

            val html = """
                <html>
                <head>
                    <style>
                        body { margin: 0; padding: 0; }
                        iframe { border: none; width: 100%; height: 100vh; }
                    </style>
                </head>
                <body>
                    <embed src="data:application/pdf;base64,$encodedPath" type="application/pdf" width="100%" height="100%">
                </body>
                </html>
            """.trimIndent()

            pdfWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            pdfWebView.visibility = View.VISIBLE
            pdfMessageText.visibility = View.GONE

        } catch (e: Exception) {
            e.printStackTrace()
            pdfWebView.visibility = View.GONE
            pdfMessageText.visibility = View.VISIBLE
            pdfMessageText.text = "Error loading PDF. The file may be corrupted or too large.\n\nPath: $pdfPath"
            Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
