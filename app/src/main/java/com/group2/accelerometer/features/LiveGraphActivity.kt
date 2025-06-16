package com.group2.accelerometer.features

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.navigation.NavigationView
import com.group2.accelerometer.MainActivity
import com.group2.accelerometer.R
import com.group2.accelerometer.features.SettingsActivity
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

class LiveGraphActivity : AppCompatActivity(), SensorEventListener, NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var tvCoordinateSystem: TextView
    private lateinit var chartX: LineChart
    private lateinit var chartY: LineChart
    private lateinit var chartZ: LineChart

    // Sensor Components
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var dataSetX: LineDataSet
    private lateinit var dataSetY: LineDataSet
    private lateinit var dataSetZ: LineDataSet

    // State Variables
    private var isCartesianMode = true
    private var isSensorActive = false
    private var calibrationX = 0f
    private var calibrationY = 0f
    private var calibrationZ = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    // Filter Variables
    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private var isFilterInitialized = false
    private val filterAlpha = 0.1f

    // Broadcast Receiver
    private lateinit var coordinateSystemReceiver: BroadcastReceiver
    private var isReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_graph)

        // Get coordinate system from intent
        val coordinateSystem = intent.getStringExtra("coordinate_system")
        isCartesianMode = coordinateSystem != "polar"

        // Initialize components
        initializeSensor()
        initializeViews()
        setupNavigationDrawer()
        setupCharts()

        // Register broadcast receiver for coordinate system changes
        registerCoordinateSystemReceiver()

        // Setup graphs with current coordinate system
        updateCoordinateSystemDisplay()
        updateGraphLabels()

        // Start sensor automatically
        startSensor()
    }

    override fun onResume() {
        super.onResume()
        if (::accelerometer.isInitialized && !isSensorActive) {
            startSensor()
        }
    }

    override fun onPause() {
        super.onPause()
        stopSensor()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensor()

        // Safe unregister - check if receiver is registered
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(coordinateSystemReceiver)
                isReceiverRegistered = false
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered, ignore
            }
        }
    }

    private fun initializeSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: run {
            Toast.makeText(this, "Accelerometer not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun initializeViews() {
        tvCoordinateSystem = findViewById(R.id.tv_coordinate_system)
        chartX = findViewById(R.id.chart_x_axis)
        chartY = findViewById(R.id.chart_y_axis)
        chartZ = findViewById(R.id.chart_z_axis)
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        findViewById<ImageButton>(R.id.btn_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupCharts() {
        dataSetX = LineDataSet(mutableListOf(), "X Axis").apply {
            color = getColor(R.color.red)
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 2f
        }
        dataSetY = LineDataSet(mutableListOf(), "Y Axis").apply {
            color = getColor(R.color.green)
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 2f
        }
        dataSetZ = LineDataSet(mutableListOf(), "Z Axis").apply {
            color = getColor(R.color.blue)
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 2f
        }

        chartX.data = LineData(dataSetX)
        chartY.data = LineData(dataSetY)
        chartZ.data = LineData(dataSetZ)

        configureChart(chartX)
        configureChart(chartY)
        configureChart(chartZ)
    }

    private fun configureChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            legend.isEnabled = true

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = -20f
                axisMaximum = 20f
                setDrawLabels(true)
                textColor = getColor(R.color.text_tertiary)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerCoordinateSystemReceiver() {
        coordinateSystemReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isCartesianMode = intent.getBooleanExtra("is_cartesian", true)
                updateCoordinateSystemDisplay()
                updateGraphLabels()
            }
        }

        val filter = IntentFilter("COORDINATE_SYSTEM_CHANGED")

        // Register with proper flags based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(coordinateSystemReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(coordinateSystemReceiver, filter)
        }
        isReceiverRegistered = true
    }

    private fun updateCoordinateSystemDisplay() {
        val coordinateText = if (isCartesianMode) {
            "Coordinate System: Cartesian (X, Y, Z)"
        } else {
            "Coordinate System: Polar (ρ, θ, Z)"
        }
        tvCoordinateSystem.text = coordinateText
    }

    private fun updateGraphLabels() {
        if (isCartesianMode) {
            dataSetX.label = "X-Axis"
            dataSetY.label = "Y-Axis"
            dataSetZ.label = "Z-Axis"
        } else {
            dataSetX.label = "ρ (Rho)"
            dataSetY.label = "θ (Theta)"
            dataSetZ.label = "Z-Axis"
        }

        // Refresh charts
        chartX.invalidate()
        chartY.invalidate()
        chartZ.invalidate()
    }

    private fun startSensor() {
        if (!isSensorActive && ::accelerometer.isInitialized) {
            isSensorActive = true
            resetFilter()
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopSensor() {
        if (isSensorActive) {
            sensorManager.unregisterListener(this)
            isSensorActive = false
        }
    }

    private fun resetFilter() {
        isFilterInitialized = false
        filteredX = 0f
        filteredY = 0f
        filteredZ = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Apply low-pass filter
        val (filteredX, filteredY, filteredZ) = applyLowPassFilter(x, y, z)

        // Calibrate filtered sensor values
        val calibratedX = filteredX - calibrationX
        val calibratedY = filteredY - calibrationY
        val calibratedZ = filteredZ - calibrationZ

        // Convert coordinates if needed
        val (displayX, displayY, displayZ) = convertToDisplay(calibratedX, calibratedY, calibratedZ)

        // Add data to charts
        val time = System.currentTimeMillis() / 1000f
        addDataEntry(dataSetX, chartX, time, displayX)
        addDataEntry(dataSetY, chartY, time, displayY)
        addDataEntry(dataSetZ, chartZ, time, displayZ)

        // Store current filtered values for calibration
        lastX = filteredX
        lastY = filteredY
        lastZ = filteredZ
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun applyLowPassFilter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        if (!isFilterInitialized) {
            filteredX = x
            filteredY = y
            filteredZ = z
            isFilterInitialized = true
        } else {
            filteredX = filterAlpha * x + (1 - filterAlpha) * filteredX
            filteredY = filterAlpha * y + (1 - filterAlpha) * filteredY
            filteredZ = filterAlpha * z + (1 - filterAlpha) * filteredZ
        }
        return Triple(filteredX, filteredY, filteredZ)
    }

    private fun convertToDisplay(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return if (isCartesianMode) {
            Triple(x, y, z)
        } else {
            cartesianToPolar(x, y, z)
        }
    }

    private fun cartesianToPolar(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        val rho = sqrt(x * x + y * y)
        val theta = atan2(y, x) * 180 / PI.toFloat()
        return Triple(rho, theta, z)
    }

    private fun addDataEntry(dataSet: LineDataSet, chart: LineChart, time: Float, value: Float) {
        dataSet.addEntry(Entry(time, value))
        if (dataSet.entryCount > 100) {
            dataSet.removeFirst()
        }

        Handler(Looper.getMainLooper()).post {
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

    private fun calibrateSensor() {
        calibrationX = lastX
        calibrationY = lastY
        calibrationZ = lastZ
        Toast.makeText(this, "Sensor calibrated!", Toast.LENGTH_SHORT).show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            R.id.nav_live_graph -> {
                // Already on Live Graph, just close drawer
            }
            R.id.nav_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                finish()
            }
            R.id.nav_calibrate -> {
                calibrateSensor()
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                finish()
            }
            R.id.nav_premium -> {
                showPremiumDialog()
            }
            R.id.nav_export -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("action", "export")
                startActivity(intent)
                finish()
            }
            R.id.nav_help -> {
                startActivity(Intent(this, HelpActivity::class.java))
                finish()
            }
            R.id.nav_about -> {
                showAboutDialog()
            }
            R.id.nav_coordinate_toggle -> {
                toggleCoordinateSystem()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun toggleCoordinateSystem() {
        isCartesianMode = !isCartesianMode
        updateCoordinateSystemDisplay()
        updateGraphLabels()

        val message = if (isCartesianMode) "Switched to Cartesian coordinates" else "Switched to Polar coordinates"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPremiumDialog() {
        val dialog = PremiumDialogFragment()
        dialog.show(supportFragmentManager, PremiumDialogFragment.TAG)
    }

    private fun showAboutDialog() {
        Toast.makeText(this, "About VibroLab - Accelerometer Analysis Tool", Toast.LENGTH_LONG).show()
    }
}