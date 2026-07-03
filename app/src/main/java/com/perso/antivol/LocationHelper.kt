package com.perso.antivol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    /**
     * Utilise le LocationManager natif d'Android (pas de dépendance Play Services,
     * plus fiable sur les appareils sans services Google ou avec data coupée).
     * Essaie GPS puis réseau, avec un délai maximum de 20 secondes.
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 1) essaie d'abord la dernière position connue (rapide, gratuit)
        val lastKnown = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                try {
                    if (lm.isProviderEnabled(provider)) lm.getLastKnownLocation(provider) else null
                } catch (e: SecurityException) { null }
            }
            .maxByOrNull { it.time }

        // Si elle a moins de 5 minutes, elle est suffisante
        if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < 5 * 60 * 1000) {
            return lastKnown
        }

        // 2) sinon demande une position fraîche avec timeout
        return suspendCancellableCoroutine { cont ->
            val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            try {
                lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            } catch (e: SecurityException) {
                cont.resume(lastKnown)
                return@suspendCancellableCoroutine
            }

            cont.invokeOnCancellation { lm.removeUpdates(listener) }

            // Timeout de secours à 20s : on renvoie la dernière position connue (même vieille) plutôt que rien
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                lm.removeUpdates(listener)
                if (cont.isActive) cont.resume(lastKnown)
            }, 20_000)
        }
    }

    fun toMapsLink(location: Location): String =
        "https://maps.google.com/?q=${location.latitude},${location.longitude}"
}
