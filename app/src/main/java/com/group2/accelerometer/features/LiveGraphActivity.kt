package com.group2.accelerometer.features

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LiveGraphActivity : AppCompatActivity() {

    private var isCartesianMode = true
    private lateinit var coordinateSystemReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get coordinate system from intent
        val coordinateSystem = intent.getStringExtra("coordinate_system")
        isCartesianMode = coordinateSystem != "polar"

        // Register broadcast receiver for coordinate system changes
        registerCoordinateSystemReceiver()

        // Setup graphs with current coordinate system
        updateGraphLabels()
    }

    private fun updateGraphLabels() {
        TODO("Not yet implemented")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerCoordinateSystemReceiver() {
        coordinateSystemReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isCartesianMode = intent.getBooleanExtra("is_cartesian", true)
                updateGraphLabels()
            }
        }

        val filter = IntentFilter("COORDINATE_SYSTEM_CHANGED")

        // Register with proper flags based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+)
            registerReceiver(
                coordinateSystemReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED  // Internal app communication
            )
        } else {
            // For older Android versions
            registerReceiver(coordinateSystemReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Safe unregister - check if receiver is registered
        try {
            unregisterReceiver(coordinateSystemReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered, ignore
        }
    }
}