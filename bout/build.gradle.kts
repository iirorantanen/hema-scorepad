import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Bout logic: plain Kotlin, no Android dependencies. All scoring and timing rules live here.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Same bytecode level as the Android app module that consumes this.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_11 }
}

dependencies {
    testImplementation(kotlin("test"))
}
