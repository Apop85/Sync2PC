package com.sync2pc.app.data

import com.sync2pc.app.l10n.Msg

/**
 * Anzeigetexte von Verlauf-Einträgen in der aktiven Sprache (0.12.0, DEC-159): Kennung, sonst der gespeicherte Text
 * (alte Einträge, unbekannter Schlüssel, Formatfehler). Die Diagnose (DEC-160) nutzt weiter die gespeicherten Texte.
 */
fun SyncRun.messageText(): String = Msg.render(messageCode, message)

fun SyncRun.triggerText(): String = Msg.render(triggerCode, trigger)

fun LogEntry.text(): String = Msg.render(code, message)
