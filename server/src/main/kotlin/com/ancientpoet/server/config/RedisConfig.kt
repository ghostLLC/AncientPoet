package com.ancientpoet.server.config

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisConfig {
    lateinit var pool: JedisPool
        private set

    fun init(config: AppConfig) {
        val poolConfig = JedisPoolConfig().apply {
            maxTotal = 10
            maxIdle = 5
            minIdle = 2
        }
        pool = if (config.redisPassword.isNullOrBlank()) {
            JedisPool(poolConfig, config.redisHost, config.redisPort)
        } else {
            JedisPool(poolConfig, config.redisHost, config.redisPort, 2000, config.redisPassword)
        }
    }
}
