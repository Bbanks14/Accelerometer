// Add to your MainActivity or base activity class

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView

class PremiumManager {

    companion object {
        private const val PREFS_NAME = "PremiumPrefs"
        private const val KEY_IS_PREMIUM = "is_premium"

        // Check if user has premium
        fun isPremiumUser(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_IS_PREMIUM, false)
        }

        // Set premium status (for testing or after successful purchase)
        fun setPremiumStatus(context: Context, isPremium: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
        }

        // Premium features list
        fun getPremiumFeatures(): List<String> {
            return listOf(
                "🚀 Unlimited data recording sessions",
                "📊 Advanced data export formats (CSV, JSON, Excel)",
                "🎨 Custom themes and UI customization",
                "📈 Real-time data visualization charts",
                "☁️ Cloud backup and sync",
                "🔔 Smart notifications and alerts",
                "📱 Widget support for home screen",
                "🛠️ Advanced calibration tools",
                "📧 Priority customer support",
                "🆕 Early access to new features"
            )
        }
    }
}

// Add this method to your MainActivity
private fun showPremiumDialog() {
    // Check if already premium
    if (PremiumManager.isPremiumUser(this)) {
        showAlreadyPremiumDialog()
        return
    }

    val dialog = Dialog(this)
    dialog.setContentView(R.layout.dialog_premium)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialog.window?.setLayout(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    // Initialize views
    val btnClose = dialog.findViewById<ImageView>(R.id.btn_close_premium)
    val btnUpgrade = dialog.findViewById<Button>(R.id.btn_upgrade_premium)
    val btnMaybeLater = dialog.findViewById<Button>(R.id.btn_maybe_later)
    val featuresContainer = dialog.findViewById<LinearLayout>(R.id.features_container)
    val btnRestorePurchases = dialog.findViewById<TextView>(R.id.btn_restore_purchases)

    // Populate features
    populatePremiumFeatures(featuresContainer)

    // Set click listeners
    btnClose.setOnClickListener {
        dialog.dismiss()
    }

    btnMaybeLater.setOnClickListener {
        dialog.dismiss()
    }

    btnUpgrade.setOnClickListener {
        handleUpgradeClick(dialog)
    }

    btnRestorePurchases.setOnClickListener {
        handleRestorePurchases(dialog)
    }

    dialog.show()
}

private fun populatePremiumFeatures(container: LinearLayout) {
    container.removeAllViews()

    PremiumManager.getPremiumFeatures().forEach { feature ->
        val featureView = LayoutInflater.from(this)
            .inflate(R.layout.item_premium_feature, container, false)

        val featureText = featureView.findViewById<TextView>(R.id.tv_feature)
        featureText.text = feature

        container.addView(featureView)
    }
}

private fun handleUpgradeClick(dialog: Dialog) {
    // Show loading state
    val btnUpgrade = dialog.findViewById<Button>(R.id.btn_upgrade_premium)
    val originalText = btnUpgrade.text
    btnUpgrade.text = "Processing..."
    btnUpgrade.isEnabled = false

    // Simulate purchase process (replace with actual billing logic)
    simulatePurchaseProcess { success ->
        runOnUiThread {
            if (success) {
                // Set premium status
                PremiumManager.setPremiumStatus(this, true)

                // Show success message
                Toast.makeText(this, "🎉 Welcome to Premium!", Toast.LENGTH_LONG).show()
                dialog.dismiss()

                // Optionally refresh UI to show premium features
                refreshPremiumUI()

            } else {
                // Reset button state
                btnUpgrade.text = originalText
                btnUpgrade.isEnabled = true

                // Show error message
                showPurchaseErrorDialog()
            }
        }
    }
}

private fun handleRestorePurchases(dialog: Dialog) {
    // Show loading
    val progressBar = dialog.findViewById<ProgressBar>(R.id.progress_restore)
    val btnRestore = dialog.findViewById<TextView>(R.id.btn_restore_purchases)

    progressBar.visibility = View.VISIBLE
    btnRestore.text = "Restoring..."

    // Simulate restore process
    simulateRestoreProcess { restored ->
        runOnUiThread {
            progressBar.visibility = View.GONE

            if (restored) {
                PremiumManager.setPremiumStatus(this, true)
                Toast.makeText(this, "Premium restored successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                refreshPremiumUI()
            } else {
                btnRestore.text = "Restore Purchases"
                Toast.makeText(this, "No purchases found to restore", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun simulatePurchaseProcess(callback: (Boolean) -> Unit) {
    // Replace this with actual Google Play Billing implementation
    Thread {
        Thread.sleep(2000) // Simulate network delay
        // Simulate 80% success rate for demo
        val success = (1..10).random() <= 8
        callback(success)
    }.start()
}

private fun simulateRestoreProcess(callback: (Boolean) -> Unit) {
    // Replace this with actual restore logic
    Thread {
        Thread.sleep(1500)
        // Simulate 50% chance of having previous purchase
        val restored = (1..10).random() <= 5
        callback(restored)
    }.start()
}

private fun showAlreadyPremiumDialog() {
    AlertDialog.Builder(this)
        .setTitle("🌟 Premium Active")
        .setMessage("You're already enjoying all premium features! Thank you for your support.")
        .setPositiveButton("Awesome!") { dialog, _ ->
            dialog.dismiss()
        }
        .setNeutralButton("Manage Subscription") { _, _ ->
            // Open Play Store subscription management
            openSubscriptionManagement()
        }
        .show()
}

private fun showPurchaseErrorDialog() {
    AlertDialog.Builder(this)
        .setTitle("Purchase Failed")
        .setMessage("We couldn't process your purchase right now. Please check your internet connection and try again.")
        .setPositiveButton("Try Again") { dialog, _ ->
            dialog.dismiss()
            showPremiumDialog() // Reopen premium dialog
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

private fun openSubscriptionManagement() {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/account/subscriptions")
        }
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, "Could not open subscription management", Toast.LENGTH_SHORT).show()
    }
}

private fun refreshPremiumUI() {
    // Update UI elements to show premium features
    // For example, remove ads, unlock features, change themes, etc.

    // Example: Update navigation drawer
    val navView = findViewById<NavigationView>(R.id.nav_view)
    val premiumItem = navView.menu.findItem(R.id.nav_premium)

    if (PremiumManager.isPremiumUser(this)) {
        premiumItem.title = "Premium Active ✓"
        premiumItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_premium_active)
    }

    // Notify other components about premium status change
    sendBroadcast(Intent("com.yourapp.PREMIUM_STATUS_CHANGED"))
}

// Helper method to check premium access before showing features
fun requiresPremiumAccess(action: () -> Unit) {
    if (PremiumManager.isPremiumUser(this)) {
        action()
    } else {
        showPremiumDialog()
    }
}

// Usage example:
private fun openAdvancedFeature() {
    requiresPremiumAccess {
        // This will only execute if user has premium
        startActivity(Intent(this, AdvancedFeatureActivity::class.java))
    }
}