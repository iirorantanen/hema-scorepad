import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin

plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.11" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

// Node.js is downloaded from the repository in settings.gradle.kts, so the Kotlin plugin adds none of its own.
allprojects {
    plugins.withType<NodeJsPlugin> { the<NodeJsEnvSpec>().downloadBaseUrl = null }
}

// Fails when a file that holds a copy of Fiore's shapes differs from what the master gives (README, Development).
val brandCheck by tasks.registering(Exec::class) {
    commandLine("python3", "scripts/brand/derive.py", "--check")
}
// The ktlint plugin adds the root check task after this script has run.
tasks.matching { it.name == "check" }.configureEach { dependsOn(brandCheck) }
