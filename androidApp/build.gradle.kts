import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val validateReleaseEndpoint = tasks.register("validateReleaseEndpoint") {
    doLast {
        val configured = providers.gradleProperty("ANCIENT_POET_API_BASE_URL").orNull
        require(!configured.isNullOrBlank()) { "Release requires -PANCIENT_POET_API_BASE_URL=https://your-server/api/v1/" }
        val uri = URI(configured)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null && uri.path.endsWith("/api/v1/")) {
            "Release API endpoint must be an HTTPS URL ending in /api/v1/, without credentials or query parameters"
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseEndpoint) }

val apiBaseUrl = providers.gradleProperty("ANCIENT_POET_API_BASE_URL")
    .orElse("http://10.0.2.2:8080/api/v1/")

android {
    namespace = "com.ancientpoet.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ancientpoet.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        getByName("debug") { buildConfigField("String", "API_BASE_URL", "\"" + apiBaseUrl.get() + "\"") }
        getByName("release") {
            buildConfigField("String", "API_BASE_URL", "\"" + providers.gradleProperty("ANCIENT_POET_API_BASE_URL").orElse("https://unconfigured.invalid/api/v1/").get() + "\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.sqldelight.android)
    implementation(libs.sqldelight.runtime)
    implementation(libs.androidx.core)
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation(libs.androidx.activity.compose)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
