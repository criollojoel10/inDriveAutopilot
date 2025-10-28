package dev.joel.indriveautopilot

import io.github.libxposed.api.XposedContext
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Method

/**
 * Entry de libxposed (API moderna) según tu AAR:
 *  - Ctor: (XposedContext, XposedModuleInterface.ModuleLoadedParam)
 *  - Callbacks: en XposedModuleInterface.*
 */
class MainModule(
    base: XposedContext,
    param: XposedModuleInterface.ModuleLoadedParam
) : XposedModule(base, param) {

    companion object {
        lateinit var iface: XposedInterface
            private set
    }

    init {
        // En tu build, XposedContext implementa XposedInterface
        iface = base as XposedInterface
        log("InDriveAutopilot: MainModule inicializado. Framework=${iface.frameworkName} v${iface.frameworkVersion}")
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val pkg = param.getPackageName()
        if (pkg != "sinet.startup.inDriver") return

        // ClassLoader seguro (default en A10+ o fallback)
        val cl: ClassLoader = try {
            param.getDefaultClassLoader()
        } catch (_: Throwable) {
            param.getClassLoader()
        }

        try {
            val activityClass = Class.forName("android.app.Activity", false, cl)
            val onResume: Method = activityClass.getDeclaredMethod("onResume")

            // Opcional: evitar inline
            deoptimize(onResume)

            // En tu AAR, hook espera una INSTANCIA de Hooker<Method>
            hook(onResume, ActivityOnResumeHooker())

            log("InDriveAutopilot: Hook instalado -> Activity.onResume() en $pkg")
        } catch (t: Throwable) {
            log("InDriveAutopilot: error instalando hooks: ${t.message}", t)
        }
    }
}

/**
 * Hooker sin anotaciones, compatible con tu api:100.
 * La interfaz genérica declara:
 *  - before(BeforeHookCallback<Method>): Unit
 *  - after (AfterHookCallback<Method>): Unit
 * (No dependemos de cb.member / cb.getMember() para evitar incompatibilidades del AAR.)
 */
class ActivityOnResumeHooker : XposedInterface.Hooker<Method> {

    override fun before(cb: XposedInterface.BeforeHookCallback<Method>) {
        try {
            MainModule.iface.log("InDriveAutopilot: before onResume()")
        } catch (_: Throwable) {
            // no-op
        }
    }

    override fun after(cb: XposedInterface.AfterHookCallback<Method>) {
        try {
            MainModule.iface.log("InDriveAutopilot: after onResume()")
        } catch (_: Throwable) {
            // no-op
        }
    }
}