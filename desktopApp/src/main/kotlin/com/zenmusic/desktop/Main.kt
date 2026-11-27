package com.zenmusic.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.zenmusic.desktop.ui.AppController
import com.zenmusic.desktop.ui.ZenMusicApp
import java.util.prefs.Preferences

fun main() = application {
    // На Windows стандартный D3D-бэкенд Skiko в оконном режиме отдаёт
    // чуть более мыльную картинку, чем в максимизированном окне.
    // OpenGL в обоих режимах рисует одинаково чётко.
    if (System.getProperty("skiko.renderApi").isNullOrBlank() &&
        System.getenv("SKIKO_RENDER_API").isNullOrBlank()
    ) {
        System.setProperty("skiko.renderApi", "OPENGL")
    }

    // Jaudiotagger любит сыпать INFO-логами на каждый файл — выключаем шум.
    java.util.logging.Logger.getLogger("").apply {
        level = java.util.logging.Level.SEVERE
        handlers.forEach { it.level = java.util.logging.Level.SEVERE }
    }
    listOf(
        "org.jaudiotagger",
        "org.jaudiotagger.audio",
        "org.jaudiotagger.tag",
    ).forEach { name ->
        java.util.logging.Logger.getLogger(name).level = java.util.logging.Level.OFF
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val controller = remember { AppController(scope) }
    val prefs = remember { Preferences.userNodeForPackage(AppController::class.java) }

    val windowState = rememberWindowState(
        width = prefs.getInt("window.width", 1180).dp,
        height = prefs.getInt("window.height", 780).dp,
        position = WindowPosition(androidx.compose.ui.Alignment.Center),
    )

    var windowVisible by remember { mutableStateOf(true) }
    var trayHintShown by remember { mutableStateOf(false) }

    val iconPainter = remember { loadWindowIcon() }
    val trayController = remember {
        if (java.awt.SystemTray.isSupported()) {
            TrayController(
                controller = controller,
                onShow = {
                    windowState.isMinimized = false
                    windowVisible = true
                },
                onExit = {
                    controller.shutdown()
                    exitApplication()
                },
            )
        } else {
            null
        }
    }

    if (trayController != null) {
        androidx.compose.runtime.DisposableEffect(trayController) {
            trayController.install()
            onDispose { trayController.remove() }
        }
        LaunchedEffect(
            controller.isPlaying,
            controller.currentTrack?.path,
            controller.playMode,
            controller.currentTab,
            controller.openPlaylistId,
        ) {
            trayController.refresh()
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            runCatching {
                prefs.putInt("window.width", windowState.size.width.value.toInt())
                prefs.putInt("window.height", windowState.size.height.value.toInt())
            }
        }
    }

    Window(
        onCloseRequest = {
            if (trayController != null) {
                // Не прячем окно безвозвратно: сворачиваем, чтобы кнопка
                // осталась на панели задач. Музыка при этом продолжает играть.
                windowState.isMinimized = true
                if (!trayHintShown) {
                    trayHintShown = true
                    trayController.showHint()
                }
            } else {
                controller.shutdown()
                exitApplication()
            }
        },
        visible = windowVisible,
        state = windowState,
        title = controller.currentTrack?.let { "${it.title} — ZenMusic" } ?: "ZenMusic",
        icon = iconPainter,
        onPreviewKeyEvent = { event -> handleShortcut(event, controller) },
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = java.awt.Dimension(980, 620)
        }
        LaunchedEffect(windowVisible) {
            if (windowVisible) {
                window.toFront()
                window.requestFocus()
            }
        }
        ZenMusicApp(controller)
    }
}

private fun handleShortcut(event: KeyEvent, controller: AppController): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (!event.isCtrlPressed) return false

    return when (event.key) {
        Key.Spacebar -> {
            controller.togglePlayPause()
            true
        }

        Key.DirectionRight -> {
            controller.next()
            true
        }

        Key.DirectionLeft -> {
            controller.previous()
            true
        }

        Key.DirectionUp -> {
            controller.updateVolume((controller.volume + 0.05f).coerceAtMost(1f))
            controller.persistVolume()
            true
        }

        Key.DirectionDown -> {
            controller.updateVolume((controller.volume - 0.05f).coerceAtLeast(0f))
            controller.persistVolume()
            true
        }

        else -> false
    }
}
