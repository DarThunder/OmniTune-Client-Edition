
package com.omnitune.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.omnitune.app.ui.MainViewModel
import com.omnitune.app.ui.components.Cover
import com.omnitune.app.ui.theme.Accent
import com.omnitune.app.ui.theme.Background
import com.omnitune.app.ui.theme.OnAccent
import com.omnitune.app.ui.theme.TextPrimary
import com.omnitune.app.ui.theme.Titanium
import com.omnitune.app.util.formatTime
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.ProgressBarRangeInfo

@Composable
fun NowPlayingScreen(main: MainViewModel, onClose: () -> Unit) {
    val s by main.playerState.collectAsStateWithLifecycle()
    val favs by main.favoriteKeys.collectAsStateWithLifecycle()
    val now = s.now

    if (now == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = s.durationMs.coerceAtLeast(1L)
    val fraction = (dragging ?: (s.positionMs.toFloat() / duration))
        .coerceIn(0f, 1f)
    val isFav = now.key in favs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Cover(
            url = now.artwork,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.15f)
                .blur(48.dp)
                .alpha(0.75f),
            shape = RectangleShape
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Background.copy(alpha = 0.48f),
                            Background.copy(alpha = 0.72f),
                            Background.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.055f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.09f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Cerrar",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Reproduciendo",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(20.dp))

            Cover(
                url = now.artwork,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.28f),
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    ),
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = now.title,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = now.artist,
                        color = Titanium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = main::toggleCurrentFavorite) {
                    Icon(
                        imageVector = if (isFav) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = if (isFav) {
                            "Quitar de favoritos"
                        } else {
                            "Añadir a favoritos"
                        },
                        tint = if (isFav) Accent else TextPrimary,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            ThinProgressSlider(
                value = fraction,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let {
                        main.seekTo((it * duration).toLong())
                    }
                    dragging = null
                },
                enabled = s.durationMs > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime((fraction * duration).toLong()),
                    color = Titanium,
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = if (s.durationMs > 0L) {
                        formatTime(s.durationMs)
                    } else {
                        "--:--"
                    },
                    color = Titanium,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.055f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = main::toggleShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = if (s.shuffle) {
                            "Desactivar aleatorio"
                        } else {
                            "Activar aleatorio"
                        },
                        tint = if (s.shuffle) Accent else Titanium
                    )
                }

                IconButton(onClick = main::previous) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Surface(
                    onClick = main::togglePlay,
                    shape = CircleShape,
                    color = Accent,
                    modifier = Modifier.size(68.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (s.buffering && !s.isPlaying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(29.dp),
                                color = OnAccent,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (s.isPlaying) {
                                    Icons.Rounded.Pause
                                } else {
                                    Icons.Rounded.PlayArrow
                                },
                                contentDescription = if (s.isPlaying) {
                                    "Pausar"
                                } else {
                                    "Reproducir"
                                },
                                tint = OnAccent,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = main::next,
                    enabled = s.hasNext
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente",
                        tint = if (s.hasNext) TextPrimary else Titanium,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(onClick = main::cycleRepeat) {
                    Icon(
                        imageVector = if (
                            s.repeatMode == Player.REPEAT_MODE_ONE
                        ) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = when (s.repeatMode) {
                            Player.REPEAT_MODE_ONE -> "Repetir canción"
                            Player.REPEAT_MODE_ALL -> "Repetir lista"
                            else -> "Activar repetición"
                        },
                        tint = if (
                            s.repeatMode == Player.REPEAT_MODE_OFF
                        ) Titanium else Accent
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ThinProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val progress = value.coerceIn(0f, 1f)
    val activeColor = Accent
    val inactiveColor = Color.White.copy(alpha = 0.27f)
    val thumbColor = Color.White

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = "Progreso de reproducción"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f
                )
                setProgress { target ->
                    if (enabled) {
                        onValueChange(target.coerceIn(0f, 1f))
                        onValueChangeFinished()
                        true
                    } else {
                        false
                    }
                }
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { offset ->
                        val newValue =
                            (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                        onValueChangeFinished()
                    }
                }
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragEnd = onValueChangeFinished,
                        onDragCancel = onValueChangeFinished
                    ) { change, _ ->
                        change.consume()
                        val newValue =
                            (change.position.x / size.width)
                                .coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                }
            }
    ) {
        val centerY = size.height / 2f
        val trackHeight = 2.dp.toPx()
        val thumbRadius = 3.5.dp.toPx()
        val thumbX = size.width * progress

        drawRoundRect(
            color = inactiveColor,
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )

        if (thumbX > 0f) {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(thumbX, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )
        }

        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = Offset(thumbX.coerceIn(thumbRadius, size.width - thumbRadius), centerY)
        )
    }
}
