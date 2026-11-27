package com.zenmusic.desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zenmusic.desktop.audio.AudioEngine
import com.zenmusic.desktop.data.AppStorage
import com.zenmusic.desktop.data.MusicScanner
import com.zenmusic.desktop.model.PlayMode
import com.zenmusic.desktop.model.PersistedState
import com.zenmusic.desktop.model.Playlist
import com.zenmusic.desktop.model.Track
import com.zenmusic.desktop.util.normalizeForSearch
import com.zenmusic.desktop.util.tracksWord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

enum class Tab { TRACKS, PLAYLISTS }

class AppController(private val scope: CoroutineScope) {

    private val stored: PersistedState = AppStorage.load()

    // ---------- Библиотека ----------
    var musicFolder by mutableStateOf(stored.musicFolder)
        private set

    /** Все найденные треки, включая скрытые (они нужны плейлистам). */
    var library by mutableStateOf<List<Track>>(emptyList())
        private set

    var libraryByPath by mutableStateOf<Map<String, Track>>(emptyMap())
        private set

    /** То, что видно в списке треков после поиска и скрытия. */
    var visibleTracks by mutableStateOf<List<Track>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isScanning by mutableStateOf(false)
        private set

    var scanDone by mutableIntStateOf(0)
        private set

    var scanTotal by mutableIntStateOf(0)
        private set

    var scanCurrentFile by mutableStateOf("")
        private set

    // ---------- Навигация ----------
    var currentTab by mutableStateOf(Tab.TRACKS)
        private set

    var openPlaylistId by mutableStateOf<Long?>(null)
        private set

    var playlistSearchQuery by mutableStateOf("")
        private set

    // ---------- Плейлисты ----------
    var playlists by mutableStateOf(stored.playlists)
        private set

    var hiddenPaths by mutableStateOf(stored.hiddenPaths.toSet())
        private set

    /** path -> список обложек плейлистов, в которых состоит трек. */
    var playlistCoversByTrack by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set

    // ---------- Плеер ----------
    val engine = AudioEngine()

    var currentTrack by mutableStateOf<Track?>(null)
        private set

    var queue by mutableStateOf<List<Track>>(emptyList())
        private set

    var queueTitle by mutableStateOf("")
        private set

    var currentQueueIndex by mutableIntStateOf(-1)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var positionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    var volume by mutableFloatStateOf(stored.volume.coerceIn(0f, 1f))
        private set

    var playMode by mutableStateOf(stored.playMode)
        private set

    // ---------- Сообщения ----------
    var message by mutableStateOf<String?>(null)
        private set

    var messageId by mutableIntStateOf(0)
        private set

