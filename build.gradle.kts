import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin

plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

// Node.js is downloaded from the repository in settings.gradle.kts, so the Kotlin plugin adds none of its own.
allprojects {
    plugins.withType<NodeJsPlugin> { the<NodeJsEnvSpec>().downloadBaseUrl = null }
}
