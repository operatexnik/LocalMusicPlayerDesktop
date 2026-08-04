import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val scope = rememberCoroutineScope()

    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    val player = remember { AudioPlayer() }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.8f) }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    // Обновление позиции
    LaunchedEffect(currentTrack) {
        while (currentTrack != null) {
            if (!isSeeking) {
                val newPos = player.getCurrentPosition()
                val newDur = player.getDuration()
                if (newDur > 0) {
                    position = newPos
                    duration = newDur
                }
            }
            delay(100)
        }
    }

    MaterialTheme {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Кнопка сканирования
            Button(
                onClick = {
                    val folder = File("D:\\Раб.стол\\tracks")
                    tracks = scanMusicFolder(folder.absolutePath)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сканировать")
            }

            Spacer(Modifier.height(12.dp))

            // Название текущего трека
            Text(
                text = currentTrack?.let { "${it.artist} — ${it.title}" } ?: "Ничего не играет",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            // Таймлайн
            Column {
                Slider(
                    value = if (duration > 0) {
                        (position.toFloat() / duration).coerceIn(0f, 1f)
                    } else 0f,
                    onValueChange = { value ->
                        isSeeking = true
                        position = (value * duration).toLong()
                    },
                    onValueChangeFinished = {
                        isSeeking = true
                        player.seek(position)
                        scope.launch {
                            delay(100)          // совсем чуть-чуть
                            isSeeking = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), style = MaterialTheme.typography.bodySmall)
                    Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Громкость
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Громкость", modifier = Modifier.padding(end = 8.dp))
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        player.setVolume(it)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Список треков
            LazyColumn {
                items(tracks) { track ->
                    val isCurrent = currentTrack?.filePath == track.filePath

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isCurrent) {
                                    if (isPaused) {
                                        player.resume()
                                        isPaused = false
                                    } else {
                                        player.pause()
                                        isPaused = true
                                    }
                                } else {
                                    player.stop()
                                    player.play(track.filePath)
                                    player.setVolume(volume)
                                    currentTrack = track
                                    isPaused = false
                                    position = 0
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (isCurrent) {
                                Text(
                                    text = if (isPaused) "⏸" else "▶",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun scanMusicFolder(path: String): List<Track> {
    val folder = File(path)
    if (!folder.exists()) return emptyList()

    return folder.listFiles()
        ?.filter { it.extension.lowercase() in listOf("mp3", "wav", "flac") }
        ?.map {
            Track(
                title = it.nameWithoutExtension,
                artist = "Unknown",
                filePath = it.absolutePath
            )
        } ?: emptyList()
}