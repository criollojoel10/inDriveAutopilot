package dev.joel.indriveautopilot.hooks

import io.github.libxposed.api.XposedModule

object ActivityHooks {
    /**
     * Hook mínimo "no-op": de momento solo emite logs para confirmar que
     * el módulo cargó en el proceso de la app objetivo.
     *
     * Cuando confirmes que carga, agregamos tus hooks reales.
     */
    fun install(classLoader: ClassLoader, module: XposedModule) {
        module.log("[Autopilot] ActivityHooks.install() - classLoader=$classLoader")
        // Aquí luego añadimos hook reales (anotaciones o helper libxposed).
    }
}