package com.zenmusic.desktop.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.zenmusic.desktop.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.images.Artwork
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/** Загружает и кэширует обложки: встроенные в теги и картинки плейлистов. */
object ArtworkLoader {

    private data class Entry(val stamp: Long, val bitmap: ImageBitmap?)

    private val cache = ConcurrentHashMap<String, Entry>()

    suspend fun loadTrack(track: Track): ImageBitmap? = withContext(Dispatchers.IO) {
        loadTrackBlocking(track)
    }

    fun loadTrackBlocking(track: Track): ImageBitmap? {
        val key = "track:${track.path}"
        cache[key]?.let { if (it.stamp == track.lastModified) return it.bitmap }

        val bytes = runCatching {
            val audioFile = AudioFileIO.read(File(track.path))
            val artwork: Artwork? = audioFile.tag?.firstArtwork
            artwork?.binaryData
        }.getOrNull()

        val bitmap = bytes?.let(::decode)
        cache[key] = Entry(track.lastModified, bitmap)
        return bitmap
    }

    suspend fun loadFile(path: String?, stampHint: Long = 0L): ImageBitmap? = withContext(Dispatchers.IO) {
        loadFileBlocking(path, stampHint)
    }

    fun loadFileBlocking(path: String?, stampHint: Long = 0L): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.isFile) return null
        val stamp = if (stampHint != 0L) stampHint else file.lastModified()
        val key = "file:${file.absolutePath}"
        cache[key]?.let { if (it.stamp == stamp) return it.bitmap }

        val bitmap = runCatching { ImageIO.read(file)?.let { toImageBitmap(it) } }.getOrNull()
        cache[key] = Entry(stamp, bitmap)
        return bitmap
    }

    fun clear() = cache.clear()

    private fun decode(bytes: ByteArray): ImageBitmap? = runCatching {
        ImageIO.read(ByteArrayInputStream(bytes))?.let { toImageBitmap(it) }
    }.getOrNull()

    private fun toImageBitmap(image: BufferedImage): ImageBitmap =
        scaleDown(image, 512).toComposeImageBitmap()

    private fun scaleDown(image: BufferedImage, maxSide: Int): BufferedImage {
        val biggest = maxOf(image.width, image.height)
        if (biggest <= maxSide) return image
        val ratio = maxSide.toDouble() / biggest
        val w = (image.width * ratio).toInt().coerceAtLeast(1)
        val h = (image.height * ratio).toInt().coerceAtLeast(1)
        val result = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = result.createGraphics()
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC,
        )
        g.drawImage(image, 0, 0, w, h, null)
        g.dispose()
        return result
    }
}
