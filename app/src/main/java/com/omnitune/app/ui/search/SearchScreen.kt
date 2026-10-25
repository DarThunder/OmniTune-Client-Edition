package com.omnitune.app.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.data.DiscoverItem
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
import com.omnitune.app.util.formatTime

@Composable
fun SearchScreen(
    main: MainViewModel,
    padding: PaddingValues,
    vm: SearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    val player by main.playerState.collectAsStateWithLifecycle()
    val downloadedMap by main.downloaded.collectAsStateWithLifecycle()
    val downloadingSet by main.downloading.collectAsStateWithLifecycle()
    var menuTrack by remember { mutableStateOf<Track?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item {
            Text(
                "Buscar",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::onQueryChange,
                singleLine = true,
                placeholder = { Text("¿Qué quieres escuchar?", color = Titanium) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Titanium) },
                trailingIcon = {
                    if (ui.query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Rounded.Close, "Borrar", tint = Titanium) }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = SurfaceHigh,
                    focusedContainerColor = SurfaceHigh,
                    unfocusedContainerColor = SurfaceHigh,
                    cursorColor = Accent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }

        if (ui.query.isBlank()) {
            item {
                Text(
                    "Busca en tu biblioteca o descubre música nueva para descargar a tu servidor.",
                    color = Titanium,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }

        if (ui.library.isNotEmpty()) {
            item { SectionTitle("En tu biblioteca") }
            itemsIndexed(ui.library, key = { i, t -> "lib-$i-${t.key}" }) { i, t ->
                TrackRow(
                    track = t,
                    thumbUrl = main.thumbUrl(t.key),
                    isCurrent = player.now?.key == t.key,
                    isPlaying = player.isPlaying,
                    onClick = { main.play(ui.library, i) },
                    onMore = { menuTrack = t },
                    downloaded = t.key in downloadedMap,
                    downloading = t.key in downloadingSet,
                    onDownload = { main.downloadTrack(context, t) },
                )
            }
        }

        if (ui.discover.isNotEmpty()) {
            item { SectionTitle("Descubrir") }
            itemsIndexed(ui.discover, key = { i, d -> "dis-$i-${d.url}" }) { _, d ->
                DiscoverRow(d, ui.downloads[d.url], onDownload = { vm.download(d) })
            }
            if (ui.hasMore) {
                item {
                    TextButton(onClick = vm::loadMoreDiscover, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("Cargar más", color = Accent)
                    }
                }
            }
        }

        item {
            when {
                ui.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                }
                ui.error != null -> Text(ui.error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
                ui.query.isNotBlank() && ui.library.isEmpty() && ui.discover.isEmpty() ->
                    Text("Sin resultados para \"${ui.query.trim()}\"", color = Titanium, modifier = Modifier.padding(24.dp))
            }
        }
    }

    menuTrack?.let { TrackMenuSheet(it, main, onDismiss = { menuTrack = null }) }
}

@Composable
private fun DiscoverRow(item: DiscoverItem, download: DownloadUi?, onDownload: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(item.thumbnail, Modifier.size(52.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                item.title ?: "Sin título",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = buildString {
                append(item.uploader ?: "Desconocido")
                item.durationSeconds?.let { append("  ·  ").append(formatTime(it * 1000)) }
            }
            Text(sub, color = Titanium, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (download != null && !download.done) {
                Text(
                    if (download.error) download.status else "${download.status} ${download.percent.orEmpty()}".trim(),
                    color = if (download.error) MaterialTheme.colorScheme.error else Accent,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            download == null || download.error -> IconButton(onClick = onDownload) {
                Icon(Icons.Rounded.Download, "Descargar al servidor", tint = TextPrimary)
            }
            download.done -> Icon(Icons.Rounded.Check, "Descargada", tint = Accent, modifier = Modifier.padding(12.dp))
            else -> CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), color = Accent, strokeWidth = 2.dp)
        }
    }
}
