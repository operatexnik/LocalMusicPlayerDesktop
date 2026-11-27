package com.zenmusic.desktop.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zenmusic.desktop.data.ArtworkLoader
import com.zenmusic.desktop.model.Playlist
import com.zenmusic.desktop.model.Track
import com.zenmusic.desktop.ui.ZenError
import com.zenmusic.desktop.ui.ZenPurple
import com.zenmusic.desktop.ui.ZenSurfaceElevated
import com.zenmusic.desktop.ui.ZenText
import com.zenmusic.desktop.ui.ZenTextSecondary
import com.zenmusic.desktop.ui.components.PlaylistCover
import com.zenmusic.desktop.util.chooseImageFile
import com.zenmusic.desktop.util.tracksWord
import java.io.File

@Composable
fun ZenDialog(
    onDismiss: () -> Unit,
    title: String,
    subtitle: String? = null,
    width: Int = 440,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A20),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.widthIn(min = 360.dp, max = width.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZenText,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZenTextSecondary,
                    )
                }
                Spacer(Modifier.height(18.dp))
                content()
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    trackToAdd: Track?,
    onDismiss: () -> Unit,
    onCreate: (name: String, cover: File?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var coverFile by remember { mutableStateOf<File?>(null) }
    val coverBitmap by produceState<ImageBitmap?>(initialValue = null, coverFile) {
        value = coverFile?.let { ArtworkLoader.loadFile(it.absolutePath) }
    }

    ZenDialog(
        onDismiss = onDismiss,
        title = "Новый плейлист",
        subtitle = trackToAdd?.let { "Трек «${it.title}» сразу добавится в новый плейлист." },
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ZenTextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onCreate(name, coverFile) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(50),
            ) {
                Text("Создать", modifier = Modifier.padding(horizontal = 10.dp))
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverPicker(
                bitmap = coverBitmap,
                hasCover = coverFile != null,
                onClick = { chooseImageFile()?.let { coverFile = it } },
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Обложка",
                    color = ZenText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Нажми на квадрат слева, чтобы выбрать картинку. Это необязательно.",
                    color = ZenTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                if (coverFile != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { coverFile = null },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text("Убрать обложку", color = ZenError, fontSize = 11.5.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("Название плейлиста") },
            shape = RoundedCornerShape(12.dp),
            colors = zenTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun RenamePlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember { mutableStateOf(playlist.name) }
    ZenDialog(
        onDismiss = onDismiss,
        title = "Переименовать",
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ZenTextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onRename(name) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(50),
            ) {
                Text("Сохранить", modifier = Modifier.padding(horizontal = 10.dp))
            }
        },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("Название") },
            shape = RoundedCornerShape(12.dp),
            colors = zenTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun DeletePlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    ZenDialog(
        onDismiss = onDismiss,
        title = "Удалить «${playlist.name}»?",
        subtitle = "Плейлист исчезнет, но музыкальные файлы на диске останутся на месте.",
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ZenTextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onDelete,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZenError,
                    contentColor = Color(0xFF2A0004),
                ),
            ) {
                Text("Удалить", modifier = Modifier.padding(horizontal = 10.dp))
            }
        },
    ) {}
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCreateNew: () -> Unit,
) {
    ZenDialog(
        onDismiss = onDismiss,
        title = "Добавить в плейлист",
        subtitle = track.title,
        actions = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ZenTextSecondary)
            }
        },
    ) {
        if (playlists.isEmpty()) {
            Text(
                text = "Плейлистов пока нет. Создай первый — трек сразу окажется в нём.",
                color = ZenTextSecondary,
                fontSize = 12.5.sp,
            )
            Spacer(Modifier.height(12.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(playlist.id) }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaylistCover(playlist, size = 38.dp, shape = RoundedCornerShape(8.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = ZenText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${playlist.trackPaths.size} ${tracksWord(playlist.trackPaths.size)}",
                                color = ZenTextSecondary,
                                fontSize = 11.2.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onCreateNew)
                .padding(horizontal = 8.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White.copy(alpha = 0.09f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = ZenPurple,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Создать новый плейлист",
                color = ZenText,
                fontSize = 13.5.sp,
            )
        }
    }
}

@Composable
fun HiddenTracksDialog(
    tracks: List<Track>,
    onDismiss: () -> Unit,
    onRestore: (Set<String>) -> Unit,
    onRestoreAll: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<String>()) }

    ZenDialog(
        onDismiss = onDismiss,
        title = "Скрытые треки",
        subtitle = if (tracks.isEmpty()) {
            "Чёрный список пуст — все треки на месте."
        } else {
            "Отметь треки, которые нужно вернуть в библиотеку."
        },
        width = 500,
        actions = {
            if (tracks.isNotEmpty()) {
                TextButton(onClick = onRestoreAll) {
                    Text("Восстановить все", color = ZenTextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onRestore(selected) },
                    enabled = selected.isNotEmpty(),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = if (selected.isEmpty()) "Вернуть" else "Вернуть (${selected.size})",
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            } else {
                Button(onClick = onDismiss, shape = RoundedCornerShape(50)) {
                    Text("Закрыть", modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        },
    ) {
        if (tracks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
            ) {
                items(tracks, key = { it.path }) { track ->
                    val checked = track.path in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selected = if (checked) selected - track.path else selected + track.path
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selected = if (checked) selected - track.path else selected + track.path
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ZenPurple,
                                checkmarkColor = Color(0xFF1A0024),
                                uncheckedColor = Color.White.copy(alpha = 0.35f),
                            ),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = ZenText,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.displayArtist,
                                color = ZenTextSecondary,
                                fontSize = 11.2.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverPicker(
    bitmap: ImageBitmap?,
    hasCover: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ZenSurfaceElevated)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Обложка плейлиста",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (hasCover) Icons.Rounded.Image else Icons.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = ZenPurple,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Выбрать",
                    color = ZenTextSecondary,
                    fontSize = 11.2.sp,
                )
            }
        }
    }
}

@Composable
private fun zenTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ZenPurple,
    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
    focusedTextColor = ZenText,
    unfocusedTextColor = ZenText,
    focusedLabelColor = ZenPurple,
    unfocusedLabelColor = ZenTextSecondary,
    cursorColor = ZenPurple,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)
