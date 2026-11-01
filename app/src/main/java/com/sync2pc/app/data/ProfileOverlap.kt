package com.sync2pc.app.data

/**
 * Erkennt doppelte Profile (0.2.1).
 *  - IDENTICAL: gleiches Ziel und dieselben Quellen -> wird nicht gespeichert bzw. beim Import übersprungen
 *  - PARTIAL:   gleiches Ziel und mindestens ein gemeinsamer Quellordner -> Warnung, Speichern erlaubt
 * Gross-/Kleinschreibung, "/" vs. "\" und abschliessende Trenner spielen keine Rolle.
 * PC-Name und IP-Adresse desselben PCs können nicht als gleich erkannt werden.
 */
object ProfileOverlap {
    enum class Kind { IDENTICAL, PARTIAL }

    data class Hit(val other: Profile, val kind: Kind, val sharedFolders: List<String>) {
        fun message(): String = when (kind) {
            Kind.IDENTICAL -> "Profil „${other.name}“ hat bereits dasselbe Ziel und dieselben Quellen."
            Kind.PARTIAL -> "Gleiches Ziel wie Profil „${other.name}“" +
                (if (sharedFolders.isNotEmpty()) " und gemeinsame Quellordner: ${sharedFolders.joinToString(", ")}" else "") +
                ". Diese Dateien würden von beiden Profilen geprüft und übertragen."
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
            if (o.id == p.id || o.type() != p.type() || target(o) != t) continue
            val hit = when (p.type()) {
                ProfileType.MIRROR -> {
                    if (p.localTreeUri.isNotBlank() && p.localTreeUri.equals(o.localTreeUri, ignoreCase = true))
                        Hit(o, Kind.IDENTICAL, emptyList()) else null
                }
                ProfileType.PHOTOS -> photosHit(p, o)
            } ?: continue
            if (hit.kind == Kind.IDENTICAL) return hit
            if (partial == null) partial = hit
        }
        return partial
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
