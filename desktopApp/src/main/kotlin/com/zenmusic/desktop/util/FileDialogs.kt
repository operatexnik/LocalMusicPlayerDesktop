package com.zenmusic.desktop.util

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun chooseMusicFolder(initialFolder: String? = null): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Выберите папку с музыкой"
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
        initialFolder?.let { path ->
            runCatching { File(path).takeIf { it.isDirectory }?.let { currentDirectory = it } }
        }
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}

fun chooseImageFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Выберите обложку"
        fileSelectionMode = JFileChooser.FILES_ONLY
        fileFilter = FileNameExtensionFilter(
            "Изображения (png, jpg, jpeg, bmp, gif)",
            "png", "jpg", "jpeg", "bmp", "gif",
        )
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}
