package com.group2.accelerometer.features

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.group2.accelerometer.R
import com.group2.accelerometer.adapters.HistoryAdapter
import com.group2.accelerometer.models.RecordedSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity(), HistoryAdapter.OnSessionActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var emptyStateView: TextView
    private val sessions = mutableListOf<RecordedSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setupToolbar()
        setupRecyclerView()
        setupEmptyState()
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
        adapter = HistoryAdapter(sessions, this)
        recyclerView.adapter = adapter
    }

    private fun setupEmptyState() {
        emptyStateView = findViewById(R.id.tv_empty_state)
    }

    private fun loadRecordedSessions() {
        val filesDir = getExternalFilesDir(null)
        val files = filesDir?.listFiles { file ->
            file.name.endsWith(".csv") && file.name.startsWith("accelerometer_data_")
        } ?: emptyArray()

        sessions.clear()
        files.forEach { file ->
            try {
                val timestamp = file.name
                    .substringAfter("accelerometer_data_")
                    .substringBefore(".csv")
                    .toLong()

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
            } catch (e: NumberFormatException) {
                // Skip files with invalid timestamp format
                e.printStackTrace()
            }
        }

        // Sort by filename (which contains timestamp) in descending order
        sessions.sortByDescending { it.id }
        adapter.notifyDataSetChanged()

        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (sessions.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState() {
        emptyStateView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun exportSessionData(session: RecordedSession) {
        val file = File(getExternalFilesDir(null), session.fileName)

        if (!file.exists()) {
            showErrorDialog("File not found: ${session.fileName}")
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Accelerometer Data: ${session.date}")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share Session Data"))
        } catch (e: Exception) {
            showErrorDialog("Error sharing file: ${e.message}")
        }
    }

    private fun deleteSession(session: RecordedSession, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Session")
            .setMessage("Are you sure you want to delete this recording session?")
            .setPositiveButton("Delete") { _, _ ->
                val file = File(getExternalFilesDir(null), session.fileName)
                if (file.exists() && file.delete()) {
                    sessions.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    updateEmptyState()
                } else {
                    showErrorDialog("Failed to delete file")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSessionDetails(session: RecordedSession) {
        val file = File(getExternalFilesDir(null), session.fileName)
        val details = StringBuilder().apply {
            append("File Name: ${session.fileName}\n")
            append("Date: ${session.date}\n")
            append("Size: ${session.size}\n")
            if (file.exists()) {
                append("Path: ${file.absolutePath}\n")
                append("Last Modified: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Session Details")
            .setMessage(details.toString())
            .setPositiveButton("Share") { _, _ -> exportSessionData(session) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // HistoryAdapter.OnSessionActionListener implementation
    override fun onSessionShare(session: RecordedSession) {
        exportSessionData(session)
    }

    override fun onSessionDelete(session: RecordedSession, position: Int) {
        deleteSession(session, position)
    }

    override fun onSessionView(session: RecordedSession) {
        showSessionDetails(session)
    }

    override fun onResume() {
        super.onResume()
        // Reload sessions when returning to this activity
        loadRecordedSessions()
    }
}