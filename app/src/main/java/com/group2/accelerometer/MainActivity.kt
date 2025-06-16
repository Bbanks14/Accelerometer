package com.group2.accelerometer

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
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
import com.group2.accelerometer.features.HelpActivity
import com.group2.accelerometer.features.HistoryActivity
import com.group2.accelerometer.features.LiveGraphActivity
import com.group2.accelerometer.features.MainActivity
import com.group2.accelerometer.features.PremiumDialogFragment
import com.group2.accelerometer.features.SettingsActivity
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
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
    private lateinit var dataSetX: LineDataSet
    private lateinit var dataSetY: LineDataSet
    private lateinit var dataSetZ: LineDataSet
    private lateinit var coordinateToggleMenuItem: MenuItem

    private var maxAcceleration = 0f
    private var isSensorActive = false
    private var calibrationX = 0f
    private var calibrationY = 0f
    private var calibrationZ = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val recordedData = mutableListOf<AccelerometerReading>()
    private var isCartesianMode = true
    private var isInitialized = false
    private var isReceiverRegistered = false

    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private var isFilterInitialized = false
    private val filterAlpha = 0.1f // Filter smoothing factor (0.1 = heavy filtering, 0.9 = light filtering)

    data class AccelerometerReading(
        val timestamp: Long,
        val x: Float,
        val y: Float,
        val z: Float,
        val magnitude: Float
    )

    private var calibrationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.group2.accelerometer.CALIBRATE_SENSOR") {
                calibrateSensor()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter("com.group2.accelerometer.CALIBRATE_SENSOR")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(calibrationReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            isReceiverRegistered = true
        } else {
            registerReceiver(calibrationReceiver, intentFilter)
            isReceiverRegistered = true
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initially set the splash screen layout
        setContentView(R.layout.splash_screen)

        // Animate the splash logo
        val splashLogo = findViewById<ImageView>(R.id.splash_logo)
        splashLogo.alpha = 0f
        splashLogo.animate().alpha(1f).duration = 1000L

        // Use Handler to delay and switch to the main UI
        Handler(Looper.getMainLooper()).postDelayed({
            initializeMainActivity()
        }, 2000) // 2 seconds delay
    }

    private fun initializeMainActivity() {
        // Switch to the main layout
        setContentView(R.layout.activity_main)

        // Initialize sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: run {
            Toast.makeText(this, "Accelerometer not available on this device", Toast.LENGTH_LONG).show()
            return // Exit if accelerometer is not available
        }

        // Initialize UI components
        setupNavigationDrawer()
        initializeViews()
        setupCharts()
        setupButtonListeners()

        // Initialize SharedPreferences
        appSettings = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Register broadcast receiver
        registerBroadcastReceiver()

        // Load preferences and update UI
        loadCoordinatePreference()
        updateGraphLabels()
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
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(calibrationReceiver)
                isReceiverRegistered = false
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Apply low-pass filter to reduce noise
        val (filteredX, filteredY, filteredZ) = applyLowPassFilter(x, y, z)

        // Calibrate filtered sensor values
        val calibratedX = filteredX - calibrationX
        val calibratedY = filteredY - calibrationY
        val calibratedZ = filteredZ - calibrationZ

        // Convert coordinates if needed
        val (displayX, displayY, displayZ) = convertToDisplay(calibratedX, calibratedY, calibratedZ)

        // Update UI
        updateTextViews(displayX, displayY, displayZ)

        // Calculate magnitude
        val magnitude = sqrt(calibratedX * calibratedX + calibratedY * calibratedY + calibratedZ * calibratedZ)

        // Update maximum acceleration
        updateMaxAcceleration(magnitude)

        // Add data to charts
        val time = System.currentTimeMillis() / 1000f // Use system time for better consistency
        addDataEntry(dataSetX, chartX, time, displayX)
        addDataEntry(dataSetY, chartY, time, displayY)
        addDataEntry(dataSetZ, chartZ, time, displayZ)

        // Store current filtered values for calibration
        lastX = filteredX
        lastY = filteredY
        lastZ = filteredZ

        // Record filtered data for export
        recordedData.add(AccelerometerReading(
            System.currentTimeMillis(),
            calibratedX,
            calibratedY,
            calibratedZ,
            magnitude
        ))

        // Limit recorded data size to prevent memory issues
        if (recordedData.size > 10000) {
            recordedData.removeAt(0)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> { /* Return to MainActivity */
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()

            }
            R.id.nav_live_graph -> {
                startLiveGraphActivity()
                finish()
            }
            R.id.nav_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                finish()
            }
            R.id.nav_calibrate -> calibrateSensor()
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
            }
            R.id.nav_about -> {
                showAboutDialog()
            }
            R.id.nav_coordinate_toggle -> {
                toggleCoordinateSystem()
                updateGraphLabels()
                return true
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        coordinateToggleMenuItem = navView.menu.findItem(R.id.nav_coordinate_toggle)
        updateCoordinateToggleTitle()

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
            setTouchEnabled(false)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            legend.isEnabled = false

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

    private fun setupButtonListeners() {
        findViewById<ImageButton>(R.id.btn_start).setOnClickListener { startSensor() }
        findViewById<ImageButton>(R.id.btn_stop).setOnClickListener { stopSensor() }
        findViewById<ImageButton>(R.id.btn_record).setOnClickListener {
            // Toggle recording functionality
            Toast.makeText(this, "Recording functionality", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btn_share).setOnClickListener {
            exportDataToCsv()
        }

        listOf(R.id.btn_x_axis, R.id.btn_y_axis, R.id.btn_z_axis).forEach { id ->
            findViewById<View>(id).setOnClickListener { highlightSelectedAxis(it.id) }
        }
    }

    private fun startSensor() {
        if (!isSensorActive && ::accelerometer.isInitialized) {
            isSensorActive = true
            resetFilter() // Reset filter when starting sensor
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            Toast.makeText(this, "Sensor started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopSensor() {
        if (isSensorActive) {
            sensorManager.unregisterListener(this)
            isSensorActive = false
            Toast.makeText(this, "Sensor stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun highlightSelectedAxis(selectedAxisId: Int) {
        listOf(R.id.btn_x_axis, R.id.btn_y_axis, R.id.btn_z_axis).forEach { id ->
            findViewById<View>(id).setBackgroundResource(R.drawable.axis_button_background)
        }
        findViewById<View>(selectedAxisId).setBackgroundResource(R.drawable.axis_button_selected)
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

    private fun updateTextViews(x: Float, y: Float, z: Float) {
        tvXValue.text = String.format("%.2f", x)
        tvYValue.text = String.format("%.2f", y)
        tvZValue.text = String.format("%.2f", z)
    }

    private fun updateMaxAcceleration(magnitude: Float) {
        if (magnitude > maxAcceleration) {
            maxAcceleration = magnitude
            tvMaximumValue.text = String.format("%.3f", maxAcceleration)
        }
    }

    private fun calibrateSensor() {
        calibrationX = lastX
        calibrationY = lastY
        calibrationZ = lastZ
        maxAcceleration = 0f // Reset max acceleration on calibration
        Toast.makeText(this, "Sensor calibrated!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Apply low-pass filter to reduce sensor noise
     * Formula: filtered_value = alpha * new_value + (1 - alpha) * previous_filtered_value
     *
     * @param x Raw X acceleration value
     * @param y Raw Y acceleration value
     * @param z Raw Z acceleration value
     * @return Triple of filtered acceleration values
     */
    private fun applyLowPassFilter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        if (!isFilterInitialized) {
            // Initialize filter with first reading
            filteredX = x
            filteredY = y
            filteredZ = z
            isFilterInitialized = true
        } else {
            // Apply low-pass filter formula
            filteredX = filterAlpha * x + (1 - filterAlpha) * filteredX
            filteredY = filterAlpha * y + (1 - filterAlpha) * filteredY
            filteredZ = filterAlpha * z + (1 - filterAlpha) * filteredZ
        }

        return Triple(filteredX, filteredY, filteredZ)
    }

    /**
     * Reset the low-pass filter state
     * Call this when you want to start fresh filtering
     */
    private fun resetFilter() {
        isFilterInitialized = false
        filteredX = 0f
        filteredY = 0f
        filteredZ = 0f
    }

    private fun exportDataToCsv() {
        if (recordedData.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        val header = "Timestamp,X,Y,Z,Magnitude\n"
        val csvData = header + recordedData.joinToString("\n") { reading ->
            "${reading.timestamp},${reading.x},${reading.y},${reading.z},${reading.magnitude}"
        }

        try {
            val file = File(getExternalFilesDir(null), "accelerometer_data_${System.currentTimeMillis()}.csv")
            file.writeText(csvData)

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share CSV"))
            Toast.makeText(this, "Data exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPremiumDialog() {
        val dialog = PremiumDialogFragment()
        dialog.show(supportFragmentManager, PremiumDialogFragment.TAG)
    }

    private fun toggleCoordinateSystem() {
        isCartesianMode = !isCartesianMode

        // Save preference
        saveCoordinatePreference()

        // Update menu item title
        updateCoordinateToggleTitle()

        // Update main activity graphs
        updateGraphLabels()

        // Broadcast change to LiveGraphActivity if it's running
        broadcastCoordinateSystemChange()

        // Show feedback to user
        val message = if (isCartesianMode) "Switched to Cartesian coordinates" else "Switched to Polar coordinates"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateCoordinateToggleTitle() {
        val title = if (isCartesianMode) {
            getString(R.string.coordinate_polar) // Show what it will switch TO
        } else {
            getString(R.string.coordinate_cartesian)
        }
        coordinateToggleMenuItem.title = title
    }

    private fun updateGraphLabels() {
        if (isCartesianMode) {
            updateCartesianLabels()
        } else {
            updatePolarLabels()
        }
    }

    private fun updateCartesianLabels() {
        dataSetX.label = "X-Axis"
        dataSetY.label = "Y-Axis"
        dataSetZ.label = "Z-Axis"
    }

    private fun updatePolarLabels() {
        dataSetX.label = "ρ (Rho)"
        dataSetY.label = "θ (Theta)"
        dataSetZ.label = "Z-Axis"
    }

    private fun startLiveGraphActivity() {
        val intent = Intent(this, LiveGraphActivity::class.java)
        intent.putExtra("coordinate_system", if (isCartesianMode) "cartesian" else "polar")
        startActivity(intent)
    }

    private fun broadcastCoordinateSystemChange() {
        val intent = Intent("COORDINATE_SYSTEM_CHANGED")
        intent.putExtra("is_cartesian", isCartesianMode)
        sendBroadcast(intent)
    }

    private fun saveCoordinatePreference() {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_cartesian_mode", isCartesianMode).apply()
    }

    private fun loadCoordinatePreference() {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        isCartesianMode = prefs.getBoolean("is_cartesian_mode", true) // Default to Cartesian
    }

    // Coordinate conversion functions
    private fun convertToDisplay(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return if (isCartesianMode) {
            Triple(x, y, z)
        } else {
            cartesianToPolar(x, y, z)
        }
    }

    private fun cartesianToPolar(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        val rho = sqrt(x * x + y * y) // ρ = √(x² + y²)
        val theta = atan2(y, x) * 180 / PI.toFloat() // θ in degrees
        return Triple(rho, theta, z) // Z remains the same
    }

    private fun showAboutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)

        // Get app version
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName

        // Set version info
        val versionText = dialogView.findViewById<TextView>(R.id.tv_version)
        versionText.text = "Version $versionName"

        // Create the dialog
        val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set up button listeners
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close)
        val btnRateApp = dialogView.findViewById<Button>(R.id.btn_rate_app)
        val btnPrivacyPolicy = dialogView.findViewById<Button>(R.id.btn_privacy_policy)

        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }

        btnRateApp.setOnClickListener {
            openPlayStore()
            alertDialog.dismiss()
        }

        btnPrivacyPolicy.setOnClickListener {
            openPrivacyPolicy()
            alertDialog.dismiss()
        }

        // Show the dialog
        alertDialog.show()

        // Make dialog background transparent for rounded corners
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If Play Store app is not available, open in browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            startActivity(intent)
        }
    }

    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yourwebsite.com/privacy-policy"))
        startActivity(intent)
    }

    // Getter for other activities/fragments
    fun isCartesianMode(): Boolean = isCartesianMode

    companion object {
        const val COORDINATE_SYSTEM_CARTESIAN = "cartesian"
        const val COORDINATE_SYSTEM_POLAR = "polar"
    }
}