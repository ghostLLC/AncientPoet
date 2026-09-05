import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("io.ktor.plugin") version "3.0.3"
}

group = "com.ancientpoet"
version = "0.2.0"

application {
    mainClass.set("com.ancientpoet.server.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${project.hasProperty("dev")}")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation("com.zaxxer:HikariCP:6.3.3")
    implementation("com.tencentcloudapi:tencentcloud-sdk-java-sms:3.2.18")
    implementation(enforcedPlatform("io.netty:netty-bom:4.1.137.Final"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.2"))
    constraints {
        implementation("org.apache.commons:commons-lang3:3.20.0")
        implementation("org.apache.commons:commons-configuration2:2.15.1")
        implementation("org.bouncycastle:bcprov-jdk18on:1.85.2")
    }
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)

    // Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation("org.jetbrains.exposed:exposed-java-time:0.57.0")
    implementation(libs.exposed.json)

    // Database
    implementation(libs.postgres.jdbc)
    implementation(libs.postgis.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)

    // Redis
    implementation(libs.jedis)

    // MinIO
    implementation("io.minio:minio:8.6.0")

    // Koin DI
    implementation(libs.koin.core)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Logging
    implementation(libs.logback.classic)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.koin.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar>().configureEach {
    mergeServiceFiles()
}
