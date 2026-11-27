package com.zenmusic.desktop.ui

import com.zenmusic.desktop.model.Track

sealed interface AppDialog {
    data class CreatePlaylist(val trackToAdd: Track? = null) : AppDialog
    data class RenamePlaylist(val playlistId: Long) : AppDialog
    data class DeletePlaylist(val playlistId: Long) : AppDialog
    data class AddToPlaylist(val track: Track) : AppDialog
    data object HiddenTracks : AppDialog
}
