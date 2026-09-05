package com.ancientpoet.server.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    lateinit var pool: HikariDataSource
        private set

    fun init(config: AppConfig) {
        pool = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://${config.postgresHost}:${config.postgresPort}/${config.postgresDb}"
                username = config.postgresUser
                password = config.postgresPassword
                maximumPoolSize = 10
                minimumIdle = 1
                connectionTimeout = 10_000
                validationTimeout = 5_000
                connectionInitSql = "SET TIME ZONE 'UTC'"
                poolName = "ancientpoet"
            }
        )
        try {
            Flyway.configure().dataSource(pool).validateMigrationNaming(true).load().migrate()
            Database.connect(pool)
        } catch (failure: Throwable) {
            pool.close()
            throw failure
        }
    }

    fun shutdown() {
        if (::pool.isInitialized) pool.close()
    }
}

/** Short transactions only. External HTTP requests must run outside this block. */
suspend fun <T> database(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
    DatabaseConfig.pool.connection.use { connection ->
        connection.autoCommit = false
        try {
            block(connection).also { connection.commit() }
        } catch (failure: Throwable) {
            connection.rollback()
            throw failure
        }
    }
}

fun PreparedStatement.bind(values: Array<out Any?>) {
    values.forEachIndexed { index, value ->
        setObject(index + 1, if (value is Instant) value.atOffset(ZoneOffset.UTC) else value)
    }
}

fun Connection.update(sql: String, vararg values: Any?): Int = prepareStatement(sql).use {
    it.bind(values)
    it.executeUpdate()
}

fun <T> Connection.rows(sql: String, vararg values: Any?, map: (ResultSet) -> T): List<T> = prepareStatement(sql).use { statement ->
    statement.bind(values)
    statement.executeQuery().use { result -> buildList { while (result.next()) add(map(result)) } }
}

fun ResultSet.instant(name: String): String? = getObject(name, java.time.OffsetDateTime::class.java)?.toInstant()?.toString()

fun ResultSet.optionalLong(name: String): Long? = getLong(name).let { if (wasNull()) null else it }
