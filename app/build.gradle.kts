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
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = false // activa luego si quieres
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
        }
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // **Necesario** para los estilos Theme.Material3.* (MDC-Android / Views)
    implementation("com.google.android.material:material:1.11.0") // o la última disponible

    // Si usas algún widget AppCompat (Toolbar, etc.)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Ya tenías esto para libxposed en compileOnly (no lo toco)
    compileOnly("io.github.libxposed:api:100")

// AndroidX Preference (incluye attrs como iconSpaceReserved)
    implementation("androidx.preference:preference-ktx:1.2.1")

}