package com.zenmusic.desktop.data

import com.zenmusic.desktop.model.PersistedState
import kotlinx.serialization.json.Json
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Простое хранилище состояния в %APPDATA%/ZenMusic/state.json.
 * Никакой БД — для плейлистов и настроек JSON более чем достаточно.
 */
object AppStorage {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    val dataDir: File by lazy {
        val base = System.getenv("APPDATA")
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?: File(System.getProperty("user.home"), ".config")
        File(base, "ZenMusic").apply { mkdirs() }
    }

    val coversDir: File by lazy {
        File(dataDir, "covers").apply { mkdirs() }
    }

    private val stateFile by lazy { File(dataDir, "state.json") }

    fun load(): PersistedState {
        if (!stateFile.exists()) return PersistedState()
        return runCatching {
            json.decodeFromString<PersistedState>(stateFile.readText(Charsets.UTF_8))
        }.getOrElse {
            // Битый файл не должен убивать приложение — откладываем в сторону.
            runCatching {
                stateFile.copyTo(File(dataDir, "state.broken.${System.currentTimeMillis()}.json"), overwrite = true)
            }
            PersistedState()
        }
    }

    @Synchronized
    fun save(state: PersistedState) {
        runCatching {
            val tmp = File(dataDir, "state.json.tmp")
            tmp.writeText(json.encodeToString(PersistedState.serializer(), state), Charsets.UTF_8)
            if (stateFile.exists() && !stateFile.delete()) {
                stateFile.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8)
                tmp.delete()
            } else {
                tmp.renameTo(stateFile)
            }
        }
    }

    /**
     * Копирует выбранную картинку в папку приложения и пересохраняет её в PNG,
     * чтобы потом её гарантированно мог прочитать ImageIO.
     */
    fun importCover(source: File, playlistId: Long): String? {
        return runCatching {
            val image = ImageIO.read(source) ?: return null
            val scaled = scaleDown(image, 900)
            val target = File(coversDir, "playlist_${playlistId}_${System.currentTimeMillis()}.png")
            ImageIO.write(scaled, "png", target)
            target.absolutePath
        }.getOrNull()
    }

    fun deleteCover(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.canonicalPath.startsWith(coversDir.canonicalPath, ignoreCase = true)) {
                file.delete()
            }
        }
    }

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
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_RENDERING,
            java.awt.RenderingHints.VALUE_RENDER_QUALITY,
        )
        g.drawImage(image, 0, 0, w, h, null)
        g.dispose()
        return result
    }
}
