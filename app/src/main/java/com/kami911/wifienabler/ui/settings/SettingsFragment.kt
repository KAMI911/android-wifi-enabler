package com.kami911.wifienabler.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kami911.wifienabler.R
import com.kami911.wifienabler.databinding.FragmentSettingsBinding
import com.kami911.wifienabler.service.WifiEnablerService

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    /**
     * Set to true while programmatically updating switch states so that
     * change listeners do not trigger ViewModel actions.
     */
    private var isUpdatingUi = false

    // ── Permission launchers ──

    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            ) {
                bgLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                showGeofenceDialog()
            }
        } else {
            setSwitch(binding.switchLocation, false)
            showSnack(getString(R.string.permission_location_denied))
        }
    }

    private val bgLocationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showGeofenceDialog()
        else {
            setSwitch(binding.switchLocation, false)
            showSnack(getString(R.string.permission_background_location_denied))
        }
    }

    // ── Lifecycle ──

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
        syncUiFromPrefs()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshWifiState()
        healServiceIfNeeded()
        syncUiFromPrefs()
        refreshBatteryOptimizationStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Observers ──

    private fun setupObservers() {
        viewModel.serviceEnabled.observe(viewLifecycleOwner) { enabled ->
            setSwitch(binding.switchService, enabled)
            updateServiceStatusUi(enabled)
        }

        viewModel.wifiCurrentlyEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.textWifiStatus.text =
                getString(if (enabled) R.string.wifi_status_on else R.string.wifi_status_off)
        }

        viewModel.snackMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                showSnack(msg)
                viewModel.snackMessageShown()
            }
        }
    }

    // ── Listeners ──

    private fun setupListeners() {
        binding.switchService.setOnCheckedChangeListener { _, checked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            viewModel.setServiceEnabled(checked, requireContext())
        }

        binding.switchScreenOn.setOnCheckedChangeListener { _, checked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            viewModel.setTriggerScreenOn(checked)
        }

        binding.switchUnlock.setOnCheckedChangeListener { _, checked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            viewModel.setTriggerUnlock(checked)
        }

        binding.switchLocation.setOnCheckedChangeListener { _, checked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            if (checked) requestLocationPermAndSetup()
            else viewModel.setTriggerLocation(false, requireContext())
        }

        binding.buttonSetLocation.setOnClickListener {
            requestLocationPermAndSetup()
        }

        binding.switchMobileData.setOnCheckedChangeListener { _, checked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            viewModel.setTriggerMobileData(checked)
        }

        binding.switchBoot.setOnCheckedChangeListener { _, checked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            viewModel.setTriggerBoot(checked)
        }

        binding.switchAutoDisable.setOnCheckedChangeListener { _, checked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            viewModel.setAutoDisable(checked)
        }

        binding.switchDebug.setOnCheckedChangeListener { _, checked ->
            if (isUpdatingUi) return@setOnCheckedChangeListener
            viewModel.setDebugMode(checked)
        }

        binding.buttonBatteryOptimization.setOnClickListener {
            requestBatteryOptimizationExemption()
        }
    }

    // ── Reliability ──

    /**
     * The "service enabled" preference records what the user wants, not what is
     * actually running — the OS can kill the foreground service (Doze, OEM power
     * savers) without clearing it. Detect that mismatch here and self-heal.
     */
    private fun healServiceIfNeeded() {
        if (viewModel.serviceEnabled.value == true && !WifiEnablerService.isRunning) {
            WifiEnablerService.start(requireContext())
            showSnack(getString(R.string.service_restarted_after_powersave))
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = requireContext().getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
    }

    private fun refreshBatteryOptimizationStatus() {
        val exempted = isIgnoringBatteryOptimizations()
        binding.textBatteryOptimizationStatus.text = getString(
            if (exempted) R.string.battery_optimization_exempted
            else R.string.battery_optimization_not_exempted
        )
        binding.buttonBatteryOptimization.isEnabled = !exempted
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${requireContext().packageName}")
        )
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    // ── UI sync ──

    private fun syncUiFromPrefs() {
        isUpdatingUi = true
        binding.switchService.isChecked = viewModel.serviceEnabled.value ?: false
        binding.switchScreenOn.isChecked = viewModel.triggerScreenOn
        binding.switchUnlock.isChecked = viewModel.triggerUnlock
        binding.switchLocation.isChecked = viewModel.triggerLocation
        binding.switchMobileData.isChecked = viewModel.triggerMobileData
        binding.switchBoot.isChecked = viewModel.triggerBoot
        binding.switchAutoDisable.isChecked = viewModel.autoDisable
        binding.switchDebug.isChecked = viewModel.debugMode
        isUpdatingUi = false

        updateServiceStatusUi(viewModel.serviceEnabled.value ?: false)
        updateGeofenceSummary()
    }

    private fun updateServiceStatusUi(enabled: Boolean) {
        val colorRes = if (enabled) R.color.status_active else R.color.status_inactive
        val textRes = if (enabled) R.string.service_status_active else R.string.service_status_inactive
        binding.textServiceStatus.text = getString(textRes)
        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.textServiceStatus.setTextColor(color)
        binding.viewStatusIndicator.setBackgroundColor(color)
    }

    private fun updateGeofenceSummary() {
        if (viewModel.geofenceSet) {
            val r = viewModel.geofenceRadius.toInt()
            binding.textGeofenceSummary.text = getString(
                R.string.geofence_summary, viewModel.geofenceLat, viewModel.geofenceLng, r
            )
            binding.textGeofenceSummary.visibility = View.VISIBLE
        } else {
            binding.textGeofenceSummary.visibility = View.GONE
        }
    }

    /** Programmatically update a switch without triggering its listener. */
    private fun setSwitch(switch: com.google.android.material.materialswitch.MaterialSwitch, checked: Boolean) {
        isUpdatingUi = true
        switch.isChecked = checked
        isUpdatingUi = false
    }

    // ── Location & Geofence ──

    private fun requestLocationPermAndSetup() {
        val fineOk = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val bgOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else true

        when {
            fineOk && bgOk -> showGeofenceDialog()
            !fineOk -> locationPermLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            else -> bgLocationPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun showGeofenceDialog() {
        val ctx = requireContext()
        val dp16 = (16 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16 * 2, dp16, dp16 * 2, 0)
        }

        fun addEditText(hint: String, inputType: Int, default: String) = EditText(ctx).apply {
            this.hint = hint
            this.inputType = inputType
            if (default.isNotEmpty()) setText(default)
            layout.addView(this)
        }

        val latField = addEditText(
            getString(R.string.geofence_latitude),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED,
            if (viewModel.geofenceLat != 0.0) viewModel.geofenceLat.toString() else ""
        )
        val lngField = addEditText(
            getString(R.string.geofence_longitude),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED,
            if (viewModel.geofenceLng != 0.0) viewModel.geofenceLng.toString() else ""
        )
        val radiusField = addEditText(
            getString(R.string.geofence_radius_hint),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            viewModel.geofenceRadius.toInt().toString()
        )

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.geofence_dialog_title)
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val lat = latField.text.toString().toDoubleOrNull()
                val lng = lngField.text.toString().toDoubleOrNull()
                val radius = radiusField.text.toString().toFloatOrNull()

                if (lat == null || lng == null || radius == null || radius <= 0) {
                    showSnack(getString(R.string.geofence_invalid_input))
                    setSwitch(binding.switchLocation, false)
                    return@setPositiveButton
                }

                viewModel.saveGeofence(lat, lng, radius,
                    onSuccess = {
                        requireActivity().runOnUiThread {
                            setSwitch(binding.switchLocation, true)
                            updateGeofenceSummary()
                            showSnack(getString(R.string.geofence_saved))
                        }
                    },
                    onFailure = { err ->
                        requireActivity().runOnUiThread {
                            setSwitch(binding.switchLocation, false)
                            showSnack(getString(R.string.geofence_error, err))
                        }
                    }
                )
            }
            .setNeutralButton(R.string.action_clear) { _, _ ->
                viewModel.clearGeofence()
                setSwitch(binding.switchLocation, false)
                updateGeofenceSummary()
            }
            .setNegativeButton(R.string.action_cancel) { _, _ ->
                if (!viewModel.geofenceSet) setSwitch(binding.switchLocation, false)
            }
            .show()
    }

    // ── Helpers ──

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), permission) ==
                PackageManager.PERMISSION_GRANTED

    private fun showSnack(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
