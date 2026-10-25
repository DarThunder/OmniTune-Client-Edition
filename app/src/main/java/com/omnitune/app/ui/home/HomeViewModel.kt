package com.omnitune.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.data.Track
import com.omnitune.app.data.net.OmniApi
import com.omnitune.app.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUi(
    val items: List<Track> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    /** null = no hay más páginas. */
    val nextSkip: Int? = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val api: OmniApi) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui.asStateFlow()

    init {
        loadMore()
    }

    fun loadMore() {
        val s = _ui.value
        val skip = s.nextSkip ?: return
        if (s.loading) return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val r = api.feed(skip, 50)
                _ui.update {
                    it.copy(items = it.items + r.feed, nextSkip = r.nextSkip, loading = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun refresh() {
        _ui.value = HomeUi()
        loadMore()
    }
}
