package com.zenmusic.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenmusic.desktop.ui.components.PlayerBar
import com.zenmusic.desktop.ui.components.Sidebar
import com.zenmusic.desktop.ui.dialogs.AddToPlaylistDialog
import com.zenmusic.desktop.ui.dialogs.CreatePlaylistDialog
import com.zenmusic.desktop.ui.dialogs.DeletePlaylistDialog
import com.zenmusic.desktop.ui.dialogs.HiddenTracksDialog
import com.zenmusic.desktop.ui.dialogs.RenamePlaylistDialog
import com.zenmusic.desktop.ui.screens.PlaylistDetailScreen
import com.zenmusic.desktop.ui.screens.PlaylistsScreen
import com.zenmusic.desktop.ui.screens.TracksScreen
import com.zenmusic.desktop.util.chooseMusicFolder

@Composable
fun ZenMusicApp(controller: AppController) {
    ZenMusicTheme {
        var dialog by remember { mutableStateOf<AppDialog?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }

        val chooseFolder: () -> Unit = {
            chooseMusicFolder(controller.musicFolder)?.let(controller::chooseMusicFolder)
        }

        LaunchedEffect(controller.messageId) {
            controller.message?.let { text ->
                snackbarHostState.showSnackbar(
                    message = text,
                    duration = SnackbarDuration.Short,
                )
                controller.consumeMessage()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ZenBackground),
        ) {
            Row(Modifier.fillMaxSize()) {
                Sidebar(
                    controller = controller,
                    onChooseFolder = chooseFolder,
                    onShowHidden = { dialog = AppDialog.HiddenTracks },
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        val playlistId = controller.openPlaylistId
                        when {
                            playlistId != null -> PlaylistDetailScreen(
                                controller = controller,
                                playlistId = playlistId,
                                onShowDialog = { dialog = it },
                            )

                            controller.currentTab == Tab.TRACKS -> TracksScreen(
                                controller = controller,
                                onChooseFolder = chooseFolder,
                                onShowDialog = { dialog = it },
                            )

                            else -> PlaylistsScreen(
                                controller = controller,
                                onShowDialog = { dialog = it },
                            )
                        }
                    }

                    if (controller.currentTrack != null) {
                        PlayerBar(controller)
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = if (controller.currentTrack != null) 132.dp else 24.dp,
                        start = 24.dp,
                        end = 24.dp,
                    ),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = ZenSurfaceElevated,
                    contentColor = ZenText,
                    actionColor = ZenPurple,
                )
            }
        }

        when (val currentDialog = dialog) {
            is AppDialog.CreatePlaylist -> {
                CreatePlaylistDialog(
                    trackToAdd = currentDialog.trackToAdd,
                    onDismiss = { dialog = null },
                    onCreate = { name, cover ->
                        if (controller.createPlaylist(name, cover, currentDialog.trackToAdd) != null) {
                            dialog = null
                        }
                    },
                )
            }

            is AppDialog.RenamePlaylist -> {
                val playlist = controller.playlistById(currentDialog.playlistId)
                if (playlist == null) {
                    dialog = null
                } else {
                    RenamePlaylistDialog(
                        playlist = playlist,
                        onDismiss = { dialog = null },
                        onRename = { name ->
                            controller.renamePlaylist(playlist.id, name)
                            dialog = null
                        },
                    )
                }
            }

            is AppDialog.DeletePlaylist -> {
                val playlist = controller.playlistById(currentDialog.playlistId)
                if (playlist == null) {
                    dialog = null
                } else {
                    DeletePlaylistDialog(
                        playlist = playlist,
                        onDismiss = { dialog = null },
                        onDelete = {
                            controller.deletePlaylist(playlist.id)
                            dialog = null
                        },
                    )
                }
            }

            is AppDialog.AddToPlaylist -> {
                AddToPlaylistDialog(
                    track = currentDialog.track,
                    playlists = controller.playlists,
                    onDismiss = { dialog = null },
                    onSelect = { playlistId ->
                        controller.addTrackToPlaylist(playlistId, currentDialog.track)
                        dialog = null
                    },
                    onCreateNew = {
                        dialog = AppDialog.CreatePlaylist(trackToAdd = currentDialog.track)
                    },
                )
            }

            AppDialog.HiddenTracks -> {
                val hiddenTracks = remember(controller.hiddenPaths, controller.library) {
                    controller.hiddenTracks()
                }
                HiddenTracksDialog(
                    tracks = hiddenTracks,
                    onDismiss = { dialog = null },
                    onRestore = { paths ->
                        controller.restoreTracks(paths)
                        dialog = null
                    },
                    onRestoreAll = {
                        controller.restoreAllTracks()
                        dialog = null
                    },
                )
            }

            null -> Unit
        }
    }
}
