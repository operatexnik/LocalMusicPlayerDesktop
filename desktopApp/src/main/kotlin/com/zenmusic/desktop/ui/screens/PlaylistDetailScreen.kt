package com.zenmusic.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmusic.desktop.model.Track
import com.zenmusic.desktop.ui.AppController
import com.zenmusic.desktop.ui.AppDialog
import com.zenmusic.desktop.ui.ZenPurple
import com.zenmusic.desktop.ui.ZenText
import com.zenmusic.desktop.ui.ZenTextSecondary
import com.zenmusic.desktop.ui.components.EmptyState
import com.zenmusic.desktop.ui.components.GrownIconButton
import com.zenmusic.desktop.ui.components.MenuAction
import com.zenmusic.desktop.ui.components.PlaylistCover
import com.zenmusic.desktop.ui.components.SearchField
import com.zenmusic.desktop.ui.components.TrackRow
import com.zenmusic.desktop.util.normalizeForSearch
import com.zenmusic.desktop.util.openInExplorer
import com.zenmusic.desktop.util.tracksWord

@Composable
fun PlaylistDetailScreen(
    controller: AppController,
    playlistId: Long,
    onShowDialog: (AppDialog) -> Unit,
) {
    val playlist = controller.playlistById(playlistId)
    if (playlist == null) {
        LaunchedEffect(playlistId) { controller.closePlaylist() }
        return
    }

    val allTracks = controller.playlistTracks(playlist)
    val query = normalizeForSearch(controller.playlistSearchQuery)
    val shownTracks: List<Track> = if (query.isEmpty()) {
        allTracks
    } else {
        allTracks.filter { track ->
            normalizeForSearch(track.title).contains(query) ||
                normalizeForSearch(track.artist).contains(query) ||
                normalizeForSearch(track.album).contains(query)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 24.dp, top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GrownIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Назад к плейлистам",
                onClick = controller::closePlaylist,
                buttonSize = 42.dp,
                iconSize = 22.dp,
            )
            Spacer(Modifier.width(10.dp))
            PlaylistCover(
                playlist = playlist,
                size = 64.dp,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ZenText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("${allTracks.size} ${tracksWord(allTracks.size)}")
                        val missing = playlist.trackPaths.size - allTracks.size
                        if (missing > 0) {
                            append(" · $missing файл(ов) не найдено")
                        }
                    },
                    color = ZenTextSecondary,
                    fontSize = 12.sp,
                )
            }
            SearchField(
                value = controller.playlistSearchQuery,
                onValueChange = controller::updatePlaylistSearchQuery,
                placeholder = "Поиск в плейлисте",
                modifier = Modifier.width(260.dp),
            )
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { controller.playQueue(shownTracks, 0, playlist.name) },
                enabled = shownTracks.isNotEmpty(),
                shape = RoundedCornerShape(50),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(18.dp),
                )
                Text("Играть", modifier = Modifier.padding(end = 6.dp))
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                allTracks.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Rounded.PlaylistPlay,
                        title = "Плейлист пуст",
                        subtitle = "Добавляй треки через меню ⋮ в общем списке треков — они появятся здесь.",
                        actionLabel = "К трекам",
                        onAction = { controller.selectTab(com.zenmusic.desktop.ui.Tab.TRACKS) },
                    )
                }

                shownTracks.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = "Ничего не найдено",
                        subtitle = "В этом плейлисте нет треков по запросу «${controller.playlistSearchQuery}».",
                        actionLabel = "Сбросить поиск",
                        onAction = { controller.updatePlaylistSearchQuery("") },
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(shownTracks, key = { it.path }) { track ->
                            TrackRow(
                                track = track,
                                isCurrent = controller.currentTrack?.path == track.path,
                                isPlaying = controller.isPlaying,
                                coverPaths = controller.playlistCoversByTrack[track.path].orEmpty(),
                                actions = listOf(
                                    MenuAction(
                                        label = "Удалить из плейлиста",
                                        icon = Icons.Rounded.DeleteSweep,
                                        danger = true,
                                    ) { controller.removeTrackFromPlaylist(playlist.id, track) },
                                    MenuAction(
                                        label = "Показать в проводнике",
                                        icon = Icons.Rounded.FolderOpen,
                                    ) { openInExplorer(track.path) },
                                    MenuAction(
                                        label = "Добавить в другой плейлист",
                                        icon = Icons.Rounded.PlaylistPlay,
                                    ) { onShowDialog(AppDialog.AddToPlaylist(track)) },
                                ),
                                onPlay = {
                                    if (controller.currentTrack?.path == track.path) {
                                        controller.togglePlayPause()
                                    } else {
                                        controller.playTrack(track, shownTracks, playlist.name)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
