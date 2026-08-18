package com.ancientpoet.server.config

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    private var flyway: Flyway? = null

    fun init(config: AppConfig) {
        val url = "jdbc:postgresql://${config.postgresHost}:${config.postgresPort}/${config.postgresDb}"
        val flyway = Flyway.configure()
            .dataSource(url, config.postgresUser, config.postgresPassword)
            .validateMigrationNaming(true)
            .load()
        flyway.migrate()
        this.flyway = flyway

        Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = config.postgresUser,
            password = config.postgresPassword
        )
    }

    fun shutdown() {
        // Exposed manages connections via HikariCP; shutdown hook handles cleanup
    }
}
