package com.zenmusic.desktop.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmusic.desktop.model.PlayMode
import com.zenmusic.desktop.ui.AppController
import com.zenmusic.desktop.ui.ZenDivider
import com.zenmusic.desktop.ui.ZenError
import com.zenmusic.desktop.ui.ZenPurple
import com.zenmusic.desktop.ui.ZenSurface
import com.zenmusic.desktop.ui.ZenText
import com.zenmusic.desktop.ui.ZenTextSecondary
import com.zenmusic.desktop.util.formatTime

@Composable
fun PlayerBar(controller: AppController, modifier: Modifier = Modifier) {
    val track = controller.currentTrack ?: return

    val duration = maxOf(controller.durationMs, track.durationMs).coerceAtLeast(1L)
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = (controller.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val shownFraction = if (dragging) dragFraction else currentFraction
    val displayedPosition = (shownFraction * duration).toLong().coerceIn(0L, duration)
    val covers = controller.playlistCoversByTrack[track.path].orEmpty()

    LaunchedEffect(track.path) {
        dragging = false
        dragFraction = currentFraction
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp)
            .background(ZenSurface),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ZenDivider),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ---------- Текущий трек ----------
            Row(
                modifier = Modifier.weight(0.95f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackArt(
                    track = track,
                    covers = covers,
                    size = 58.dp,
                    shape = RoundedCornerShape(11.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = ZenText,
                        fontSize = 14.4.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (controller.queueTitle.isNotBlank()) {
                            "${track.displayArtist} · ${controller.queueTitle}"
                        } else {
                            track.displayArtist
                        },
                        color = ZenTextSecondary,
                        fontSize = 11.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ---------- Управление и перемотка ----------
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    GrownIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Предыдущий трек",
                        onClick = controller::previous,
                        buttonSize = 36.dp,
                        iconSize = 22.dp,
                    )
                    Spacer(Modifier.width(4.dp))
                    PlayPauseButton(
                        isPlaying = controller.isPlaying,
                        onClick = controller::togglePlayPause,
                    )
                    Spacer(Modifier.width(4.dp))
                    GrownIconButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Следующий трек",
                        onClick = controller::next,
                        buttonSize = 36.dp,
                        iconSize = 22.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    GrownIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Остановить и очистить",
                        onClick = { controller.stopPlayback(clearTrack = true) },
                        buttonSize = 30.dp,
                        iconSize = 17.dp,
                        tint = ZenTextSecondary,
                    )
                }

                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTime(displayedPosition),
                        color = ZenTextSecondary,
                        fontSize = 11.2.sp,
                        modifier = Modifier.width(44.dp),
                    )
                    ZenSlider(
                        fraction = shownFraction,
                        onFractionChange = {
                            dragging = true
                            dragFraction = it
                        },
                        onValueChangeFinished = {
                            val target = (dragFraction * duration).toLong()
                            dragging = false
                            controller.seekTo(target)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatTime(duration),
                        color = ZenTextSecondary,
                        fontSize = 11.2.sp,
                        modifier = Modifier.width(48.dp),
                    )
                }
            }

            // ---------- Режим и громкость ----------
            Row(
                modifier = Modifier.weight(0.95f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (modeIcon, modeLabel, modeTint) = when (controller.playMode) {
                    PlayMode.ALL -> Triple(Icons.Rounded.Repeat, "Повтор: все треки", ZenTextSecondary)
                    PlayMode.ONE -> Triple(Icons.Rounded.RepeatOne, "Повтор: один трек", ZenPurple)
                    PlayMode.SHUFFLE -> Triple(Icons.Rounded.Shuffle, "Перемешивание", ZenPurple)
                }
                GrownIconButton(
                    icon = modeIcon,
                    contentDescription = modeLabel,
                    onClick = controller::cyclePlayMode,
                    tint = modeTint,
                    buttonSize = 34.dp,
                    iconSize = 19.dp,
                )
                Spacer(Modifier.width(10.dp))
                val volumeIcon = when {
                    controller.volume <= 0.001f -> Icons.Rounded.VolumeMute
                    controller.volume < 0.5f -> Icons.Rounded.VolumeDown
                    else -> Icons.Rounded.VolumeUp
                }
                Icon(
                    imageVector = volumeIcon,
                    contentDescription = "Громкость",
                    tint = ZenTextSecondary,
                    modifier = Modifier.size(19.dp),
                )
                ZenSlider(
                    fraction = controller.volume,
                    onFractionChange = controller::updateVolume,
                    onValueChangeFinished = controller::persistVolume,
                    activeColor = ZenText.copy(alpha = 0.75f),
                    thumbColor = ZenText,
                    modifier = Modifier
                        .width(112.dp)
                        .padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(ZenPurple)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Пауза" else "Играть",
            tint = Color(0xFF1A0024),
            modifier = Modifier.size(26.dp),
        )
    }
}
