package com.zenmusic.desktop.util

import java.awt.Desktop
import java.io.File
import java.text.Normalizer
import java.util.Locale

/** 197000 -> "3:17", 3670000 -> "1:01:10" */
fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

fun tracksWord(count: Int): String {
    val n = count % 100
    if (n in 11..14) return "треков"
    return when (count % 10) {
        1 -> "трек"
        2, 3, 4 -> "трека"
        else -> "треков"
    }
}

fun playlistsWord(count: Int): String {
    val n = count % 100
    if (n in 11..14) return "плейлистов"
    return when (count % 10) {
        1 -> "плейлист"
        2, 3, 4 -> "плейлиста"
        else -> "плейлистов"
    }
}

fun fileNameWithoutExtension(name: String): String = name.substringBeforeLast('.')

fun normalizeForSearch(value: String): String =
    Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFC)

/**
 * Android-версия тащит этот костыль из-за криво записанных тегов.
 * На десктопе теги бывают испорчены ещё и перепутанным порядком байт UTF-16
 * (например, «Pyrokinesis» превращается в «倀礀爀漀欀椀渀攀猀椀猀»).
 */
fun fixEncoding(text: String): String {
    if (text.isEmpty()) return text
    val cp1251 = runCatching { charset("windows-1251") }.getOrNull() ?: return text

    val candidates = LinkedHashSet<String>()
    candidates.add(text)

    // Перепутанный порядок байт UTF-16: меняем местами байты каждого code unit.
    if (text.none { it.isSurrogate() }) {
        val swapped = CharArray(text.length) { index ->
            val code = text[index].code
            (((code and 0xFF) shl 8) or ((code shr 8) and 0xFF)).toChar()
        }
        candidates.add(String(swapped))
    }

    // Классические однобайтовые кракозябры пробуем исправлять только если
    // строка реально на них похожа. Иначе можно сломать нормальный текст
    // с диакритикой (Mötley Crüe, café и т.п.).
    if (looksLikeBrokenEncoding(text)) {
        runCatching { candidates.add(String(text.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)) }
        runCatching { candidates.add(String(text.toByteArray(Charsets.ISO_8859_1), cp1251)) }
        runCatching { candidates.add(String(text.toByteArray(Charsets.UTF_8), cp1251)) }
        runCatching { candidates.add(String(text.toByteArray(cp1251), Charsets.UTF_8)) }
    }

    val best = candidates.minByOrNull(::mojibakeScore) ?: text
    return Normalizer.normalize(best, Normalizer.Form.NFC)
}

private fun looksLikeBrokenEncoding(text: String): Boolean {
    if (text.any { it == '\uFFFD' || it.code in 0x7F..0x9F }) return true
    if (text.any {
            it.code in 0x3000..0x9FFF ||   // CJK, хирагана, катакана
                it.code in 0x0590..0x06FF || // иврит, арабский
                it.code in 0xFF00..0xFFEF    // полноширинные символы
        }
    ) {
        return true
    }
    if (text.contains("Ð") || text.contains("Ñ") || text.contains("Ã") ||
        text.contains("Â") || text.contains("â€")
    ) {
        return true
    }

    val letters = text.count { it.isLetter() }
    val latin1Letters = text.count { it.isLetter() && it.code in 0x80..0xFF }
    return letters > 0 && latin1Letters * 100 / letters >= 40
}

private fun mojibakeScore(s: String): Int {
    var score = 0
    for (c in s) {
        val code = c.code
        score += when {
            c == '\uFFFD' -> 1000
            code < 0x20 -> 200
            code in 0x7F..0x9F -> 200
            isExotic(code) -> 40
            code in 0x80..0xFF -> 8
            else -> 0
        }
    }
    return score
}

/**
 * Символы из «экзотических» блоков Unicode, которые почти наверняка
 * появились из-за перепутанного UTF-16, а не из-за реального названия трека.
 * Латинская и кириллическая письменность, пунктуация и эмодзи разрешены.
 */
private fun isExotic(code: Int): Boolean {
    if (code < 0x0530) return false
    if (code in 0xD800..0xDFFF) return false
    return when (code) {
        in 0x2000..0x206F,
        in 0x20A0..0x20CF,
        in 0x2100..0x214F,
        in 0x2190..0x21FF,
        in 0x2200..0x22FF,
        in 0x25A0..0x25FF,
        in 0x2600..0x27BF,
        in 0x1F000..0x1FAFF -> false
        else -> true
    }
}

fun openInExplorer(path: String) {
    runCatching {
        val file = File(path)
        if (file.isDirectory) {
            Desktop.getDesktop().open(file)
        } else {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                // Windows: открыть проводник и выделить файл
                Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", file.absolutePath))
            } else {
                Desktop.getDesktop().open(file.parentFile)
            }
        }
    }
}
