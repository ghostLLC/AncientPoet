package com.ancientpoet.server.repository

import com.ancientpoet.server.config.*
import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.plugin.*
import kotlin.math.abs

class CityRepository {
    suspend fun requireDynasty(dynastyId: String): MapMetadataResponse = database { db ->
        db.rows(
            """SELECT d.*,(SELECT count(*) FROM dynasty_cities c WHERE c.dynasty_id=d.id) AS cities
            FROM dynasties d WHERE d.id=?""",
            dynastyId
        ) {
            MapMetadataResponse(
                it.getString("id"),
                it.getString("name"),
                it.getInt("start_year"),
                it.getInt("end_year"),
                MapImageSize(2000, 1500),
                it.getInt("cities")
            )
        }.singleOrNull() ?: notFound()
    }
    suspend fun list(dynastyId: String): List<CityResponse> {
        requireDynasty(dynastyId)
        return database { db ->
            db.rows("SELECT * FROM dynasty_cities WHERE dynasty_id=? ORDER BY is_capital DESC,id", dynastyId) {
                val lat = it.getDouble("lat")
                val lng = it.getDouble("lng")
                // Legacy coordinates retained for clients; current map uses one geographic projection.
                CityResponse(
                    it.getString("name"),
                    it.getString("modern_name"),
                    it.getString("province"),
                    lat,
                    lng,
                    ((lng - 95) / 30 * 2000).toInt(),
                    ((45 - lat) / 25 * 1500).toInt(),
                    it.getBoolean("is_capital")
                )
            }
        }
    }
    suspend fun requireCity(dynastyId: String, name: String, lat: Double, lng: Double): CityResponse {
        if (!lat.isFinite() || !lng.isFinite()) invalid("城市坐标无效")
        return list(dynastyId).singleOrNull { it.name == name && abs(it.lat - lat) < 0.001 && abs(it.lng - lng) < 0.001 }
            ?: invalid("请选择本朝地图中的城市")
    }
}
