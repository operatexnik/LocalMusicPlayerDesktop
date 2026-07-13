actual class AudioPlayer {
    actual fun play(filePath: String) {}
    actual fun pause() {}
    actual fun resume() {}
    actual fun stop() {}
    actual fun isPlaying(): Boolean = false
}