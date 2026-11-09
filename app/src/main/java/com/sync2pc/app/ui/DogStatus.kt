package com.sync2pc.app.ui

import android.annotation.SuppressLint
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sync2pc.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Zustandsbild auf der Startseite */
enum class DogState { RUNNING, WAITING, SLEEPING, PAUSED, ATTENTION }

/**
 * Referenzdauer wie im Video (37 Bilder bei 24 fps); steuert nur noch den Statusbogen.
 */
private const val LOOP_MS = 1542L

/** Zeit pro Laufbild (Video-Tempo 100 %, 1542 ms / 37 Bilder); gleich wie flipInterval in notif_dog_flipper.xml. */
private const val FRAME_MS = 42L

// Höhen in dp. Wichtig: Jedes Bild bekommt Höhe UND volle Breite (ContentScale.Fit). Nur mit einer Höhe
// zeichnet Compose ein Bild in seiner Pixelgrösse – so war es bis 0.1.4 (laufender Hund 160 px, sitzender 256 px).
/** Sitzender Hund (wie bisher auf dem Gerät des Inhabers) */
private val SITTING_HEIGHT = 96.dp
/** Schlafender Hund: liegend, flach (wie bisher, ca. 98 dp breit) */
private val SLEEPING_HEIGHT = 50.dp
/** Pausiert (vom Benutzer): Hund auf dem Rücken, Querformat ca. 3:2 -> etwa so breit wie der laufende Hund */
private val PAUSED_HEIGHT = 72.dp
/** Laufender Hund: Das Bild ist quadratisch, der Hund darin ca. 86 % hoch -> etwa so hoch wie der sitzende Hund */
private val RUNNING_HEIGHT = 112.dp
/** Wartet auf dich (0.4.1): stehender Hund, Hochformat -> so hoch wie der Bildbereich */
private val ATTENTION_HEIGHT = 112.dp
/** Bildbereich: immer gleich hoch, damit die Startseite beim Zustandswechsel nicht springt */
private val AREA_HEIGHT = RUNNING_HEIGHT

// Statusfarben des Kreises hinter Leo (SYNC-20261109-054, Orientierung am Herobild der Webseite)
private val COLOR_OK = Color(0xFF3DBE6B)
private val COLOR_WAIT = Color(0xFF7EC8F2)
private val COLOR_PAUSED = Color(0xFF9AA3AE)
private val COLOR_ATTENTION = Color(0xFFE5484D)

private fun circleColor(state: DogState): Color = when (state) {
    DogState.RUNNING, DogState.SLEEPING -> COLOR_OK
    DogState.WAITING -> COLOR_WAIT
    DogState.PAUSED -> COLOR_PAUSED
    DogState.ATTENTION -> COLOR_ATTENTION
}

/**
 * Gefüllter, halbtransparenter Kreis mit Verlauf (innen heller, am Rand dunkler), zwei feinen Ringen und weichem Schein in der
 * Statusfarbe. Laufend: ein Bogen am Rand dreht im Uhrzeigersinn (nur bei sichtbarer Kachel; ohne Systemanimationen steht er).
 * Die Farbe ist nie der einzige Hinweis: Statustext und Leo-Pose bleiben bestehen (der Kreis ist rein dekorativ).
 */
