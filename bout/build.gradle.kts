import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Bout logic: shared Kotlin with no Android and no JVM-only API. All scoring and timing rules live here.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
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
