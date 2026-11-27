package com.zenmusic.desktop.model

import kotlinx.serialization.Serializable

/** Режим воспроизведения — как кнопка повтора в Android-версии. */
@Serializable
enum class PlayMode {
    ALL,
    ONE,
    SHUFFLE,
}

/** Плейлист. Треки хранятся абсолютными путями к файлам. */
@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val coverPath: String? = null,
    val trackPaths: List<String> = emptyList(),
)

/** Трек локальной библиотеки. */
data class Track(
    val path: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val lastModified: Long = 0L,
) {
    val id: String get() = path

    val displayArtist: String
        get() = artist.ifBlank { "Неизвестный исполнитель" }
}

/** То, что сохраняется на диск между запусками. */
@Serializable
data class PersistedState(
    val musicFolder: String? = null,
    val playlists: List<Playlist> = emptyList(),
    val hiddenPaths: List<String> = emptyList(),
    val volume: Float = 0.8f,
    val playMode: PlayMode = PlayMode.ALL,
)
