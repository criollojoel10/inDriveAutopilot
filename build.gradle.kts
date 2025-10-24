 url=https://github.com/cavirovi/inDriveAutopilot/blob/36a58df935437927f26c7d5c00e9d0e62f75fc27/app/build.gradle.kts
plugins {
    // Usa los aliases del version catalog (gradle/libs.versions.toml)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.joel.indriveautopilot"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.joel.indriveautopilot"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // ===== Compose configuration =====
    buildFeatures {
        compose = true
    }

    composeOptions {
        // Si Android Studio te indica otra versión compatible con Kotlin 2.0.21,
        // cambia este valor al recomendado por el error.
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += setOf("META-INF/**.kotlin_module")
        }
    }
}

dependencies {
    // BOM para alinear versiones de Compose
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // Tu dependencia original en modo compileOnly si corresponde
    compileOnly("io.github.libxposed:api:100")
}
