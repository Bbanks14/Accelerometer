package com.group2.accelerometer.features

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.group2.accelerometer.R

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Help"

        setupHelpContent()
    }

    private fun setupHelpContent() {
        val helpContent = findViewById<TextView>(R.id.tv_help_content)

        val helpText = """
            Welcome to the Accelerometer App!
            
            📱 WHAT IS AN ACCELEROMETER?
            An accelerometer is a sensor that measures acceleration forces acting on your device in three dimensions (X, Y, Z axes).
            
            🎯 HOW TO USE THIS APP:
            
            • X-Axis: Measures tilt left/right
            • Y-Axis: Measures tilt forward/backward  
            • Z-Axis: Measures up/down movement
            
            📊 READING THE VALUES:
            • Values range from -10 to +10 m/s²
            • Positive X: Device tilted to the right
            • Negative X: Device tilted to the left
            • Positive Y: Device tilted away from you
            • Negative Y: Device tilted toward you
            • Positive Z: Device face up
            • Negative Z: Device face down
            
            🔧 FEATURES:
            • Real-time accelerometer data
            • Visual graphs and charts
            • Data logging capabilities
            • Calibration options
            
            💡 TIPS:
            • Keep the device steady for accurate readings
            • Calibrate when needed for better precision
            • Use landscape mode for better visualization
            
            ❓ TROUBLESHOOTING:
            • If readings seem off, try recalibrating
            • Restart the app if sensor stops responding
            • Ensure your device has an accelerometer sensor
            
            For more information, visit our website or contact support.
        """.trimIndent()

        helpContent.text = helpText
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
