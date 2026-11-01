package com.sync2pc.app.sync

/**
 * Netzwerk-/Verbindungsfehler erkennen (WLAN weg, PC im Ruhezustand, Leerlauf-Trennung durch Windows …).
 * Solche Fehler sind erwartbar: Sie unterbrechen den Lauf, zählen aber nicht als Datei-Fehler.
 */
fun isNetworkError(e: Throwable): Boolean {
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

/** Kurze, lesbare Fehlermeldung: innerste Ursache statt der ganzen Kette */
fun shortError(e: Throwable): String {
    var root: Throwable = e
    var depth = 0
    while (root.cause != null && root.cause !== root && depth < 8) { root = root.cause!!; depth++ }
    val msg = root.message?.substringAfterLast(": ")?.takeIf { it.isNotBlank() } ?: root.message
    return root.javaClass.simpleName + (if (!msg.isNullOrBlank()) ": $msg" else "")
}
