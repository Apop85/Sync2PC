package com.sync2pc.app.mirror

import com.sync2pc.app.sync.isNetworkError
import kotlinx.coroutines.CancellationException

/** Ein Eintrag in einem Ordner einer [TrashSide] */
data class TrashNode(val name: String, val isDir: Boolean)

/** Der Anbieter (Handy-Ordner, Freigabe) kann Ordner nicht umbenennen bzw. Dateien nicht verschieben: nichts tun, später wieder versuchen */
class TrashMigrationUnsupported(message: String = "nicht unterstützt") : Exception(message)

/**
 * Eine Seite (Handy oder PC) aus Sicht der Papierkorb-Übernahme: genau die Operationen, die [TrashMigration] braucht
 * (SYNC-20261111-244). Pfade relativ zum Profilordner, Trenner "/", "" = Profilordner. Die Implementierungen für das Handy
 * ([PhoneTrashSide]) und den PC ([PcTrashSide]) stehen in `TrashMigrationSides.kt`; Tests benutzen ein Fake im Speicher.
 * Es gibt bewusst **keine** Operation "kopieren" und keine rekursive Löschung.
 */
interface TrashSide {
    /** Direkte Einträge des Ordners [rel]; leer, wenn er fehlt. Fehler beim Lesen werden geworfen (nie "leer" vortäuschen) */
    fun list(rel: String): List<TrashNode>

    /** Ordner (inkl. Zwischenebenen) anlegen, falls nötig */
    fun makeDir(rel: String)

    /** Ordner [rel] im selben Elternordner in [newName] umbenennen (Ziel darf nicht existieren) */
    fun renameDir(rel: String, newName: String)

    /**
     * Datei verschieben (Ziel darf nicht existieren, Zielordner muss es). Nie überschreiben, nie kopieren + löschen:
     * Kann der Anbieter nicht verschieben, [TrashMigrationUnsupported].
     */
    fun moveFile(fromRel: String, toRel: String)

    /** Ordner löschen, nur wenn er (inkl. Unterordnern) keine Datei mehr enthält; true = danach weg */
    fun deleteDirIfEmpty(rel: String): Boolean

    /**
     * Die Marker-Datei `.nomedia` direkt im Ordner [rel] löschen (0 Byte, kein Papierkorb-Inhalt; SYNC-20261112-249); nichts tun,
     * wenn sie fehlt. Die einzige Löschung einer Datei der Übernahme.
     */
    fun deleteNoMedia(rel: String)
}

/**
 * Papierkorb-Übernahme (SYNC-20261111-244, Stufe B): ".LeoSync-Bin" -> "@LeoSync-Bin", je Seite getrennt. Der Kern ist rein
 * (nur [TrashSide]) und in TrashMigrationTest getestet.
 *
 * Ablauf einer Seite:
 *  (a) nur der Altname vorhanden: Ordner **umbenennen** (eine atomare Operation, keine Datei wird angefasst);
 *  (b) beide Namen vorhanden: Inhalt Datei für Datei **verschieben** (nie kopieren), Datumsordner zusammenführen; gleicher
 *      Dateiname im Ziel: Uhrzeit anhängen ([TrashIndex.uniqueTrashName]), nichts wird überschrieben. Ein Ordner des Altnamens
 *      wird erst gelöscht, wenn er leer ist ([TrashSide.deleteDirIfEmpty]);
 *  (c) jeder Schritt ist einzeln wiederholbar: Eine Datei ist entweder noch im alten oder schon im neuen Ordner (verschieben
 *      ist atomar), nach einem Abbruch (Prozessende, Verbindungsverlust) sieht der nächste Lauf den Rest und macht weiter,
 *      ohne etwas zu doppeln; vorhandene leere Zielordner werden wiederverwendet;
 *  (d) kann der Anbieter nicht umbenennen/verschieben: Abbruch ohne Wirkung, der Altname bleibt les- und wiederherstellbar.
 * Die Übernahme ändert weder den Dateistand noch den Sync-Zustand: der Papierkorb ist nie Teil des Sync-Inhalts.
 */
object TrashMigration {
    enum class Status {
        /** Kein Altname-Ordner vorhanden: nichts zu tun */
        NOTHING,
        /** Alles übernommen, der Altname-Ordner ist weg */
        MIGRATED,
        /** Anbieter kann nicht umbenennen/verschieben: nichts (mehr) getan, später wieder versuchen */
        UNSUPPORTED,
        /** Es blieb Inhalt zurück (Namenskonflikt Datei/Ordner, Ordner nicht leer): später wieder versuchen */
        INCOMPLETE,
        /** Mindestens ein Fehler ([Result.error] ist der erste); später wieder versuchen */
        FAILED,
    }

    data class Result(val status: Status, val moved: Int = 0, val error: Exception? = null)

    /**
     * Darf die Übernahme jetzt laufen? Nur wenn für das Profil keine Wiederherstellung vorgemerkt ist ([pendingRestores], auch
     * fehlgeschlagene), kein Leeren des PC-Papierkorbs ([purgePending]) und keine ausstehende Ordner-Bestätigung
     * ([folderCheckPending]): Die Datenbank bzw. der PurgeStore speichern Papierkorb-Pfade mit dem Ordnernamen. Sie zeigten
     * sonst auf einen Ordner, der nicht mehr existiert. Die Übernahme wartet dann auf einen späteren Lauf.
     */
    fun mayRun(pendingRestores: Int, purgePending: Boolean, folderCheckPending: Boolean): Boolean =
        pendingRestores == 0 && !purgePending && !folderCheckPending

