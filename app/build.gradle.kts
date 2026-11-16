plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.iirorantanen.hemascoreboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.iirorantanen.hemascoreboard"
        minSdk = 26 // first level with VibrationEffect, so vibration needs no legacy branch
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":bout"))
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.13.0")
}
