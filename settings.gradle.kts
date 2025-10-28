// settings.gradle.kts — Repos centralizados

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // Kotlin/otros plugins
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Si publicas un AAR local de libxposed, mantenlo primero
        mavenLocal {
            content { includeGroup("io.github.libxposed") }
        }
        google()
        mavenCentral()
        // NO agregar https://api.xposed.info/ (solo para API legado)
    }
}

rootProject.name = "inDriveAutopilot"
include(":app")