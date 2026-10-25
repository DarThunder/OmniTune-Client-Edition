package com.omnitune.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.data.DiscoverItem
import com.omnitune.app.data.Track
import com.omnitune.app.data.net.OmniApi
import com.omnitune.app.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class DownloadUi(
    val status: String,
    val percent: String? = null,
    val done: Boolean = false,
    val error: Boolean = false,
) {
    val active: Boolean get() = !done && !error
}

data class SearchUi(
    val query: String = "",
    val library: List<Track> = emptyList(),
    val discover: List<DiscoverItem> = emptyList(),
    val discoverPage: Int = 1,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    /** Descargas en curso, indexadas por la URL de YouTube. */
    val downloads: Map<String, DownloadUi> = emptyMap(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(private val api: OmniApi) : ViewModel() {

    private val _ui = MutableStateFlow(SearchUi())
    val ui: StateFlow<SearchUi> = _ui.asStateFlow()

    private val queries = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queries.debounce(400.milliseconds).distinctUntilChanged().collectLatest { runSearch(it.trim()) }
        }
    }

    fun onQueryChange(q: String) {
        _ui.update { it.copy(query = q) }
        queries.value = q
    }

    private suspend fun runSearch(q: String) {
        if (q.isEmpty()) {
            _ui.update {
                it.copy(library = emptyList(), discover = emptyList(), hasMore = false, loading = false, error = null)
            }
            return
        }
        _ui.update { it.copy(loading = true, error = null) }
        coroutineScope {
            val lib = async { runCatching { api.search(q) } }
            val dis = async { runCatching { api.discover(q, 1) } }
            val l = lib.await()
            val d = dis.await()
            _ui.update {
                it.copy(
                    loading = false,
                    library = l.getOrDefault(emptyList()),
                    discover = d.getOrNull()?.results.orEmpty(),
                    discoverPage = 1,
                    hasMore = d.getOrNull()?.hasMore == true,
                    error = if (l.isFailure && d.isFailure) l.exceptionOrNull()?.userMessage() else null,
                )
            }
        }
    }

    fun loadMoreDiscover() {
        val s = _ui.value
        if (s.loading || !s.hasMore || s.query.isBlank()) return
        _ui.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val next = s.discoverPage + 1
                val r = api.discover(s.query.trim(), next)
                _ui.update {
                    it.copy(
                        loading = false,
                        discover = it.discover + r.results,
                        discoverPage = next,
                        hasMore = r.hasMore,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    // ---- Descargas (POST /music + polling de /music/progress/{task_id}) ----

    private fun setDownload(url: String, d: DownloadUi) =
        _ui.update { it.copy(downloads = it.downloads + (url to d)) }

    fun download(item: DiscoverItem) {
        val url = item.url ?: return
        if (_ui.value.downloads[url]?.active == true) return
        viewModelScope.launch {
            setDownload(url, DownloadUi("starting", "0%"))
            try {
                val taskId = api.startDownload(url).taskId
                repeat(400) {
                    delay(1500.milliseconds)
                    val p = api.downloadProgress(taskId)
                    val status = (p["status"] ?: "").lowercase()
                    val finished = status == "completed"
                    val failed = status == "error"
                    setDownload(url, DownloadUi(status, p["percent"], done = finished, error = failed))
                    if (finished || failed) {
                        if (finished) runSearch(_ui.value.query.trim())
                        return@launch
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setDownload(url, DownloadUi(e.userMessage(), error = true))
            }
        }
    }
}
