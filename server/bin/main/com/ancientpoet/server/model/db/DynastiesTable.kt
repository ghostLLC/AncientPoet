package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table

object DynastiesTable : Table("dynasties") {
    val id = varchar("id", 20)
    val name = varchar("name", 50)
    val startYear = integer("start_year")
    val endYear = integer("end_year")
    val mapUrl = text("map_url").nullable()
    val description = text("description").nullable()
    override val primaryKey = PrimaryKey(id)
}

object DynastyCitiesTable : Table("dynasty_cities") {
    val id = long("id").autoIncrement()
    val dynastyId = varchar("dynasty_id", 20)
    val name = varchar("name", 100)
    val modernName = varchar("modern_name", 100).nullable()
    val province = varchar("province", 100).nullable()
    val lat = double("lat")
    val lng = double("lng")
    val isCapital = bool("is_capital").default(false)
    override val primaryKey = PrimaryKey(id)
}
