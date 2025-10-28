plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "dev.joel.indriveautopilot"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.joel.indriveautopilot"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Puedes ofuscar; ya incluimos reglas R8 actualizadas
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Asegurar Java/Kotlin 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
        }
    }
}

dependencies {
    // UI opcional (para SettingsActivity, preferencias, etc.)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // API moderna de libxposed: NO empaquetar
    compileOnly("io.github.libxposed:api:100")
}