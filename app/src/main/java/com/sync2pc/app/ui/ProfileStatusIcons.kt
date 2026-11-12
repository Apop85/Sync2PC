package com.sync2pc.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sync2pc.app.R

/** Statussymbole oben rechts auf der Profilkachel (SYNC-20261112-285) */
enum class ProfileIcon(@DrawableRes val drawable: Int, @StringRes val description: Int) {
    WIFI(R.drawable.ic_wifi, R.string.icon_on_wifi),
    PC(R.drawable.ic_pc_known, R.string.home_profile_pc_known),
    ENCRYPTED(R.drawable.ic_encrypted, R.string.icon_encrypted),
}

/** Welche Symbole die Kopfzeile der Profilkachel zeigt (rein, getestet in ProfileStatusIconsTest) */
object ProfileStatusIcons {
    /**
     * Nur die zutreffenden Symbole, immer in der Reihenfolge WLAN, PC, Verschlüsselt (von links nach rechts); nicht
     * zutreffende entfallen ohne Lücke, die Gruppe steht am rechten Rand. [onWifi]: das aktuelle WLAN gehört zum Profil;
     * [pcKnown]: die PC-Kennung ist erkannt (PC-Erkennung per Kennung und gespeicherte Kennung bekannt); [encrypted]: die letzte
     * Anmeldung war verschlüsselt.
     */
    fun visible(onWifi: Boolean, pcKnown: Boolean, encrypted: Boolean): List<ProfileIcon> = buildList {
        if (onWifi) add(ProfileIcon.WIFI)
        if (pcKnown) add(ProfileIcon.PC)
        if (encrypted) add(ProfileIcon.ENCRYPTED)
    }
}