    init {
        engine.setVolume(volume)
        engine.onFinished = {
            scope.launch(Dispatchers.Main) { handleTrackFinished() }
        }
        engine.onError = { text ->
            scope.launch(Dispatchers.Main) {
                isPlaying = false
                showMessage(text)
            }
        }

        refreshPlaylistCovers()

        // Позиция для полосы прогресса.
        scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (isPlaying) {
                    val enginePosition = engine.currentPositionMs
                    if (enginePosition >= 0) positionMs = enginePosition
                    if (durationMs <= 0L && engine.durationMs > 0L) {
                        durationMs = engine.durationMs
                    }
                }
                delay(150)
            }
        }

        if (musicFolder != null || playlists.isNotEmpty() || hiddenPaths.isNotEmpty()) {
            scanLibrary(silent = true)
        }
    }

    // =====================================================================
    // Библиотека
    // =====================================================================

    fun chooseMusicFolder(folder: File) {
        if (!folder.isDirectory) return
        musicFolder = folder.absolutePath
        persist()
        scanLibrary(silent = false)
    }

    fun scanLibrary(silent: Boolean = false) {
        if (isScanning) return
        val folder = musicFolder?.let(::File)
        val extraPaths = buildSet {
            playlists.forEach { addAll(it.trackPaths) }
            addAll(hiddenPaths)
        }
        if (folder == null && extraPaths.isEmpty()) {
            if (!silent) showMessage("Сначала выберите папку с музыкой")
            return
        }

        isScanning = true
        scanDone = 0
        scanTotal = 0
        scanCurrentFile = ""

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                MusicScanner.scan(folder, extraPaths) { done, total, current ->
                    // Не дёргаем UI на каждый файл — для больших библиотек это лишнее.
                    if (done == 0 || done % 5 == 0 || done >= total - 1) {
                        scope.launch(Dispatchers.Main) {
                            scanDone = done
                            scanTotal = total
                            scanCurrentFile = current
                        }
                    }
                }
            }

            library = result.tracks
            libraryByPath = result.tracks.associateBy { it.path }
            applyFilters()

            // Обновляем объект текущего трека, если метаданные изменились.
            currentTrack?.let { old ->
                libraryByPath[old.path]?.let { currentTrack = it }
            }

            isScanning = false
            scanCurrentFile = ""

            if (!silent) {
                showMessage("Найдено: ${result.tracks.size} ${tracksWord(result.tracks.size)}")
            }
            if (result.failedFiles > 0) {
                showMessage("Не удалось прочитать ${result.failedFiles} файл(ов)")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        val query = normalizeForSearch(searchQuery)
        var list = library.filter { it.path !in hiddenPaths }
        if (query.isNotEmpty()) {
            list = list.filter { track ->
                normalizeForSearch(track.title).contains(query) ||
                    normalizeForSearch(track.artist).contains(query) ||
                    normalizeForSearch(track.album).contains(query)
            }
        }
        visibleTracks = list
    }

    fun folderName(): String {
        val folder = musicFolder ?: return "Папка не выбрана"
        return runCatching { File(folder).name.ifBlank { folder } }.getOrDefault(folder)
    }

    // =====================================================================
    // Воспроизведение
    // =====================================================================

    fun playTrack(track: Track, sourceQueue: List<Track> = visibleTracks, sourceName: String = "Треки") {
        val list = sourceQueue.ifEmpty { listOf(track) }
        val index = list.indexOfFirst { it.path == track.path }.let { if (it < 0) 0 else it }
        queue = list
        queueTitle = sourceName
        startIndex(index)
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0, sourceName: String) {
        if (tracks.isEmpty()) return
        queue = tracks
        queueTitle = sourceName
        startIndex(startIndex.coerceIn(0, tracks.lastIndex))
    }

    private fun startIndex(index: Int) {
        val track = queue.getOrNull(index) ?: return
        currentQueueIndex = index
        currentTrack = track
        durationMs = track.durationMs
        positionMs = 0L
        isPlaying = true
        engine.play(track.path, 0L)
    }

    fun togglePlayPause() {
        val track = currentTrack ?: return
        when {
            engine.isPlaying -> {
                engine.pause()
                isPlaying = false
                positionMs = engine.currentPositionMs
            }

            engine.isPaused -> {
                engine.resume()
                isPlaying = true
            }

            else -> {
                // Плеер остановился/закончил — начинаем заново.
                if (currentQueueIndex in queue.indices) startIndex(currentQueueIndex)
                else playTrack(track)
            }
        }
    }

    fun next() {
        if (queue.isEmpty()) return
        val nextIndex = if (currentQueueIndex >= queue.lastIndex) 0 else currentQueueIndex + 1
        startIndex(nextIndex)
    }

    fun previous() {
        if (queue.isEmpty()) return
        // Всегда переключаем на предыдущий трек, даже если текущий играет
        // больше 2–3 секунд. Перемотка в начало — это отдельное действие.
        val prevIndex = if (currentQueueIndex <= 0) queue.lastIndex else currentQueueIndex - 1
        startIndex(prevIndex)
    }

    fun seekTo(milliseconds: Long) {
        val track = currentTrack ?: return
        val max = if (durationMs > 0) durationMs else track.durationMs
        val target = milliseconds.coerceIn(0L, if (max > 0) max else milliseconds)
        positionMs = target
        engine.seekTo(target)
        isPlaying = engine.isPlaying
    }

    fun stopPlayback(clearTrack: Boolean = true) {
        engine.stop()
        isPlaying = false
        positionMs = 0L
        if (clearTrack) {
            currentTrack = null
            queue = emptyList()
            queueTitle = ""
            currentQueueIndex = -1
            durationMs = 0L
        }
    }

    private fun handleTrackFinished() {
        if (queue.isEmpty()) {
            isPlaying = false
            return
        }
        val nextIndex = when (playMode) {
            PlayMode.ONE -> currentQueueIndex.coerceAtLeast(0)
            PlayMode.ALL -> if (currentQueueIndex >= queue.lastIndex) 0 else currentQueueIndex + 1
            PlayMode.SHUFFLE -> {
                if (queue.size <= 1) 0
                else {
                    var candidate = Random.nextInt(queue.size)
                    if (candidate == currentQueueIndex) candidate = (candidate + 1) % queue.size
                    candidate
                }
            }
        }
        startIndex(nextIndex)
    }

    fun cyclePlayMode() {
        playMode = when (playMode) {
            PlayMode.ALL -> PlayMode.ONE
            PlayMode.ONE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.ALL
        }
        persist()
        showMessage(
            when (playMode) {
                PlayMode.ALL -> "Повтор: все треки"
                PlayMode.ONE -> "Повтор: один трек"
                PlayMode.SHUFFLE -> "Перемешивание"
            },
        )
    }

    fun updateVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        engine.setVolume(volume)
    }

    fun persistVolume() = persist()

    // =====================================================================
    // Плейлисты
    // =====================================================================

    fun playlistById(id: Long): Playlist? = playlists.firstOrNull { it.id == id }

    fun playlistTracks(playlist: Playlist): List<Track> =
        playlist.trackPaths.mapNotNull { libraryByPath[it] }

    fun createPlaylist(name: String, coverSource: File?, trackToAdd: Track? = null): Long? {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            showMessage("Введи название плейлиста")
            return null
        }
        val id = (playlists.maxOfOrNull { it.id } ?: 0L) + 1L
        val coverPath = coverSource?.let { AppStorage.importCover(it, id) }
        val playlist = Playlist(
            id = id,
            name = cleanName,
            coverPath = coverPath,
            trackPaths = trackToAdd?.let { listOf(it.path) } ?: emptyList(),
        )
        playlists = listOf(playlist) + playlists
        refreshPlaylistCovers()
        persist()
        showMessage(
            if (trackToAdd != null) "Создан плейлист «$cleanName» и добавлен трек"
            else "Создан плейлист «$cleanName»",
        )
        return id
    }

    fun renamePlaylist(id: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        playlists = playlists.map { if (it.id == id) it.copy(name = clean) else it }
        persist()
        showMessage("Плейлист переименован")
    }

    fun deletePlaylist(id: Long) {
        val playlist = playlistById(id) ?: return
        AppStorage.deleteCover(playlist.coverPath)
        playlists = playlists.filterNot { it.id == id }
        if (openPlaylistId == id) openPlaylistId = null
        refreshPlaylistCovers()
        persist()
        showMessage("Плейлист «${playlist.name}» удалён")
    }

    fun setPlaylistCover(id: Long, source: File) {
        val playlist = playlistById(id) ?: return
        val newCover = AppStorage.importCover(source, id) ?: run {
            showMessage("Не удалось прочитать картинку")
            return
        }
        AppStorage.deleteCover(playlist.coverPath)
        playlists = playlists.map { if (it.id == id) it.copy(coverPath = newCover) else it }
        refreshPlaylistCovers()
        persist()
        showMessage("Обложка обновлена")
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        val playlist = playlistById(playlistId) ?: return
        if (playlist.trackPaths.contains(track.path)) {
            showMessage("«${track.title}» уже есть в «${playlist.name}»")
            return
        }
        playlists = playlists.map {
            if (it.id == playlistId) it.copy(trackPaths = it.trackPaths + track.path) else it
        }
        refreshPlaylistCovers()
        persist()
        showMessage("Добавлено в «${playlist.name}»")
    }

    fun removeTrackFromPlaylist(playlistId: Long, track: Track) {
        playlists = playlists.map {
            if (it.id == playlistId) it.copy(trackPaths = it.trackPaths - track.path) else it
        }
        refreshPlaylistCovers()
        persist()
        showMessage("Удалено из плейлиста")
    }

    private fun refreshPlaylistCovers() {
        val result = LinkedHashMap<String, MutableList<String>>()
        playlists.forEach { playlist ->
            val cover = playlist.coverPath ?: return@forEach
            playlist.trackPaths.forEach { path ->
                result.getOrPut(path) { mutableListOf() }.add(cover)
            }
        }
        playlistCoversByTrack = result.mapValues { it.value.distinct() }
    }

    // =====================================================================
    // Скрытые треки
    // =====================================================================

    fun hideTrack(track: Track) {
        if (hiddenPaths.contains(track.path)) return
        hiddenPaths = hiddenPaths + track.path
        if (currentTrack?.path == track.path) stopPlayback(clearTrack = true)
        applyFilters()
        persist()
        showMessage("Скрыто: ${track.title}")
    }

    fun restoreTracks(paths: Collection<String>) {
        if (paths.isEmpty()) return
        hiddenPaths = hiddenPaths - paths.toSet()
        applyFilters()
        persist()
        showMessage("Восстановлено треков: ${paths.size}")
    }

    fun restoreAllTracks() {
        if (hiddenPaths.isEmpty()) {
            showMessage("Список скрытых треков пуст")
            return
        }
        val count = hiddenPaths.size
        hiddenPaths = emptySet()
        applyFilters()
        persist()
        showMessage("Восстановлено треков: $count")
    }

    fun hiddenTracks(): List<Track> =
        hiddenPaths.mapNotNull { path ->
            libraryByPath[path] ?: runCatching { MusicScanner.readTrack(path) }.getOrNull()
        }.sortedBy { it.title.lowercase() }

    // =====================================================================
    // Навигация
    // =====================================================================

    fun selectTab(tab: Tab) {
        currentTab = tab
        openPlaylistId = null
        playlistSearchQuery = ""
    }

    fun openPlaylist(id: Long) {
        openPlaylistId = id
        playlistSearchQuery = ""
        currentTab = Tab.PLAYLISTS
    }

    fun closePlaylist() {
        openPlaylistId = null
        playlistSearchQuery = ""
    }

    fun updatePlaylistSearchQuery(query: String) {
        playlistSearchQuery = query
    }

    // =====================================================================
    // Служебное
    // =====================================================================

    fun showMessage(text: String) {
        message = text
        messageId++
    }

    fun consumeMessage() {
        message = null
    }

    private fun persist() {
        AppStorage.save(
            PersistedState(
                musicFolder = musicFolder,
                playlists = playlists,
                hiddenPaths = hiddenPaths.toList(),
                volume = volume,
                playMode = playMode,
            ),
        )
    }

    fun shutdown() {
        engine.stop()
        persist()
    }
}
