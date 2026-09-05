pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "AncientPoet"

include(":shared")
include(":server")
include(":androidApp")
include(":desktopApp")