    private class Ctx(val side: TrashSide, val check: () -> Unit, val propagateNetwork: Boolean, val time: () -> String) {
        var moved = 0
        var error: Exception? = null
        var incomplete = false
    }

    /**
     * Eine Seite übernehmen. [check] wirft, wenn der Lauf abgebrochen wird (Coroutine). [propagateNetworkErrors]: Netzwerkfehler
     * gehen nach aussen (PC-Seite: der Lauf wird wie sonst unterbrochen), sonst gelten sie als gewöhnlicher Fehler.
     * [time] liefert die Uhrzeit "HH-mm-ss" für Namenskonflikte (Tests: fest).
     */
    fun run(
        side: TrashSide, check: () -> Unit = {}, propagateNetworkErrors: Boolean = false,
        time: () -> String = { TrashIndex.timeNow() },
    ): Result {
        val ctx = Ctx(side, check, propagateNetworkErrors, time)
        try {
            val root = side.list("")
            val olds = root.filter { it.isDir && it.name.equals(MirrorEngine.DOT_TRASH_DIR, ignoreCase = true) }
            if (olds.isEmpty()) return Result(Status.NOTHING)
            // Eine gleichnamige Datei statt Ordner würde das Anlegen/Umbenennen verhindern: nichts tun
            if (root.any { !it.isDir && it.name.equals(MirrorEngine.TRASH_DIR, ignoreCase = true) }) return Result(Status.INCOMPLETE)
            val news = root.filter { it.isDir && it.name.equals(MirrorEngine.TRASH_DIR, ignoreCase = true) }
            if (news.isEmpty() && olds.size == 1) {
                // (a) nur der Altname: eine atomare Umbenennung
                side.renameDir(olds[0].name, MirrorEngine.TRASH_DIR)
                return Result(Status.MIGRATED)
            }
            // (b) Zusammenführen: vorhandenen neuen Ordner (in seiner Schreibweise) wiederverwenden, sonst anlegen
            val target = news.firstOrNull()?.name ?: MirrorEngine.TRASH_DIR.also { side.makeDir(it) }
            for (old in olds) {
                ctx.check()
                mergeDir(ctx, old.name, target)
                try {
                    if (!side.deleteDirIfEmpty(old.name)) ctx.incomplete = true
                } catch (e: Exception) { fail(ctx, e) }
            }
            return when {
                ctx.error != null -> Result(Status.FAILED, ctx.moved, ctx.error)
                ctx.incomplete -> Result(Status.INCOMPLETE, ctx.moved)
                else -> Result(Status.MIGRATED, ctx.moved)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TrashMigrationUnsupported) {
            return Result(Status.UNSUPPORTED, ctx.moved)
        } catch (e: Exception) {
            if (propagateNetworkErrors && isNetworkError(e)) throw e
            return Result(Status.FAILED, ctx.moved, e)
        }
    }

    /** Einen Fehler einer einzelnen Datei/eines Ordners festhalten und weitermachen (Netzwerk/Abbruch/"geht nicht" gehen nach aussen) */
    private fun fail(ctx: Ctx, e: Exception) {
        if (e is CancellationException || e is TrashMigrationUnsupported) throw e
        if (ctx.propagateNetwork && isNetworkError(e)) throw e
        if (ctx.error == null) ctx.error = e
        ctx.incomplete = true
    }

    /** Inhalt von [src] nach [dst] verschieben (beides Ordner, [dst] existiert); leere Unterordner von [src] werden entfernt */
    private fun mergeDir(ctx: Ctx, src: String, dst: String) {
        val side = ctx.side
        val taken = side.list(dst).associateByTo(HashMap()) { it.name.lowercase() }
        for (node in side.list(src)) {
            ctx.check()
            val from = "$src/${node.name}"
            try {
                // `.nomedia` direkt im Altnamen-Ordner ist kein Inhalt: nicht ins Ziel verschieben (kein Uhrzeit-Anhang), sondern
                // entfernen, sonst bliebe der Altname-Ordner dauerhaft nicht leer (INCOMPLETE). Das Ziel bekommt seine eigene
                // (nur Handy, siehe LocalTree.ensureTrashNoMedia).
                if (!node.isDir && '/' !in src && TrashNoMedia.isMarker(node.name)) { side.deleteNoMedia(src); continue }
                val existing = taken[node.name.lowercase()]
                if (node.isDir) {
                    if (existing != null && !existing.isDir) { ctx.incomplete = true; continue } // Datei im Weg: nicht anfassen
                    val dstName = existing?.name ?: node.name
                    val to = "$dst/$dstName"
                    if (existing == null) { side.makeDir(to); taken[node.name.lowercase()] = TrashNode(node.name, true) }
                    mergeDir(ctx, from, to)
                    if (!side.deleteDirIfEmpty(from)) ctx.incomplete = true
                } else {
                    val name = TrashIndex.uniqueTrashName(node.name, ctx.time()) { it.lowercase() in taken }
                    side.moveFile(from, "$dst/$name")
                    taken[name.lowercase()] = TrashNode(name, false)
                    ctx.moved++
                }
            } catch (e: Exception) {
                fail(ctx, e)
            }
        }
    }
}
