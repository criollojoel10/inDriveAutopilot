#############################################
# R8 / ProGuard - inDriveAutopilot (Modern API)
#############################################

# 1) Clase de entrada del módulo: conservar clase y firma del constructor moderna
-keep class dev.joel.indriveautopilot.MainModule {
    public <init>(io.github.libxposed.api.XposedInterface,
                  io.github.libxposed.api.XposedModule$ModuleLoadedParam);
    *;
}
-keepnames class dev.joel.indriveautopilot.MainModule

# 2) API moderna de libxposed (no la empaquetas, pero conserva firmas para el runtime)
-keep class io.github.libxposed.** { *; }
-dontwarn io.github.libxposed.**

# 3) Hookers por anotaciones
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# 4) Mantener los archivos de configuración del módulo moderno
-keepresourcefiles META-INF/xposed/java_init.list
-keepresourcefiles META-INF/xposed/module.prop
-keepresourcefiles META-INF/xposed/scope.list