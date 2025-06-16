class SettingsFragment : PreferenceFragmentCompat(), 
    SharedPreferences.OnSharedPreferenceChangeListener {
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupPreferenceClickListeners()
    }
    
    private fun setupPreferenceClickListeners() {
        findPreference<Preference>("manual_calibration")?.setOnPreferenceClickListener {
            calibrateSensor()
            true
        }
        
        findPreference<Preference>("clear_all_data")?.setOnPreferenceClickListener {
            showClearDataDialog()
            true
        }
        
        findPreference<Preference>("privacy_policy")?.setOnPreferenceClickListener {
            openPrivacyPolicy()
            true
        }
    }
    
    private fun calibrateSensor() {
        // Send calibration command to MainActivity
        val intent = Intent("com.group2.accelerometer.CALIBRATE_SENSOR")
        requireContext().sendBroadcast(intent)
        
        Toast.makeText(context, "Sensor calibrated", Toast.LENGTH_SHORT).show()
    }
    
    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all recorded sessions. Continue?")
            .setPositiveButton("Delete") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearAllData() {
        val filesDir = requireContext().getExternalFilesDir(null)
        filesDir?.listFiles { file -> file.name.endsWith(".csv") }
            ?.forEach { it.delete() }
        
        Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            "sampling_rate" -> updateSamplingRate(sharedPreferences)
            "sensitivity" -> updateSensitivity(sharedPreferences)
            "auto_calibration" -> toggleCalibration(sharedPreferences)
            "theme_preference" -> applyTheme(sharedPreferences)
            "show_grid" -> updateChartSettings(sharedPreferences)
            "chart_refresh_rate" -> updateRefreshRate(sharedPreferences)
        }
    }
    
    private fun updateSensitivity(prefs: SharedPreferences) {
        val sensitivity = prefs.getInt("sensitivity", 10) / 100f
        saveAppSetting("sensitivity_threshold", sensitivity)
    }
    
    private fun updateChartSettings(prefs: SharedPreferences) {
        val showGrid = prefs.getBoolean("show_grid", true)
        saveAppSetting("show_grid", showGrid)
    }
    
    private fun saveAppSetting(key: String, value: Any) {
        val appPrefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        with(appPrefs.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
            }
            apply()
        }
    }
}
