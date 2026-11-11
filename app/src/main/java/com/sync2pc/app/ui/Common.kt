@file:OptIn(ExperimentalMaterial3Api::class)

package com.sync2pc.app.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sync2pc.app.R
import com.sync2pc.app.l10n.Texts
import com.sync2pc.app.l10n.TimeFormat
import com.sync2pc.app.l10n.str
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun LeoSyncTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = str(R.string.common_back))
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

/**
 * Karte, die sich auf- und zuklappen lässt (0.9.0, für optionale Bereiche im Profil). Zugeklappt steht nur
 * [closedHint] unter dem Titel; der Zustand bleibt beim Drehen bzw. Zurückkehren erhalten.
 */
@Composable
fun CollapsibleCard(
    title: String,
    startOpen: Boolean,
    closedHint: String,
    /** Wert, der zugeklappt hinter dem Titel steht («Verschlüsselung · Wenn möglich»), SYNC-20261111-146 */
    titleSuffix: String? = null,
    /** Zählt hoch, wenn die Karte von aussen aufklappen soll (z. B. nach einem Fehler) */
    openSignal: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(startOpen) }
    androidx.compose.runtime.LaunchedEffect(openSignal) { if (openSignal > 0) open = true }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                Text(if (!open && titleSuffix != null) "$title · $titleSuffix" else title,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Icon(
                    if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = when {
                        open -> str(R.string.profile_section_collapse)
                        // R8a: eingeklappt mit Wert (nicht «Nicht eingestellt»)
                        titleSuffix != null -> str(R.string.profile_section_collapsed_value, titleSuffix)
                        else -> str(R.string.profile_section_unset)
                    },
                )
            }
            if (open) content() else Hint(closedHint)
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/**
 * Snackbar-Host mit der Aktion («Rückgängig») unter dem Text (SYNC-20261111-218 B4): bei grosser Schrift nimmt die Aktion sonst
 * Breite weg und der Text bricht mitten im Wort um.
 */
@Composable
fun LeoSnackbarHost(state: androidx.compose.material3.SnackbarHostState) {
    androidx.compose.material3.SnackbarHost(state) { data ->
        androidx.compose.material3.Snackbar(data, actionOnNewLine = true)
    }
}

/**
 * UNC-Pfad ohne Zeilenumbruch direkt nach dem Präfix «\\» (SYNC-20261111-218 B2): Wortverbinder (U+2060) hinter jedem
 * Backslash des Präfixes, damit bei grosser Schrift nicht ein einzelnes «\» in eigener Zeile steht. Nur zur Anzeige.
 */
fun uncNoBreak(path: String): String =
    if (path.startsWith("\\\\")) "\\⁠\\⁠" + path.substring(2) else path

/**
 * SYNC-20261111-222 B2b: Pfad mit Umbruchmöglichkeit nur nach "\" (Zero-Width-Space). Der erste Pfadteil (bis zum ersten
 * "\" nach dem Anfang) bleibt zusammen, damit ein vorangestelltes Icon nicht allein in einer Zeile steht.
 */
fun pathBreakAfterBackslash(path: String): String {
    val h = path.indexOf('\\', path.lastIndexOf('\u2060') + 1)
    return if (h < 0) path else path.substring(0, h + 1) + path.substring(h + 1).replace("\\", "\\\u200B")
}

/**
 * Dialogtext vertikal scrollbar (-122 Punkt 1): Titel und Schaltflächen bleiben stehen, der Text ist bei jeder
 * Systemschrift bis zum letzten Wort erreichbar (der Material3-AlertDialog scrollt seinen Textbereich nicht selbst).
 * SYNC-20261111-176 R2: Solange Text überläuft, zeigt ein Scrollbalken am Rand und ein Verlauf am unteren Rand,
 * dass es weitergeht.
 */
