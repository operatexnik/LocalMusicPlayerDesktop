import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    val player = remember { AudioPlayer() }
    var currentTrack by remember { mutableStateOf<String?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.8f) }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // Кнопки сканирования и остановки
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val folder = java.io.File("D:\\Загрузки")
                    tracks = scanMusicFolder(folder.absolutePath)
                }, modifier = Modifier.weight(1f)) {
                    Text("Сканировать")
                }
                Button(onClick = {
                    player.stop()
                    currentTrack = null
                    isPaused = false
                }, modifier = Modifier.weight(1f)) {
                    Text("Стоп")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Ползунок громкости
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

            LazyColumn {
                items(tracks) { track ->
                    val isCurrent = currentTrack == track.filePath
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
                                    currentTrack = track.filePath
                                    isPaused = false
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(track.title, style = MaterialTheme.typography.titleMedium)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall)
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

fun scanMusicFolder(path: String): List<Track> {
    val folder = File(path)
    if (!folder.exists()) return emptyList()
    return folder.listFiles()
        ?.filter { it.extension.lowercase() in listOf("mp3", "wav", "flac") }
        ?.map { Track(title = it.nameWithoutExtension, artist = "Unknown", filePath = it.absolutePath) }
        ?: emptyList()
}