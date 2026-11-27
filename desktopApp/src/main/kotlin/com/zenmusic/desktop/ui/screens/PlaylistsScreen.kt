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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmusic.desktop.ui.AppController
import com.zenmusic.desktop.ui.AppDialog
import com.zenmusic.desktop.ui.ZenText
import com.zenmusic.desktop.ui.ZenTextSecondary
import com.zenmusic.desktop.ui.components.EmptyState
import com.zenmusic.desktop.ui.components.MenuAction
import com.zenmusic.desktop.ui.components.PlaylistRow
import com.zenmusic.desktop.util.chooseImageFile
import com.zenmusic.desktop.util.playlistsWord

@Composable
fun PlaylistsScreen(
    controller: AppController,
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
                    text = "Плейлисты",
                    style = MaterialTheme.typography.displaySmall,
                    color = ZenText,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${controller.playlists.size} ${playlistsWord(controller.playlists.size)}",
                    color = ZenTextSecondary,
                    fontSize = 12.sp,
                )
            }
            Button(
                onClick = { onShowDialog(AppDialog.CreatePlaylist()) },
                shape = RoundedCornerShape(50),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text("Создать", modifier = Modifier.padding(end = 6.dp))
            }
        }

        Box(Modifier.fillMaxSize()) {
            if (controller.playlists.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = "Плейлистов пока нет",
                    subtitle = "Создай первый плейлист, а потом добавляй в него треки через меню ⋮ в списке треков.",
                    actionLabel = "Создать плейлист",
                    onAction = { onShowDialog(AppDialog.CreatePlaylist()) },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(controller.playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            actions = listOf(
                                MenuAction(
                                    label = "Переименовать",
                                    icon = Icons.Rounded.Edit,
                                ) { onShowDialog(AppDialog.RenamePlaylist(playlist.id)) },
                                MenuAction(
                                    label = "Сменить обложку",
                                    icon = Icons.Rounded.Image,
                                ) {
                                    chooseImageFile()?.let { controller.setPlaylistCover(playlist.id, it) }
                                },
                                MenuAction(
                                    label = "Удалить плейлист",
                                    icon = Icons.Rounded.Delete,
                                    danger = true,
                                ) { onShowDialog(AppDialog.DeletePlaylist(playlist.id)) },
                            ),
                            onOpen = { controller.openPlaylist(playlist.id) },
                        )
                    }
                }
            }
        }
    }
}
