package com.perso.antivol

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.perso.antivol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val refused = results.filterValues { !it }.keys
        if (refused.isEmpty()) {
            toast("Toutes les permissions accordées.")
        } else {
            toast("Permissions refusées : ${refused.joinToString()}. L'app ne pourra pas fonctionner correctement sans elles.")
        }
    }

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            try {
                dpm.setCameraDisabled(admin, false)
            } catch (e: Exception) {
            }
            toast("Administrateur activé.")
        } else {
            toast("Administrateur non activé.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chargerConfigExistante()

        binding.btnSave.setOnClickListener { sauvegarderConfig() }
        binding.btnPermissions.setOnClickListener { demanderPermissions() }
        binding.btnDeviceAdmin.setOnClickListener { activerDeviceAdmin() }
        binding.btnBatteryOpt.setOnClickListener { desactiverOptimisationBatterie() }
    }

    private fun chargerConfigExistante() {
        binding.editOwnerNumber.setText(Prefs.getOwnerNumber(this))
        binding.editPin.setText(Prefs.getPin(this))
        binding.editEmail.setText(Prefs.getEmailAddress(this))
    }

    private fun sauvegarderConfig() {
        val number = binding.editOwnerNumber.text.toString().trim()
        val pin = binding.editPin.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val appPassword = binding.editAppPassword.text.toString().trim()

        if (number.isBlank() || pin.isBlank()) {
            toast("Le numéro et le PIN sont obligatoires.")
            return
        }
        if (pin.length < 4) {
            toast("Choisis un PIN d'au moins 4 chiffres — idéalement 6 ou plus.")
            return
        }

        Prefs.setOwnerNumber(this, number)
        Prefs.setPin(this, pin)
        if (email.isNotBlank() && appPassword.isNotBlank()) {
            Prefs.setEmailCredentials(this, email, appPassword)
        }

        binding.textStatus.text = "Configuration enregistrée. N'oublie pas les 3 étapes ci-dessous."
        toast("Configuration enregistrée.")
    }

    private fun demanderPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun activerDeviceAdmin() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (dpm.isAdminActive(admin)) {
            toast("Déjà activé.")
            return
        }

        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.device_admin_explanation)
            )
        }
        deviceAdminLauncher.launch(intent)
    }

    private fun desactiverOptimisationBatterie() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            toast("Déjà exclu de l'optimisation de batterie.")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
