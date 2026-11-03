package com.sync2pc.app.data

import com.sync2pc.app.R
import com.sync2pc.app.l10n.str

/**
 * Erkennt doppelte Profile (0.2.1).
 *  - IDENTICAL: gleiches Ziel und dieselben Quellen -> wird nicht gespeichert bzw. beim Import übersprungen
 *  - PARTIAL:   gleiches Ziel und mindestens ein gemeinsamer Quellordner -> Warnung, Speichern erlaubt
 * Gross-/Kleinschreibung, "/" vs. "\" und abschliessende Trenner spielen keine Rolle.
 * PC-Name und IP-Adresse desselben PCs können nicht als gleich erkannt werden.
 */
object ProfileOverlap {
    enum class Kind { IDENTICAL, PARTIAL }

    data class Hit(
        val other: Profile, val kind: Kind, val sharedFolders: List<String>,
        /** Ordner-Profile (0.4.1): ein Ordner liegt innerhalb des Ordners des anderen Profils */
        val nested: Boolean = false,
    ) {
        fun message(): String = when {
            kind == Kind.IDENTICAL -> str(R.string.overlap_identical, other.name)
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

    /** Schwerster Treffer gegen [others] (IDENTICAL vor PARTIAL), null = keine Überschneidung */
    fun check(p: Profile, others: List<Profile>): Hit? {
        val t = target(p)
        if (t.isEmpty()) return null
        var partial: Hit? = null
        for (o in others) {
            if (o.id == p.id || o.type() != p.type()) continue
            val hit = when (p.type()) {
                ProfileType.MIRROR -> mirrorHit(p, o, t)
                ProfileType.PHOTOS -> if (target(o) == t) photosHit(p, o) else null
            } ?: continue
            if (hit.kind == Kind.IDENTICAL) return hit
            if (partial == null) partial = hit
        }
        return partial
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
        return if (remoteNested || localNested) Hit(o, Kind.PARTIAL, emptyList(), nested = true) else null
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
