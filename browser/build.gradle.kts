// The browser version: a screen in plain HTML and CSS and two adapters. It holds no scoring or timing rule (ADR 0002).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    js {
        browser() // webpack.config.d/ adjusts the dev server
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies { implementation(project(":bout")) }
    }
}
