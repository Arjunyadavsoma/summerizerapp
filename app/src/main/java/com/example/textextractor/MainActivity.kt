package com.example.textextractor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var extractedTextView: TextView
    private lateinit var summaryTextView: TextView
    private val client = OkHttpClient()
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        extractedTextView = findViewById(R.id.extractedTextView)
        summaryTextView = findViewById(R.id.summaryTextView)
        val uploadButton: Button = findViewById(R.id.uploadButton)

        uploadButton.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                fileUri = uri
                uploadFile(uri)
            }
        }
    }

    private fun uploadFile(fileUri: Uri) {
        val file = File(getRealPathFromURI(fileUri))
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, RequestBody.create(MediaType.parse("multipart/form-data"), file))
            .build()

        val request = Request.Builder()
            .url("https://your-replit-project.repl.co/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Upload failed.", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val extractedText = jsonResponse.getString("extractedText")
                    val summary = jsonResponse.getString("summary")

                    runOnUiThread {
                        extractedTextView.text = extractedText
                        summaryTextView.text = summary
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Error processing file.", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val columnIndex = cursor?.getColumnIndex(android.provider.MediaStore.Files.FileColumns.DATA)
        val filePath = columnIndex?.let { cursor.getString(it) }
        cursor?.close()
        return filePath ?: ""
    }
}