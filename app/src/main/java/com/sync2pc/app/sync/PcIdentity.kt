package com.sync2pc.app.sync

import com.sync2pc.app.R
import com.sync2pc.app.l10n.Msg
import com.sync2pc.app.l10n.str

/**
 * PC-Erkennung per SMB-Server-Kennung (SYNC-20261109-047, Vorgabe SYNC-20261109-048).
 * Reine Regel ohne Android/SMB: entscheidet, ob Zugangsdaten an den Server gesendet werden dürfen.
 * Die Kennung ist nicht geheim; sie schützt vor einem fremden Server in einem fremden Netz, nicht vor
 * einem gezielten Angreifer, der sie vorher im eigenen Netz gelesen hat.
 */

/** Erkennungsart je Profil (Profil.pcCheck): nur WLAN-Name, nur Kennung, oder beides gemeinsam erfüllt */
enum class PcCheckMode {
    WIFI, GUID, BOTH;

    /** Kennung wird vor dem Anmelden geprüft */
    val checksGuid: Boolean get() = this != WIFI

    companion object {
        /** Unbekannte oder leere Werte (alte Sicherung, Fehler) ergeben WIFI = heutiges Verhalten */
        fun parse(s: String?): PcCheckMode = values().firstOrNull { it.name == s?.trim()?.uppercase() } ?: WIFI
    }
}

/**
 * Zugriffsart (SYNC-20261109-055/-058, V1): SETUP = Profil-Editor (Verbindung testen, Freigaben/Ordner wählen, Lernen nach dem
 * Speichern; nur hier darf eine unbekannte Kennung angemeldet und gelernt werden); USER = vom Benutzer ausgelöst (manueller Sync,
 * Ansichten); AUTO = Hintergrund. USER und AUTO lernen nie: bei GUID/BOTH ohne bekannte Kennung wird nicht angemeldet.
 */
enum class PcAccess { USER, AUTO, SETUP }

enum class PcDecision {
    /** Kennung stimmt (oder wird bei WIFI nicht verlangt): anmelden */
    ALLOW,

    /** Anmelden; nach erfolgreicher Anmeldung die Kennung lernen (nur wenn noch keine gespeichert ist) */
    ALLOW_AND_LEARN,

    /** GUID/BOTH, AUTO, noch keine Kennung für diesen Host: nicht anmelden */
    UNKNOWN,

    /** Gespeicherte Kennung weicht ab: nicht anmelden (Neuvertrauen nur durch den Benutzer) */
    CHANGED,

    /** GUID/BOTH, der Server liefert keine brauchbare Kennung (leer/Null-UUID): nie anmelden */
    NO_ID,

    /** BOTH, SETUP, noch keine Kennung: das aktuelle WLAN steht nicht in der Liste des Profils (oder ist unlesbar): nicht anmelden */
    WRONG_NET;

    /** Dürfen Zugangsdaten gesendet werden? */
    val mayAuthenticate: Boolean get() = this == ALLOW || this == ALLOW_AND_LEARN
}

object PcIdentity {

    /** Kennung als kleingeschriebener String; null bei leer/ungültig/Null-UUID (nur Nullen) */
    fun normalize(guid: String?): String? {
        val s = guid?.trim()?.trim('{', '}')?.lowercase().orEmpty()
        if (s.isEmpty()) return null
        val hex = s.filter { it != '-' }
        if (hex.isEmpty() || hex.all { it == '0' }) return null
        return s
    }

    fun normalize(guid: java.util.UUID?): String? = normalize(guid?.toString())

    /** Host für Vergleich und Speicherung (kleingeschrieben) */
    fun normalizeHost(host: String): String = host.trim().lowercase()

    /** Gespeicherte Kennung, die für [host] gilt; null wenn leer/ungültig oder zu einem anderen Host gelernt */
    fun storedFor(pcGuid: String, pcGuidHost: String, host: String): String? {
        if (pcGuidHost.isBlank() || normalizeHost(pcGuidHost) != normalizeHost(host)) return null
        return normalize(pcGuid)
    }

    /**
     * Entscheidung vor dem Senden der Zugangsdaten.
     * - WIFI: nie blockieren; still lernen, wenn für [host] noch keine Kennung gespeichert ist und der Server eine gültige liefert.
     * - GUID/BOTH: ohne gültige [seenGuid] nie anmelden; ohne gespeicherte Kennung für [host]: nur bei [PcAccess.SETUP] und
     *   [learnNetOk] anmelden (und lernen), bei SETUP ohne [learnNetOk] [PcDecision.WRONG_NET], sonst UNKNOWN;
     *   gespeicherte Kennung muss gleich sein.
     * [learnNetOk]: Netzbedingung fürs erste Kennenlernen, vom Aufrufer ermittelt ([NetworkRules.learnNetOk]).
     */
    fun decide(
        mode: PcCheckMode, pcGuid: String, pcGuidHost: String, host: String,
        seenGuid: String?, access: PcAccess, learnNetOk: Boolean = true,
    ): PcDecision {
        val seen = normalize(seenGuid)
        val stored = storedFor(pcGuid, pcGuidHost, host)
        if (mode == PcCheckMode.WIFI) {
            return if (seen != null && stored == null) PcDecision.ALLOW_AND_LEARN else PcDecision.ALLOW
        }
        if (seen == null) return PcDecision.NO_ID
        if (stored == null) return when {
            access != PcAccess.SETUP -> PcDecision.UNKNOWN
            learnNetOk -> PcDecision.ALLOW_AND_LEARN
            else -> PcDecision.WRONG_NET
        }
        return if (stored == seen) PcDecision.ALLOW else PcDecision.CHANGED
    }

