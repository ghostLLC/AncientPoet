package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table


// FK constraints are enforced at DB level via Flyway V1 schema
object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val phone = varchar("phone", 20).uniqueIndex()
    val nickname = varchar("nickname", 50).nullable()
    val avatarUrl = text("avatar_url").nullable()
    val bio = text("bio").nullable()
    val createdAt = text("created_at")
    val updatedAt = text("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object UserLocationsTable : Table("user_locations") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val dynastyId = varchar("dynasty_id", 20)
    val locationName = varchar("location_name", 100)
    val lat = double("lat")
    val lng = double("lng")
    val status = varchar("status", 10).default("settled")
    val movingToName = varchar("moving_to_name", 100).nullable()
    val movingToLat = double("moving_to_lat").nullable()
    val movingToLng = double("moving_to_lng").nullable()
    val movingStartTime = text("moving_start_time").nullable()
    val movingArrivalTime = text("moving_arrival_time").nullable()
    val updatedAt = text("updated_at")
    override val primaryKey = PrimaryKey(id)
}
