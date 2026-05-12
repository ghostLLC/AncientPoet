package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamptz

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val phone = varchar("phone", 20).uniqueIndex()
    val nickname = varchar("nickname", 50).nullable()
    val avatarUrl = text("avatar_url").nullable()
    val bio = text("bio").nullable()
    val createdAt = timestamptz("created_at")
    val updatedAt = timestamptz("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object UserLocationsTable : Table("user_locations") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(UsersTable.id)
    val dynastyId = varchar("dynasty_id", 20).references(DynastiesTable.id)
    val locationName = varchar("location_name", 100)
    val lat = double("lat")
    val lng = double("lng")
    val status = varchar("status", 10).default("settled")
    val movingToName = varchar("moving_to_name", 100).nullable()
    val movingToLat = double("moving_to_lat").nullable()
    val movingToLng = double("moving_to_lng").nullable()
    val movingStartTime = timestamptz("moving_start_time").nullable()
    val movingArrivalTime = timestamptz("moving_arrival_time").nullable()
    // geom column is maintained by PostgreSQL trigger (sync_geom_from_latlng)
    val updatedAt = timestamptz("updated_at")
    override val primaryKey = PrimaryKey(id)
}