    /**
     * Grundsatz (V7): eine bei WIFI still gelernte Kennung erzeugt nie Vertrauen. Gespeicherte Kennung ist leer zu werten,
     * wenn der Modus in der Datenbank WIFI ist, der wirksame Modus aber GUID/BOTH.
     */
    fun trustedStored(dbMode: PcCheckMode, effectiveMode: PcCheckMode, pcGuid: String, pcGuidHost: String): Pair<String, String> =
        if (dbMode == PcCheckMode.WIFI && effectiveMode.checksGuid) "" to "" else pcGuid to pcGuidHost

    /** Wirksamer Modus: der strengere von Datenbank und Kopie (eine veraltete WIFI-Kopie weicht nie auf) */
    fun effectiveMode(dbMode: PcCheckMode, copyMode: PcCheckMode): PcCheckMode =
        if (dbMode != PcCheckMode.WIFI) dbMode else copyMode
}

/** Reine Entscheidungen des Profil-Editors (als JVM-Test prüfbar, SYNC-20261109-055 T3, V5–V7) */
object PcEditorRules {

    /**
     * Beim Speichern: darf die in der Datenbank gelernte Kennung bleiben? Nur bei gleichem Host und nie, wenn der Modus in der
     * Datenbank WIFI war und neu GUID/BOTH ist (V7: WIFI-Kennung erzeugt nie Vertrauen).
     */
    fun keepStoredOnSave(dbMode: PcCheckMode?, newMode: PcCheckMode, dbHost: String?, newHost: String): Boolean {
        if (dbMode == null || newHost.isBlank() || dbHost.isNullOrBlank()) return false
        if (PcIdentity.normalizeHost(dbHost) != PcIdentity.normalizeHost(newHost)) return false
        return !(dbMode == PcCheckMode.WIFI && newMode.checksGuid)
    }

    /**
     * Hostwechsel im Editor eines bereits gespeicherten Profils (nur GUID/BOTH): vor jedem Senden von Zugangsdaten an den neuen,
     * unbekannten Host (Speichern mit Lernen, «Verbindung testen», Ordner wählen) bestätigen lassen (SYNC-20261109-079).
     * Ersteinrichtung (neues Profil), WIFI, gleicher Host und ein in dieser Sitzung schon bestätigter Host fragen nicht.
     */
    fun needsHostConfirm(existing: Boolean, mode: PcCheckMode, savedHost: String?, newHost: String, confirmedHost: String?): Boolean {
        if (!existing || !mode.checksGuid || newHost.isBlank() || savedHost.isNullOrBlank()) return false
        val n = PcIdentity.normalizeHost(newHost)
        return n != PcIdentity.normalizeHost(savedHost) && n != confirmedHost?.let { PcIdentity.normalizeHost(it) }
    }

    /** Moduswechsel im Editor: still gelernte Kennung löschen (WIFI → GUID/BOTH); verringert nur Vertrauen (V6) */
    fun clearOnModeChange(old: PcCheckMode, new: PcCheckMode): Boolean = new.checksGuid && !old.checksGuid

    /**
     * Nach dem Speichern die Kennung lernen (V5)? Nur bei GUID/BOTH, gesetztem Host und Benutzer, ohne gültige Kennung
     * für diesen Host.
     */
    fun learnAfterSave(mode: PcCheckMode, host: String, hasUser: Boolean, pcGuid: String, pcGuidHost: String): Boolean =
        mode.checksGuid && host.isNotBlank() && hasUser && PcIdentity.storedFor(pcGuid, pcGuidHost, host) == null
}

/**
 * Zugangsdaten wurden NICHT gesendet, weil der PC nicht sicher erkannt ist. Bewusst keine [java.io.IOException]
 * und kein Netzwerkfehler ([isNetworkError] wertet sie ausdrücklich als false): Wiederholen ändert nichts.
 */
class PcIdentityException(val decision: PcDecision, val host: String) : Exception() {

    @get:androidx.annotation.StringRes
    val messageId: Int
        get() = when (decision) {
            PcDecision.CHANGED -> R.string.smb_err_pc_changed
            PcDecision.NO_ID -> R.string.smb_err_pc_no_id
            PcDecision.WRONG_NET -> R.string.smb_err_pc_wrong_net
            else -> R.string.smb_err_pc_unknown
        }

    override val message: String get() = str(messageId, host)

    /** Kennung für Verlauf/Anzeige (sprachunabhängig gespeichert) */
    fun toMsg(): Msg = Msg.of(messageId, host)
}
