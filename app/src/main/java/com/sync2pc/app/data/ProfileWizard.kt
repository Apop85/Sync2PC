package com.sync2pc.app.data

import com.sync2pc.app.smb.SmbTarget

/**
 * Schritte des Profil-Assistenten (0.5.0). Dieselbe Reihenfolge gilt für die Karten beim Bearbeiten eines Profils.
 * Rein (ohne Android), getestet in ProfileWizardTest.
 */
object ProfileWizard {
    enum class Step(val title: String, val optional: Boolean) {
        NAME("Profilname", false),
        SOURCE("Quelle", false),
        TARGET("Ziel", false),
        OPTIONS("Sync-Optionen", false),
        FILTER("Filter", true),
        /** Nur Ordner-Profile */
        GUARD("Schutzbremse", true),
    }

    fun steps(p: Profile): List<Step> =
        if (p.type() == ProfileType.MIRROR) Step.entries.toList() else Step.entries.filter { it != Step.GUARD }

    /** Ab dem ersten optionalen Schritt kann direkt gespeichert werden */
    fun canSaveAt(p: Profile, step: Step): Boolean =
        step.optional || steps(p).all { missing(p, it) == null } && steps(p).indexOf(step) == steps(p).lastIndex

    /** Was fehlt in diesem Schritt noch? null = vollständig, weiter möglich */
    fun missing(p: Profile, step: Step): String? = when (step) {
        Step.NAME -> if (p.name.isBlank()) "Bitte gib dem Profil einen Namen." else null
        Step.SOURCE -> when {
            p.type() == ProfileType.MIRROR && p.localTreeUri.isBlank() -> "Bitte wähle den Ordner auf dem Handy."
            p.type() == ProfileType.PHOTOS && p.folders().isEmpty() -> "Bitte wähle mindestens einen Ordner auf dem Telefon."
            p.type() == ProfileType.PHOTOS && !p.includeImages && !p.includeVideos -> "Bitte schalte Fotos, Videos oder beides ein."
            else -> null
        }
        Step.TARGET -> if (SmbTarget.parse(p.smbPath) == null) "Bitte gib den Freigabe-Pfad ein, z. B. \\\\192.168.1.20\\Fotos." else null
        Step.OPTIONS, Step.FILTER, Step.GUARD -> null
    }

    /** Erster Schritt, in dem noch etwas fehlt (null = alles vollständig) */
    fun firstIncomplete(p: Profile): Step? = steps(p).firstOrNull { missing(p, it) != null }
}
