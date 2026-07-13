import javazoom.jl.player.Player
import java.io.FileInputStream
import javax.sound.sampled.*

actual class AudioPlayer {
    private var player: Player? = null
    private var thread: Thread? = null
    private var isPlaying = false
    private var currentFilePath: String? = null
    private var currentPosition: Long = 0

    actual fun play(filePath: String) {
        stop()
        currentFilePath = filePath
        currentPosition = 0
        startPlayback()
    }

    actual fun pause() {
        player?.close()
        isPlaying = false
    }

    actual fun resume() {
        currentFilePath?.let { path ->
            startPlayback(resumePosition = currentPosition)
        }
    }

    actual fun stop() {
        player?.close()
        thread?.interrupt()
        currentFilePath = null
        currentPosition = 0
        isPlaying = false
    }

    actual fun isPlaying(): Boolean = isPlaying

    actual fun setVolume(volume: Float) {
        // Глобальная громкость для javax.sound (работает на системном уровне)
        try {
            val mixer = AudioSystem.getMixer(null)
            if (mixer.isLineSupported(Port.Info.SPEAKER)) {
                val port = mixer.getLine(Port.Info.SPEAKER) as Port
                port.open()
                (port.getControl(FloatControl.Type.VOLUME) as? FloatControl)?.value = volume
                port.close()
            }
        } catch (e: Exception) {
            // Не все системы поддерживают
        }
    }

    private fun startPlayback(resumePosition: Long = 0) {
        val path = currentFilePath ?: return
        isPlaying = true
        thread = Thread {
            try {
                FileInputStream(path).use { fis ->
                    if (resumePosition > 0) fis.skip(resumePosition)
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
}