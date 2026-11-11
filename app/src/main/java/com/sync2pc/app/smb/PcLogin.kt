package com.sync2pc.app.smb

import com.sync2pc.app.data.Profile
import com.sync2pc.app.data.SmbEncryption
import com.sync2pc.app.sync.PcAccess
import com.sync2pc.app.sync.PcCheckMode
import com.sync2pc.app.sync.PcDecision
import com.sync2pc.app.sync.PcIdentity
import com.sync2pc.app.sync.PcIdentityException

/**
 * Anmeldelogik der PC-Erkennung (SYNC-20261109-047/-055/-058), getrennt von smbj, damit sie ohne Gerät testbar ist (T1):
 * connect -> Kennung lesen -> entscheiden -> erst dann anmelden. [SmbSession] liefert die smbj-Verbindung dahinter.
 * Es gibt keinen zweiten Weg zum Anmelden.
 */

/** Eine ausgehandelte, noch nicht angemeldete Verbindung */
interface PcLink<S> {
    /** Server-Kennung aus der Aushandlung (vor der Anmeldung), null wenn nicht lesbar */
    fun serverGuid(): String?

    /**
     * Ist für die ausgehandelte Verbindung SMB3-Verschlüsselung verfügbar (Dialekt SMB 3.x und ein Verschlüsselungsverfahren
     * ausgehandelt)? Vor der Anmeldung abfragbar; bei Zweifel false (sicherer Ausfall, SYNC-20261111-144).
     */
    fun encryptionAvailable(): Boolean

    /** Anmelden (sendet die Zugangsdaten) */
    fun authenticate(): S

    /** Verschlüsselt diese angemeldete Sitzung tatsächlich (Schlüssel vorhanden, Pakete werden verschlüsselt)? Bei Zweifel false. */
    fun sessionEncrypted(session: S): Boolean

    fun close()
}

/**
 * SMB3-Verschlüsselung ist für das Profil «Erforderlich», aber nicht verfügbar (SYNC-20261111-144). Bewusst keine
 * [java.io.IOException] und kein Netzwerkfehler ([com.sync2pc.app.sync.isNetworkError] wertet sie als false): Wiederholen
 * ändert nichts, nur der Benutzer (Einstellung ändern oder Verschlüsselung am PC aktivieren). Wird wie eine abgewiesene
 * Aushandlung behandelt (eine Meldung, kein Timer, keine Dauerbenachrichtigung).
 * [afterLogin] false: die Zugangsdaten wurden NICHT gesendet (Prüfung vor der Anmeldung bzw. Aushandlung ohne SMB 3).
 * [afterLogin] true: Anmeldung erfolgte, die Sitzung hat aber keinen Verschlüsselungsschlüssel; sie wurde geschlossen.
 */
/** «Zugriff verweigert» beim Verbinden mit der Freigabe bei Verschlüsselung «Aus»: kann ein Rechte- oder ein Verschlüsselungsproblem sein (SYNC-20261111-232) */
class SmbShareAccessDeniedException(cause: Throwable) : Exception(cause)

class SmbEncryptionUnavailableException(
    val host: String, val afterLogin: Boolean = false, cause: Throwable? = null,
) : Exception(cause) {

    @get:androidx.annotation.StringRes
    val messageId: Int
        get() = if (afterLogin) com.sync2pc.app.R.string.smb_err_encryption_not_established
        else com.sync2pc.app.R.string.smb_err_encryption_required

    override val message: String get() = com.sync2pc.app.l10n.str(messageId, host)

    /** Kennung für Verlauf/Anzeige (sprachunabhängig gespeichert) */
    fun toMsg(): com.sync2pc.app.l10n.Msg = com.sync2pc.app.l10n.Msg.of(messageId, host)
}

/** Ergebnis der Aushandlung, das je Host (und Verschlüsselungsmodus) einmal je Lauf gemerkt wird */
internal class PcNeg(val guid: String?, val encryption: Boolean)

interface PcTransport<S> {
    /** TCP und SMB-Aushandlung, keine Anmeldung */
    fun connect(host: String): PcLink<S>
}

/** Von diesem Lauf bereits gesehene Kennungen je Host (nur im Speicher, F5): Aushandeln je Host einmal, Entscheidung je Profil */
class PcSeen {
    // Schlüssel enthält den Verschlüsselungsmodus: die Verfügbarkeit hängt von der Verbindungskonfiguration ab
    // (z. B. meldet ein Profil mit "Aus" keine Verschlüsselung an, SMB 3.0/3.0.2 gilt dann als "nicht verfügbar")
    private val seen = HashMap<String, Result<PcNeg>>()
    private fun key(host: String, mode: SmbEncryption) = PcIdentity.normalizeHost(host) + "|" + mode.name
    internal fun get(host: String, mode: SmbEncryption): Result<PcNeg>? = seen[key(host, mode)]
    internal fun put(host: String, mode: SmbEncryption, r: Result<PcNeg>) { seen[key(host, mode)] = r }
}

