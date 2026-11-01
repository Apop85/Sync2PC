package com.sync2pc.app.ui

import android.annotation.SuppressLint
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sync2pc.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Zustandsbild auf der Startseite */
enum class DogState { RUNNING, WAITING, SLEEPING }

/** Gleiches Tempo wie die Animation in der Benachrichtigung (notif_dog_flipper.xml) */
private const val FRAME_MS = 48L

@Composable
fun DogStatus(state: DogState, modifier: Modifier = Modifier) {
    val m = modifier.height(96.dp)
    when (state) {
        DogState.RUNNING -> RunningDog(m)
        DogState.WAITING -> Image(painterResource(R.drawable.dog_wait), "Wartet", m)
        DogState.SLEEPING -> Image(painterResource(R.drawable.dog_idle), "Alles gesichert", m)
    }
}

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
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(FRAME_MS)
                index = (index + 1) % frames.size
            }
        }
    }

    val frame = frames.getOrNull(index)
    if (frame != null) Image(frame, "Synchronisiert", modifier)
    else Image(painterResource(R.drawable.dog_wait), "Synchronisiert", modifier) // bis die Bilder geladen sind
}
