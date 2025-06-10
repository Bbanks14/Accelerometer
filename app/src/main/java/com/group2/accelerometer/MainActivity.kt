package com.example.vibrolab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.navigation.NavigationView
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var tvXValue: TextView
    private lateinit var tvYValue: TextView
    private lateinit var tvZValue: TextView
    private lateinit var tvMaximumValue: TextView
    private lateinit var chartX: LineChart
    private lateinit var chartY: LineChart
    private lateinit var chartZ: LineChart
    private lateinit var appSettings: SharedPreferences

    private var maxAcceleration = 0f
    private var timeCounter = 0f
    private var isSensorActive = false
    private var isRecording = false
    private var calibrationX = 0f
    private var calibrationY = 0f
    private var calibrationZ = 0f
    private val recordedData = mutableListOf<AccelerometerReading>()

    data class AccelerometerReading(
        val timestamp: Long,
        val x: Float,
        val y: Float,
        val z: Float,
        val magnitude: Float
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: run {
            Toast.makeText(this, "Accelerometer not available on this device", Toast.LENGTH_LONG).show()
            // Disable sensor-related UI elements or functionality
            return
        }

        // Initialize UI components
        setupNavigationDrawer()
        initializeViews()
        setupCharts()
        setupButtonListeners()

        // Initialize SharedPreferences
        appSettings = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Register broadcast receiver for calibration
        registerReceiver(calibrationReceiver, IntentFilter("com.group2.accelerometer.CALIBRATE_SENSOR"))
    }

    private fun addDataEntry(dataSet: LineDataSet, chart: LineChart, time: Float, value: Float) {
        dataSet.addEntry(Entry(time, value))
        if (dataSet.entryCount > 100) {
            dataSet.removeFirst()
        }
        chart.notifyDataSetChanged()
        chart.invalidate()
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

    private fun initializeViews() {
        tvXValue = findViewById(R.id.tv_x_value)
        tvYValue = findViewById(R.id.tv_y_value)
        tvZValue = findViewById(R.id.tv_z_value)
        tvMaximumValue = findViewById(R.id.tv_maximum_value)
        chartX = findViewById(R.id.chart_x_axis)
        chartY = findViewById(R.id.chart_y_axis)
        chartZ = findViewById(R.id.chart_z_axis)
    }

    private fun setupCharts() {
        configureChart(chartX)
        configureChart(chartY)
        configureChart(chartZ)
    }

    private fun configureChart(chart: LineChart) {
        with(chart) {
            description.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            axisRight.isEnabled = false

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
                setDrawLabels(false)
            }
        }
    }

    private fun setupButtonListeners() {
        findViewById<ImageButton>(R.id.btn_start).setOnClickListener { startSensor() }
        findViewById<ImageButton>(R.id.btn_stop).setOnClickListener { stopSensor() }
        findViewById<ImageButton>(R.id.btn_record).setOnClickListener { /* Recording functionality */ }
        findViewById<ImageButton>(R.id.btn_share).setOnClickListener { /* Sharing functionality */ }

        // Axis selection buttons
        listOf(R.id.btn_x_axis, R.id.btn_y_axis, R.id.btn_z_axis).forEach { id ->
            findViewById<View>(id).setOnClickListener { highlightSelectedAxis(it.id) }
        }
    }

    private fun startSensor() {
        if (!isSensorActive) {
            isSensorActive = true
            maxAcceleration = 0f
            timeCounter = 0f
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopSensor() {
        if (isSensorActive) {
            sensorManager.unregisterListener(this)
            isSensorActive = false
        }
    }

    private fun highlightSelectedAxis(selectedAxisId: Int) {
        // Reset all backgrounds
        listOf(R.id.btn_x_axis, R.id.btn_y_axis, R.id.btn_z_axis).forEach { id ->
            findViewById<View>(id).setBackgroundResource(R.drawable.axis_button_background)
        }

        // Highlight selected axis
        findViewById<View>(selectedAxisId).setBackgroundResource(R.drawable.axis_button_selected)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Update current values
        tvXValue.text = String.format("%.1f", x)
        tvYValue.text = String.format("%.1f", y)
        tvZValue.text = String.format("%.1f", z)

        // Calculate magnitude
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude > maxAcceleration) {
            maxAcceleration = magnitude
            tvMaximumValue.text = String.format("%.3f", maxAcceleration)
        }

        // Add new data points
        val time = event.timestamp / 1_000_000_000f // Convert to seconds
        addDataEntry(dataSetX, chartX, time, x)
        addDataEntry(dataSetY, chartY, time, y)
        addDataEntry(dataSetZ, chartZ, time, z)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks
        when (item.itemId) {
            R.id.nav_home -> { /* Already on main activity */ }
            R.id.nav_live_graph -> { startActivity(Intent(this, LiveGraphActivity::class.java)) }
            R.id.nav_history -> { startActivity(Intent(this, HistoryActivity::class.java)) }
            R.id.nav_calibrate -> calibrateSensor()
            R.id.nav_share -> { /* Share functionality */ }
            R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java)) }
            R.id.nav_premium -> { showPremiumDialog() }
            R.id.nav_export -> exportDataToCsv()
            R.id.nav_help -> { startActivity(Intent(this, HelpActivity::class.java)) }
            R.id.nav_about -> { showAboutDialog() }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onPause() {
        super.onPause()
        stopSensor()
    }

    private fun calibrateSensor() {
        // Calibration logic here
        val calibratedX = x - calibrationX
        val calibratedY = y - calibrationY
        val calibratedZ = z - calibrationZ
        val magnitude = sqrt(calibratedX * calibratedX + calibratedY * calibratedY + calibratedZ * calibratedZ)
        addDataEntry(dataSetX, chartX, time, calibratedX)
        addDataEntry(dataSetY, chartY, time, calibratedY)
        addDataEntry(dataSetZ, chartZ, time, calibratedZ)
        // Store current values as baseline
        calibrationX = lastX
        calibrationY = lastY
        calibrationZ = lastZ
    }

    private fun exportDataToCsv() {
        val csvData = recordedData.joinToString("\n") { reading ->
            "${reading.timestamp},${reading.x},${reading.y},${reading.z},${reading.magnitude}"
        }
        // Save to file or share
        val file = File(getExternalFilesDir(null), "accelerometer_data_${System.currentTimeMillis()}.csv")
        file.writeText(csvData)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share CSV"))
    }

    private fun updateCharts() {
        // Update all charts together, less frequently
        chartX.data?.notifyDataChanged()
        chartY.data?.notifyDataChanged()
        chartZ.data?.notifyDataChanged()

        // Invalidate all at once
        chartX.invalidate()
        chartY.invalidate()
        chartZ.invalidate()
    }

    private fun showPremiumDialog() {
        val dialog = PremiumDialogFragment()
        dialog.show(supportFragmentManager, PremiumDialogFragment.TAG)
    }

    private val calibrationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.group2.accelerometer.CALIBRATE_SENSOR") {
                calibrateSensor()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(calibrationReceiver)
    }
}

