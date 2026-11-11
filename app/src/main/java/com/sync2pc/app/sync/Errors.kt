package com.sync2pc.app.sync

/**
 * Netzwerk-/Verbindungsfehler erkennen (WLAN weg, PC im Ruhezustand, Leerlauf-Trennung durch Windows …).
 * Solche Fehler sind erwartbar: Sie unterbrechen den Lauf, zählen aber nicht als Datei-Fehler.
 */
fun isNetworkError(e: Throwable): Boolean {
    // PC nicht sicher erkannt (SYNC-20261109-047): nie ein Netzwerkfehler, auch nicht als Ursache in der Kette.
    // Zuerst prüfen, damit eine umhüllende Netzwerk-Exception sie nicht überdeckt.
    // Ebenso "Verschlüsselung erforderlich, aber nicht verfügbar" (SYNC-20261111-144): Wiederholen ändert nichts
    if (generateSequence(e) { x -> x.cause?.takeIf { it !== x } }.take(9)
            .any { it is PcIdentityException || it is com.sync2pc.app.smb.SmbEncryptionUnavailableException }) return false
    var t: Throwable? = e
    var depth = 0
    while (t != null && depth < 8) {
        if (t is java.net.SocketException || t is java.net.SocketTimeoutException ||
            t is java.net.UnknownHostException || t is java.io.EOFException ||
            t.javaClass.name.contains("TransportException") ||
            // smbj: Verbindung/Share bereits geschlossen
            t.message?.contains("already been closed", ignoreCase = true) == true ||
            t.message?.contains("not connected", ignoreCase = true) == true
        ) return true
        t = t.cause; depth++
    }
    return false
}

/** Eintrag war beim Scan regulär, ist aber inzwischen pending/trashed (z. B. Umbenennung durch System-UI). */
class LocalItemNotReadyException(message: String, cause: Throwable) : java.io.IOException(message, cause)

/** Fehler des MediaProviders "Only owner is able to interact with pending/trashed item" (Android 10+) */
fun isPendingOrTrashed(e: Throwable): Boolean =
    generateSequence(e) { t -> t.cause?.takeIf { it !== t } }.take(9).any {
        it is LocalItemNotReadyException ||
            (it is IllegalStateException && it.message?.contains("pending/trashed", ignoreCase = true) == true)
    }

/**
 * Verständliche Meldung mit nächstem Schritt für häufige Fehler (0.11.0), sonst null.
 * Der technische Text ([shortError]) bleibt im Verlauf (aufklappen) und in der Diagnose.
 */
fun friendlyError(e: Throwable): String? =
    pcIdentityOf(e)?.message ?: encryptionOf(e)?.message ?: friendlyErrorId(e)?.let { com.sync2pc.app.l10n.str(it) }

/** Als Kennung (Verlauf, 0.12.0): verständlicher Text in der aktiven Sprache, sonst der technische Text wörtlich */
fun userErrorMsg(e: Throwable): com.sync2pc.app.l10n.Msg =
    pcIdentityOf(e)?.toMsg()
        ?: encryptionOf(e)?.toMsg()
        ?: friendlyErrorId(e)?.let { com.sync2pc.app.l10n.Msg.of(it) } ?: com.sync2pc.app.l10n.Msg.lit(shortError(e))

/** "PC nicht sicher erkannt" (Text mit Host als Argument, deshalb ausserhalb von [friendlyErrorId]) */
private fun pcIdentityOf(e: Throwable): PcIdentityException? =
    generateSequence(e) { t -> t.cause?.takeIf { it !== t } }.take(9).firstOrNull { it is PcIdentityException } as? PcIdentityException

/** Ist die Ursache «Verschlüsselung erforderlich, aber nicht verfügbar»? (SYNC-20261111-172) */
fun isEncryptionUnavailable(e: Throwable): Boolean = encryptionOf(e) != null

/** "Verschlüsselung erforderlich, aber nicht verfügbar" (Text mit Host als Argument, SYNC-20261111-144) */
private fun encryptionOf(e: Throwable): com.sync2pc.app.smb.SmbEncryptionUnavailableException? =
    generateSequence(e) { t -> t.cause?.takeIf { it !== t } }.take(9)
        .firstOrNull { it is com.sync2pc.app.smb.SmbEncryptionUnavailableException } as? com.sync2pc.app.smb.SmbEncryptionUnavailableException

internal fun friendlyErrorId(e: Throwable): Int? {
    val chain = generateSequence(e) { t -> t.cause?.takeIf { it !== t } }.take(9).toList()
    val text = chain.joinToString(" | ") { "${it.javaClass.name} ${it.message.orEmpty()}" }
    fun has(vararg keys: String) = keys.any { text.contains(it, ignoreCase = true) }
    val id = when {
        has("STATUS_LOGON_FAILURE", "STATUS_WRONG_PASSWORD", "STATUS_ACCOUNT_RESTRICTION",
            "STATUS_ACCOUNT_LOCKED_OUT", "STATUS_PASSWORD_EXPIRED", "STATUS_ACCOUNT_DISABLED") ->
            com.sync2pc.app.R.string.err_friendly_login
        has("STATUS_BAD_NETWORK_NAME") -> com.sync2pc.app.R.string.err_friendly_share_missing
        has("STATUS_OBJECT_NAME_NOT_FOUND", "STATUS_OBJECT_PATH_NOT_FOUND") -> com.sync2pc.app.R.string.err_friendly_folder_missing
        has("STATUS_ACCESS_DENIED") -> com.sync2pc.app.R.string.err_friendly_access
        has("STATUS_DISK_FULL") -> com.sync2pc.app.R.string.err_friendly_pc_full
        has("ENOSPC", "No space left") -> com.sync2pc.app.R.string.err_friendly_phone_full
        chain.any { it is java.net.UnknownHostException } -> com.sync2pc.app.R.string.err_friendly_unknown_host
        chain.any { it is java.net.ConnectException || it is java.net.NoRouteToHostException ||
            it is java.net.SocketTimeoutException } ||
            has("ECONNREFUSED", "EHOSTUNREACH", "ENETUNREACH", "ETIMEDOUT") -> com.sync2pc.app.R.string.err_friendly_unreachable
        else -> return null
    }
    return id
}

/** Für Anzeigen (Statuskachel, Verlauf-Karte): verständlich, wenn bekannt, sonst technisch */
fun userError(e: Throwable): String = friendlyError(e) ?: shortError(e)

/** Kurze, lesbare Fehlermeldung: innerste Ursache statt der ganzen Kette */
fun shortError(e: Throwable): String {
    // Nie als rohe Ausnahme anzeigen (SYNC-20261111-144): auch die technische Kurzform ist der verständliche Text
    encryptionOf(e)?.let { return it.message }
    var root: Throwable = e
    var depth = 0
    while (root.cause != null && root.cause !== root && depth < 8) { root = root.cause!!; depth++ }
    val msg = root.message?.substringAfterLast(": ")?.takeIf { it.isNotBlank() } ?: root.message
    return root.javaClass.simpleName + (if (!msg.isNullOrBlank()) ": $msg" else "")
}
