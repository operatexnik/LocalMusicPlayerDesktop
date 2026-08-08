actual class AudioPlayer {
    actual fun play(filePath: String) {}
    actual fun pause() {}
    actual fun resume() {}
    actual fun stop() {}
    actual fun isPlaying(): Boolean = false
    actual fun setVolume(volume: Float) {}
    actual fun getCurrentPosition(): Long = 0
    actual fun getDuration(): Long = 0
    actual fun seek(positionMs: Long) {}
    actual fun setOnFinished(listener: (() -> Unit)?) {}
}