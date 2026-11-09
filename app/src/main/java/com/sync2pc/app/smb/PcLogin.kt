package com.sync2pc.app.smb

import com.sync2pc.app.data.Profile
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

    /** Anmelden (sendet die Zugangsdaten) */
    fun authenticate(): S

    fun close()
}

interface PcTransport<S> {
    /** TCP und SMB-Aushandlung, keine Anmeldung */
    fun connect(host: String): PcLink<S>
}

/** Von diesem Lauf bereits gesehene Kennungen je Host (nur im Speicher, F5): Aushandeln je Host einmal, Entscheidung je Profil */
class PcSeen {
    private val seen = HashMap<String, Result<String?>>()
    internal fun get(host: String): Result<String?>? = seen[PcIdentity.normalizeHost(host)]
    internal fun put(host: String, r: Result<String?>) { seen[PcIdentity.normalizeHost(host)] = r }
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

    private fun <S> negotiate(transport: PcTransport<S>, host: String): Pair<PcLink<S>, String?> {
        val link = transport.connect(host)
        return try {
            link to PcIdentity.normalize(try { link.serverGuid() } catch (e: Exception) { null })
        } catch (e: Exception) {
            link.close(); throw e
        }
    }

    /**
     * Der EINZIGE Weg zu einem Server. [authenticate] false: nur aushandeln und entscheiden (Verbindung wird geschlossen);
     * mit [seenCache] wird je Host nur einmal ausgehandelt. [learn] true: eine unbekannte Kennung darf nach erfolgreicher
     * Anmeldung gelernt werden (nur Speichern und Verbindung testen, V4); bei WIFI lernt der Weg still wie bisher.
     * [onChangedGuid]: nur bei [PcAccess.SETUP] und abweichender Kennung. Ablauf (-055 Punkt 3): aushandeln, Kennung lesen,
     * Verbindung schliessen, fragen, neu verbinden, die gesehene Kennung muss GENAU der bestätigten entsprechen, erst dann
     * anmelden; Übernahme erst nach erfolgreicher Anmeldung. Wirft [PcIdentityException], ohne Zugangsdaten zu senden.
     */
    fun <S> connect(
        profile: Profile, host: String, transport: PcTransport<S>, store: PcStore?, access: PcAccess,
        learn: Boolean, learnNetOk: Boolean,
        onChangedGuid: ((host: String, oldGuid: String, newGuid: String) -> Boolean)?,
        authenticate: Boolean, seenCache: PcSeen? = null,
    ): PcChecked<S> {
        var link: PcLink<S>? = null
        try {
            val cached = if (!authenticate) seenCache?.get(host) else null
            val seen: String?
            if (cached != null) {
                seen = cached.getOrThrow()
            } else {
                val (l, g) = try {
                    negotiate(transport, host)
                } catch (e: Exception) {
                    if (!authenticate) seenCache?.put(host, Result.failure(e)) // auch Fehler je Host nur einmal erfragen
                    throw e
                }
                link = l; seen = g
                if (!authenticate) seenCache?.put(host, Result.success(g))
            }
            val cur = currentIdentity(profile, store)
            val decision = PcIdentity.decide(cur.mode, cur.guid, cur.guidHost, host, seen, access, learnNetOk)
            val c = PcChecked<S>(decision)
            if (!authenticate) { link?.close(); link = null; return c }
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
                    val (l2, g2) = negotiate(transport, host)
                    link = l2; c.link = l2
                    if (g2 != seen) throw PcIdentityException(decision, host) // nicht die bestätigte Kennung: nicht anmelden
                    c.trust = Triple(PcIdentity.normalizeHost(host), old, seen)
                }
                PcDecision.UNKNOWN, PcDecision.NO_ID, PcDecision.WRONG_NET -> throw PcIdentityException(decision, host)
            }
            c.session = link!!.authenticate()
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
