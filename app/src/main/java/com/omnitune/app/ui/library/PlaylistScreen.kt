package com.omnitune.app.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.ui.MainViewModel
import com.omnitune.app.ui.components.DownloadAllButton
import com.omnitune.app.ui.theme.Titanium

@Composable
fun PlaylistScreen(
    main: MainViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    vm: PlaylistViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.deleted) { if (ui.deleted) onBack() }
    val context = LocalContext.current
    val rev by main.coverRev.collectAsStateWithLifecycle()
    val playlistId = ui.detail?.id
    val localCover = remember(rev, playlistId) { playlistId?.let(main::playlistCover) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && playlistId != null) main.setPlaylistCover(context, playlistId, uri)
    }

    TrackListScreen(
        title = ui.detail?.name ?: "Playlist",
        subtitle = ui.detail?.let { d ->
            listOfNotNull(d.description.takeIf { it.isNotBlank() }, "${d.items.size} canciones").joinToString(" · ")
        },
        coverUrl = localCover ?: main.absolute(ui.detail?.coverUrl),
        isFavoritesList = false,
        tracks = ui.detail?.items.orEmpty(),
        main = main,
        padding = padding,
        onBack = onBack,
        loading = ui.loading,
        error = ui.error,
        emptyText = "Esta playlist está vacía. Agrega canciones desde el menú ⋮ de cualquier tema.",
        topActions = {
            if (playlistId != null) {
                IconButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Icon(Icons.Rounded.Image, "Cambiar portada", tint = Titanium) }
                if (localCover != null) {
                    IconButton(onClick = { main.removePlaylistCover(playlistId) }) {
                        Icon(Icons.Rounded.HideImage, "Quitar portada", tint = Titanium)
                    }
                }
            }
            DownloadAllButton(ui.detail?.items.orEmpty(), main)
            IconButton(onClick = vm::delete) { Icon(Icons.Rounded.Delete, "Eliminar playlist", tint = Titanium) }
        },
        onRemove = vm::remove,
    )
}
