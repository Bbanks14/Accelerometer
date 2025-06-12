package com.group2.accelerometer.features

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ShareActivity : AppCompatActivity() {

    private lateinit var shareOptionsGroup: RadioGroup
    private lateinit var shareButton: Button
    private lateinit var backButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var includeDataCheckBox: CheckBox
    private lateinit var includeGraphCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        shareOptionsGroup = findViewById(R.id.shareOptionsGroup)
        shareButton = findViewById(R.id.shareButton)
        backButton = findViewById(R.id.backButton)
        messageEditText = findViewById(R.id.messageEditText)
        includeDataCheckBox = findViewById(R.id.includeDataCheckBox)
        includeGraphCheckBox = findViewById(R.id.includeGraphCheckBox)

        // Set default message
        messageEditText.setText("Check out my sensor data from the app!")
    }

    private fun setupClickListeners() {
        shareButton.setOnClickListener {
            handleShare()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun handleShare() {
        val selectedOptionId = shareOptionsGroup.checkedRadioButtonId

        when (selectedOptionId) {
            R.id.shareTextOnly -> shareTextOnly()
            R.id.shareWithData -> shareWithData()
            R.id.shareWithGraph -> shareWithGraph()
            R.id.shareComplete -> shareComplete()
            else -> {
                Toast.makeText(this, "Please select a sharing option", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    private fun shareTextOnly() {
        val message = messageEditText.text.toString()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, "Sensor Data App")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun shareWithData() {
        try {
            val dataFile = exportCurrentData()
            val message = messageEditText.text.toString()

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                dataFile
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/csv"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, "Sensor Data")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share data via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing data: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareWithGraph() {
        try {
            val graphImage = captureCurrentGraph()
            val imageFile = saveImageToFile(graphImage)
            val message = messageEditText.text.toString()

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, "Sensor Graph")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share graph via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing graph: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareComplete() {
        try {
            val files = mutableListOf<Uri>()
            val message = messageEditText.text.toString()

            // Add data file if checkbox is checked
            if (includeDataCheckBox.isChecked) {
                val dataFile = exportCurrentData()
                files.add(FileProvider.getUriForFile(this, "${packageName}.fileprovider", dataFile))
            }

            // Add graph image if checkbox is checked
            if (includeGraphCheckBox.isChecked) {
                val graphImage = captureCurrentGraph()
                val imageFile = saveImageToFile(graphImage)
                files.add(FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile))
            }

            if (files.isEmpty()) {
                shareTextOnly()
                return
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "*/*"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, "Complete Sensor Report")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share complete report via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing complete report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportCurrentData(): File {
        // Get current sensor data from your data source
        val sensorData = getCurrentSensorData() // You'll need to implement this

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "sensor_data_$timestamp.csv"
        val file = File(getExternalFilesDir(null), fileName)

        file.writeText(buildString {
            appendLine("Timestamp,X,Y,Z,Magnitude")
            sensorData.forEach { data ->
                appendLine("${data.timestamp},${data.x},${data.y},${data.z},${data.magnitude}")
            }
        })

        return file
    }

    private fun captureCurrentGraph(): Bitmap {
        // This should capture your current graph view
        // You'll need to pass the graph view or recreate it here
        val graphView = getGraphView() // You'll need to implement this

        val bitmap = Bitmap.createBitmap(
            graphView.width,
            graphView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        graphView.draw(canvas)

        return bitmap
    }

    private fun saveImageToFile(bitmap: Bitmap): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "sensor_graph_$timestamp.png"
        val file = File(getExternalFilesDir(null), fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file
    }

    // You'll need to implement these methods based on your app's data structure
    private fun getCurrentSensorData(): List<SensorDataPoint> {
        // Return current sensor data from your database or data source
        // This is a placeholder - replace with your actual data retrieval logic
        return emptyList()
    }

    private fun getGraphView(): View {
        // Return your graph view or recreate it
        // This is a placeholder - replace with your actual graph view logic
        return View(this)
    }

    // Data class for sensor data points
    data class SensorDataPoint(
        val timestamp: Long,
        val x: Float,
        val y: Float,
        val z: Float,
        val magnitude: Float
    )
}