import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.*
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

actual class AudioPlayer {

    private var line: SourceDataLine? = null
    private var audioStream: AudioInputStream? = null
    private var playThread: Thread? = null

    private val playing = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    private var volume: Float = 0.8f
    private var currentFilePath: String? = null

    // для будущего seek / прогресса
    private var bytesPerSecond: Float = 0f
    private var totalBytesRead: Long = 0

    actual fun play(filePath: String) {
        stop()
        currentFilePath = filePath
        startPlayback()
    }

    actual fun pause() {
        if (playing.get() && !paused.get()) {
            paused.set(true)
            line?.stop()          // останавливаем вывод звука
        }
    }

    actual fun resume() {
        if (playing.get() && paused.get()) {
            paused.set(false)
            line?.start()         // продолжаем с того же места
            // будим поток, если он ждал
            synchronized(this) {
                (this as Object).notifyAll()
            }
        }
    }

    actual fun stop() {
        stopRequested.set(true)
        paused.set(false)
        playing.set(false)

        try {
            line?.stop()
            line?.flush()
            line?.close()
        } catch (_: Exception) {}

        try {
            audioStream?.close()
        } catch (_: Exception) {}

        playThread?.interrupt()
        playThread = null
        line = null
        audioStream = null
        totalBytesRead = 0
        currentFilePath = null
    }

    actual fun isPlaying(): Boolean = playing.get() && !paused.get()

    actual fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        applyVolume()
    }

    private fun applyVolume() {
        val sourceLine = line ?: return
        try {
            if (sourceLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = sourceLine.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val gainDb = if (volume <= 0.0001f) {
                    gainControl.minimum
                } else {
                    val db = (20.0 * ln(volume.toDouble()) / ln(10.0)).toFloat()
                    max(gainControl.minimum, min(gainControl.maximum, db))
                }
                gainControl.value = gainDb
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPlayback() {
        val path = currentFilePath ?: return
        val file = File(path)
        if (!file.exists()) return

        stopRequested.set(false)
        paused.set(false)
        playing.set(true)
        totalBytesRead = 0

        playThread = Thread {
            try {
                // 1. Открываем файл (mp3spi подхватывает MP3)
                var stream = AudioSystem.getAudioInputStream(file)

                // 2. Конвертируем в PCM, который понимает SourceDataLine
                val baseFormat = stream.format
                val decodedFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.sampleRate,
                    16,
                    baseFormat.channels,
                    baseFormat.channels * 2,
                    baseFormat.sampleRate,
                    false
                )
                stream = AudioSystem.getAudioInputStream(decodedFormat, stream)
                audioStream = stream

                // 3. Открываем линию
                val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)
                val sourceLine = AudioSystem.getLine(info) as SourceDataLine
                sourceLine.open(decodedFormat)
                line = sourceLine

                bytesPerSecond = decodedFormat.sampleRate * decodedFormat.frameSize

                applyVolume()
                sourceLine.start()

                val buffer = ByteArray(4096)

                while (!stopRequested.get()) {
                    // Если на паузе — ждём
                    if (paused.get()) {
                        synchronized(this) {
                            while (paused.get() && !stopRequested.get()) {
                                try {
                                    (this as Object).wait(200)
                                } catch (_: InterruptedException) {
                                    break
                                }
                            }
                        }
                        if (stopRequested.get()) break
                    }

                    val bytesRead = stream.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) break   // конец файла

                    totalBytesRead += bytesRead
                    sourceLine.write(buffer, 0, bytesRead)
                }

                // Доигрываем буфер
                if (!stopRequested.get()) {
                    sourceLine.drain()
                }

            } catch (e: Exception) {
                if (!stopRequested.get()) {
                    e.printStackTrace()
                }
            } finally {
                playing.set(false)
                paused.set(false)
                try {
                    line?.stop()
                    line?.close()
                    audioStream?.close()
                } catch (_: Exception) {}
                line = null
                audioStream = null
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }
}