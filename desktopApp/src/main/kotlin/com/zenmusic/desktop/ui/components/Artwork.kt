package com.zenmusic.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenmusic.desktop.data.ArtworkLoader
import com.zenmusic.desktop.model.Playlist
import com.zenmusic.desktop.model.Track
import kotlinx.coroutines.delay
import java.io.File

/**
 * Обложка трека. Если трек лежит в плейлистах с обложками — они плавно
 * сменяют друг друга раз в минуту (как в Android-версии).
 */
@Composable
fun TrackArt(
    track: Track,
    covers: List<String> = emptyList(),
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier,
) {
    var coverIndex by remember(track.path, covers) { mutableIntStateOf(0) }

    LaunchedEffect(track.path, covers) {
        if (covers.size > 1) {
            while (true) {
                delay(60_000)
                coverIndex = (coverIndex + 1) % covers.size
            }
        }
    }

    val artwork by produceState<ImageBitmap?>(initialValue = null, track.path, covers, coverIndex) {
        value = if (covers.isNotEmpty()) {
            ArtworkLoader.loadFile(covers[coverIndex % covers.size])
        } else {
            ArtworkLoader.loadTrack(track)
        }
    }

    ArtworkBox(
        artwork = artwork,
        size = size,
        shape = shape,
        fallback = Icons.Rounded.MusicNote,
        modifier = modifier,
    )
}

@Composable
fun PlaylistCover(
    playlist: Playlist,
    size: Dp = 52.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    modifier: Modifier = Modifier,
) {
    val path = playlist.coverPath
    val stamp = remember(path) {
        path?.let { runCatching { File(it).lastModified() }.getOrDefault(0L) } ?: 0L
    }
    val artwork by produceState<ImageBitmap?>(initialValue = null, path, stamp) {
        value = ArtworkLoader.loadFile(path, stamp)
    }

    ArtworkBox(
        artwork = artwork,
        size = size,
        shape = shape,
        fallback = Icons.Rounded.PlaylistPlay,
        modifier = modifier,
    )
}

@Composable
fun ArtworkBox(
    artwork: ImageBitmap?,
    size: Dp,
    shape: Shape,
    fallback: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0x1FFFFFFF)),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = fallback,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}
