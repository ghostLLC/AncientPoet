package com.ancientpoet.server.repository

import com.ancientpoet.server.model.db.*
import com.ancientpoet.server.model.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class PoetRepository {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun findAll(): List<Poet> = withContext(Dispatchers.IO) {
        transaction {
            PoetsTable.innerJoin(DynastiesTable).selectAll().orderBy(PoetsTable.id).map { it.toPoet() }
        }
    }

    suspend fun findById(poetId: Long): Poet? = withContext(Dispatchers.IO) {
        transaction {
            PoetsTable.innerJoin(DynastiesTable).selectAll().where { PoetsTable.id eq poetId }.singleOrNull()?.toPoet()
        }
    }

    suspend fun findLocation(poetId: Long, year: Int): PoetMovement? = withContext(Dispatchers.IO) {
        transaction {
            PoetMovementsTable.selectAll().where {
                (PoetMovementsTable.poetId eq poetId) and (PoetMovementsTable.yearStart lessEq year) and (PoetMovementsTable.yearEnd greaterEq year)
            }.orderBy(PoetMovementsTable.yearStart, SortOrder.DESC).firstOrNull()?.toMovement()
        }
    }

    suspend fun findPoemsByPoetId(poetId: Long): List<Poem> = withContext(Dispatchers.IO) {
        transaction { PoemsTable.selectAll().where { PoemsTable.poetId eq poetId }.orderBy(PoemsTable.yearWritten).map { it.toPoem() } }
    }

    suspend fun findLifeEventsByPoetId(poetId: Long): List<PoetLifeEvent> = withContext(Dispatchers.IO) {
        transaction { PoetLifeEventsTable.selectAll().where { PoetLifeEventsTable.poetId eq poetId }.orderBy(PoetLifeEventsTable.year).map { it.toLifeEvent() } }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toPoet(): Poet = Poet(
        id = this[PoetsTable.id], name = this[PoetsTable.name], courtesyName = this[PoetsTable.courtesyName],
        artName = this[PoetsTable.artName], dynastyId = this[PoetsTable.dynastyId], dynastyName = this[DynastiesTable.name],
        birthYear = this[PoetsTable.birthYear], deathYear = this[PoetsTable.deathYear],
        personalityProfile = PersonalityProfile(traits = emptyList()),
        writingStyle = this[PoetsTable.writingStyle], systemPrompt = this[PoetsTable.systemPrompt],
        biographySummary = this[PoetsTable.biographySummary], portraitUrl = this[PoetsTable.portraitUrl], isFree = this[PoetsTable.isFree],
    )

    private fun org.jetbrains.exposed.sql.ResultRow.toMovement() = PoetMovement(
        id = this[PoetMovementsTable.id], poetId = this[PoetMovementsTable.poetId],
        yearStart = this[PoetMovementsTable.yearStart], yearEnd = this[PoetMovementsTable.yearEnd],
        locationName = this[PoetMovementsTable.locationName], lat = this[PoetMovementsTable.lat], lng = this[PoetMovementsTable.lng],
        eventDescription = this[PoetMovementsTable.eventDescription], eventType = this[PoetMovementsTable.eventType],
    )

    private fun org.jetbrains.exposed.sql.ResultRow.toPoem() = Poem(
        id = this[PoemsTable.id], poetId = this[PoemsTable.poetId],
        title = this[PoemsTable.title], content = this[PoemsTable.content], yearWritten = this[PoemsTable.yearWritten],
        context = this[PoemsTable.context], translation = this[PoemsTable.translation], appreciation = this[PoemsTable.appreciation], tags = emptyList(),
    )

    private fun org.jetbrains.exposed.sql.ResultRow.toLifeEvent() = PoetLifeEvent(
        id = this[PoetLifeEventsTable.id], poetId = this[PoetLifeEventsTable.poetId],
        year = this[PoetLifeEventsTable.year], age = this[PoetLifeEventsTable.age],
        title = this[PoetLifeEventsTable.title], description = this[PoetLifeEventsTable.description],
        locationName = this[PoetLifeEventsTable.locationName], eventType = this[PoetLifeEventsTable.eventType],
        delayMultiplier = this[PoetLifeEventsTable.delayMultiplier].toDouble(), sortOrder = this[PoetLifeEventsTable.sortOrder],
    )
}
