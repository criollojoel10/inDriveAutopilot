pluginManagement {repositories {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Añade esta línea para el repositorio de Xposed
        maven { url = uri("https://api.xposed.info/") }
    }
}
rootProject.name = "inDriveAutopilot"
include(":app")
