package com.perso.antivol

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommandService : Service() {

    companion object {
        const val EXTRA_COMMAND = "commande"
        const val EXTRA_ARG = "argument"
        private const val CHANNEL_ID = "antivol_service"
        private const val NOTIF_ID = 1
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        val commande = intent?.getStringExtra(EXTRA_COMMAND) ?: "VEILLE"
        val argument = intent?.getStringExtra(EXTRA_ARG)

        scope.launch {
            try {
                when (commande) {
                    "LOCALISER" -> handleLocaliser()
                    "VERROUILLER" -> handleVerrouiller()
                    "ALARME" -> handleAlarme()
                    "PHOTO" -> handlePhoto()
                    "BATTERIE" -> handleBatterie()
                    "EFFACER" -> if (argument == "CONFIRMER") handleEffacer()
                    "VEILLE" -> { /* service juste maintenu actif après boot */ }
                }
            } catch (e: Throwable) {
                Log.e("AntiVol-CommandService", "Erreur pendant l'exécution de $commande", e)
                sendReply("Erreur lors de l'exécution de $commande : ${e.message ?: e.javaClass.simpleName}")
            }
            kotlinx.coroutines.delay(3000)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun sendReply(text: String) {
        val owner = Prefs.getOwnerNumber(applicationContext) ?: return
        try {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(owner, null, parts, null, null)
        } catch (e: Exception) {
        }
    }

    private suspend fun handleLocaliser() {
        val location = LocationHelper.getCurrentLocation(applicationContext)
        if (location != null) {
            sendReply("Position : ${LocationHelper.toMapsLink(location)} (précision ${location.accuracy.toInt()}m)")
        } else {
            sendReply("Impossible d'obtenir la position (GPS désactivé ou permission manquante).")
        }
    }

    private fun handleVerrouiller() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
            sendReply("Téléphone verrouillé.")
        } else {
            sendReply("Échec verrouillage : administrateur de l'appareil non activé dans l'app.")
        }
    }

    private fun handleAlarme() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val player = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_ALARM)
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            android.os.Handler(mainLooper).postDelayed({
                if (player.isPlaying) player.stop()
                player.release()
            }, 60_000)

            sendReply("Alarme déclenchée pendant 60 secondes.")
        } catch (e: Exception) {
            sendReply("Échec du déclenchement de l'alarme.")
        }
    }

    private suspend fun handlePhoto() {
        val photo = kotlinx.coroutines.withTimeoutOrNull(15000) {
            CameraHelper.takePhoto(applicationContext, useFrontCamera = true)
        }
        if (photo == null) {
            sendReply("Échec de la prise de photo (permission, caméra indisponible, ou délai dépassé).")
            return
        }
        val envoye = EmailSender.sendPhoto(
            applicationContext,
            photo,
            "Photo de sécurité - téléphone",
            "Photo prise automatiquement suite à ta commande PHOTO."
        )
        sendReply(if (envoye) "Photo prise et envoyée par email." else "Photo prise mais pas de connexion data pour l'envoyer — réessaie plus tard.")
    }

    private fun handleBatterie() {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val niveau = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        sendReply("Batterie : $niveau%")
    }

    private fun handleEffacer() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            sendReply("Effacement des données en cours...")
            dpm.wipeData(0)
        } else {
            sendReply("Échec effacement : administrateur de l'appareil non activé.")
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Service de sécurité",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sécurité Perso active")
            .setContentText("Protection anti-vol en fonctionnement")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }
}
