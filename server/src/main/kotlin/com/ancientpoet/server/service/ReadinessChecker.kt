package com.ancientpoet.server.service

import com.ancientpoet.server.config.*
import kotlinx.serialization.Serializable

@Serializable
data class ReadinessReport(
    val postgres: Boolean,
    val redis: Boolean = false,
    val minio: Boolean = false,
    val optionalServicesRequired: Boolean = false
) {
    val ready: Boolean get() = postgres && (!optionalServicesRequired || (redis && minio))
}
fun interface ReadinessChecker {
    suspend fun check(): ReadinessReport
}
class InfrastructureReadinessChecker(config: AppConfig) : ReadinessChecker {
    override suspend fun check() = ReadinessReport(
        postgres = runCatching {
            database { db -> db.rows("SELECT 1") { it.getInt(1) == 1 }.single() }
        }.getOrDefault(false)
    )
}
