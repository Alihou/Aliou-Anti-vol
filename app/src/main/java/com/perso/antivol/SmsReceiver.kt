package com.perso.antivol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AntiVol-SmsReceiver"
        private const val PREFIX = "AV:"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs.isConfigured(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val sender = messages.firstOrNull()?.originatingAddress ?: ""

        if (!fullBody.startsWith(PREFIX)) return

        val parts = fullBody.trim().split(":")
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

        try {
            abortBroadcast()
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'intercepter le SMS (non prioritaire) : ${e.message}")
        }
        tenterSuppressionSms(context, sender, fullBody)

        val serviceIntent = Intent(context, CommandService::class.java).apply {
            putExtra(CommandService.EXTRA_COMMAND, commande)
            putExtra(CommandService.EXTRA_ARG, argument)
        }
        context.startForegroundService(serviceIntent)
    }

    private fun tenterSuppressionSms(context: Context, sender: String, body: String) {
        Thread {
            try {
                Thread.sleep(1500)
                val uri = android.net.Uri.parse("content://sms/inbox")
                val cursor = context.contentResolver.query(
                    uri, arrayOf("_id", "address", "body"), null, null, "date DESC LIMIT 5"
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val msgBody = it.getString(it.getColumnIndexOrThrow("body"))
                        if (msgBody == body) {
                            val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                            context.contentResolver.delete(
                                android.net.Uri.parse("content://sms/$id"), null, null
                            )
                            Log.i(TAG, "SMS de commande supprimé de la boîte de réception")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Suppression du SMS impossible sur cet appareil : ${e.message}")
            }
        }.start()
    }
}
