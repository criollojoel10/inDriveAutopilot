// settings.gradle.kts — Repos centralizados

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // Necesario para resolver org.jetbrains.kotlin.plugin.compose y otros plugins
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 👉 Primero mavenLocal para que tome el AAR local de libxposed
        mavenLocal {
            content {
                includeGroup("io.github.libxposed")
            }
        }
        google()
        mavenCentral()
        // NO agregar https://api.xposed.info/ si usas Modern API (solo legacy).
    }
}

rootProject.name = "inDriveAutopilot"
include(":app")