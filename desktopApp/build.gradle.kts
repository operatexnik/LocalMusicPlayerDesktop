import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Чтение тегов и обложек: mp3, flac, ogg, wav, m4a
    implementation("net.jthink:jaudiotagger:3.0.1")

    // Декодеры для Java Sound
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("com.googlecode.soundlibs:vorbisspi:1.0.3.3")
    implementation("org.jflac:jflac-codec:1.5.2")
}

compose.desktop {
    application {
        mainClass = "com.zenmusic.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "ZenMusic"
            packageVersion = "1.0.0"
            description = "ZenMusic — локальный музыкальный плеер"
            vendor = "ZenMusic"

            windows {
                iconFile.set(project.file("src/main/resources/app.ico"))
                menu = true
                shortcut = true
                perUserInstall = true
                upgradeUuid = "b5f4f2d0-6a4c-4c8e-9f8e-2c1f2a7f9e11"
            }
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("-Dfile.encoding=UTF-8")
}
