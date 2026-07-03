package com.perso.antivol

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Envoie un email directement depuis le téléphone via SMTP (Gmail par défaut),
 * sans passer par un serveur intermédiaire que tu devrais héberger.
 *
 * Pré-requis : dans MainActivity, l'utilisateur configure une adresse Gmail et
 * un "mot de passe d'application" généré depuis myaccount.google.com/apppasswords
 * (jamais le vrai mot de passe du compte).
 *
 * Limite assumée : nécessite une connexion data au moment de l'envoi. Si aucune
 * donnée n'est disponible, l'échec est silencieux — la photo reste stockée en
 * local et une prochaine commande PHOTO retentera l'envoi.
 */
object EmailSender {

    private const val TAG = "AntiVol-Email"

    fun sendPhoto(context: Context, photo: File, subject: String, bodyText: String): Boolean {
        val address = Prefs.getEmailAddress(context) ?: return false
        val appPassword = Prefs.getEmailAppPassword(context) ?: return false

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        return try {
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(address, appPassword)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(address))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(address))
                setSubject(subject)
            }

            val textPart = MimeBodyPart().apply { setText(bodyText) }

            val attachmentPart = MimeBodyPart().apply {
                dataHandler = DataHandler(FileDataSource(photo))
                fileName = photo.name
            }

            val multipart = MimeMultipart().apply {
                addBodyPart(textPart)
                addBodyPart(attachmentPart)
            }

            message.setContent(multipart)
            Transport.send(message)
            Log.i(TAG, "Photo envoyée par email avec succès")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Échec envoi email (pas de data ou identifiants invalides) : ${e.message}")
            false
        }
    }
}
