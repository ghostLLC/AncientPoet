pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AncientPoet"

include(":shared")
include(":server")
include(":androidApp")
include(":desktopApp")
