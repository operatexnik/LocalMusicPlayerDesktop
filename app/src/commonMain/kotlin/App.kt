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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    val player = remember { AudioPlayer() }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = {
                val folder = java.io.File("D:\\Загрузки")
                tracks = scanMusicFolder(folder.absolutePath)
            }) {
                Text("Сканировать музыку")
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(tracks) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { player.play(track.filePath) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = { player.stop() }) {
                Text("Стоп")
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