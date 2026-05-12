package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamptz

object ConversationsTable : Table("conversations") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(UsersTable.id)
    val poetId = long("poet_id").references(PoetsTable.id)
    val mode = varchar("mode", 10).default("open")
    val dynastyId = varchar("dynasty_id", 20).references(DynastiesTable.id)
    val storylineCurrentYear = integer("storyline_current_year").nullable()
    val storylineCompleted = bool("storyline_completed").default(false)
    val backgroundSetting = text("background_setting").nullable()
    val createdAt = timestamptz("created_at")
    val updatedAt = timestamptz("updated_at")
    override val primaryKey = PrimaryKey(id)
}
