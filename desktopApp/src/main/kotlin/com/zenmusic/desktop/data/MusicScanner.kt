package com.zenmusic.desktop.data

import com.zenmusic.desktop.model.Track
import com.zenmusic.desktop.util.fileNameWithoutExtension
import com.zenmusic.desktop.util.fixEncoding
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioSystem

/**
 * Сканирует папку (рекурсивно) и вытаскивает из файлов название, исполнителя,
 * альбом и длительность. Умеет читать mp3/wav/flac/ogg/aiff.
 */
object MusicScanner {

    val supportedExtensions = setOf("mp3", "wav", "flac", "ogg", "oga", "aiff", "aif")

    private data class CacheEntry(
        val size: Long,
        val modified: Long,
        val track: Track,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    data class ScanResult(
        val tracks: List<Track>,
        val scannedFiles: Int,
        val failedFiles: Int,
    )

    fun scan(
        folder: File?,
        extraPaths: Collection<String> = emptyList(),
        onProgress: (done: Int, total: Int, current: String) -> Unit = { _, _, _ -> },
    ): ScanResult {
        val files = collectFiles(folder, extraPaths)
        val tracks = ArrayList<Track>(files.size)
        var failed = 0

        files.forEachIndexed { index, file ->
            onProgress(index, files.size, file.name)
            val track = runCatching { readTrack(file) }
                .onFailure { failed++ }
                .getOrNull()
            if (track != null) tracks.add(track)
        }

        val sorted = tracks.sortedWith(
            compareByDescending<Track> { it.lastModified }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )
        return ScanResult(sorted, files.size, failed)
    }

    fun collectFiles(folder: File?, extraPaths: Collection<String>): List<File> {
        val byCanonicalPath = LinkedHashMap<String, File>()

        fun add(file: File) {
            if (!file.isFile) return
            if (file.extension.lowercase() !in supportedExtensions) return
            val key = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
            byCanonicalPath.putIfAbsent(key, file)
        }

        if (folder != null && folder.isDirectory) {
            runCatching {
                Files.walk(Paths.get(folder.absolutePath)).use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .forEach { path ->
                            val file = path.toFile()
                            val name = file.name
                            // не лезем в скрытые и системные папки
                            if (name.startsWith(".")) return@forEach
                            add(file)
                        }
                }
            }
        }

        // Треки из плейлистов и «скрытые» тоже должны быть в библиотеке,
        // иначе они пропадут из плейлистов после смены папки.
        extraPaths.forEach { path ->
            runCatching { add(File(path)) }
        }

        return byCanonicalPath.values.toList()
    }

    /** Читает метаданные одного файла. Использует кэш по размеру+дате изменения. */
    fun readTrack(file: File): Track {
        val absolute = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
        val size = file.length()
        val modified = file.lastModified()

        cache[absolute]?.let { cached ->
            if (cached.size == size && cached.modified == modified) return cached.track
        }

        var tagTitle = ""
        var tagArtist = ""
        var tagAlbum = ""
        var durationMs = 0L

        runCatching {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            if (tag != null) {
                tagTitle = runCatching { tag.getFirst(FieldKey.TITLE) }.getOrNull().orEmpty().trim()
                tagArtist = runCatching { tag.getFirst(FieldKey.ARTIST) }.getOrNull().orEmpty().trim()
                tagAlbum = runCatching { tag.getFirst(FieldKey.ALBUM) }.getOrNull().orEmpty().trim()
            }
            val seconds = runCatching { audioFile.audioHeader.trackLength }.getOrNull() ?: 0
            durationMs = seconds.toLong() * 1000L
        }

        // Android-версия парсит имя файла как «Artist - Title». Заодно
        // превращаем подчёркивания в пробелы — так аккуратнее для файлов
        // вида "Artist_-_Track_(source).mp3".
        val baseName = fileNameWithoutExtension(file.name)
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        var fileArtist = ""
        var fileTitle = baseName
        if (baseName.contains(" - ")) {
            val parts = baseName.split(" - ", limit = 2)
            fileArtist = parts[0].trim()
            fileTitle = parts[1].trim()
        }

        val title = fixEncoding(tagTitle.ifBlank { fileTitle })
        val artist = fixEncoding(tagArtist.ifBlank { fileArtist })
        val album = fixEncoding(tagAlbum)

        if (durationMs <= 0L) {
            durationMs = readDurationFallback(file)
        }

        val track = Track(
            path = absolute,
            title = title.ifBlank { baseName },
            artist = artist,
            album = album,
            durationMs = durationMs,
            lastModified = modified,
        )

        cache[absolute] = CacheEntry(size, modified, track)
        return track
    }

    fun readTrack(path: String): Track? {
        val file = File(path)
        if (!file.isFile) return null
        return runCatching { readTrack(file) }.getOrNull()
    }

    private fun readDurationFallback(file: File): Long {
        runCatching {
            val format = AudioSystem.getAudioFileFormat(file)
            val properties = format.properties()
            val micros = properties["duration"]
            if (micros is Long && micros > 0) return micros / 1000
            if (micros is Number && micros.toLong() > 0) return micros.toLong() / 1000
            val frames = format.frameLength
            val rate = format.format.frameRate
            if (frames > 0 && rate > 0) return (frames / rate * 1000).toLong()
        }

        runCatching {
            AudioSystem.getAudioInputStream(file).use { stream ->
                val frames = stream.frameLength
                val rate = stream.format.frameRate
                if (frames > 0 && rate > 0) return (frames / rate * 1000).toLong()
            }
        }

        return 0L
    }
}
