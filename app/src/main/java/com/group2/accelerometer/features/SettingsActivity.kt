package com.group2.accelerometer.features

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.group2.accelerometer.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var etSamplingRate: TextInputEditText
    private lateinit var rgDecimalPlaces: RadioGroup
    private lateinit var btnSaveSettings: Button
    private lateinit var btnBack: ImageView
    private lateinit var sharedPrefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "AppSettings"
        const val KEY_SAMPLING_RATE = "sampling_rate"
        const val KEY_DECIMAL_PLACES = "decimal_places"
        const val DEFAULT_SAMPLING_RATE = 50  // More realistic default for accelerometer
        const val MIN_SAMPLING_RATE = 1       // Minimum reasonable value
        const val MAX_SAMPLING_RATE = 500    // Maximum reasonable value
        const val DEFAULT_DECIMAL_PLACES = 2

        // Helper functions to access settings from other activities
        fun getSamplingRate(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_SAMPLING_RATE, DEFAULT_SAMPLING_RATE)
        }

        fun getDecimalPlaces(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_DECIMAL_PLACES, DEFAULT_DECIMAL_PLACES)
        }

        fun formatNumber(context: Context, number: Double): String {
            val decimalPlaces = getDecimalPlaces(context)
            return "%.${decimalPlaces}f".format(number)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        initSharedPreferences()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        etSamplingRate = findViewById(R.id.et_sampling_rate)
        rgDecimalPlaces = findViewById(R.id.rg_decimal_places)
        btnSaveSettings = findViewById(R.id.btn_save_settings)
        btnBack = findViewById(R.id.btn_back)
    }

    private fun initSharedPreferences() {
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadSettings() {
        // Load sampling rate with validation
        val savedSamplingRate = sharedPrefs.getInt(KEY_SAMPLING_RATE, DEFAULT_SAMPLING_RATE).coerceIn(
            MIN_SAMPLING_RATE,
            MAX_SAMPLING_RATE
        )
        etSamplingRate.setText(savedSamplingRate.toString())

        // Load decimal places
        val savedDecimalPlaces = sharedPrefs.getInt(KEY_DECIMAL_PLACES, DEFAULT_DECIMAL_PLACES)
        val radioId = when (savedDecimalPlaces) {
            1 -> R.id.rb_one_decimal
            2 -> R.id.rb_two_decimal
            3 -> R.id.rb_three_decimal
            else -> R.id.rb_two_decimal
        }
        rgDecimalPlaces.check(radioId)
    }

    private fun setupListeners() {
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun saveSettings() {
        val samplingRateText = etSamplingRate.text.toString().trim()

        if (samplingRateText.isEmpty()) {
            etSamplingRate.error = "Please enter a sampling rate"
            return
        }

        val samplingRate = try {
            samplingRateText.toInt()
        } catch (e: NumberFormatException) {
            etSamplingRate.error = "Please enter a valid number"
            return
        }

        // Validate realistic accelerometer range
        if (samplingRate !in MIN_SAMPLING_RATE..MAX_SAMPLING_RATE) {
            etSamplingRate.error = "Sampling rate should be between $MIN_SAMPLING_RATE and $MAX_SAMPLING_RATE Hz"
            return
        }

        val decimalPlaces = when (rgDecimalPlaces.checkedRadioButtonId) {
            R.id.rb_one_decimal -> 1
            R.id.rb_two_decimal -> 2
            R.id.rb_three_decimal -> 3
            else -> DEFAULT_DECIMAL_PLACES
        }

        sharedPrefs.edit().apply {
            putInt(KEY_SAMPLING_RATE, samplingRate)
            putInt(KEY_DECIMAL_PLACES, decimalPlaces)
            apply()
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}