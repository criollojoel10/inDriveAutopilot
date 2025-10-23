plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

    packaging {
        resources {
            // Asegura que empaquetamos recursos modernos sin conflictos
            excludes += setOf("META-INF/**.kotlin_module")
        }
    }
}

dependencies {
    // Modern Xposed API: proveída por el framework en runtime
    compileOnly("io.github.libxposed:api:100")
}
