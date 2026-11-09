package com.sync2pc.app.data

import androidx.annotation.StringRes
import com.sync2pc.app.R
import com.sync2pc.app.l10n.str
import com.sync2pc.app.smb.SmbTarget

/**
 * Schritte des Profil-Assistenten (0.5.0). Dieselbe Reihenfolge gilt für die Karten beim Bearbeiten eines Profils.
 * Rein (ohne Android), getestet in ProfileWizardTest.
 */
object ProfileWizard {
    enum class Step(@StringRes private val titleRes: Int, val optional: Boolean) {
        NAME(R.string.wizard_step_name, false),
        SOURCE(R.string.wizard_step_source, false),
        TARGET(R.string.wizard_step_target, false),
        OPTIONS(R.string.wizard_step_options, false),
        FILTER(R.string.wizard_step_filter, true),
        /** Nur Ordner-Profile */
        GUARD(R.string.wizard_step_guard, true);

        val title: String get() = str(titleRes)
    }

    fun steps(p: Profile): List<Step> =
        if (p.type() == ProfileType.MIRROR) Step.entries.toList() else Step.entries.filter { it != Step.GUARD }

    /** Ab dem ersten optionalen Schritt kann direkt gespeichert werden */
    fun canSaveAt(p: Profile, step: Step): Boolean =
        step.optional || steps(p).all { missing(p, it) == null } && steps(p).indexOf(step) == steps(p).lastIndex

    /** Was fehlt in diesem Schritt noch? null = vollständig, weiter möglich */
    fun missing(p: Profile, step: Step): String? = when (step) {
        Step.NAME -> if (p.name.isBlank()) str(R.string.wizard_missing_name) else null
        Step.SOURCE -> when {
            p.type() == ProfileType.MIRROR && p.localTreeUri.isBlank() -> str(R.string.wizard_missing_local)
            p.type() == ProfileType.PHOTOS && p.folders().isEmpty() -> str(R.string.wizard_missing_sources)
            p.type() == ProfileType.PHOTOS && !p.includeImages && !p.includeVideos -> str(R.string.wizard_missing_media)
            else -> null
        }
        Step.TARGET -> if (SmbTarget.parse(p.smbPath) == null) str(R.string.wizard_missing_target) else null
        Step.OPTIONS, Step.FILTER, Step.GUARD -> null
    }

    /**
     * Ordnerstruktur wählbar? Nur Fotos, solange noch nichts übertragen wurde ([locked] = `SyncDao.layoutLocked`)
     * und kein Lauf aktiv ist ([syncRunning]; vorsichtig: ein Lauf bearbeitet alle Profile nacheinander).
     */
    fun layoutEditable(p: Profile, locked: Boolean, syncRunning: Boolean): Boolean =
        p.type() == ProfileType.PHOTOS && !locked && !syncRunning

    /**
     * Anderes Fotoprofil mit gleichem Ziel (Vergleich wie [ProfileOverlap.target]): dessen Vorlage gilt auch hier
     * (Sperrregel, SYNC-20261109-083). null = kein solches Profil.
     */
    fun layoutOwner(p: Profile, others: List<Profile>): Profile? {
        if (p.type() != ProfileType.PHOTOS) return null
        val t = ProfileOverlap.target(p)
        if (t.isEmpty()) return null
        return others.firstOrNull { it.id != p.id && it.type() == ProfileType.PHOTOS && ProfileOverlap.target(it) == t }
    }

    /**
     * Wirksame Ordnerstruktur (SYNC-20261109-089). [own] = zuletzt vom Benutzer selbst gewählte Vorlage (oder beim
     * Bearbeiten der gespeicherte Wert; null = keine). Gleiches Ziel wie ein anderes Fotoprofil ([ownerLayout] != null)
     * -> dessen Vorlage, ohne die eigene Wahl zu überschreiben; entfällt der Eigentümer, gilt die eigene Wahl wieder
     * (ohne eigene Wahl V1). Nicht änderbar ([editable] = false) -> unverändert [current].
     */
    fun effectiveLayout(own: FolderLayout?, ownerLayout: FolderLayout?, editable: Boolean, current: FolderLayout): FolderLayout =
        if (!editable) current else ownerLayout ?: own ?: FolderLayout.MONTH_SUFFIX

    /** Erster Schritt, in dem noch etwas fehlt (null = alles vollständig) */
    fun firstIncomplete(p: Profile): Step? = steps(p).firstOrNull { missing(p, it) != null }
}
