package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table
// timestamps stored as text in ISO format

// FK constraints are enforced at DB level via Flyway V1 schema
object ConversationsTable : Table("conversations") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val poetId = long("poet_id")
    val mode = varchar("mode", 10).default("open")
    val dynastyId = varchar("dynasty_id", 20)
    val storylineCurrentYear = integer("storyline_current_year").nullable()
    val storylineCompleted = bool("storyline_completed").default(false)
    val backgroundSetting = text("background_setting").nullable()
    val createdAt = text("created_at")
    val updatedAt = text("updated_at")
    override val primaryKey = PrimaryKey(id)
}
