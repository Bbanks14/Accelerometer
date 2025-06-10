
package com.group2.accelerometer.features

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.group2.accelerometer.R
import com.group2.accelerometer.adapters.HistoryAdapter
import com.group2.accelerometer.models.RecordedSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val sessions = mutableListOf<RecordedSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setupToolbar()
        setupRecyclerView()
        loadRecordedSessions()
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Recording History"
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_history)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = HistoryAdapter(sessions) { session ->
            // Handle session click
            exportSessionData(session)
        }

        recyclerView.adapter = adapter
    }

    private fun loadRecordedSessions() {
        val filesDir = getExternalFilesDir(null)
        val files = filesDir?.listFiles { file ->
            file.name.endsWith(".csv") && file.name.startsWith("accelerometer_data_")
        } ?: emptyArray()

        sessions.clear()
        files.forEach { file ->
            val timestamp = file.name.substringAfter("accelerometer_data_").substringBefore(".csv").toLong()
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val sizeMB = String.format("%.2f MB", file.length() / (1024.0 * 1024.0))

            sessions.add(
                RecordedSession(
                    id = file.name,
                    fileName = file.name,
                    date = dateFormat.format(Date(timestamp)),
                    size = sizeMB
                )
            )
        }
        sessions.sortByDescending { it.id }
        adapter.notifyDataSetChanged()
    }

    private fun exportSessionData(session: RecordedSession) {
        val file = File(getExternalFilesDir(null), session.fileName)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Accelerometer Data: ${session.date}")
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                this@HistoryActivity,
                "${packageName}.fileprovider",
                file
            ))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Session Data"))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // Additional session handling methods
    override fun onSessionShare(session: RecordedSession) {
        exportSessionData(session)
    }

    override fun onSessionDelete(session: RecordedSession, position: Int) {
        // Handle post-deletion cleanup if needed
        if (sessions.isEmpty()) {
            showEmptyState()
        }
    }

    override fun onSessionView(session: RecordedSession) {
        showSessionDetails(session)
    }
}
