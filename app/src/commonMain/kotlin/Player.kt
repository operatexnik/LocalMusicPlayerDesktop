expect class AudioPlayer {
    fun play(filePath: String)
    fun pause()
    fun resume()
    fun stop()
    fun isPlaying(): Boolean
    fun setVolume(volume: Float)
}