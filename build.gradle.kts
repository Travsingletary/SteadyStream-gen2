plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("com.android.application") version "8.7.2" apply false
}

allprojects {
    group = "com.skyras.tv"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        google()
    }
}
