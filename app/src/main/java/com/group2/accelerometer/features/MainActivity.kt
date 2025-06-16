package com.group2.accelerometer.features

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        navView = binding.navView

        setupNavigationDrawer()
    }

    private fun setupNavigationDrawer() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Handle home navigation specially
                    if (!isTaskRoot) {
                        // We're not the root activity, finish everything above
                        finishAffinity()
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // Already home, just close drawer
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                }
                R.id.nav_live_graph -> startLiveGraphActivity()
                R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.nav_calibrate -> calibrateSensor()
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_premium -> showPremiumDialog()
                R.id.nav_export -> exportDataToCsv()
                R.id.nav_help -> startActivity(Intent(this, HelpActivity::class.java))
                R.id.nav_about -> showAboutDialog()
                R.id.nav_coordinate_toggle -> {
                    toggleCoordinateSystem()
                    updateGraphLabels()
                }
            }

            // For non-home items, close drawer normally
            if (menuItem.itemId != R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            true
        }
    }

    private fun startLiveGraphActivity() {
        startActivity(Intent(this, LiveGraphActivity::class.java))
    }

    private fun calibrateSensor() {
        // Calibration implementation
    }

    private fun toggleCoordinateSystem() {
        // Toggle between Cartesian/Polar
    }

    private fun updateGraphLabels() {
        // Update UI labels
    }

    private fun showPremiumDialog() {
        // Show premium features dialog
    }

    private fun exportDataToCsv() {
        // Export data implementation
    }

    private fun showAboutDialog() {
        // Show about dialog
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}