package com.perso.antivol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.isConfigured(context)) return

        // Vérifie si la carte SIM a changé pendant que le téléphone était éteint
        SimChangeReceiver.checkAndAlert(context)

        // Redémarre le service de fond (nécessaire pour caméra/localisation en tâche de fond)
        val serviceIntent = Intent(context, CommandService::class.java).apply {
            putExtra(CommandService.EXTRA_COMMAND, "VEILLE")
        }
        context.startForegroundService(serviceIntent)
    }
}
