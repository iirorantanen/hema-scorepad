// The browser version: a screen in plain HTML and CSS and two adapters. It holds no scoring or timing rule (ADR 0002).
import java.security.MessageDigest

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

// The site gets its service worker here and not from the resources: the service worker names every file of the site and a hash
// of their contents, so a change to any file makes it a new version (src/jsMain/sw.js).
// The development server has no service worker, so it never serves a page from a cache.
tasks.named<Sync>("jsBrowserDistribution") {
    val worker = file("src/jsMain/sw.js")
    inputs.file(worker)
    doLast {
        // The source map is for the browser's developer tools; the scoreboard does not need it.
        val files = destinationDir.walk().filter { it.isFile && it.extension != "map" }.sorted().toList()
        val digest = MessageDigest.getInstance("SHA-256")
        files.forEach { digest.update(it.readBytes()) }
        val version = digest.digest().take(8).joinToString("") { "%02x".format(it) }
        // "./" is the address of the site itself, which answers with index.html.
        val names = (listOf("./") + files.map { it.relativeTo(destinationDir).invariantSeparatorsPath }).joinToString { "\"$it\"" }
        destinationDir.resolve("sw.js").writeText("const VERSION = \"$version\", FILES = [$names];\n" + worker.readText())
    }
}
