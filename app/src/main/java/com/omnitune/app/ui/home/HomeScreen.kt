package com.omnitune.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.data.Track
import com.omnitune.app.ui.MainViewModel
import com.omnitune.app.ui.components.Cover
import com.omnitune.app.ui.components.SectionTitle
import com.omnitune.app.ui.components.TrackMenuSheet
import com.omnitune.app.ui.components.TrackRow
import com.omnitune.app.ui.theme.Accent
import com.omnitune.app.ui.theme.SurfaceHigh
import com.omnitune.app.ui.theme.TextPrimary
import com.omnitune.app.ui.theme.Titanium

@Composable
fun HomeScreen(
    main: MainViewModel,
    padding: PaddingValues,
    vm: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    val player by main.playerState.collectAsStateWithLifecycle()
    val downloadedMap by main.downloaded.collectAsStateWithLifecycle()
    val downloadingSet by main.downloading.collectAsStateWithLifecycle()
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    val listState = rememberLazyListState()

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearEnd, ui.items.size) { if (nearEnd && ui.items.isNotEmpty()) vm.loadMore() }

    val quick = ui.items.take(6)
    val shelf = ui.items.drop(6).take(14)

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item {
            Text(
                "Inicio",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        items(quick.chunked(2).size) { row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quick.chunked(2)[row].forEachIndexed { i, t ->
                    QuickTile(
                        track = t,
                        thumbUrl = main.thumbUrl(t.key),
                        onClick = { main.play(ui.items, row * 2 + i) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (quick.chunked(2)[row].size == 1) Box(Modifier.weight(1f))
            }
        }

        if (shelf.isNotEmpty()) {
            item { SectionTitle("Agregado recientemente") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(shelf, key = { i, t -> "$i-${t.key}" }) { i, t ->
                        AlbumCard(
                            track = t,
                            thumbUrl = main.thumbUrl(t.key),
                            onClick = { main.play(ui.items, 6 + i) },
                        )
                    }
                }
            }
        }

        if (ui.items.isNotEmpty()) item { SectionTitle("Toda tu música") }

        itemsIndexed(ui.items, key = { i, t -> "$i-${t.key}" }) { i, t ->
            TrackRow(
                track = t,
                thumbUrl = main.thumbUrl(t.key),
                isCurrent = player.now?.key == t.key,
                isPlaying = player.isPlaying,
                onClick = { main.play(ui.items, i) },
                onMore = { menuTrack = t },
                downloaded = t.key in downloadedMap,
                downloading = t.key in downloadingSet,
                onDownload = { main.downloadTrack(context, t) },
            )
        }

        item {
            when {
                ui.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                }
                ui.error != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { vm.loadMore() }) { Text("Reintentar", color = Accent) }
                }
                ui.items.isEmpty() -> Text(
                    "Tu biblioteca está vacía. Ve a Buscar para descargar música.",
                    color = Titanium,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }

    menuTrack?.let { TrackMenuSheet(it, main, onDismiss = { menuTrack = null }) }
}

@Composable
private fun QuickTile(track: Track, thumbUrl: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceHigh)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(thumbUrl, Modifier.size(56.dp), shape = RoundedCornerShape(0.dp))
        Text(
            track.displayTitle,
            color = TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

@Composable
private fun AlbumCard(track: Track, thumbUrl: String, onClick: () -> Unit) {
    Column(Modifier.width(140.dp).clickable(onClick = onClick)) {
        Cover(thumbUrl, Modifier.size(140.dp))
        Text(
            track.displayTitle,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            track.displayArtist,
            color = Titanium,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
