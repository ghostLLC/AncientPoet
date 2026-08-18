package com.ancientpoet.server.service

import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.config.RedisConfig
import io.minio.BucketExistsArgs
import io.minio.MinioClient
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ReadinessReport(
    val postgres: Boolean,
    val redis: Boolean,
    val minio: Boolean,
) {
    val ready: Boolean get() = postgres && redis && minio
}

fun interface ReadinessChecker {
    suspend fun check(): ReadinessReport
}

class InfrastructureReadinessChecker(
    config: AppConfig,
) : ReadinessChecker {
    private val minio = MinioClient.builder()
        .endpoint(config.minioEndpoint)
        .credentials(config.minioAccessKey, config.minioSecretKey)
        .build()
        .also { it.setTimeout(CHECK_TIMEOUT_MS, CHECK_TIMEOUT_MS, CHECK_TIMEOUT_MS) }
    private val minioBucket = config.minioBucket

    override suspend fun check(): ReadinessReport = ReadinessReport(
        postgres = boundedCheck {
            transaction {
                exec("SELECT 1") { result -> result.next() } ?: false
            }
        },
        redis = boundedCheck {
            RedisConfig.pool.resource.use { jedis -> jedis.ping() == "PONG" }
        },
        minio = boundedCheck {
            // A successful call proves reachability. The upload path creates the
            // application bucket lazily when a clean environment is first used.
            minio.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build())
            true
        },
    )

    private suspend fun boundedCheck(block: () -> Boolean): Boolean =
        withTimeoutOrNull(CHECK_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                runCatching(block).getOrDefault(false)
            }
        } ?: false

    private companion object {
        const val CHECK_TIMEOUT_MS = 2_000L
    }
}
