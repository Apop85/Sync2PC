package com.sync2pc.app.data

import com.sync2pc.app.R
import com.sync2pc.app.l10n.str

/**
 * V1 (SYNC-20261110-115): Beim Speichern eines bestehenden Ordner-Profils muss der Benutzer bestätigen, wenn sich gegenüber dem
 * gespeicherten Stand (a) die Richtung ändert oder (b) «Löschungen übernehmen» eingeschaltet wird. Neues Profil: nie.
 */
object ChangeConfirm {
    fun directionChanged(base: Profile?, now: Profile): Boolean = applies(base, now) && base!!.dir() != now.dir()

    fun propagateEnabled(base: Profile?, now: Profile): Boolean = applies(base, now) && !base!!.propagateDeletes && now.propagateDeletes

    fun needed(base: Profile?, now: Profile): Boolean = directionChanged(base, now) || propagateEnabled(base, now)

    /** Dialogtext (Absätze je Auslöser) oder null, wenn keine Bestätigung nötig ist */
    fun text(base: Profile?, now: Profile, trashName: String): String? {
        val parts = mutableListOf<String>()
        if (directionChanged(base, now)) parts += str(
            when (now.dir()) {
                SyncDirection.UPLOAD -> R.string.profile_confirm_dir_up
                SyncDirection.DOWNLOAD -> R.string.profile_confirm_dir_down
                SyncDirection.BOTH -> R.string.profile_confirm_dir_both
            },
            base!!.dir().label, now.dir().label, trashName,
        )
        if (propagateEnabled(base, now)) parts += str(R.string.profile_confirm_propagate, trashName)
        return parts.joinToString("\n\n").ifEmpty { null }
    }

    private fun applies(base: Profile?, now: Profile): Boolean =
        base != null && now.id != 0L && base.id == now.id && now.type() == ProfileType.MIRROR
}
