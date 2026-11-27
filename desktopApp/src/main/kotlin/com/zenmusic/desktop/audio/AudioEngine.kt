package com.zenmusic.desktop.audio

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.log10

/**
 * Аудиодвижок на Java Sound.
 *
 * MP3/OGG/FLAC декодируются через SPI-провайдеры (mp3spi/vorbisspi/jflac),
 * WAV поддерживается из коробки. Потоковое воспроизведение через SourceDataLine,
 * поэтому даже часовые записи не съедают гигабайты памяти.
 */
class AudioEngine {

    private val stateLock = Object()

    @Volatile private var line: SourceDataLine? = null
    @Volatile private var worker: Thread? = null

    @Volatile private var generation = 0L
    @Volatile private var paused = false
    @Volatile private var playing = false
    @Volatile private var closing = false

    @Volatile private var currentPath: String? = null
    @Volatile private var positionMs = 0L
    @Volatile private var decodedDurationMs = 0L
    @Volatile private var volume = 0.8f

    var onFinished: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    val isPlaying: Boolean get() = playing
    val isPaused: Boolean get() = paused
    val currentPositionMs: Long get() = positionMs
    val durationMs: Long get() = decodedDurationMs

    fun play(path: String, startMs: Long = 0L, startPaused: Boolean = false) {
        teardown(resetPath = false)

        val myGeneration: Long
        synchronized(stateLock) {
            generation++
            myGeneration = generation
            currentPath = path
            positionMs = startMs.coerceAtLeast(0L)
            decodedDurationMs = 0L
            paused = startPaused
            playing = !startPaused
            closing = false
            stateLock.notifyAll()
        }

        val thread = Thread(
            { runPlayback(path, startMs.coerceAtLeast(0L), myGeneration) },
            "zenmusic-audio-player",
        ).apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY
        }
        worker = thread
        thread.start()
    }

    fun pause() {
        synchronized(stateLock) {
            if (currentPath == null) return
            paused = true
            playing = false
            runCatching { line?.stop() }
            stateLock.notifyAll()
        }
    }

    fun resume() {
        synchronized(stateLock) {
            if (currentPath == null) return
            paused = false
            playing = true
            runCatching { line?.start() }
            stateLock.notifyAll()
        }
    }

    fun seekTo(ms: Long) {
        val path = currentPath ?: return
        val wasPaused = paused
        val target = ms.coerceAtLeast(0L)
        // Проще всего перезапустить поток с нужной позиции.
        play(path, target, startPaused = wasPaused)
    }

    fun stop() {
        teardown(resetPath = true)
    }

    fun setVolume(value: Float) {
        val v = value.coerceIn(0f, 1f)
        volume = v
        line?.let { applyVolume(it, v) }
    }

    private fun teardown(resetPath: Boolean) {
        synchronized(stateLock) {
            generation++
            closing = true
            paused = false
            playing = false
            stateLock.notifyAll()
        }
        val oldLine = line
        line = null
        runCatching {
            oldLine?.stop()
            oldLine?.flush()
            oldLine?.close()
        }
        worker?.interrupt()
        worker = null
        if (resetPath) {
            currentPath = null
            positionMs = 0L
            decodedDurationMs = 0L
        }
    }

    private fun runPlayback(path: String, startMs: Long, myGeneration: Long) {
        var raw: AudioInputStream? = null
        var pcm: AudioInputStream? = null
        var dataLine: SourceDataLine? = null
        var finishedNaturally = false
        var failure: Throwable? = null

        try {
            val file = File(path)
            if (!file.isFile) error("Файл не найден: ${file.name}")

            raw = AudioSystem.getAudioInputStream(file)
            val sourceFormat = raw.format
            val targetFormat = makePcmFormat(sourceFormat)
            pcm = if (sameFormat(sourceFormat, targetFormat)) {
                raw
            } else {
                AudioSystem.getAudioInputStream(targetFormat, raw)
            }

            val format = pcm.format
            val frameSize = format.frameSize.coerceAtLeast(1)
            val lineForPlayback = AudioSystem.getSourceDataLine(format)
            dataLine = lineForPlayback

            val rawBufferSize = (format.frameRate * frameSize / 4).toInt()
                .coerceIn(4096, 512 * 1024)
            // Буфер SourceDataLine тоже должен состоять из целого числа фреймов.
            val bufferSize = (rawBufferSize / frameSize) * frameSize
            lineForPlayback.open(format, bufferSize)
            line = lineForPlayback
            applyVolume(lineForPlayback, volume)

            if (pcm.frameLength > 0 && format.frameRate > 0f) {
                decodedDurationMs = (pcm.frameLength * 1000.0 / format.frameRate).toLong()
            }

            if (myGeneration != generation) return

            // «Перемотка» для потоковых декодеров: читаем и выбрасываем PCM до нужной позиции.
            // Важно округлить до целого фрейма, иначе после перемотки поток
            // начинается с середины фрейма и SourceDataLine.write падает.
            val bytesPerMs = format.frameRate * frameSize / 1000.0
            var toSkip = (startMs * bytesPerMs).toLong().coerceAtLeast(0L)
            toSkip -= toSkip % frameSize
            if (toSkip > 0) {
                val skipBuffer = ByteArray(32 * 1024)
                while (toSkip > 0) {
                    if (myGeneration != generation) return
                    val want = minOf(skipBuffer.size.toLong(), toSkip).toInt()
                    val read = pcm.read(skipBuffer, 0, want)
                    if (read < 0) break
                    toSkip -= read
                }
                positionMs = startMs
            }

            synchronized(stateLock) {
                if (myGeneration != generation) return
                if (!paused) {
                    lineForPlayback.start()
                    playing = true
                }
            }

            val buffer = ByteArray(64 * 1024)
            // Хвост, который не составил целый фрейм, переносим в начало
            // следующего буфера. Так поток никогда не «съезжает» по каналам.
            var carry = 0

            while (true) {
                synchronized(stateLock) {
                    while (paused && myGeneration == generation) {
                        stateLock.wait(150)
                    }
                }
                if (myGeneration != generation) return

                val read = pcm.read(buffer, carry, buffer.size - carry)
                if (read < 0) {
                    finishedNaturally = true
                    break
                }
                if (read == 0) continue

                val available = carry + read
                val aligned = available - (available % frameSize)
                if (aligned <= 0) {
                    carry = available
                    continue
                }

                var offset = 0
                while (offset < aligned) {
                    if (myGeneration != generation) return
                    synchronized(stateLock) {
                        while (paused && myGeneration == generation) {
                            stateLock.wait(150)
                        }
                    }
                    val written = lineForPlayback.write(buffer, offset, aligned - offset)
                    if (written <= 0) break
                    offset += written
                }

                carry = available - aligned
                if (carry > 0) {
                    System.arraycopy(buffer, aligned, buffer, 0, carry)
                }

                if (format.frameRate > 0f) {
                    positionMs = startMs + (lineForPlayback.longFramePosition * 1000.0 / format.frameRate).toLong()
                }
            }

            if (myGeneration == generation) {
                runCatching { lineForPlayback.drain() }
                if (decodedDurationMs > 0) positionMs = decodedDurationMs
                else if (format.frameRate > 0f) {
                    positionMs = startMs + (lineForPlayback.longFramePosition * 1000.0 / format.frameRate).toLong()
                }
            }
        } catch (t: Throwable) {
            if (myGeneration == generation && t !is InterruptedException) {
                failure = t
            }
        } finally {
            synchronized(stateLock) {
                if (myGeneration == generation) {
                    playing = false
                    if (finishedNaturally) paused = false
                }
                closing = false
            }

            runCatching {
                dataLine?.stop()
                dataLine?.flush()
                dataLine?.close()
            }
            runCatching { pcm?.close() }
            runCatching { raw?.close() }
            if (line === dataLine) line = null

            if (myGeneration == generation) {
                failure?.let { error -> onError?.invoke(describeError(error)) }
                if (finishedNaturally && failure == null) onFinished?.invoke()
            }
        }
    }

    private fun describeError(t: Throwable): String {
        val name = currentPath?.let { File(it).name } ?: "файл"
        val reason = t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
        return "Не удалось воспроизвести «$name»: $reason"
    }

    private fun makePcmFormat(source: AudioFormat): AudioFormat {
        if (source.encoding == AudioFormat.Encoding.PCM_SIGNED && source.sampleSizeInBits == 16) {
            return source
        }
        val sampleRate = if (source.sampleRate > 0f) source.sampleRate else 44100f
        val channels = source.channels.coerceAtLeast(1)
        return AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            channels,
            channels * 2,
            sampleRate,
            false,
        )
    }

    private fun sameFormat(a: AudioFormat, b: AudioFormat): Boolean =
        a.encoding == b.encoding &&
            a.sampleRate == b.sampleRate &&
            a.sampleSizeInBits == b.sampleSizeInBits &&
            a.channels == b.channels &&
            a.frameSize == b.frameSize &&
            a.isBigEndian == b.isBigEndian

    private fun applyVolume(target: SourceDataLine, value: Float) {
        runCatching {
            val control = target.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val db = if (value <= 0.001f) {
                control.minimum
            } else {
                (20.0 * log10(value.toDouble())).toFloat().coerceIn(control.minimum, control.maximum)
            }
            control.value = db
        }
    }
}
