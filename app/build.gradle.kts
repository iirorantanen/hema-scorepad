plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Optional release inputs, set by .github/workflows/release.yml as -P flags or
// ORG_GRADLE_PROJECT_* environment variables. Local builds need none of them.
fun releaseInput(name: String): String? = providers.gradleProperty(name).orNull

android {
    namespace = "io.github.iirorantanen.hemascoreboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.iirorantanen.hemascoreboard"
        minSdk = 26 // first level with VibrationEffect, so vibration needs no legacy branch
        targetSdk = 36
        versionCode = releaseInput("versionCode")?.toInt() ?: 1
        versionName = releaseInput("versionName") ?: "0.1"
    }

    signingConfigs {
        releaseInput("signingStoreFile")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = releaseInput("signingStorePassword")
                keyAlias = releaseInput("signingKeyAlias")
                keyPassword = releaseInput("signingKeyPassword")
            }
        }
    }

    buildTypes {
        // Without the signing inputs there is no "release" config and the APK stays unsigned.
        release { signingConfig = signingConfigs.findByName("release") }
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":bout"))
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.13.0")
}
