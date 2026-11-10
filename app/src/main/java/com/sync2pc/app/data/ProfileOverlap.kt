package com.sync2pc.app.data

import com.sync2pc.app.R
import com.sync2pc.app.l10n.str

/**
 * Erkennt doppelte Profile (0.2.1).
 *  - IDENTICAL: gleiches Ziel und dieselben Quellen -> wird nicht gespeichert bzw. beim Import übersprungen
 *  - PARTIAL:   gleiches Ziel und mindestens ein gemeinsamer Quellordner -> Warnung, Speichern erlaubt
 *  - BLOCKED:   (nur Fotos, SYNC-20261109-083) gleiches Ziel, aber andere Ordnerstruktur -> Speichern nicht möglich.
 *               Gilt gegen jedes andere Fotoprofil (auch deaktiviert oder zum Löschen vorgemerkt, solange es noch in der DB steht)
 *               und hat Vorrang vor IDENTICAL/PARTIAL. Verschachtelte Ziele (eines im anderen) bleiben unberücksichtigt.
 * Gross-/Kleinschreibung, "/" vs. "\" und abschliessende Trenner spielen keine Rolle.
 * PC-Name und IP-Adresse desselben PCs können nicht als gleich erkannt werden.
 */
object ProfileOverlap {
    enum class Kind { IDENTICAL, PARTIAL, BLOCKED }

    data class Hit(
        val other: Profile, val kind: Kind, val sharedFolders: List<String>,
        /** Ordner-Profile (0.4.1): ein Ordner liegt innerhalb des Ordners des anderen Profils */
        val nested: Boolean = false,
        /** Welche Seite überschneidet sich (nur [nested]): PC-Ordner, Handy-Ordner oder beide (P11) */
        val remoteSide: Boolean = true, val localSide: Boolean = true,
    ) {
        fun message(): String = when {
            kind == Kind.BLOCKED -> str(R.string.overlap_blocked_layout, other.name, other.layout().label)
            kind == Kind.IDENTICAL -> str(R.string.overlap_identical, other.name)
            nested && remoteSide && !localSide -> str(R.string.overlap_nested_remote, other.name)
            nested && localSide && !remoteSide -> str(R.string.overlap_nested_local, other.name)
            nested -> str(R.string.overlap_nested, other.name)
            sharedFolders.isNotEmpty() -> str(R.string.overlap_shared_folders, other.name, sharedFolders.joinToString(", "))
            else -> str(R.string.overlap_same_target, other.name)
        }
    }

    /** Vergleichbarer Zielpfad: Freigabe + Unterordner */
    fun target(p: Profile): String {
        fun norm(s: String) = s.trim().removePrefix("smb:").replace('/', '\\')
            .split('\\').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\\").lowercase()
        return listOf(norm(p.smbPath), norm(p.remoteSubfolder)).filter { it.isNotEmpty() }.joinToString("\\")
    }

    private fun folder(f: String) = f.trim().replace('\\', '/').trim('/').lowercase() + "/"

    /** Schwerster Treffer gegen [others] (BLOCKED vor IDENTICAL vor PARTIAL), null = keine Überschneidung */
    fun check(p: Profile, others: List<Profile>): Hit? {
        val t = target(p)
        if (t.isEmpty()) return null
        var partial: Hit? = null
        var identical: Hit? = null
        for (o in others) {
            if (o.id == p.id || o.type() != p.type()) continue
            val hit = when (p.type()) {
                ProfileType.MIRROR -> mirrorHit(p, o, t)
                ProfileType.PHOTOS -> if (target(o) == t) {
                    // Gleiches Ziel, andere Ordnerstruktur: blockiert (die Struktur eines Ziels muss einheitlich sein)
                    if (o.layout() != p.layout()) Hit(o, Kind.BLOCKED, emptyList()) else photosHit(p, o)
                } else null
            } ?: continue
            when (hit.kind) {
                Kind.BLOCKED -> return hit
                Kind.IDENTICAL -> if (identical == null) identical = hit
                Kind.PARTIAL -> if (partial == null) partial = hit
            }
        }
        return identical ?: partial
    }

    /**
     * Ordner-Profile: gleiches Ziel + gleicher Handy-Ordner -> IDENTICAL.
     * Ein PC-Ordner oder Handy-Ordner liegt im anderen (verschachtelt) -> PARTIAL mit Warnung (0.4.1).
     */
    private fun mirrorHit(p: Profile, o: Profile, t: String): Hit? {
        val ot = target(o)
        val lp = treePath(p.localTreeUri)
        val lo = treePath(o.localTreeUri)
        if (ot == t && lp != null && lp == lo) return Hit(o, Kind.IDENTICAL, emptyList())
        // Gleicher PC-Ordner mit anderem Handy-Ordner zählt auch: beide Profile sähen die Dateien des anderen
        val remoteNested = ot.isNotEmpty() && (ot == t || inside(t, ot, '\\') || inside(ot, t, '\\'))
        val localNested = lp != null && lo != null && lp != lo && (inside(lp, lo, '/') || inside(lo, lp, '/'))
        return if (remoteNested || localNested) Hit(o, Kind.PARTIAL, emptyList(), nested = true, remoteSide = remoteNested, localSide = localNested) else null
    }

    /** Liegt [child] innerhalb von [parent]? (Pfade in Kleinbuchstaben, ohne abschliessenden Trenner) */
    private fun inside(child: String, parent: String, sep: Char): Boolean =
        parent.isNotEmpty() && child.startsWith(parent + sep)

    /**
     * Vergleichbarer Pfad eines SAF-Baums: "content://…/tree/primary%3ADocuments%2FScans" -> "primary:documents/scans".
     * null = kein Ordner gewählt.
     */
    fun treePath(uri: String): String? {
        if (uri.isBlank()) return null
        val raw = uri.substringAfter("/tree/", uri).substringBefore("/document/")
        val decoded = try { java.net.URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") } catch (e: Exception) { raw }
        val authority = uri.substringAfter("://", "").substringBefore('/')
        return (authority + "|" + decoded.trimEnd('/')).lowercase()
    }

    private fun photosHit(a: Profile, b: Profile): Hit? {
        val fa = a.folders().map(::folder).toSet()
        val fb = b.folders().map(::folder).toSet()
        val shared = fa.filter { x ->
            fb.any { y -> x == y || (b.includeSubfolders && x.startsWith(y)) || (a.includeSubfolders && y.startsWith(x)) }
        }
        if (shared.isEmpty()) return null
        val identical = fa == fb && a.includeSubfolders == b.includeSubfolders &&
            a.includeImages == b.includeImages && a.includeVideos == b.includeVideos
        // Anzeige in Originalschreibweise
        val display = a.folders().filter { folder(it) in shared }
        return Hit(b, if (identical) Kind.IDENTICAL else Kind.PARTIAL, display)
    }
}
