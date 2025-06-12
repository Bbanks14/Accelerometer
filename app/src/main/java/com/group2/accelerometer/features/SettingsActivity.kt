
override fun onSupportNavigateUp(): Boolean {
    finish()
    return true
}

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "sampling_rate" -> updateSamplingRate(sharedPreferences)
            "auto_calibration" -> toggleCalibration(sharedPreferences)
            "theme_preference" -> applyTheme(sharedPreferences)
        }
    }

    private fun updateSamplingRate(prefs: SharedPreferences) {
        val rate = prefs.getString("sampling_rate", "UI") ?: "UI"
        val sensorDelay = when (rate) {
            "FASTEST" -> SensorManager.SENSOR_DELAY_FASTEST
            "GAME" -> SensorManager.SENSOR_DELAY_GAME
            "NORMAL" -> SensorManager.SENSOR_DELAY_NORMAL
            else -> SensorManager.SENSOR_DELAY_UI
        }

        // This would typically be handled in MainActivity
        val appPrefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        appPrefs.edit().putInt("sensor_delay", sensorDelay).apply()
    }

    private fun toggleCalibration(prefs: SharedPreferences) {
        val enabled = prefs.getBoolean("auto_calibration", true)
        // This would be handled in MainActivity
        val appPrefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        appPrefs.edit().putBoolean("auto_calibration", enabled).apply()
    }

    private fun applyTheme(prefs: SharedPreferences) {
        val theme = prefs.getString("theme_preference", "dark") ?: "dark"
        // Apply theme logic would be more complex in a real app
    }
}
