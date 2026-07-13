import javazoom.jl.player.Player
import java.io.FileInputStream

actual class AudioPlayer {
    private var player: Player? = null
    private var thread: Thread? = null
    private var isPlaying = false

    actual fun play(filePath: String) {
        stop()
        isPlaying = true
        thread = Thread {
            try {
                FileInputStream(filePath).use { fis ->
                    player = Player(fis)
                    player?.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isPlaying = false
        }
        thread?.start()
    }

    actual fun pause() {
        player?.close()
        isPlaying = false
    }

    actual fun resume() {
        // JLayer не поддерживает resume — перезапускаем заново
        // Ограничение библиотеки
    }

    actual fun stop() {
        try {
            player?.close()
            thread?.interrupt()
        } catch (e: Exception) {}
        isPlaying = false
    }

    actual fun isPlaying(): Boolean = isPlaying
}