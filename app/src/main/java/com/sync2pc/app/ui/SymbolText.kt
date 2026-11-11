package com.sync2pc.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.unit.em
import com.sync2pc.app.R

/**
 * Eigene Icons statt Emojis und Symbolzeichen (SYNC-20261111-195, Stil «Variante C Duotone»).
 *
 * [Text] in diesem Paket ersetzt `androidx.compose.material3.Text` (die UI-Dateien importieren Material-`Text` nicht mehr, ein
 * Import aus demselben Paket geht vor Sternimporten): Jedes bekannte Symbolzeichen im Text wird als Icon in die Zeile gesetzt
 * (InlineTextContent). Das Icon ist so gross wie die Schrift (1,25 em), liegt auf der Textlinie und umbricht bei grosser Schrift
 * mit dem Text. Die Strings selbst bleiben unverändert (Benachrichtigungen und Diagnose-Export bleiben Klartext).
 * Hell/Dunkel regeln `drawable` und `drawable-night` (die App folgt dem System, siehe `Common.kt`).
 * Bewusst Text geblieben: der Pfeil → in Beschriftungen, ⏭ (Verlaufszeile «folgt später»), ⤼ und ≤.
 */
object Symbols {
    class Sym(@DrawableRes val icon: Int, @StringRes val description: Int)

    private val folder = Sym(R.drawable.ic_folder, R.string.icon_folder)
    private val camera = Sym(R.drawable.ic_camera, R.string.icon_camera)
    private val phone = Sym(R.drawable.ic_phone, R.string.icon_phone)
    private val pc = Sym(R.drawable.ic_pc, R.string.icon_pc)
    private val server = Sym(R.drawable.ic_server, R.string.icon_server)
    private val add = Sym(R.drawable.ic_add, R.string.icon_add)
    private val up = Sym(R.drawable.ic_upload, R.string.icon_up)
    private val down = Sym(R.drawable.ic_download, R.string.icon_down)
    private val pause = Sym(R.drawable.ic_pause, R.string.icon_pause)
    private val play = Sym(R.drawable.ic_play, R.string.icon_play)
    private val ok = Sym(R.drawable.ic_ok, R.string.icon_ok)
    private val error = Sym(R.drawable.ic_error, R.string.icon_error)
    private val warn = Sym(R.drawable.ic_warn, R.string.icon_warn)
    private val sync = Sym(R.drawable.ic_sync, R.string.icon_sync)
    private val bin = Sym(R.drawable.ic_bin, R.string.icon_bin)
    private val both = Sym(R.drawable.ic_both, R.string.icon_both)

    /** Symbolzeichen (als String, wegen Emojis ausserhalb der BMP) -> Icon */
    val map: Map<String, Sym> = mapOf(
        "📁" to folder, // 📁
        "📷" to camera, // 📷
        "📱" to phone, // 📱
        "💻" to pc, // 💻
        "🖧" to server, // 🖧
        "➕" to add, // ➕
        "⬆" to up, // ⬆
        "↑" to up, // ↑
        "↓" to down, // ↓
        "⏸" to pause, // ⏸
        "▶" to play, // ▶
        "✓" to ok, // ✓
        "✗" to error, // ✗
        "⚠" to warn, // ⚠
        "↻" to sync, // ↻
        "↺" to sync, // ↺
        "🗑" to bin, // 🗑
        "⇄" to both, // ⇄
        "↔" to both, // ↔
    )

    private val chars: Set<Char> = map.keys.map { it[0] }.toSet()

    /** Schnelle Vorprüfung: enthält der Text überhaupt ein Symbol? */
    fun containsAny(text: String): Boolean = text.any { it in chars }

    /** Variantenwähler für Emoji-Darstellung (U+FE0F/U+FE0E) gehört zum Symbol */
    private fun isVariationSelector(c: Char) = c == '️' || c == '︎'

    sealed interface Part {
        data class Plain(val text: String) : Part
        data class Icon(val sym: Sym, val index: Int) : Part
    }

    /**
     * Zerlegt [text] in Text- und Icon-Teile. Folgen auf ein Symbol mehrere Leerzeichen (z. B. «📷  Fotos»), bleibt eines.
     * Ein Symbol am Zeilenanfang oder direkt nach einem Leerzeichen bekommt kein zusätzliches Leerzeichen davor.
     */
    fun split(text: String): List<Part> {
        val out = ArrayList<Part>()
        val sb = StringBuilder()
        var i = 0
        var n = 0
        while (i < text.length) {
            val len = if (text[i].isHighSurrogate() && i + 1 < text.length) 2 else 1
            val key = text.substring(i, i + len)
            val sym = map[key]
            if (sym != null) {
                if (sb.isNotEmpty()) { out += Part.Plain(sb.toString()); sb.setLength(0) }
                out += Part.Icon(sym, n++)
                i += len
                if (i < text.length && isVariationSelector(text[i])) i++
                // mehrfache Leerzeichen nach dem Symbol auf eines verkürzen
                var j = i
                while (j < text.length && text[j] == ' ') j++
                if (j - i > 1) i = j - 1
            } else { sb.append(key); i += len }
        }
        if (sb.isNotEmpty()) out += Part.Plain(sb.toString())
        return out
    }
}

private fun symId(index: Int) = "sym$index"

/** Wie `androidx.compose.material3.Text`, aber Symbolzeichen erscheinen als eigene Icons (siehe [Symbols]) */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    if (!Symbols.containsAny(text)) {
        androidx.compose.material3.Text(
            text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign,
            lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout ?: {}, style,
        )
        return
    }
    val parts = remember(text) { Symbols.split(text) }
    val annotated = remember(parts) {
        buildAnnotatedString {
            for (p in parts) when (p) {
                is Symbols.Part.Plain -> append(p.text)
                // Ersatztext (Bedienhilfe): das Symbolzeichen selbst wird nicht vorgelesen, die Beschreibung steht im Icon
                is Symbols.Part.Icon -> appendInlineContent(symId(p.index), " ")
            }
        }
    }
    val inline = HashMap<String, InlineTextContent>()
    for (p in parts) if (p is Symbols.Part.Icon) {
        val desc = stringResource(p.sym.description)
        inline[symId(p.index)] = InlineTextContent(
            Placeholder(1.25.em, 1.25.em, PlaceholderVerticalAlign.TextCenter)
        ) {
            Image(
                painterResource(p.sym.icon), contentDescription = null,
                modifier = Modifier.fillMaxSize().semantics { contentDescription = desc },
            )
        }
    }
    androidx.compose.material3.Text(
        annotated, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign,
        lineHeight, overflow, softWrap, maxLines, minLines, inline, onTextLayout ?: {}, style,
    )
}

/** AnnotatedString unverändert an Material weiterreichen (keine Symbolersetzung) */
@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) = androidx.compose.material3.Text(
    text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign,
    lineHeight, overflow, softWrap, maxLines, minLines, inlineContent, onTextLayout ?: {}, style,
)
