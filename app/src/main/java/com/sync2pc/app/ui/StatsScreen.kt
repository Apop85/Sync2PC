@file:OptIn(ExperimentalMaterial3Api::class)

package com.sync2pc.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sync2pc.app.R
import com.sync2pc.app.l10n.plural
import com.sync2pc.app.l10n.str
import com.sync2pc.app.data.AppDatabase
import com.sync2pc.app.data.StatRow
import com.sync2pc.app.sync.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val videoExt = setOf("mp4", "mov", "3gp", "3g2", "mkv", "webm", "avi", "m4v", "mts", "m2ts")

private data class Bucket(val label: String, val count: Int, val bytes: Long)

private data class Stats(
    val files: Int,
    val bytes: Long,
    val photos: Int,
    val videos: Int,
    val years: List<Bucket>,
    val countries: List<Bucket>,
)

private val yearSegment = Regex("""^\d{4}$""")

private fun compute(rows: List<StatRow>): Stats {
    val years = HashMap<String, Pair<Int, Long>>()
    val countries = HashMap<String, Pair<Int, Long>>()
    var videos = 0
    for (r in rows) {
        if (r.sourceKey.substringAfterLast('.', "").lowercase() in videoExt) videos++
        val y = r.year?.toString()
            ?: r.remotePath.split('\\').firstOrNull { yearSegment.matches(it) }
            ?: str(R.string.stats_unknown)
        val c = com.sync2pc.app.geo.CountryLookup.displayName(r.countryIso, r.countryName) ?: str(R.string.stats_no_location)
        years[y] = (years[y]?.first ?: 0) + 1 to (years[y]?.second ?: 0L) + r.size
        countries[c] = (countries[c]?.first ?: 0) + 1 to (countries[c]?.second ?: 0L) + r.size
    }
    return Stats(
        files = rows.size,
        bytes = rows.sumOf { it.size },
        photos = rows.size - videos,
        videos = videos,
        years = years.map { Bucket(it.key, it.value.first, it.value.second) }.sortedByDescending { it.label },
        countries = countries.map { Bucket(it.key, it.value.first, it.value.second) }.sortedByDescending { it.count },
    )
}

/** Statistik: gesicherte Dateien und Speicher pro Jahr und Land. */
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dao = remember { AppDatabase.get(ctx).dao() }
    // Statistik nur für Bildordner-Profile (Jahr/Land)
    val allProfiles by remember { dao.profilesFlow() }.collectAsState(initial = emptyList())
    val profiles = allProfiles.filter { it.type() == com.sync2pc.app.data.ProfileType.PHOTOS }
    var selected by remember { mutableStateOf<Long?>(null) }
    var stats by remember { mutableStateOf<Stats?>(null) }

    LaunchedEffect(profiles) { if (selected == null) selected = profiles.firstOrNull()?.id }
    LaunchedEffect(selected) {
        val id = selected ?: return@LaunchedEffect
        stats = null
        stats = withContext(Dispatchers.IO) { compute(dao.statRows(id)) }
    }

    ScreenScaffold(title = str(R.string.home_cd_stats), onBack = onBack) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (profiles.size > 1) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profiles, key = { it.id }) { p ->
                        FilterChip(selected = p.id == selected, onClick = { selected = p.id }, label = { Text(p.name) })
                    }
                }
            }
            val s = stats
            when {
                profiles.isEmpty() -> item { Text(str(R.string.stats_no_profile)) }
                s == null -> item { CircularProgressIndicator() }
                s.files == 0 -> item { Text(str(R.string.stats_no_files)) }
                else -> {
                    item {
                        SectionCard(str(R.string.stats_total)) {
                            Text(plural(R.plurals.trash_day_files, s.files, s.files, Notifications.formatBytes(s.bytes)),
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(str(R.string.stats_photos_videos, s.photos, s.videos))
                        }
                    }
                    item { BucketCard(str(R.string.stats_per_year), s.years) }
                    item { BucketCard(str(R.string.stats_per_country), s.countries) }
                }
            }
        }
    }
}

@Composable
private fun BucketCard(title: String, buckets: List<Bucket>) {
    val max = buckets.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    SectionCard(title) {
        // Beschriftung (0.9.0): Die Balken richten sich nach der Anzahl, nicht nach der Grösse
        Hint(str(R.string.stats_bar_hint))
        buckets.forEach { b ->
            Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.label, modifier = Modifier.weight(1f))
                    Text(plural(R.plurals.stats_bucket_value, b.count, b.count, Notifications.formatBytes(b.bytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier.fillMaxWidth(b.count.toFloat() / max).height(6.dp)
                            .clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
