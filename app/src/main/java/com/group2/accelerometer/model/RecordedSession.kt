package com.group2.accelerometer.models

import java.io.Serializable

data class RecordedSession(
    val id: String,
    val fileName: String,
    val date: String,
    val size: String,
    val duration: String = "Unknown",
    val maxAcceleration: Float = 0f,
    val dataPoints: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    
    fun getFormattedSize(): String {
        return when {
            size.contains("MB") -> size
            size.contains("KB") -> size
            else -> {
                val sizeBytes = size.toLongOrNull() ?: 0L
                when {
                    sizeBytes < 1024 -> "${sizeBytes}B"
                    sizeBytes < 1024 * 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
                    else -> String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
                }
            }
        }
    }
    
    fun getFormattedDuration(): String {
        if (duration != "Unknown") return duration
        
        // Try to calculate from filename if it contains timestamp info
        return "Unknown"
    }
    
    companion object {
        fun fromFile(file: java.io.File): RecordedSession {
            val timestamp = try {
                file.name.substringAfter("accelerometer_data_")
                    .substringBefore(".csv").toLong()
            } catch (e: Exception) {
                file.lastModified()
            }
            
            val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            val sizeMB = String.format("%.2f MB", file.length() / (1024.0 * 1024.0))
            
            return RecordedSession(
                id = file.name,
                fileName = file.name,
                date = dateFormat.format(java.util.Date(timestamp)),
                size = sizeMB,
                timestamp = timestamp
            )
        }
    }
}
