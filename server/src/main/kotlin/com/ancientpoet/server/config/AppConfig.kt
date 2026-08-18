package com.ancientpoet.server.config

data class AppConfig(
    val port: Int,
    val host: String,
    val postgresHost: String,
    val postgresPort: Int,
    val postgresDb: String,
    val postgresUser: String,
    val postgresPassword: String,
    val redisHost: String,
    val redisPort: Int,
    val redisPassword: String?,
    val minioEndpoint: String,
    val minioAccessKey: String,
    val minioSecretKey: String,
    val minioBucket: String,
    val deepseekApiKey: String,
    val deepseekBaseUrl: String,
    val deepseekModelChat: String,
    val deepseekModelVision: String,
    val deepseekTemperature: Double,
    val deepseekMaxTokens: Int,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtAccessExpireMinutes: Long,
    val jwtRefreshExpireDays: Long,
    val smsProvider: String,
    val smsAccessKey: String,
    val smsAccessSecret: String,
    val smsSignName: String,
    val smsTemplateCode: String,
    val jpushAppKey: String,
    val jpushMasterSecret: String,
) {
    companion object {
        fun fromEnvironment(): AppConfig = AppConfig(
            port = envInt("APP_PORT", 8080),
            host = env("APP_HOST", "0.0.0.0"),
            postgresHost = env("POSTGRES_HOST", "localhost"),
            postgresPort = envInt("POSTGRES_PORT", 5432),
            postgresDb = env("POSTGRES_DB", "ancientpoet"),
            postgresUser = env("POSTGRES_USER", "ancientpoet"),
            postgresPassword = env("POSTGRES_PASSWORD", "changeme"),
            redisHost = env("REDIS_HOST", "localhost"),
            redisPort = envInt("REDIS_PORT", 6379),
            redisPassword = optionalEnv("REDIS_PASSWORD"),
            minioEndpoint = env("MINIO_ENDPOINT", "http://localhost:9000"),
            minioAccessKey = env("MINIO_ACCESS_KEY", "minioadmin"),
            minioSecretKey = env("MINIO_SECRET_KEY", "minioadmin"),
            minioBucket = env("MINIO_BUCKET", "ancientpoet"),
            deepseekApiKey = env("DEEPSEEK_API_KEY", ""),
            deepseekBaseUrl = env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
            deepseekModelChat = env("DEEPSEEK_MODEL_CHAT", "deepseek-v4-flash"),
            deepseekModelVision = env("DEEPSEEK_MODEL_VISION", "deepseek-v4-flash"),
            deepseekTemperature = envDouble("DEEPSEEK_TEMPERATURE", 0.7),
            deepseekMaxTokens = envInt("DEEPSEEK_MAX_TOKENS", 4096),
            jwtSecret = env("JWT_SECRET", "change-me-in-production-at-least-32-chars"),
            jwtIssuer = env("JWT_ISSUER", "ancientpoet"),
            jwtAudience = env("JWT_AUDIENCE", "ancientpoet-client"),
            jwtAccessExpireMinutes = envLong("JWT_ACCESS_EXPIRE_MINUTES", 60),
            jwtRefreshExpireDays = envLong("JWT_REFRESH_EXPIRE_DAYS", 30),
            smsProvider = env("SMS_PROVIDER", "aliyun"),
            smsAccessKey = env("SMS_ACCESS_KEY", ""),
            smsAccessSecret = env("SMS_ACCESS_SECRET", ""),
            smsSignName = env("SMS_SIGN_NAME", "鸿雁"),
            smsTemplateCode = env("SMS_TEMPLATE_CODE", "SMS_000000"),
            jpushAppKey = env("JPUSH_APP_KEY", ""),
            jpushMasterSecret = env("JPUSH_MASTER_SECRET", ""),
        )

        private fun env(key: String, default: String?): String =
            System.getenv(key) ?: default ?: throw IllegalStateException("Missing env var: $key")

        private fun optionalEnv(key: String): String? =
            System.getenv(key)?.takeIf { it.isNotBlank() }

        private fun envInt(key: String, default: Int): Int =
            System.getenv(key)?.toIntOrNull() ?: default

        private fun envLong(key: String, default: Long): Long =
            System.getenv(key)?.toLongOrNull() ?: default

        private fun envDouble(key: String, default: Double): Double =
            System.getenv(key)?.toDoubleOrNull() ?: default
    }
}
