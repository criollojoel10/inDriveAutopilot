package dev.joel.indriveautopilot

import io.github.libxposed.api.XposedContext
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * Entry de la Modern API (libxposed) para el artefacto que espera XposedContext.
 * Declarado en: src/main/resources/META-INF/xposed/java_init.list
 * FQCN: dev.joel.indriveautopilot.MainModule
 */
class MainModule(
    base: XposedContext,
    param: ModuleLoadedParam
) : XposedModule(base, param) {

    private val targetPkg = "sinet.startup.inDriver"

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != targetPkg) return

        try {
            log("[Autopilot] Cargando en ${param.packageName}")

            dev.joel.indriveautopilot.hooks.ActivityHooks.install(
                classLoader = param.classLoader,
                module = this
            )

            log("[Autopilot] Instalación mínima OK en ${param.packageName}")
        } catch (t: Throwable) {
            log("[Autopilot][ERROR] ${t.stackTraceToString()}")
        }
    }
}