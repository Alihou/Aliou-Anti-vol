package com.perso.antivol

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Tant que cette app est active en tant qu'administrateur de l'appareil,
 * Android empêche une désinstallation directe : il faut d'abord désactiver
 * l'admin (Réglages > Sécurité > Administrateurs de l'appareil), ce qui
 * donne le temps de réagir si quelqu'un essaie de retirer l'app.
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Protection anti-désinstallation activée", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Si tu désactives ceci, l'app ne pourra plus verrouiller ni localiser le téléphone en cas de vol."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Protection anti-vol désactivée", Toast.LENGTH_LONG).show()
    }
}
