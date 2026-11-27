package com.zenmusic.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.zenmusic.desktop.ui.AppController
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Иконка в системном трее. Окно можно закрыть — музыка продолжит играть.
 */
class TrayController(
    private val controller: AppController,
    private val onShow: () -> Unit,
    private val onExit: () -> Unit,
) {
    private var trayIcon: TrayIcon? = null
    private var popup: PopupMenu? = null

    fun install() {
        if (trayIcon != null) return
        val image = loadTrayImage() ?: return
        val icon = TrayIcon(image, "ZenMusic")
        icon.isImageAutoSize = true
        icon.addActionListener { onShow() }
        val menu = buildMenu()
        popup = menu
        icon.popupMenu = menu
        runCatching {
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
        }
    }

    fun refresh() {
        val icon = trayIcon ?: return
        val menu = buildMenu()
        popup = menu
        icon.popupMenu = menu
        val title = controller.currentTrack?.title
        icon.toolTip = if (title != null) "ZenMusic — $title" else "ZenMusic"
    }

    fun showHint() {
        trayIcon?.displayMessage(
            "ZenMusic",
            "Плеер свёрнут и продолжает играть. Окно осталось на панели задач, а в трее доступно меню управления.",
            TrayIcon.MessageType.INFO,
        )
    }

    fun remove() {
        trayIcon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
        popup = null
    }

    private fun buildMenu(): PopupMenu {
        val menu = PopupMenu()

        val open = MenuItem("Открыть ZenMusic")
        open.addActionListener { onShow() }
        menu.add(open)

        val track = controller.currentTrack
        val nowPlaying = MenuItem(
            track?.let { "♪ ${it.title}".take(64) } ?: "Ничего не играет",
        )
        nowPlaying.isEnabled = false
        menu.add(nowPlaying)

        menu.addSeparator()

        val toggle = MenuItem(if (controller.isPlaying) "Пауза" else "Воспроизвести")
        toggle.isEnabled = track != null
        toggle.addActionListener { controller.togglePlayPause() }
        menu.add(toggle)

        val next = MenuItem("Следующий трек")
        next.isEnabled = controller.queue.isNotEmpty()
        next.addActionListener { controller.next() }
        menu.add(next)

        val previous = MenuItem("Предыдущий трек")
        previous.isEnabled = controller.queue.isNotEmpty()
        previous.addActionListener { controller.previous() }
        menu.add(previous)

        menu.addSeparator()

        val exit = MenuItem("Выход")
        exit.addActionListener { onExit() }
        menu.add(exit)

        return menu
    }

    private fun loadTrayImage(): BufferedImage? = runCatching {
        val stream = TrayController::class.java.getResourceAsStream("/icon.png") ?: return null
        val original = ImageIO.read(stream) ?: return null
        val size = 32
        val scaled = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC,
        )
        g.drawImage(original, 0, 0, size, size, null)
        g.dispose()
        scaled
    }.getOrNull()
}

fun loadWindowIcon(): Painter? = runCatching {
    val stream = TrayController::class.java.getResourceAsStream("/icon.png") ?: return null
    val image = ImageIO.read(stream) ?: return null
    BitmapPainter(image.toComposeImageBitmap())
}.getOrNull()
