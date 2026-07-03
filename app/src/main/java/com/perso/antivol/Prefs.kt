package com.perso.antivol

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Toutes les données sensibles (PIN secret, numéro du propriétaire, identifiants
 * d'envoi d'email) sont stockées dans un SharedPreferences chiffré au niveau
 * matériel (Android Keystore), jamais en clair.
 */
object Prefs {

    private const val FILE_NAME = "antivol_secure_prefs"

    private fun get(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setOwnerNumber(context: Context, number: String) =
        get(context).edit().putString("owner_number", number).apply()

    fun getOwnerNumber(context: Context): String? =
        get(context).getString("owner_number", null)

    fun setPin(context: Context, pin: String) =
        get(context).edit().putString("pin", pin).apply()

    fun getPin(context: Context): String? =
        get(context).getString("pin", null)

    fun setEmailCredentials(context: Context, address: String, appPassword: String) {
        get(context).edit()
            .putString("email_address", address)
            .putString("email_app_password", appPassword)
            .apply()
    }

    fun getEmailAddress(context: Context): String? =
        get(context).getString("email_address", null)

    fun getEmailAppPassword(context: Context): String? =
        get(context).getString("email_app_password", null)

    fun setLastKnownSimSerial(context: Context, serial: String) =
        get(context).edit().putString("last_sim_serial", serial).apply()

    fun getLastKnownSimSerial(context: Context): String? =
        get(context).getString("last_sim_serial", null)

    fun isConfigured(context: Context): Boolean =
        !getOwnerNumber(context).isNullOrBlank() && !getPin(context).isNullOrBlank()
}
