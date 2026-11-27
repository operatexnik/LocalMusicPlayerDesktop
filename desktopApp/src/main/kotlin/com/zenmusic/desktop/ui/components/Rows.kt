package com.zenmusic.desktop.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmusic.desktop.model.Playlist
import com.zenmusic.desktop.model.Track
import com.zenmusic.desktop.ui.ZenError
import com.zenmusic.desktop.ui.ZenPurple
import com.zenmusic.desktop.ui.ZenText
import com.zenmusic.desktop.ui.ZenTextSecondary
import com.zenmusic.desktop.util.formatTime
import com.zenmusic.desktop.util.tracksWord

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    coverPaths: List<String>,
    actions: List<MenuAction>,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var rowWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val menuWidth = 240.dp
    val menuY = (-8).dp

    val background = when {
        isCurrent -> Color(0x33FFFFFF)
        menuOpen -> Color(0x1FFFFFFF)
        hovered -> Color(0x14FFFFFF)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onPlay,
                )
                .pointerInput(density) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                                val maxX = with(density) {
                                    (rowWidthPx - menuWidth.toPx() - 6.dp.toPx())
                                        .coerceAtLeast(0f)
                                        .toDp()
                                }
                                menuOffset = with(density) {
                                    DpOffset(
                                        position.x.toDp().coerceAtMost(maxX),
                                        menuY,
                                    )
                                }
                                menuOpen = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isCurrent -> Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = if (isPlaying) "Играет" else "Пауза",
                        tint = ZenPurple,
                        modifier = Modifier.size(18.dp),
                    )

                    hovered -> Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Играть",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            TrackArt(
                track = track,
                covers = coverPaths,
                size = 42.dp,
                shape = CircleShape,
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = ZenText,
                    fontSize = 14.4.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track.displayArtist,
                    color = ZenTextSecondary,
                    fontSize = 11.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (track.durationMs > 0) {
                Text(
                    text = formatTime(track.durationMs),
                    color = ZenTextSecondary,
                    fontSize = 11.2.sp,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }

            IconButton(
                onClick = {
                    // DropdownMenu сам добавляет высоту якоря, поэтому отрицательный
                    // offset возвращает меню ровно под «⋮».
                    val x = with(density) {
                        (rowWidthPx - menuWidth.toPx() - 6.dp.toPx())
                            .coerceAtLeast(0f)
                            .toDp()
                    }
                    menuOffset = DpOffset(x, menuY)
                    menuOpen = true
                },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Действия",
                    tint = ZenTextSecondary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            offset = menuOffset,
            modifier = Modifier.width(menuWidth),
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = action.label,
                            color = if (action.danger) ZenError else ZenText,
                            fontSize = 13.6.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = if (action.danger) ZenError else ZenTextSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

@Composable
fun PlaylistRow(
    playlist: Playlist,
    actions: List<MenuAction>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var rowWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val menuWidth = 240.dp
    val menuY = (-14).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        menuOpen -> Color(0x1FFFFFFF)
                        hovered -> Color(0x12FFFFFF)
                        else -> Color.Transparent
                    },
                )
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onOpen,
                )
                .pointerInput(density) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                                val maxX = with(density) {
                                    (rowWidthPx - menuWidth.toPx() - 6.dp.toPx())
                                        .coerceAtLeast(0f)
                                        .toDp()
                                }
                                menuOffset = with(density) {
                                    DpOffset(
                                        position.x.toDp().coerceAtMost(maxX),
                                        menuY,
                                    )
                                }
                                menuOpen = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistCover(playlist = playlist, size = 54.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = ZenText,
                    fontSize = 15.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${playlist.trackPaths.size} ${tracksWord(playlist.trackPaths.size)}",
                    color = ZenTextSecondary,
                    fontSize = 12.sp,
                )
            }
            IconButton(
                onClick = {
                    // DropdownMenu сам добавляет высоту якоря, поэтому отрицательный
                    // offset возвращает меню ровно под «⋮».
                    val x = with(density) {
                        (rowWidthPx - menuWidth.toPx() - 6.dp.toPx())
                            .coerceAtLeast(0f)
                            .toDp()
                    }
                    menuOffset = DpOffset(x, menuY)
                    menuOpen = true
                },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Действия",
                    tint = ZenTextSecondary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            offset = menuOffset,
            modifier = Modifier.width(menuWidth),
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = action.label,
                            color = if (action.danger) ZenError else ZenText,
                            fontSize = 13.6.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = if (action.danger) ZenError else ZenTextSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
