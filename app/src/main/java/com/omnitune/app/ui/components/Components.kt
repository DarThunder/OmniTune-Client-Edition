package com.omnitune.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.omnitune.app.data.Track
import com.omnitune.app.player.PlayerState
import com.omnitune.app.ui.MainViewModel
import com.omnitune.app.ui.theme.Accent
import com.omnitune.app.ui.theme.OnAccent
import com.omnitune.app.ui.theme.SurfaceHigh
import com.omnitune.app.ui.theme.Titanium
import com.omnitune.app.ui.theme.TextPrimary
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.rounded.ArrowCircleDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun Cover(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    Box(modifier.clip(shape).background(SurfaceHigh), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, null, tint = Titanium, modifier = Modifier.fillMaxSize(0.4f))
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
fun EqualizerBars(playing: Boolean, modifier: Modifier = Modifier, color: Color = Accent) {
    val transition = rememberInfiniteTransition(label = "eq")
    val durations = listOf(450, 620, 530)
    val fractions = durations.mapIndexed { i, ms ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(ms, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(i * 130),
            ),
            label = "bar$i",
        )
    }
    Canvas(modifier.size(14.dp)) {
        val barW = 3.dp.toPx()
        val gap = (size.width - 3 * barW) / 2
        fractions.forEachIndexed { i, f ->
            val h = size.height * if (playing) f.value else 0.2f
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    thumbUrl: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    downloaded: Boolean = false,
    downloading: Boolean = false,
    onDownload: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(thumbUrl, Modifier.size(52.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) {
                    EqualizerBars(playing = isPlaying)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    track.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) Accent else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DownloadDot(downloaded, downloading, onDownload)
                Spacer(Modifier.width(4.dp))
                Text(
                    track.displayArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Titanium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreVert, "Más opciones", tint = Titanium) }
    }
}

@Composable
private fun DownloadDot(downloaded: Boolean, downloading: Boolean, onDownload: (() -> Unit)?) {
    val canTap = !downloaded && !downloading && onDownload != null
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(enabled = canTap) { onDownload?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        when {
            downloading -> CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            downloaded -> Icon(Icons.Rounded.ArrowCircleDown, "Descargada", tint = Accent, modifier = Modifier.size(18.dp))
            else -> Icon(Icons.Outlined.ArrowCircleDown, "Descargar", tint = Titanium.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun FastScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
) {
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    val thumbHeight = 48.dp
    val thumbHeightPx = with(LocalDensity.current) { thumbHeight.toPx() }

    val progress by remember {
        derivedStateOf {
            val total = state.layoutInfo.totalItemsCount
            if (total <= 1) 0f
            else (state.firstVisibleItemIndex.toFloat() / (total - 1)).coerceIn(0f, 1f)
        }
    }

    fun jumpTo(y: Float) {
        val total = state.layoutInfo.totalItemsCount
        if (total == 0 || trackHeightPx <= thumbHeightPx) return
        val fraction = ((y - thumbHeightPx / 2) / (trackHeightPx - thumbHeightPx)).coerceIn(0f, 1f)
        scope.launch { state.scrollToItem((fraction * (total - 1)).roundToInt()) }
    }

    AnimatedVisibility(
        visible = state.isScrollInProgress || dragging,
        enter = fadeIn(),
        exit = fadeOut(tween(durationMillis = 400, delayMillis = 800)),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .padding(top = topPadding, bottom = bottomPadding)
                .fillMaxHeight()
                .width(28.dp)
                .onSizeChanged { trackHeightPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true; jumpTo(it.y) },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, _ -> jumpTo(change.position.y) },
                    )
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, ((trackHeightPx - thumbHeightPx) * progress).roundToInt()) }
                    .padding(end = 4.dp)
                    .size(width = if (dragging) 10.dp else 6.dp, height = thumbHeight)
                    .clip(RoundedCornerShape(50))
                    .background(if (dragging) Accent else Titanium),
            )
        }
    }
}