@Composable
private fun StatusCircle(state: DogState) {
    val ctx = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = circleColor(state)
    val animationsOff = remember {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    // Der Bogen läuft nur im Zustand RUNNING; die Übergangsanimation hängt am Frame-Takt und ruht, wenn die App nicht sichtbar ist
    // Der Wert wird erst im Zeichenblock gelesen (nur neu zeichnen, nicht bei jedem Frame neu komponieren; QA Q1)
    val angle: State<Float>? = if (state == DogState.RUNNING && !animationsOff) {
        val t = rememberInfiniteTransition(label = "statusArc")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(LOOP_MS.toInt() * 2, easing = LinearEasing)), label = "arc")
    } else null
    Canvas(Modifier.size(AREA_HEIGHT)) {
        val c = center
        val r = 40.dp.toPx()
        val thin = 1.dp.toPx()
        // weicher Schein
        drawCircle(Brush.radialGradient(listOf(base.copy(alpha = 0.30f), Color.Transparent), c, r * 1.4f), r * 1.4f, c)
        // Füllung: Mitte heller, Rand dunkler, halbtransparent
        drawCircle(
            Brush.radialGradient(listOf(lerp(base, Color.White, 0.35f).copy(alpha = 0.55f), lerp(base, Color.Black, 0.15f).copy(alpha = 0.85f)), c, r),
            r, c,
        )
        // zwei feine Ringe
        drawCircle(base.copy(alpha = 0.40f), r * 1.12f, c, style = Stroke(thin))
        drawCircle(base.copy(alpha = 0.22f), r * 1.24f, c, style = Stroke(thin))
        // Pausiert im Dunkelmodus: helle Kontur, damit das Grau erkennbar bleibt
        if (state == DogState.PAUSED && dark) drawCircle(Color.White.copy(alpha = 0.75f), r, c, style = Stroke(2.5.dp.toPx()))
        // Laufanimation: Bogen am Rand, im Hellmodus dunkler als der Kreis, im Dunkelmodus blass
        if (state == DogState.RUNNING) {
            val arc = if (dark) lerp(base, Color.White, 0.7f) else lerp(base, Color.Black, 0.35f)
            drawArc(
                arc, startAngle = (angle?.value ?: 0f) - 90f, sweepAngle = 90f, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun DogStatus(state: DogState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(AREA_HEIGHT), contentAlignment = Alignment.Center) {
        StatusCircle(state)
        when (state) {
            DogState.RUNNING -> RunningDog(sized(RUNNING_HEIGHT))
            DogState.WAITING -> Image(painterResource(R.drawable.dog_wait), com.sync2pc.app.l10n.str(R.string.dog_waiting), sized(SITTING_HEIGHT))
            DogState.SLEEPING -> Image(painterResource(R.drawable.dog_idle), com.sync2pc.app.l10n.str(R.string.dog_idle), sized(SLEEPING_HEIGHT))
            DogState.PAUSED -> Image(painterResource(R.drawable.dog_pause), com.sync2pc.app.l10n.str(R.string.log_status_paused), sized(PAUSED_HEIGHT))
            DogState.ATTENTION -> Image(painterResource(R.drawable.dog_attention), com.sync2pc.app.l10n.str(R.string.log_status_waiting), sized(ATTENTION_HEIGHT))
        }
    }
}

/** Feste Höhe, Breite frei -> das Bild wird auf diese Höhe skaliert (ContentScale.Fit ist Standard) */
private fun sized(height: Dp) = Modifier.fillMaxWidth().height(height)

/**
 * Laufender Hund. Die Einzelbilder werden einmal im Hintergrund geladen. Die Animation läuft nur,
 * solange die App sichtbar ist (repeatOnLifecycle STARTED) – im Hintergrund kostet sie keinen Akku.
 * Sind Animationen in den Android-Einstellungen aus, bleibt das erste Bild stehen.
 */
@SuppressLint("DiscouragedApi")
@Composable
private fun RunningDog(modifier: Modifier) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var frames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        frames = withContext(Dispatchers.Default) {
            // anim_dog_01, anim_dog_02 … – feste Quell-Assets (16 Bilder, siehe SYNC-20261109-066)
            generateSequence(1) { it + 1 }
                .map { ctx.resources.getIdentifier("anim_dog_%02d".format(it), "drawable", ctx.packageName) }
                .takeWhile { it != 0 }
                .map { ImageBitmap.imageResource(ctx.resources, it) }
                .toList()
        }
    }
    LaunchedEffect(frames) {
        if (frames.size < 2) return@LaunchedEffect
        val animationsOff = Settings.Global.getFloat(
            ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
        if (animationsOff) return@LaunchedEffect
        val frameMs = FRAME_MS
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(frameMs)
                index = (index + 1) % frames.size
            }
        }
    }

    val frame = frames.getOrNull(index)
    if (frame != null) Image(frame, com.sync2pc.app.l10n.str(R.string.dog_syncing), modifier)
    else Image(painterResource(R.drawable.dog_wait), com.sync2pc.app.l10n.str(R.string.dog_syncing), modifier) // bis die Bilder geladen sind
}
