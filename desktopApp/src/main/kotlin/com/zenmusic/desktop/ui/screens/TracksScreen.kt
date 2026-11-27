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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmusic.desktop.ui.AppController
import com.zenmusic.desktop.ui.AppDialog
import com.zenmusic.desktop.ui.ZenPurple
import com.zenmusic.desktop.ui.ZenText
import com.zenmusic.desktop.ui.ZenTextSecondary
import com.zenmusic.desktop.ui.components.EmptyState
import com.zenmusic.desktop.ui.components.GrownIconButton
import com.zenmusic.desktop.ui.components.MenuAction
import com.zenmusic.desktop.ui.components.SearchField
import com.zenmusic.desktop.ui.components.TrackRow
import com.zenmusic.desktop.util.openInExplorer
import com.zenmusic.desktop.util.tracksWord

@Composable
fun TracksScreen(
    controller: AppController,
    onChooseFolder: () -> Unit,
    onShowDialog: (AppDialog) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 24.dp, top = 22.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Треки",
                    style = MaterialTheme.typography.displaySmall,
                    color = ZenText,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append("${controller.visibleTracks.size} ${tracksWord(controller.visibleTracks.size)}")
                        if (controller.musicFolder != null) {
                            append(" · ")
                            append(controller.folderName())
                        }
                    },
                    color = ZenTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            SearchField(
                value = controller.searchQuery,
                onValueChange = controller::updateSearchQuery,
                placeholder = "Поиск по названию или исполнителю",
                modifier = Modifier.width(300.dp),
            )
            Spacer(Modifier.width(10.dp))
            if (controller.isScanning) {
                Box(Modifier.width(38.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = ZenPurple,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(9.dp),
                    )
                }
            } else {
                GrownIconButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "Обновить библиотеку",
                    onClick = { controller.scanLibrary(silent = false) },
                    tint = ZenTextSecondary,
                )
            }
        }

        if (controller.isScanning) {
            val progress = if (controller.scanTotal > 0) {
                controller.scanDone.toFloat() / controller.scanTotal
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                color = ZenPurple,
                trackColor = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        Box(Modifier.fillMaxSize()) {
            when {
                controller.musicFolder == null && controller.library.isEmpty() && !controller.isScanning -> {
                    EmptyState(
                        icon = Icons.Rounded.MusicNote,
                        title = "Здесь будет музыка",
                        subtitle = "Выбери папку с MP3, FLAC, OGG или WAV — ZenMusic найдёт все треки и прочитает обложки.",
                        actionLabel = "Выбрать папку",
                        onAction = onChooseFolder,
                    )
                }

                controller.library.isEmpty() && controller.isScanning -> {
                    ScanningState(controller)
                }

                controller.visibleTracks.isEmpty() && controller.searchQuery.isNotBlank() -> {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = "Ничего не найдено",
                        subtitle = "По запросу «${controller.searchQuery}» треков нет.",
                        actionLabel = "Сбросить поиск",
                        onAction = { controller.updateSearchQuery("") },
                    )
                }

                controller.visibleTracks.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Rounded.FolderOpen,
                        title = "В папке пусто",
                        subtitle = "В выбранной папке не нашлось поддерживаемых аудиофайлов. Попробуй другую папку.",
                        actionLabel = "Выбрать другую папку",
                        onAction = onChooseFolder,
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(controller.visibleTracks, key = { it.path }) { track ->
                            TrackRow(
                                track = track,
                                isCurrent = controller.currentTrack?.path == track.path,
                                isPlaying = controller.isPlaying,
                                coverPaths = controller.playlistCoversByTrack[track.path].orEmpty(),
                                actions = listOf(
                                    MenuAction(
                                        label = "Добавить в плейлист",
                                        icon = Icons.Rounded.PlaylistAdd,
                                    ) { onShowDialog(AppDialog.AddToPlaylist(track)) },
                                    MenuAction(
                                        label = "Скрыть трек",
                                        icon = Icons.Rounded.VisibilityOff,
                                        danger = true,
                                    ) { controller.hideTrack(track) },
                                    MenuAction(
                                        label = "Показать в проводнике",
                                        icon = Icons.Rounded.FolderOpen,
                                    ) { openInExplorer(track.path) },
                                ),
                                onPlay = {
                                    if (controller.currentTrack?.path == track.path) {
                                        controller.togglePlayPause()
                                    } else {
                                        val queueName = if (controller.searchQuery.isBlank()) {
                                            "Треки"
                                        } else {
                                            "Поиск"
                                        }
                                        controller.playTrack(track, controller.visibleTracks, queueName)
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

@Composable
private fun ScanningState(controller: AppController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = ZenPurple,
            strokeWidth = 3.dp,
            modifier = Modifier.padding(10.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Сканируем музыку...",
            style = MaterialTheme.typography.headlineSmall,
            color = ZenText,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (controller.scanTotal > 0) {
                "${controller.scanDone} из ${controller.scanTotal}"
            } else {
                "Ищем аудиофайлы"
            },
            color = ZenTextSecondary,
            fontSize = 12.5.sp,
        )
        if (controller.scanCurrentFile.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = controller.scanCurrentFile,
                color = ZenTextSecondary.copy(alpha = 0.6f),
                fontSize = 11.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
        }
    }
}
