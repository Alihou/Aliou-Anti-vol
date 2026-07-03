package com.perso.antivol

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import android.telephony.SmsManager

class SimChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        checkAndAlert(context)
    }

    companion object {
        /**
         * Compare le numéro de série de la SIM actuelle à celui enregistré la dernière
         * fois. Si elle a changé (typiquement : un voleur a remplacé la puce), un SMS
         * d'alerte est envoyé au numéro du propriétaire — via le réseau SMS de base,
         * qui fonctionne même sans forfait data actif sur la nouvelle SIM.
         */
        fun checkAndAlert(context: Context) {
            if (!Prefs.isConfigured(context)) return

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
            ) return

            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val currentSerial = try {
                tm.simSerialNumber ?: tm.subscriberId ?: "inconnu"
            } catch (e: SecurityException) {
                "inconnu"
            }

            val lastSerial = Prefs.getLastKnownSimSerial(context)

            if (lastSerial != null && lastSerial != currentSerial) {
                val owner = Prefs.getOwnerNumber(context) ?: return
                val message = "ALERTE : la carte SIM de ton téléphone a été changée. " +
                        "Nouveau numéro détecté : ${tm.line1Number ?: "inconnu"}."
                try {
                    @Suppress("DEPRECATION")
                    val smsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(owner, null, message, null, null)
                } catch (e: Exception) {
                    // Pas de réseau dispo pour l'instant, on retentera au prochain event
                }
            }

            Prefs.setLastKnownSimSerial(context, currentSerial)
        }
    }
}
