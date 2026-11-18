import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

// Bout logic: shared Kotlin with no Android and no JVM-only API. All scoring and timing rules live here.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlinx.kover")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    jvm {
        // Same bytecode level as the Android app module that consumes this.
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    js { nodejs() } // the bout tests run in Node.js

    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

// Fails when a bout test run on the JVM leaves a line or a branch of the bout module unrun, and names the line.
// Kover's own verification gives a percentage and no line, so this reads the lines out of its report.
val boutCoverage = tasks.register("boutCoverage") {
    val report = layout.buildDirectory.file("reports/kover/report.xml")
    dependsOn("koverXmlReport")
    doLast {
        val lines = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(report.get().asFile).getElementsByTagName("line")
        // Per line the report counts missed and covered instructions (mi, ci) and missed branches (mb).
        val missed = (0 until lines.length).map { lines.item(it) as Element }
            .filter { it.getAttribute("mi") != "0" || it.getAttribute("mb") != "0" }
            .map { line ->
                val part = if (line.getAttribute("ci") == "0") "not run" else "run in part"
                "  ${(line.parentNode as Element).getAttribute("name")}:${line.getAttribute("nr")} $part"
            }
        if (missed.isNotEmpty()) throw GradleException("No bout test runs all of:\n" + missed.joinToString("\n"))
    }
}
tasks.check { dependsOn(boutCoverage) }
