plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ancientpoet.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ancientpoet.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["JPUSH_APPKEY"] = System.getenv("JPUSH_APP_KEY") ?: ""
        manifestPlaceholders["JPUSH_CHANNEL"] = "developer-default"
    }

    buildFeatures {
        compose = true
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
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // JPush (极光推送) — uncomment + sync after creating JPush account at jiguang.cn
    // implementation("cn.jiguang.sdk:jpush:5.3.0")
    // implementation("cn.jiguang.sdk:jcore:4.1.0")
}
