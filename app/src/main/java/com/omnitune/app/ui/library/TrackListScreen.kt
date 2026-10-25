package com.omnitune.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.data.Track
import com.omnitune.app.ui.MainViewModel
import com.omnitune.app.ui.components.Cover
import com.omnitune.app.ui.components.FastScrollbar
import com.omnitune.app.ui.components.PlayCircle
import com.omnitune.app.ui.components.TrackMenuSheet
import com.omnitune.app.ui.components.TrackRow
import com.omnitune.app.ui.theme.Accent
import com.omnitune.app.ui.theme.Background
import com.omnitune.app.ui.theme.OnAccent
import com.omnitune.app.ui.theme.SurfaceHigh
import com.omnitune.app.ui.theme.TextPrimary
import com.omnitune.app.ui.theme.Titanium

@Composable
fun TrackListScreen(
    title: String,
    subtitle: String?,
    coverUrl: String?,
    isFavoritesList: Boolean,
    tracks: List<Track>,
    main: MainViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    loading: Boolean = false,
    error: String? = null,
    emptyText: String = "Aún no hay canciones aquí.",
    topActions: @Composable RowScope.() -> Unit = {},
    onRemove: ((Track) -> Unit)? = null,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val downloadingSet by main.downloading.collectAsStateWithLifecycle()
    val player by main.playerState.collectAsStateWithLifecycle()
    val downloadedMap by main.downloaded.collectAsStateWithLifecycle()
    var menuTrack by remember { mutableStateOf<Track?>(null) }

    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(tracks, query) {
        val q = query.trim()
        if (q.isEmpty()) tracks
        else tracks.filter {
            it.title?.contains(q, ignoreCase = true) == true || it.uploader?.contains(q, ignoreCase = true) == true
        }
    }
    val isFiltering = query.isNotBlank()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(SurfaceHigh, Background)))
                        .padding(top = padding.calculateTopPadding()),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = TextPrimary) }
                        Box(Modifier.weight(1f))
                        topActions()
                    }
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        if (isFavoritesList) {
                            Box(
                                Modifier.size(180.dp).clip(RoundedCornerShape(8.dp)).background(Accent),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Favorite, null, tint = OnAccent, modifier = Modifier.size(72.dp)) }
                        } else {
                            Cover(coverUrl, Modifier.size(180.dp), shape = RoundedCornerShape(8.dp))
                        }
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(subtitle, color = Titanium, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                    }
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PlayCircle(onClick = { main.play(filtered, 0) })
                        IconButton(onClick = { main.play(filtered.shuffled(), 0) }) {
                            Icon(Icons.Rounded.Shuffle, "Aleatorio", tint = TextPrimary, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            if (tracks.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Buscar en esta lista", color = Titanium) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Titanium) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Borrar", tint = Titanium) }
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            itemsIndexed(filtered, key = { i, t -> "$i-${t.key}" }) { i, t ->
                TrackRow(
                    track = t,
                    thumbUrl = main.thumbUrl(t.key),
                    isCurrent = player.now?.key == t.key,
                    isPlaying = player.isPlaying,
                    onClick = {
                        focusManager.clearFocus()
                        main.play(filtered, i)
                    },
                    onMore = { menuTrack = t },
                    downloaded = t.key in downloadedMap,
                    downloading = t.key in downloadingSet,
                    onDownload = { main.downloadTrack(context, t) },
                )
            }

            item {
                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                    }
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
                    tracks.isEmpty() -> Text(emptyText, color = Titanium, modifier = Modifier.padding(24.dp))
                    isFiltering && filtered.isEmpty() ->
                        Text("Sin resultados para \"${query.trim()}\"", color = Titanium, modifier = Modifier.padding(24.dp))
                }
            }
        }

        FastScrollbar(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd),
            topPadding = padding.calculateTopPadding(),
            bottomPadding = padding.calculateBottomPadding(),
        )
    }

    menuTrack?.let { t ->
        TrackMenuSheet(
            track = t,
            main = main,
            onDismiss = { menuTrack = null },
            onRemoveFromPlaylist = onRemove?.let { remove -> { remove(t) } },
        )
    }
}
