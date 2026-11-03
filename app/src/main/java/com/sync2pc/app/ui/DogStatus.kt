package com.sync2pc.app.ui

import android.annotation.SuppressLint
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
 * Dauer einer Schleife wie im Video (37 Bilder bei 24 fps, siehe tools/make-anim-video.ps1).
 * Die Zeit pro Bild ergibt sich aus der Anzahl Einzelbilder – gleiches Tempo wie in der Benachrichtigung.
 */
private const val LOOP_MS = 1542L

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

@Composable
fun DogStatus(state: DogState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(AREA_HEIGHT), contentAlignment = Alignment.Center) {
        when (state) {
            DogState.RUNNING -> RunningDog(sized(RUNNING_HEIGHT))
            DogState.WAITING -> Image(painterResource(R.drawable.dog_wait), "Wartet", sized(SITTING_HEIGHT))
            DogState.SLEEPING -> Image(painterResource(R.drawable.dog_idle), "Alles gesichert", sized(SLEEPING_HEIGHT))
            DogState.PAUSED -> Image(painterResource(R.drawable.dog_pause), "Pausiert", sized(PAUSED_HEIGHT))
            DogState.ATTENTION -> Image(painterResource(R.drawable.dog_attention), "Wartet auf dich", sized(ATTENTION_HEIGHT))
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
            // anim_dog_01, anim_dog_02 … – die Anzahl hängt von tools/make-anim-video.ps1 ab
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
        val frameMs = (LOOP_MS / frames.size).coerceAtLeast(16L)
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(frameMs)
                index = (index + 1) % frames.size
            }
        }
    }

    val frame = frames.getOrNull(index)
    if (frame != null) Image(frame, "Synchronisiert", modifier)
    else Image(painterResource(R.drawable.dog_wait), "Synchronisiert", modifier) // bis die Bilder geladen sind
}
