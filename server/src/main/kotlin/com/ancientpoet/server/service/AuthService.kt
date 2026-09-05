package com.ancientpoet.server.service

import com.ancientpoet.server.config.*
import com.ancientpoet.server.plugin.*
import com.ancientpoet.server.repository.UserRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

class AuthService(
    private val config: AppConfig,
    private val userRepository: UserRepository,
    private val smsSender: SmsSender = TencentSmsSender(config)
) {
    private val random = SecureRandom()

    suspend fun sendSms(rawPhone: String): Boolean {
        val phone = normalizePhone(rawPhone)
        val code = if (config.development) "123456" else (random.nextInt(900000) + 100000).toString()
        val hash = codeHash(phone, code)
        val now = Instant.now()
        database { db ->
            if (!config.development) {
                val day = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
                // Lock budget rows in a consistent order across all server processes.
                for ((key, limit) in listOf("global" to config.smsGlobalPerDay, "phone:" + phone to config.smsPerPhonePerDay)) {
                    val accepted = db.rows(
                        """
                        INSERT INTO sms_daily_budgets(budget_key,budget_day,attempts) VALUES (?,?,1)
                        ON CONFLICT(budget_key,budget_day) DO UPDATE SET attempts=sms_daily_budgets.attempts+1
                        WHERE sms_daily_budgets.attempts<? RETURNING attempts
                        """.trimIndent(),
                        key,
                        day,
                        limit
                    ) { it.getInt(1) }
                    if (accepted.isEmpty()) throw ApiException(HttpStatusCode.TooManyRequests, "sms_daily_limit", "今日验证码发送已达上限，请明天再试")
                }
                db.update("DELETE FROM sms_daily_budgets WHERE budget_day<?", day.minusDays(2))
            }
            // Atomic per-phone cooldown, shared by every server process.
            val accepted = db.rows(
                """
                INSERT INTO sms_challenges(phone,code_hash,expires_at,sent_at)
                VALUES (?,?,?,?)
                ON CONFLICT(phone) DO UPDATE SET code_hash=excluded.code_hash,
                  expires_at=excluded.expires_at,sent_at=excluded.sent_at,attempts=0,consumed=false
                WHERE sms_challenges.sent_at <= excluded.sent_at - interval '60 seconds'
                RETURNING phone
                """.trimIndent(),
                phone,
                hash,
                now.plusSeconds(300),
                now
            ) { it.getString(1) }
            if (accepted.isEmpty()) throw ApiException(HttpStatusCode.TooManyRequests, "sms_cooldown", "请稍后再获取验证码")
        }
        if (!config.development) {
            try {
                smsSender.send(phone, code)
            } catch (failure: Exception) {
                database { it.update("DELETE FROM sms_challenges WHERE phone=? AND code_hash=?", phone, hash) }
                throw failure
            }
        }
        return true
    }

    suspend fun verifySms(rawPhone: String, code: String): TokenPair? {
        val phone = normalizePhone(rawPhone)
        if (!code.matches(Regex("[0-9]{6}"))) invalid("请输入六位验证码")
        val valid = if (config.development && code == "123456") {
            true
        } else {
            database { db ->
                val challenge = db.rows(
                    """
                SELECT code_hash,expires_at,attempts,consumed FROM sms_challenges WHERE phone=? FOR UPDATE
                    """.trimIndent(),
                    phone
                ) {
                    Triple(
                        it.getString("code_hash"),
                        it.getObject("expires_at", java.time.OffsetDateTime::class.java).toInstant(),
                        it.getInt("attempts") to it.getBoolean("consumed")
                    )
                }.singleOrNull()
                if (challenge == null || challenge.second.isBefore(Instant.now()) || challenge.third.first >= 5 || challenge.third.second) {
                    false
                } else {
                    val matches = MessageDigest.isEqual(challenge.first.toByteArray(), codeHash(phone, code).toByteArray())
                    db.update("UPDATE sms_challenges SET attempts=attempts+1,consumed=? WHERE phone=?", matches, phone)
                    matches
                }
            }
        }
        if (!valid) return null
        val user = userRepository.findByPhone(phone) ?: userRepository.create(phone)
        return createSession(user.id)
    }

    /** Cryptographic validation only; refresh() also validates and rotates the stored session. */
    fun verifyRefreshToken(token: String): Long? = decodeRefresh(token)?.getClaim("userId")?.asLong()

    private fun decodeRefresh(token: String) = try {
        JWT.require(Algorithm.HMAC256(config.jwtSecret)).withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience).withClaim("type", "refresh").build().verify(token)
    } catch (_: Exception) {
        null
    }

    suspend fun refresh(token: String): TokenPair? {
        val jwt = decodeRefresh(token) ?: return null
        val userId = jwt.getClaim("userId").asLong() ?: return null
        val sid = jwt.getClaim("sid").asString() ?: return null
        val tokenHash = digest(token)
        return database { db ->
            val row = db.rows(
                """
                SELECT refresh_hash FROM auth_sessions WHERE id=? AND user_id=?
                  AND revoked_at IS NULL AND expires_at>now() FOR UPDATE
                """.trimIndent(),
                sid,
                userId
            ) { it.getString(1) }.singleOrNull() ?: return@database null
            if (!MessageDigest.isEqual(row.toByteArray(), tokenHash.toByteArray())) {
                val reused = db.rows("SELECT 1 FROM used_refresh_tokens WHERE token_hash=? AND session_id=?", tokenHash, sid) { true }.isNotEmpty()
                if (reused) db.update("UPDATE auth_sessions SET revoked_at=now() WHERE id=?", sid)
                return@database null
            }
            val next = tokenPair(userId, sid)
            db.update(
                "INSERT INTO used_refresh_tokens(token_hash,session_id,expires_at) VALUES (?,?,?) ON CONFLICT DO NOTHING",
                tokenHash,
                sid,
                jwt.expiresAt.toInstant()
            )
            db.update(
                "UPDATE auth_sessions SET refresh_hash=?,expires_at=? WHERE id=?",
                digest(next.refreshToken),
                Instant.now().plusSeconds(config.jwtRefreshExpireDays * 86400),
                sid
            )
            next
        }
    }

    suspend fun isSessionActive(sid: String, userId: Long): Boolean = database { db ->
        db.rows("SELECT 1 FROM auth_sessions WHERE id=? AND user_id=? AND revoked_at IS NULL AND expires_at>now()", sid, userId) { true }.isNotEmpty()
    }

    suspend fun logout(sid: String, userId: Long) {
        database { it.update("UPDATE auth_sessions SET revoked_at=now() WHERE id=? AND user_id=?", sid, userId) }
    }

    private suspend fun createSession(userId: Long): TokenPair {
        val sid = UUID.randomUUID().toString()
        val pair = tokenPair(userId, sid)
        database { db ->
            db.update(
                "INSERT INTO auth_sessions(id,user_id,refresh_hash,expires_at) VALUES (?,?,?,?)",
                sid,
                userId,
                digest(pair.refreshToken),
                Instant.now().plusSeconds(config.jwtRefreshExpireDays * 86400)
            )
            db.update("DELETE FROM used_refresh_tokens WHERE expires_at<now()")
            db.update("DELETE FROM sms_challenges WHERE expires_at<now()-interval '1 day'")
        }
        return pair
    }

    private fun tokenPair(userId: Long, sid: String) = TokenPair(
        generateAccessToken(config, userId, sid),
        generateRefreshToken(config, userId, sid),
        config.jwtAccessExpireMinutes * 60,
        userId
    )
    private fun codeHash(phone: String, code: String) = digest("${config.jwtSecret}:$phone:$code")

    companion object {
        fun normalizePhone(raw: String): String {
            val phone = raw.trim().removePrefix("+86")
            if (!phone.matches(Regex("1[3-9][0-9]{9}"))) invalid("请输入有效的中国大陆手机号")
            return phone
        }
        private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

data class TokenPair(val accessToken: String, val refreshToken: String, val expiresIn: Long, val userId: Long)
