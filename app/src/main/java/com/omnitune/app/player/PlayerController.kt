package com.omnitune.app.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.omnitune.app.data.Repository
import com.omnitune.app.data.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException

data class NowPlaying(val key: String, val title: String, val artist: String, val artwork: String?)

data class PlayerState(
    val now: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: Repository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val queued = mutableListOf<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            if (c.hasNextMediaItem()) {
                c.seekToNextMediaItem()
                c.prepare()
                c.play()
            }
        }
    }

    init {
        scope.launch { connect() }
        scope.launch {
            while (true) {
                delay(500.milliseconds)
                val c = controller ?: continue
                if (c.isPlaying) _state.update { it.copy(positionMs = c.currentPosition.coerceAtLeast(0)) }
            }
        }
    }

    private fun connect() {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(listener)
            sync(c)
            queued.forEach { it(c) }
            queued.clear()
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun withController(block: (MediaController) -> Unit) {
        controller?.let(block) ?: queued.add(block)
    }

    @OptIn(UnstableApi::class)
    private fun sync(p: Player) {
        val item = p.currentMediaItem
        val md = item?.mediaMetadata
        val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: md?.durationMs ?: 0L
        _state.value = PlayerState(
            now = item?.let {
                NowPlaying(
                    key = it.mediaId,
                    title = md?.title?.toString().orEmpty(),
                    artist = md?.artist?.toString().orEmpty(),
                    artwork = md?.artworkUri?.toString(),
                )
            },
            isPlaying = p.isPlaying,
            buffering = p.playbackState == Player.STATE_BUFFERING,
            positionMs = p.currentPosition.coerceAtLeast(0),
            durationMs = duration,
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            hasNext = p.hasNextMediaItem(),
            hasPrevious = p.hasPreviousMediaItem(),
        )
    }

    @OptIn(UnstableApi::class)
    private fun toItem(t: Track) = MediaItem.Builder()
        .setMediaId(t.key)
        .setUri(repo.localUri(t.key) ?: repo.streamUrl(t.key).toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(t.displayTitle)
                .setArtist(t.displayArtist)
                .setArtworkUri(repo.thumbUrl(t.key).toUri())
                .apply { t.durationSeconds?.let { setDurationMs(it * 1000) } }
                .build()
        )
        .build()

    fun play(tracks: List<Track>, startIndex: Int) = withController { c ->
        if (tracks.isEmpty()) return@withController
        c.setMediaItems(tracks.map(::toItem), startIndex.coerceIn(0, tracks.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun next() = withController { it.seekToNextMediaItem() }

    fun previous() = withController {
        if (it.currentPosition > 3_000 || !it.hasPreviousMediaItem()) it.seekTo(0) else it.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun stop() = withController {
        it.stop()
        it.clearMediaItems()
    }
}
