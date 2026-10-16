plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":core-model"))
    implementation(project(":core-iptv"))
    implementation(project(":core-xtream"))
    implementation(project(":feature-ai-assist"))
    implementation(project(":feature-sports-picks"))
    implementation(project(":feature-community"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    // OkHttp & MockWebServer for local test server
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
}

tasks.test {
    useJUnitPlatform()
}
