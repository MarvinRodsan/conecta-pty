pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ConectaPTY"
include(":app")
// Genera el Baseline Profile y mide el arranque en frío (necesita un teléfono o emulador)
include(":baselineprofile")