@Composable
fun MiniPlayer(
    state: PlayerState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = state.now ?: return
    val fraction = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    Surface(
        onClick = onOpen,
        color = SurfaceHigh,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column {
            Row(Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(now.artwork, Modifier.size(44.dp))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(now.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(now.artist, color = Titanium, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onToggle) {
                    Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Reproducir o pausar", tint = TextPrimary)
                }
                IconButton(onClick = onNext, enabled = state.hasNext) {
                    Icon(Icons.Rounded.SkipNext, "Siguiente", tint = if (state.hasNext) TextPrimary else Titanium)
                }
            }
            LinearProgressIndicator(
                progress = { fraction },
                color = Accent,
                trackColor = Titanium.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }
    }
}

@Composable
fun PlayCircle(onClick: () -> Unit, modifier: Modifier = Modifier, size: Int = 56, playing: Boolean = false) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Accent, modifier = modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Reproducir",
                tint = OnAccent,
                modifier = Modifier.size((size * 0.55f).dp),
            )
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, text: String, tint: Color = TextPrimary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = tint)
        Text(text, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuSheet(
    track: Track,
    main: MainViewModel,
    onDismiss: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val downloaded by main.downloaded.collectAsStateWithLifecycle()
    val favs by main.favoriteKeys.collectAsStateWithLifecycle()
    val playlists by main.playlists.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val isFav = track.key in favs

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceHigh) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(main.thumbUrl(track.key), Modifier.size(48.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(track.displayTitle, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.displayArtist, color = Titanium, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Titanium.copy(alpha = 0.2f))

            if (!picking) {
                MenuItem(
                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (isFav) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (isFav) Accent else TextPrimary,
                ) {
                    main.toggleFavorite(track)
                    onDismiss()
                }
                MenuItem(Icons.AutoMirrored.Rounded.PlaylistAdd, "Agregar a playlist") { picking = true }
                if (onRemoveFromPlaylist != null) {
                    MenuItem(Icons.Rounded.RemoveCircleOutline, "Quitar de esta playlist") {
                        onRemoveFromPlaylist()
                        onDismiss()
                    }
                }
                if (track.key !in downloaded) {
                    MenuItem(Icons.Rounded.Download, "Descargar") {
                        main.downloadTrack(context, track)
                        onDismiss()
                    }
                }
            } else {
                MenuItem(Icons.Rounded.Add, "Nueva playlist", tint = Accent) { creating = true }
                playlists.forEach { pl ->
                    MenuItem(Icons.AutoMirrored.Rounded.QueueMusic, pl.name) {
                        main.addToPlaylist(pl, track)
                        onDismiss()
                    }
                }
            }
        }
    }

    if (creating) {
        NewPlaylistDialog(
            onDismiss = { creating = false },
            onCreate = { name ->
                main.createPlaylist(name, thenAdd = track)
                creating = false
                onDismiss()
            },
        )
    }
}

@Composable
fun DownloadAllButton(tracks: List<Track>, main: MainViewModel) {
    val context = LocalContext.current
    val downloaded by main.downloaded.collectAsStateWithLifecycle()
    val bulk by main.bulk.collectAsStateWithLifecycle()
    val b = bulk

    when {
        b != null -> Text(
            "${b.done + 1}/${b.total}",
            color = Accent,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        tracks.isNotEmpty() && tracks.all { it.key in downloaded } ->
            Icon(Icons.Rounded.DownloadDone, "Descargada", tint = Accent, modifier = Modifier.padding(12.dp))
        else -> IconButton(onClick = { main.downloadTracks(context, tracks) }, enabled = tracks.isNotEmpty()) {
            Icon(Icons.Rounded.Download, "Descargar todo", tint = Titanium)
        }
    }
}

@Composable
fun NewPlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceHigh,
        title = { Text("Nueva playlist", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Nombre") },
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name) }, enabled = name.isNotBlank()) {
                Text("Crear", color = if (name.isNotBlank()) Accent else Titanium)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Titanium) } },
    )
}
