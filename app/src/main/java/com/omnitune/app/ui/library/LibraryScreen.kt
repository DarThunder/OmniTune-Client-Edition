package com.omnitune.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.ui.MainViewModel
import com.omnitune.app.ui.components.Cover
import com.omnitune.app.ui.components.NewPlaylistDialog
import com.omnitune.app.ui.theme.Accent
import com.omnitune.app.ui.theme.OnAccent
import com.omnitune.app.ui.theme.TextPrimary
import com.omnitune.app.ui.theme.Titanium

@Composable
fun LibraryScreen(
    main: MainViewModel,
    padding: PaddingValues,
    onOpenFavorites: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val playlists by main.playlists.collectAsStateWithLifecycle()
    val favorites by main.favorites.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val rev by main.coverRev.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tu biblioteca",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                )
                IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, "Nueva playlist", tint = TextPrimary) }
            }
        }

        item {
            LibraryRow(
                title = "Favoritos",
                subtitle = "Playlist · ${favorites.size} canciones",
                onClick = onOpenFavorites,
                leading = {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)).background(Accent),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Favorite, null, tint = OnAccent) }
                },
            )
        }

        items(playlists, key = { it.id }) { pl ->
            LibraryRow(
                title = pl.name,
                subtitle = "Playlist · ${pl.itemCount} canciones",
                onClick = { onOpenPlaylist(pl.id) },
                leading = { Cover(main.playlistCover(pl.id) ?: main.absolute(pl.coverUrl), Modifier.size(56.dp)) },
            )
        }

        item {
            Text(
                "Cerrar sesión",
                color = Titanium,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onLogout).padding(24.dp),
            )
        }
    }

    if (creating) {
        NewPlaylistDialog(
            onDismiss = { creating = false },
            onCreate = {
                main.createPlaylist(it)
                creating = false
            },
        )
    }
}

@Composable
private fun LibraryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Titanium, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
