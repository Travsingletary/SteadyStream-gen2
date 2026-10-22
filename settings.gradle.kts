pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "skyras-tv"
include(":core-model")
include(":core-iptv")
include(":core-xtream")
include(":core-prefs")
include(":feature-ai-assist")
include(":feature-sports-picks")
include(":feature-community")
include(":app-tv")
