package com.sync2pc.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.sync2pc.app.sync.WifiHelper

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
