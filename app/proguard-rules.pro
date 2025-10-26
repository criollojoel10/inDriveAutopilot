#############################################
# R8 / ProGuard - inDriveAutopilot (Modern API)
#############################################

# 1) Entry class del módulo (no cambiar nombre ni constructor)
-keep class dev.joel.indriveautopilot.MainModule { *; }
-keepnames class dev.joel.indriveautopilot.MainModule
-keep class dev.joel.indriveautopilot.MainModule {
    public <init>(io.github.libxposed.api.XposedContext,
                  io.github.libxposed.api.XposedModuleInterface$ModuleLoadedParam);
}

# 2) API moderna de libxposed (framework la inyecta en runtime)
-keep class io.github.libxposed.** { *; }
-dontwarn io.github.libxposed.**

# 3) Hookers (por si minificas agresivo)
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }
# (Opcional) Conservar nombres de before/after si tu flujo los requiere explícitos
# -keepclassmembers class * implements io.github.libxposed.api.XposedInterface$Hooker {
#     public void before(...);
#     public void after(...);
# }

# 4) Mantener metadatos y permitir adaptar nombres en recursos
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-adaptresourcefilenames
-adaptresourcefilecontents

# 5) Asegurar archivos del módulo moderno (META-INF/xposed/**)
-keepresourcefiles META-INF/xposed/java_init.list
-keepresourcefiles META-INF/xposed/module.prop
-keepresourcefiles META-INF/xposed/scope.list