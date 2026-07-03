package com.perso.antivol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * Format des commandes SMS attendu :
 *   AV:<PIN>:<COMMANDE>[:<ARGUMENT>]
 *
 * Exemples :
 *   AV:4471:LOCALISER
 *   AV:4471:VERROUILLER
 *   AV:4471:ALARME
 *   AV:4471:PHOTO
 *   AV:4471:BATTERIE
 *   AV:4471:EFFACER:CONFIRMER      (double confirmation, action destructrice)
 *
 * Sécurité : la commande n'est traitée que si le PIN correspond exactement à
 * celui configuré dans l'app. Choisis un PIN long (pas "1234") — c'est ta
 * seule protection contre une commande envoyée par quelqu'un d'autre que toi.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AntiVol-SmsReceiver"
        private const val PREFIX = "AV:"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs.isConfigured(context)) return // app pas encore configurée

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val sender = messages.firstOrNull()?.originatingAddress ?: ""

        if (!fullBody.startsWith(PREFIX)) return

        val parts = fullBody.trim().split(":")
        // parts[0] = "AV", parts[1] = PIN, parts[2] = commande, parts[3] (optionnel) = argument
        if (parts.size < 3) return

        val pinRecu = parts[1]
        val pinAttendu = Prefs.getPin(context)
        if (pinRecu != pinAttendu) {
            Log.w(TAG, "PIN invalide reçu depuis $sender — commande ignorée")
            return
        }

        val commande = parts[2].uppercase()
        val argument = parts.getOrNull(3)?.uppercase()

        Log.i(TAG, "Commande valide reçue : $commande")

        // On masque le SMS de commande pour ne pas polluer la boîte de réception
        // (et éviter qu'un voleur ne le voie passer)
        try {
            abortBroadcast()
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'intercepter le SMS (non prioritaire) : ${e.message}")
        }

        val serviceIntent = Intent(context, CommandService::class.java).apply {
            putExtra(CommandService.EXTRA_COMMAND, commande)
            putExtra(CommandService.EXTRA_ARG, argument)
        }
        context.startForegroundService(serviceIntent)
    }
}
