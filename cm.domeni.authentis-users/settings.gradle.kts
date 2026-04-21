rootProject.name = "cm.domeni.authentis-users"

val defaultKapitaPlatformLibsDir = file("../../cm.domeni.kapita/platform-libs")
val kapitaPlatformLibsDir =
    startParameter.projectProperties["kapitaPlatformLibsDir"]?.let(::file)
        ?: System.getenv("KAPITA_PLATFORM_LIBS_DIR")?.let(::file)
        ?: defaultKapitaPlatformLibsDir

if (kapitaPlatformLibsDir.exists()) {
    includeBuild(kapitaPlatformLibsDir)
}