@Composable
fun DialogScroll(
    spacing: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberScrollState()
    val bar = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val fade = AlertDialogDefaults.containerColor
    Column(
        Modifier
            .drawWithContent {
                drawContent()
                val max = state.maxValue
                if (max > 0 && max != Int.MAX_VALUE) {
                    val h = size.height
                    val fadeH = 32.dp.toPx().coerceAtMost(h)
                    if (state.canScrollForward) drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, fade), startY = h - fadeH, endY = h),
                        topLeft = Offset(0f, h - fadeH), size = Size(size.width, fadeH),
                    )
                    val thumb = (h * h / (h + max)).coerceAtLeast(24.dp.toPx())
                    val top = (h - thumb) * state.value / max
                    drawRoundRect(bar, Offset(size.width - 4.dp.toPx(), top), Size(3.dp.toPx(), thumb), CornerRadius(2.dp.toPx()))
                }
            }
            .verticalScroll(state),
        verticalArrangement = spacing,
        content = content,
    )
}

/**
 * Knopfspalte für Dialoge mit drei oder mehr Knöpfen (SYNC-20261111-176 R1): gestapelt, je Knopf volle Breite,
 * Reihenfolge von oben nach unten wie übergeben. In den Knopfplatz `confirmButton` setzen, `dismissButton` weglassen.
 */
@Composable
fun DialogButtonColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), content = content)
}

@Composable
fun ColumnScope.StackedButton(text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    // SYNC-20261111-221 B9: Text zentriert, auch wenn er bei grosser Schrift auf mehrere Zeilen umbricht
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text, color = color, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Fehlermeldung im Rotton des Themes (SYNC-20261109-071): gut lesbar in Hell und Dunkel, nur für echte Fehler. */
@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
}

/**
 * Langer Hinweis, aufgeräumt (0.11.0): sichtbar ist nur der erste Satz, der Rest hinter "Mehr dazu".
 * Kurze Texte (bis [limit] Zeichen oder nur ein Satz) erscheinen wie [Hint].
 */
@Composable
fun HintMore(text: String, limit: Int = 120) {
    // Satzende = ". ", "! " oder "? " nach mindestens zwei Zeichen ohne Punkt -> "z. B." und "e.g." trennen nicht
    val cut = SENTENCE_END.find(text)?.range?.first?.minus(1)?.takeIf { it in 1 until text.length - 2 }
    if (text.length <= limit || cut == null) { Hint(text); return }
    var open by rememberSaveable(text) { mutableStateOf(false) }
    Column {
        Hint(if (open) text else text.substring(0, cut + 1))
        Text(
            str(if (open) R.string.common_less else R.string.common_more),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { open = !open }.padding(vertical = 6.dp),
        )
    }
}

private val SENTENCE_END = Regex("(?<=[^\\s.]{2}[.!?]) ")

/** Technischer Fehlertext zum Aufklappen unter einer verständlichen Meldung (0.11.0) */
@Composable
fun ErrorDetails(technical: String) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(
            str(if (open) R.string.common_details_hide else R.string.common_details),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { open = !open }.padding(vertical = 6.dp),
        )
        if (open) Hint(technical)
    }
}

/** Datum + Uhrzeit nach Systemeinstellung (SYNC-20261111-233); nur Uhrzeit: [formatClock], nur Datum: [formatDate] */
fun formatTime(ms: Long): String = TimeFormat.dateTime(ms)

/** Datum + Uhrzeit mit Sekunden, damit Versionen derselben Minute unterscheidbar sind */
fun formatTimeSec(ms: Long): String = TimeFormat.dateTime(ms, seconds = true)

/** Nur Uhrzeit nach Systemeinstellung (12 h/24 h) */
fun formatClock(ms: Long): String = TimeFormat.clock(ms)

/** Nur Datum nach Systemeinstellung */
fun formatDate(ms: Long): String = TimeFormat.date(ms)

/** Monatsnamen in der App-Sprache (Januar … / January …) */
val monthNames: List<String>
    get() = DateFormatSymbols.getInstance(Texts.locale).months.take(12)
