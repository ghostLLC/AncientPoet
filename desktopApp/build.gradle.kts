plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        desktopMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ancientpoet.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg
            )
            packageName = "AncientPoet"
            packageVersion = "0.1.0"
        }
    }
}