/** Zu lernende Kennung: Profil, Host (klein), Kennung, Modus, der beim Schreiben noch in der Datenbank stehen muss */
class PcLearn(val id: Long, val host: String, val guid: String, val mode: String)

/** Zwischenstand: Verbindung (offen, solange nicht geschlossen), Entscheidung, ggf. Sitzung und Lern-/Vertrauensauftrag */
class PcChecked<S>(val decision: PcDecision) {
    var link: PcLink<S>? = null
    var session: S? = null

    /** Nach erfolgreicher Anmeldung zu lernen: Profil-ID, Host (klein), Kennung */
    var learn: PcLearn? = null

    /** Nach erfolgreicher Anmeldung neu zu vertrauen: Host, alte Kennung, neue Kennung */
    var trust: Triple<String, String, String>? = null

    fun close() {
        try { link?.close() } catch (ignored: Exception) {}
        link = null
    }
}

object PcLogin {

    /** Stand für die Entscheidung: wirksamer Modus, gespeicherte Kennung (nach V7), in der Datenbank?, Datenbank-Modus */
    class Ident(
        val mode: PcCheckMode, val guid: String, val guidHost: String,
        val inDb: Boolean, val id: Long, val dbMode: PcCheckMode, val dbHost: String = "",
    ) {
        /** Gehört [host] zum in der Datenbank gespeicherten Pfad des Profils? */
        fun sameHost(host: String): Boolean = dbHost.isNotBlank() && PcIdentity.normalizeHost(dbHost) == PcIdentity.normalizeHost(host)
    }

    /**
     * Aktueller Stand: Profil per ID neu aus der Datenbank (Kennung und Host kommen von dort). Modus = der strengere von Kopie
     * und Datenbank; eine bei WIFI gelernte Kennung gilt bei GUID/BOTH als leer (V7). Nicht gespeichert (id 0 / nicht in der
     * Datenbank): Werte der Kopie, ohne Lernen. Ohne [store] bei gespeichertem Profil: Fehler (sicherer Ausfall).
     */
    fun currentIdentity(p: Profile, store: PcStore?): Ident {
        val copyMode = PcCheckMode.parse(p.pcCheck)
        if (p.id == 0L) return Ident(copyMode, p.pcGuid, p.pcGuidHost, false, 0L, copyMode)
        val s = store ?: throw IllegalStateException("PcStore not initialised")
        val db = s.profile(p.id) ?: return Ident(copyMode, p.pcGuid, p.pcGuidHost, false, 0L, copyMode)
        val dbMode = PcCheckMode.parse(db.pcCheck)
        val mode = PcIdentity.effectiveMode(dbMode, copyMode)
        val (guid, guidHost) = PcIdentity.trustedStored(dbMode, mode, db.pcGuid, db.pcGuidHost)
        return Ident(mode, guid, guidHost, true, db.id, dbMode, SmbTarget.hostOf(db.smbPath).orEmpty())
    }

    private fun <S> negotiate(transport: PcTransport<S>, host: String): Pair<PcLink<S>, PcNeg> {
        val link = transport.connect(host)
        return try {
            link to PcNeg(
                PcIdentity.normalize(try { link.serverGuid() } catch (e: Exception) { null }),
                encryptionOk(link),
            )
        } catch (e: Exception) {
            link.close(); throw e
        }
    }

    /** Verschlüsselung verfügbar? Jeder Fehler beim Abfragen zählt als "nicht verfügbar" (sicherer Ausfall) */
    private fun <S> encryptionOk(link: PcLink<S>): Boolean = try { link.encryptionAvailable() } catch (e: Exception) { false }

