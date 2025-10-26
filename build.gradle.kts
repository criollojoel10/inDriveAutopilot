// build.gradle.kts (root) — Alinea toolchains/targets para Java y Kotlin en todos los módulos

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Mantén estos alias si usas Version Catalog (libs.versions.toml)
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// No declares repositories aquí (se controlan desde settings.gradle.kts)

subprojects {
    // Unifica la toolchain de Java a 17 para módulos que aplican el plugin Java
    plugins.withType<JavaPlugin> {
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    // Unifica la toolchain y el bytecode de Kotlin (Android/JVM) a 17
    plugins.withType(KotlinBasePluginWrapper::class.java) {
        // Toolchain de Kotlin (elige JDK 17 para compilar)
        extensions.configure(KotlinProjectExtension::class.java) {
            jvmToolchain(17)
        }
        // Bytecode target para Kotlin
        tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                // Puedes agregar flags extra si lo necesitas:
                // freeCompilerArgs.add("-Xjsr305=strict")
            }
        }
    }
}