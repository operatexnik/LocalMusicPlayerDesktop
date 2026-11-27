pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "ZenMusicDesktop"

// Старая экспериментальная KMP-заготовка больше не участвует в сборке.
// Рабочее десктоп-приложение находится в модуле :desktopApp.
include(":desktopApp")