    /**
     * Der EINZIGE Weg zu einem Server. [authenticate] false: nur aushandeln und entscheiden (Verbindung wird geschlossen);
     * mit [seenCache] wird je Host nur einmal ausgehandelt. [learn] true: eine unbekannte Kennung darf nach erfolgreicher
     * Anmeldung gelernt werden (nur Speichern und Verbindung testen, V4); bei WIFI lernt der Weg still wie bisher.
     * [onChangedGuid]: nur bei [PcAccess.SETUP] und abweichender Kennung. Ablauf (-055 Punkt 3): aushandeln, Kennung lesen,
     * Verbindung schliessen, fragen, neu verbinden, die gesehene Kennung muss GENAU der bestätigten entsprechen, erst dann
     * anmelden; Übernahme erst nach erfolgreicher Anmeldung. Wirft [PcIdentityException], ohne Zugangsdaten zu senden.
     * Verschlüsselung ([Profile.encryption], SYNC-20261111-144): bei REQUIRED wird vor der Anmeldung geprüft, ob sie verfügbar ist,
     * und danach, ob die Sitzung verschlüsselt ist; sonst [SmbEncryptionUnavailableException] (vor der Anmeldung ohne Zugangsdaten).
     */
    fun <S> connect(
        profile: Profile, host: String, transport: PcTransport<S>, store: PcStore?, access: PcAccess,
        learn: Boolean, learnNetOk: Boolean,
        onChangedGuid: ((host: String, oldGuid: String, newGuid: String) -> Boolean)?,
        authenticate: Boolean, seenCache: PcSeen? = null,
    ): PcChecked<S> {
        var link: PcLink<S>? = null
        try {
            val encMode = profile.encryption()
            val cached = if (!authenticate) seenCache?.get(host, encMode) else null
            val seen: String?
            val encAvailable: Boolean
            if (cached != null) {
                val n = cached.getOrThrow()
                seen = n.guid; encAvailable = n.encryption
            } else {
                val (l, n) = try {
                    negotiate(transport, host)
                } catch (e: Exception) {
                    if (!authenticate) seenCache?.put(host, encMode, Result.failure(e)) // auch Fehler je Host nur einmal erfragen
                    throw e
                }
                link = l; seen = n.guid; encAvailable = n.encryption
                if (!authenticate) seenCache?.put(host, encMode, Result.success(n))
            }
            val cur = currentIdentity(profile, store)
            val decision = PcIdentity.decide(cur.mode, cur.guid, cur.guidHost, host, seen, access, learnNetOk)
            val c = PcChecked<S>(decision)
            if (!authenticate) {
                link?.close(); link = null
                // Vorab-Prüfung: «Erforderlich» ohne Verschlüsselung am PC -> Abbruch (die Kennungs-Entscheidung hat Vorrang)
                if (encMode == SmbEncryption.REQUIRED && decision.mayAuthenticate && !encAvailable)
                    throw SmbEncryptionUnavailableException(host)
                return c
            }
            c.link = link
            when (decision) {
                PcDecision.ALLOW -> {}
                PcDecision.ALLOW_AND_LEARN ->
                    // Lernen nur, wenn der Modus in der Datenbank dem wirksamen entspricht (kein Lernen für einen noch
                    // ungespeicherten Moduswechsel, V7) und nur auf ausdrückliche Anforderung (V4); bei WIFI wie bisher still
                    // und nur, wenn der Host der Verbindung dem gespeicherten Host des Profils entspricht (Editor mit ungespeichertem
                    // Hostwechsel lernt nicht, M1); das DAO prüft den Modus zusätzlich beim Schreiben (L1)
                    if (cur.inDb && seen != null && cur.dbMode == cur.mode && cur.sameHost(host) && (learn || cur.mode == PcCheckMode.WIFI))
                        c.learn = PcLearn(cur.id, PcIdentity.normalizeHost(host), seen, cur.dbMode.name)
                PcDecision.CHANGED -> {
                    val old = PcIdentity.storedFor(cur.guid, cur.guidHost, host)
                    if (access != PcAccess.SETUP || !cur.inDb || !cur.sameHost(host) || old == null || seen == null || onChangedGuid == null)
                        throw PcIdentityException(decision, host)
                    // Rückfrage ohne offene, nicht angemeldete Verbindung (Windows kann sie trennen)
                    link?.close(); link = null; c.link = null
                    if (!onChangedGuid(PcIdentity.normalizeHost(host), old, seen)) throw PcIdentityException(decision, host)
                    val (l2, n2) = negotiate(transport, host)
                    link = l2; c.link = l2
                    if (n2.guid != seen) throw PcIdentityException(decision, host) // nicht die bestätigte Kennung: nicht anmelden
                    c.trust = Triple(PcIdentity.normalizeHost(host), old, seen)
                }
                PcDecision.UNKNOWN, PcDecision.NO_ID, PcDecision.WRONG_NET -> throw PcIdentityException(decision, host)
            }
            val active = link!!
            // «Erforderlich»: Pflichtprüfung VOR der Anmeldung (am aktuell gültigen Link, also auch nach neuer Aushandlung)
            if (encMode == SmbEncryption.REQUIRED && !encryptionOk(active)) throw SmbEncryptionUnavailableException(host)
            val session = active.authenticate()
            // ... und danach: die Sitzung muss wirklich verschlüsselt sein, sonst abbrechen (der Catch-Block schliesst die Verbindung)
            if (encMode == SmbEncryption.REQUIRED && !(try { active.sessionEncrypted(session) } catch (e: Exception) { false }))
                throw SmbEncryptionUnavailableException(host, afterLogin = true)
            c.session = session
            return c
        } catch (e: Exception) {
            try { link?.close() } catch (ignored: Exception) {}
            throw e
        }
    }

    /**
     * Lernen/Neuvertrauen NACH erfolgreicher Anmeldung (und connectShare bzw. Freigabenliste). Nie still überschreiben: das DAO
     * ändert nur bei leerer Kennung bzw. passender alter Kennung. Ein Fehler hier darf die Sitzung nicht beenden.
     */
    fun <S> commit(c: PcChecked<S>, store: PcStore?) {
        val s = store ?: return
        try {
            c.learn?.let { s.learn(it.id, it.host, it.guid, it.mode) }
            c.trust?.let { (host, old, new) -> s.trust(host, old, new) }
        } catch (ignored: Exception) {
        }
    }
}
