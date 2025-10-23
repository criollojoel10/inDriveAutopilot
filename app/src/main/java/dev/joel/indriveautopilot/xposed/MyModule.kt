package dev.joel.indriveautopilot.xposed

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.LoadedModuleParam
import io.github.libxposed.api.PackageLoadedParam

class MyModule(base: XposedInterface, param: LoadedModuleParam) : XposedModule(base, param) {
    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "ru.indriver.android") return
        base.log("inDriveAutopilot: cargado en ${param.packageName}")
        // TODO: añade aquí tus hooks (Modern API).
        // Consejo: empieza con un hook simple y logs para validar carga.
    }
}
