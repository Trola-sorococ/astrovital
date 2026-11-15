plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.orbita.bienestar"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "org.orbita.bienestar"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "0.7.0"

    // Kotlin y Java compilan para la misma versión de bytecode requerida por Android Gradle Plugin.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    }

kotlinOptions { jvmTarget = "17" }
}
