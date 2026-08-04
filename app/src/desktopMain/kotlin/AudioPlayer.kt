import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.*
import kotlin.math.roundToInt

actual class AudioPlayer {

    private var line: SourceDataLine? = null
    private var audioStream: AudioInputStream? = null
    private var playThread: Thread? = null

    private val playing = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    // Громкость храним атомарно, чтобы поток воспроизведения всегда видел актуальное значение
    private val volume = AtomicReference(0.8f)

    private var currentFilePath: String? = null

    actual fun play(filePath: String) {
        stop()
        currentFilePath = filePath
        startPlayback()
    }

    actual fun pause() {
        if (playing.get() && !paused.get()) {
            paused.set(true)
            // Не вызываем line.stop() — это и даёт щелчок
        }
    }

    actual fun resume() {
        if (playing.get() && paused.get()) {
            paused.set(false)
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
        currentFilePath = null
    }

    actual fun isPlaying(): Boolean = playing.get() && !paused.get()

    actual fun setVolume(volume: Float) {
        this.volume.set(volume.coerceIn(0f, 1f))
        // Громкость применяется мгновенно в цикле записи — ничего больше делать не нужно
    }

    private fun startPlayback() {
        val path = currentFilePath ?: return
        val file = File(path)
        if (!file.exists()) return

        stopRequested.set(false)
        paused.set(false)
        playing.set(true)

        playThread = Thread {
            try {
                var stream = AudioSystem.getAudioInputStream(file)

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

                val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)
                val sourceLine = AudioSystem.getLine(info) as SourceDataLine
                // Чуть меньший буфер = меньше задержка, но стабильность обычно нормальная
                sourceLine.open(decodedFormat, 4096 * 4)
                line = sourceLine
                sourceLine.start()

                val buffer = ByteArray(4096)

                while (!stopRequested.get()) {
                    // Пауза без stop()/start() линии — нет щелчка
                    if (paused.get()) {
                        synchronized(this) {
                            while (paused.get() && !stopRequested.get()) {
                                try {
                                    (this as Object).wait(50)
                                } catch (_: InterruptedException) {
                                    break
                                }
                            }
                        }
                        if (stopRequested.get()) break
                    }

                    val bytesRead = stream.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) break

                    // === Программная громкость ===
                    applySoftwareVolume(buffer, bytesRead, volume.get())

                    sourceLine.write(buffer, 0, bytesRead)
                }

                if (!stopRequested.get()) {
                    sourceLine.drain()
                }

            } catch (e: Exception) {
                if (!stopRequested.get()) e.printStackTrace()
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

    /**
     * Умножает 16-bit PCM сэмплы на коэффициент громкости.
     * Работает мгновенно и без щелчков.
     */
    private fun applySoftwareVolume(buffer: ByteArray, len: Int, vol: Float) {
        if (vol >= 0.999f) return          // почти максимум — ничего не трогаем
        if (vol <= 0.001f) {               // тишина
            buffer.fill(0, 0, len)
            return
        }

        var i = 0
        while (i < len - 1) {
            // little-endian 16-bit
            var sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            // приводим к signed
            if (sample >= 32768) sample -= 65536

            sample = (sample * vol).roundToInt().coerceIn(-32768, 32767)

            buffer[i] = sample.toByte()
            buffer[i + 1] = (sample shr 8).toByte()
            i += 2
        }
    }
}