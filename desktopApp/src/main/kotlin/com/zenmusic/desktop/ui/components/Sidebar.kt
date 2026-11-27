package com.zenmusic.desktop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmusic.desktop.ui.AppController
import com.zenmusic.desktop.ui.Tab
import com.zenmusic.desktop.ui.ZenPurple
import com.zenmusic.desktop.ui.ZenSurfaceVariant
import com.zenmusic.desktop.ui.ZenText
import com.zenmusic.desktop.ui.ZenTextSecondary
import com.zenmusic.desktop.util.tracksWord

private val SidebarBackground = Color(0xFF09090C)

@Composable
fun Sidebar(
    controller: AppController,
    onChooseFolder: () -> Unit,
    onShowHidden: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(234.dp)
            .background(SidebarBackground)
            .padding(horizontal = 14.dp, vertical = 20.dp),
    ) {
        // ---------- Логотип ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ZenPurple),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF1A0024),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "ZenMusic",
                    color = ZenText,
                    fontSize = 19.2.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )
                Text(
                    text = "локальный плеер",
                    color = ZenTextSecondary,
                    fontSize = 10.4.sp,
                )
            }
        }

        Spacer(Modifier.height(30.dp))
        SectionLabel(
            text = "Библиотека",
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.height(8.dp))

        SidebarItem(
            label = "Треки",
            icon = Icons.Rounded.LibraryMusic,
            selected = controller.currentTab == Tab.TRACKS && controller.openPlaylistId == null,
            onClick = { controller.selectTab(Tab.TRACKS) },
        )
        SidebarItem(
            label = "Плейлисты",
            icon = Icons.Rounded.PlaylistPlay,
            selected = controller.currentTab == Tab.PLAYLISTS,
            onClick = { controller.selectTab(Tab.PLAYLISTS) },
        )
        SidebarItem(
            label = "Скрытые треки",
            icon = Icons.Rounded.VisibilityOff,
            selected = false,
            badge = controller.hiddenPaths.size,
            onClick = onShowHidden,
        )

        Spacer(Modifier.weight(1f))

        // ---------- Папка с музыкой ----------
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ZenSurfaceVariant,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = ZenPurple,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = controller.folderName(),
                        color = ZenText,
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (controller.isScanning) {
                        CircularProgressIndicator(
                            color = ZenPurple,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        IconButton(
                            onClick = { controller.scanLibrary(silent = false) },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Обновить библиотеку",
                                tint = ZenTextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${controller.library.size} ${tracksWord(controller.library.size)}",
                    color = ZenTextSecondary,
                    fontSize = 11.2.sp,
                )
                Spacer(Modifier.height(11.dp))
                Button(
                    onClick = onChooseFolder,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.09f),
                        contentColor = ZenText,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Сменить папку", fontSize = 11.2.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "ZenMusic · desktop",
            color = ZenTextSecondary.copy(alpha = 0.55f),
            fontSize = 10.4.sp,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Int = 0,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        selected -> Color(0x24BB86FC)
        hovered -> Color(0x10FFFFFF)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .hoverable(interaction)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) ZenPurple else ZenTextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(11.dp))
        Text(
            text = label,
            color = if (selected) ZenText else ZenTextSecondary,
            fontSize = 13.6.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        CountBadge(badge)
    }
}
