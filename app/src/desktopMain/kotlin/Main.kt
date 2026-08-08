import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

private const val APP_NAME = "Мой Плеер"   // ← своё название

fun main() = application {
    val windowState = rememberWindowState(width = 480.dp, height = 700.dp)
    var isVisible by remember { mutableStateOf(true) }
    var isAppRunning by remember { mutableStateOf(true) }

    val trayIcon = remember {
        createTrayIcon(
            onShow = { isVisible = true },
            onExit = {
                isAppRunning = false
                exitApplication()
            }
        )
    }

    DisposableEffect(Unit) {
        if (SystemTray.isSupported()) {
            try {
                SystemTray.getSystemTray().add(trayIcon)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            if (SystemTray.isSupported()) {
                try {
                    SystemTray.getSystemTray().remove(trayIcon)
                } catch (_: Exception) {}
            }
        }
    }

    if (isAppRunning) {
        Window(
            onCloseRequest = { isVisible = false },
            visible = isVisible,
            title = APP_NAME,
            state = windowState
        ) {
            App()
        }
    }
}

private fun createTrayIcon(
    onShow: () -> Unit,
    onExit: () -> Unit
): TrayIcon {
    val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.color = Color(30, 144, 255)
    g.fillOval(0, 0, 16, 16)
    g.dispose()

    val trayIcon = TrayIcon(image, APP_NAME)
    trayIcon.isImageAutoSize = true

    val popup = PopupMenu()

    val showItem = MenuItem("Открыть")
    showItem.addActionListener { SwingUtilities.invokeLater { onShow() } }

    val exitItem = MenuItem("Выход")
    exitItem.addActionListener { SwingUtilities.invokeLater { onExit() } }

    popup.add(showItem)
    popup.addSeparator()
    popup.add(exitItem)

    trayIcon.popupMenu = popup
    trayIcon.addActionListener { SwingUtilities.invokeLater { onShow() } }

    return trayIcon
}