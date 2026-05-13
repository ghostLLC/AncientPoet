package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table


// FK constraints are enforced at DB level via Flyway V1 schema
object PoetsTable : Table("poets") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 50)
    val courtesyName = varchar("courtesy_name", 50).nullable()
    val artName = varchar("art_name", 50).nullable()
    val dynastyId = varchar("dynasty_id", 20)
    val birthYear = integer("birth_year")
    val deathYear = integer("death_year")
    val personalityProfile = text("personality_profile")
    val writingStyle = text("writing_style")
    val systemPrompt = text("system_prompt")
    val biographySummary = text("biography_summary").nullable()
    val portraitUrl = text("portrait_url").nullable()
    val isFree = bool("is_free").default(true)
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(id)
}

object PoetMovementsTable : Table("poet_movements") {
    val id = long("id").autoIncrement()
    val poetId = long("poet_id")
    val yearStart = integer("year_start")
    val yearEnd = integer("year_end")
    val locationName = varchar("location_name", 100)
    val lat = double("lat")
    val lng = double("lng")
    val eventDescription = text("event_description").nullable()
    val eventType = varchar("event_type", 20).default("normal")
    override val primaryKey = PrimaryKey(id)
}

object PoetLifeEventsTable : Table("poet_life_events") {
    val id = long("id").autoIncrement()
    val poetId = long("poet_id")
    val year = integer("year")
    val age = integer("age")
    val title = varchar("title", 200)
    val description = text("description")
    val locationName = varchar("location_name", 100).nullable()
    val eventType = varchar("event_type", 20).default("milestone")
    val delayMultiplier = float("delay_multiplier").default(1.0f)
    val sortOrder = integer("sort_order").default(0)
    override val primaryKey = PrimaryKey(id)
}

object PoemsTable : Table("poems") {
    val id = long("id").autoIncrement()
    val poetId = long("poet_id")
    val title = varchar("title", 200)
    val content = text("content")
    val yearWritten = integer("year_written").nullable()
    val context = text("context").nullable()
    val translation = text("translation").nullable()
    val appreciation = text("appreciation").nullable()
    val tags = text("tags").nullable()
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(id)
}
