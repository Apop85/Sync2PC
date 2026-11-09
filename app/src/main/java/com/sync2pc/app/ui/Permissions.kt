package com.sync2pc.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.sync2pc.app.sync.WifiHelper

/**
 * Welche Berechtigungen gerade gebraucht werden (0.11.0: erst bei Bedarf fragen).
 * [media] = Fotos-Profil, [wifi] = Profil mit WLAN-Namen, [base] = Benachrichtigungen + Akku (sobald es Profile gibt).
 */
data class PermNeeds(val media: Boolean, val wifi: Boolean, val base: Boolean) {
    companion object {
        val ALL = PermNeeds(media = true, wifi = true, base = true)
        fun of(profiles: List<com.sync2pc.app.data.Profile>) = PermNeeds(
            media = profiles.any { it.type() == com.sync2pc.app.data.ProfileType.PHOTOS },
            // Standort nur, wenn ein Profil den WLAN-Namen braucht (WIFI/BOTH mit Namen; GUID nie)
            wifi = profiles.any { com.sync2pc.app.sync.NetworkRules.needsWifiName(it.pcMode()) && it.ssids().isNotEmpty() },
            base = profiles.isNotEmpty(),
        )
    }
}

/** Fehlt etwas, das [needs] verlangt? */
fun PermStatus.missing(needs: PermNeeds): Boolean =
    (needs.media && (!media || !mediaLocation)) ||
        (needs.wifi && (!location || !backgroundLocation || !locationServiceOn)) ||
        (needs.base && (!notifications || !batteryUnrestricted))

/** Überblick über alle Berechtigungen, die die App für den Hintergrund-Sync braucht. */
data class PermStatus(
    val media: Boolean,
    val mediaLocation: Boolean,
    val location: Boolean,
    val backgroundLocation: Boolean,
    val notifications: Boolean,
    val batteryUnrestricted: Boolean,
    val locationServiceOn: Boolean,
) {
    val allOk: Boolean
        get() = media && mediaLocation && location && backgroundLocation && notifications &&
            batteryUnrestricted && locationServiceOn

    companion object {
        val mediaPermissions: Array<String>
            get() = when {
                Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                Build.VERSION.SDK_INT == 29 -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private fun granted(ctx: Context, p: String) =
            ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

        fun read(ctx: Context): PermStatus = PermStatus(
            media = mediaPermissions.all { granted(ctx, it) },
            mediaLocation = granted(ctx, Manifest.permission.ACCESS_MEDIA_LOCATION),
            location = granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION),
            backgroundLocation = granted(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            notifications = Build.VERSION.SDK_INT < 33 || granted(ctx, Manifest.permission.POST_NOTIFICATIONS),
            batteryUnrestricted = ctx.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(ctx.packageName) == true,
            locationServiceOn = WifiHelper.isLocationEnabled(ctx),
        )
    }
}
