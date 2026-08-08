import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

actual class AudioPlayer {

    private val factory = MediaPlayerFactory("--no-video", "--quiet")
    private var mediaPlayer: MediaPlayer? = null

    private val playing = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val volume = AtomicReference(0.8f)

    private val currentPositionMs = AtomicLong(0)
    private val durationMs = AtomicLong(0)

    private var onFinished: (() -> Unit)? = null

    actual fun play(filePath: String) {
        stop()

        val player = factory.mediaPlayers().newMediaPlayer()
        mediaPlayer = player

        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                playing.set(true)
                paused.set(false)
                // VLC иногда сбрасывает громкость при старте — ставим снова
                mediaPlayer.audio().setVolume((volume.get() * 100).toInt().coerceIn(0, 100))
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                paused.set(true)
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                playing.set(false)
                paused.set(false)
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                playing.set(false)
                paused.set(false)
                onFinished?.invoke()
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                currentPositionMs.set(newTime)
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                durationMs.set(newLength)
            }
        })

        player.audio().setVolume((volume.get() * 100).toInt().coerceIn(0, 100))
        player.media().play(filePath)
    }

    actual fun pause() {
        mediaPlayer?.controls()?.setPause(true)
        paused.set(true)
    }

    actual fun resume() {
        mediaPlayer?.controls()?.setPause(false)
        paused.set(false)
    }

    actual fun stop() {
        try {
            mediaPlayer?.controls()?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        playing.set(false)
        paused.set(false)
        currentPositionMs.set(0)
    }

    actual fun isPlaying(): Boolean = playing.get() && !paused.get()

    actual fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        this.volume.set(v)
        mediaPlayer?.audio()?.setVolume((v * 100).toInt().coerceIn(0, 100))
    }

    actual fun getCurrentPosition(): Long = currentPositionMs.get()

    actual fun getDuration(): Long = durationMs.get()

    actual fun seek(positionMs: Long) {
        mediaPlayer?.controls()?.setTime(positionMs.coerceAtLeast(0))
        currentPositionMs.set(positionMs.coerceAtLeast(0))
    }

    actual fun setOnFinished(listener: (() -> Unit)?) {
        onFinished = listener
    }
}