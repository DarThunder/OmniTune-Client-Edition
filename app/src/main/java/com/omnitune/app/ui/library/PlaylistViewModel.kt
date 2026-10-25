package com.omnitune.app.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.data.PlaylistDetail
import com.omnitune.app.data.Repository
import com.omnitune.app.data.Track
import com.omnitune.app.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUi(
    val detail: PlaylistDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: Repository,
) : ViewModel() {

    private val id: String = checkNotNull(savedState["id"])
    private val _ui = MutableStateFlow(PlaylistUi())
    val ui: StateFlow<PlaylistUi> = _ui.asStateFlow()

    init {
        repo.cachedPlaylist(id)?.let { d -> _ui.update { it.copy(detail = d, loading = false) } }
        load()
    }

    fun load() {
        _ui.update { it.copy(loading = it.detail == null, error = null) }
        viewModelScope.launch {
            try {
                val d = repo.playlist(id)
                _ui.update { it.copy(detail = d, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun remove(track: Track) {
        viewModelScope.launch {
            try {
                repo.removeFromPlaylist(id, track)
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.userMessage()) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                repo.deletePlaylist(id)
                _ui.update { it.copy(deleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.userMessage()) }
            }
        }
    }
}
