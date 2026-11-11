package com.sync2pc.app.mirror

import com.hierynomus.mserref.NtStatus
import com.hierynomus.mssmb2.SMBApiException
import com.sync2pc.app.smb.RemoteStore
import com.sync2pc.app.sync.isNetworkError
import kotlinx.coroutines.CancellationException

/**
 * Datei auf dem PC in den Papierkorb verschieben (SYNC-20261111-244, aus der [MirrorEngine] ausgelagert): nach
 * "<Basis>\@LeoSync-Bin\<Datum>\<Pfad>"; gleicher Name dort -> Uhrzeit anhängen ("a (14-32-05).txt", 0.7.0).
 *
 * Rückfall: Kann der Ordner "@LeoSync-Bin" auf dem PC nicht angelegt werden, weil der Server den Namen ablehnt
 * ([nameRejected]: Namens-Statuscodes, nicht Zugriff verweigert, Quota oder Sitzungsfehler), wird für den
 * Rest des Laufs [MirrorEngine.DOT_TRASH_DIR] benutzt; [onFallback] erhält den Grund (einmal, für die Verlaufswarnung).
 * Netzwerkfehler gelten nie als Namensproblem: sie gehen unverändert nach aussen. Es geht dabei nichts verloren, die Datei
 * wird erst verschoben, wenn der Zielordner steht. Ein Fehler beim Verschieben selbst (Datei gesperrt …) bleibt ein
 * gewöhnlicher Dateifehler: er lässt sich nicht von einem Namensproblem unterscheiden und löst keinen Rückfall aus.
 */
class PcTrashMover(
    /** Ist der Fehler eine Ablehnung des Ordnernamens (nur dann Rückfall)? Zugriff verweigert, Quota, Sitzung abgelaufen … nicht (Review -244 M2) */
    private val isNameRejection: (Exception) -> Boolean = { e -> nameRejected(e) },
    /** Beginnt gleich mit dem Ausweichnamen (der PC lehnt den neuen Namen laut Merker ab, [PcTrashNameMemory]) */
    startWithFallback: Boolean = false,
    private val onFallback: (Exception) -> Unit = {},
) {
    companion object {
        /** SMB-Statuscodes, mit denen ein Server einen unzulässigen oder unmöglichen Ordnernamen ablehnt */
        private val NAME_STATUSES = setOf(
            NtStatus.STATUS_OBJECT_NAME_INVALID.value,
            NtStatus.STATUS_OBJECT_NAME_COLLISION.value,
            NtStatus.STATUS_INVALID_PARAMETER.value,
            NtStatus.STATUS_NOT_SUPPORTED.value,
        )

        /**
         * Strenger als [nameRejected]: nur «Name ungültig». Für die Übernahme (QA-Check -244 L5): Der erste Fehler irgendeiner
         * Verschiebung (z. B. Kollision mit einem anderen Gerät) ist keine Namensablehnung und setzt keinen Merker.
         */
        internal fun nameInvalid(e: Exception): Boolean =
            generateSequence<Throwable>(e) { t -> t.cause?.takeIf { it !== t } }.take(9)
                .any { it is SMBApiException && it.statusCode == NtStatus.STATUS_OBJECT_NAME_INVALID.value }

        /** Eine [SMBApiException] mit Namensstatus irgendwo in der Ursachenkette */
        internal fun nameRejected(e: Exception): Boolean =
            generateSequence<Throwable>(e) { t -> t.cause?.takeIf { it !== t } }.take(9)
                .any { it is SMBApiException && it.statusCode in NAME_STATUSES }
    }

    /** Papierkorb-Name, der gerade benutzt wird (nach einem Rückfall der Ausweichname, geteilt zwischen parallelen Übertragungen) */
    @Volatile var dirName: String = if (startWithFallback) MirrorEngine.DOT_TRASH_DIR else MirrorEngine.TRASH_DIR
        private set

    /** Ab sofort den Ausweichnamen benutzen (die Übernahme hat gezeigt, dass der PC den neuen Namen ablehnt) */
    fun useFallback() { dirName = MirrorEngine.DOT_TRASH_DIR }

    /** Liefert den vollen Pfad der Datei im Papierkorb. [rel] relativ zum Profilordner, Trenner "/" */
    fun trash(s: RemoteStore, rel: String, today: String, time: String = TrashIndex.timeNow()): String {
        val parent = rel.substringBeforeLast('/', "").replace('/', '\\')
        fun dirFor(name: String) = s.path(listOf(name, today, parent).filter { it.isNotEmpty() }.joinToString("\\"))
        val used = dirName // einmal lesen: ein paralleler Rückfall ändert [dirName] während dieser Aufruf läuft
        var trashDir = dirFor(used)
        try {
            s.ensureDir(trashDir)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isNetworkError(e) || used == MirrorEngine.DOT_TRASH_DIR || !isNameRejection(e)) throw e
            trashDir = dirFor(MirrorEngine.DOT_TRASH_DIR)
            s.ensureDir(trashDir) // scheitert auch das, geht der Fehler nach aussen (Datei bleibt unberührt)
            dirName = MirrorEngine.DOT_TRASH_DIR
            onFallback(e)
        }
        val name = TrashIndex.uniqueTrashName(rel.substringAfterLast('/'), time) { s.stat("$trashDir\\$it") != null }
        s.move(s.path(rel.replace('/', '\\')), "$trashDir\\$name")
        return "$trashDir\\$name"
    }
}
