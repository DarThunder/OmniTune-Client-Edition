package com.omnitune.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.data.PlaylistSummary
import com.omnitune.app.data.Repository
import com.omnitune.app.data.SessionManager
import com.omnitune.app.data.Track
import com.omnitune.app.player.PlayerController
import com.omnitune.app.player.PlayerState
import com.omnitune.app.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BulkDownload(val done: Int, val total: Int)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: Repository,
    private val player: PlayerController,
    private val session: SessionManager,
) : ViewModel() {
    private val _downloading = MutableStateFlow<Set<String>>(emptySet())
    val downloading: StateFlow<Set<String>> = _downloading.asStateFlow()
    val downloaded: StateFlow<Map<String, String>> = repo.downloaded
    val loggedIn: StateFlow<Boolean> = session.loggedIn
    val playerState: StateFlow<PlayerState> = player.state
    val favorites: StateFlow<List<Track>> = repo.favorites
    val favoriteKeys: StateFlow<Set<String>> = repo.favoriteKeys
    val playlists: StateFlow<List<PlaylistSummary>> = repo.playlists
    val coverRev: StateFlow<Int> = repo.coverRev

    private val _bulk = MutableStateFlow<BulkDownload?>(null)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val bulk: StateFlow<BulkDownload?> = _bulk.asStateFlow()

    init {
        viewModelScope.launch {
            session.loggedIn.collect { logged ->
                if (logged) refreshLibrary() else player.stop()
            }
        }
    }

    fun downloadTracks(context: Context, tracks: List<Track>) {
        if (_bulk.value != null) return
        val appContext = context.applicationContext
        val pending = tracks
            .filter { it.key.isNotEmpty() && it.key !in downloaded.value }
            .distinctBy { it.key }
        if (pending.isEmpty()) {
            _messages.tryEmit("Ya está todo descargado")
            return
        }
        launchSafe {
            var failed = 0
            try {
                pending.forEachIndexed { i, t ->
                    _bulk.value = BulkDownload(i, pending.size)
                    _downloading.update { it + t.key }
                    try {
                        repo.downloadTrack(appContext, t)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                    } finally {
                        _downloading.update { it - t.key }
                    }
                }
            } finally {
                _bulk.value = null
            }
            _messages.tryEmit(
                if (failed == 0) "Playlist lista para escuchar offline"
                else "Listo, pero fallaron $failed canciones"
            )
        }
    }

    fun thumbUrl(key: String) = repo.thumbUrl(key)
    fun absolute(path: String?) = repo.absolute(path)

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(e.userMessage())
            }
        }
    }

    fun refreshLibrary() = launchSafe {
        repo.refreshFavorites()
        repo.refreshPlaylists()
        repo.syncCovers()
    }

    fun play(tracks: List<Track>, index: Int) = player.play(tracks, index)
    fun togglePlay() = player.togglePlay()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()

    fun downloadTrack(context: Context, track: Track) {
        val key = track.key
        if (key.isEmpty() || key in _downloading.value || key in downloaded.value) return
        val appContext = context.applicationContext
        _downloading.update { it + key }
        launchSafe {
            try {
                repo.downloadTrack(appContext, track)
                _messages.tryEmit("Guardado en Descargas/Omnitune")
            } finally {
                _downloading.update { it - key }
            }
        }
    }
    fun toggleFavorite(track: Track) = launchSafe { repo.toggleFavorite(track) }

    fun toggleCurrentFavorite() {
        val key = playerState.value.now?.key ?: return
        toggleFavorite(Track(hash = key, title = playerState.value.now?.title))
    }

    fun addToPlaylist(playlist: PlaylistSummary, track: Track) = launchSafe {
        repo.addToPlaylist(playlist.id, track)
        _messages.tryEmit("Agregada a ${playlist.name}")
    }

    fun createPlaylist(name: String, thenAdd: Track? = null) = launchSafe {
        val id = repo.createPlaylist(name)
        if (thenAdd != null) {
            repo.addToPlaylist(id, thenAdd)
            _messages.tryEmit("Playlist \"${name.trim()}\" creada con la canción")
        } else {
            _messages.tryEmit("Playlist \"${name.trim()}\" creada")
        }
    }

    fun playlistCover(id: String) = repo.playlistCover(id)

    fun setPlaylistCover(context: Context, id: String, uri: android.net.Uri) = launchSafe {
        repo.uploadCover(context.applicationContext, id, uri)
        _messages.tryEmit("Portada actualizada")
    }

    fun removePlaylistCover(id: String) = launchSafe {
        repo.deleteCover(id)
        _messages.tryEmit("Portada eliminada")
    }

    fun logout() {
        viewModelScope.launch { session.logout() }
    }
}
