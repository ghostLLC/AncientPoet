package com.ancientpoet.server.service

import com.ancientpoet.server.config.*
import com.ancientpoet.server.plugin.*
import com.ancientpoet.server.repository.PoetRepository

class PoetLocationService(private val poets: PoetRepository) {
    suspend fun getPoetLocation(poetId: Long, year: Int): com.ancientpoet.server.model.domain.PoetMovement? {
        val poet = poets.findById(poetId) ?: notFound()
        if (year !in poet.birthYear..poet.deathYear) invalid("请选择诗人生平内的年代")
        return poets.findLocation(poetId, year)
    }
    suspend fun getDefaultYear(poetId: Long): Int {
        val poet = poets.findById(poetId) ?: notFound()
        val preferred = (poet.birthYear + 42).coerceAtMost(poet.deathYear)
        return database { db ->
            db.rows(
                """SELECT GREATEST(year_start,LEAST(?,year_end)) AS year FROM poet_movements
                WHERE poet_id=? AND year_start<=? AND year_end>=?
                ORDER BY abs(GREATEST(year_start,LEAST(?,year_end))-?),year_start DESC LIMIT 1
                """.trimIndent(),
                preferred,
                poetId,
                poet.deathYear,
                poet.birthYear,
                preferred,
                preferred
            ) { it.getInt(1) }
                .singleOrNull()?.coerceIn(poet.birthYear, poet.deathYear) ?: invalid("这位诗人的行迹资料暂缺")
        }
    }
}
