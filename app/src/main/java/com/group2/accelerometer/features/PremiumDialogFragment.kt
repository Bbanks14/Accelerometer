
package com.group2.accelerometer.features

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.group2.accelerometer.R
import com.group2.accelerometer.databinding.DialogPremiumBinding

class PremiumDialogFragment : DialogFragment() {

    private lateinit var binding: DialogPremiumBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            setupPremiumFeatures()
            setupButtonListeners()
        } catch (e: Exception) {
            // Handle any exceptions that may occur during setup
            dismiss()
        }
    }

    private fun setupPremiumFeatures() {
        val featuresList = listOf(
            "Unlimited recording time",
            "Advanced data analytics",
            "Cloud synchronization",
            "Export to multiple formats",
            "Priority support",
            "Remove advertisements",
            "Custom sensitivity settings",
            "Historical data comparison"
        )

        featuresList.forEach { feature ->
            addFeatureToList(feature)
        }
    }

    private fun addFeatureToList(feature: String) {
        val featureView = LayoutInflater.from(context)
            .inflate(R.layout.list_item_premium_feature, binding.featuresList, false)
        featureView.findViewById<TextView>(R.id.tv_feature)?.text = feature
        binding.featuresList.addView(featureView)
    }

    private fun setupButtonListeners() {
        with(binding) {
            btnPurchase.setOnClickListener {
                handlePurchase()
            }

            btnRestore.setOnClickListener {
                handleRestore()
            }

            btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun handlePurchase() {
        // TODO: Implement billing client
        // For now, just show a toast
        Toast.makeText(context, "Purchase functionality coming soon!", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun handleRestore() {
        // TODO: Implement purchase restoration
        Toast.makeText(context, "Restore functionality coming soon!", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        const val TAG = "PremiumDialogFragment"
    }
}
