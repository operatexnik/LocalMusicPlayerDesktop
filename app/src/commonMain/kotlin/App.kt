import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.compose.runtime.remember
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    val player = remember { AudioPlayer() }
    var currentTrack by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
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
                }, modifier = Modifier.weight(1f)) {
                    Text("Стоп")
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(tracks) { track ->
                    val isPlaying = currentTrack == track.filePath
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isPlaying) {
                                    player.stop()
                                    currentTrack = null
                                } else {
                                    player.play(track.filePath)
                                    currentTrack = track.filePath
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlaying)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall)
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