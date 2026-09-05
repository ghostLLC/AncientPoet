package com.ancientpoet.server.plugin

import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.repository.UserRepository
import com.ancientpoet.server.service.AuthService
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JwtTokenTest {
    private val config = testConfig()

    @Test
    fun protectedRouteRejectsRefreshToken() = testApplication {
        application {
            configureAuthentication(config)
            routing { authenticate("auth-jwt") { get("/protected") { call.respondText("ok") } } }
        }
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/protected") {
                bearerAuth(generateRefreshToken(config, 42L))
            }.status
        )
        assertEquals(
            HttpStatusCode.OK,
            client.get("/protected") {
                bearerAuth(generateAccessToken(config, 42L))
            }.status
        )
    }

    @Test
    fun accessTokenContainsIdentityAndStandardClaims() {
        val token = JWT.decode(generateAccessToken(config, 42L))

        assertEquals(42L, token.getClaim("userId").asLong())
        assertEquals(config.jwtIssuer, token.issuer)
        assertEquals(listOf(config.jwtAudience), token.audience)
        assertEquals("access", token.getClaim("type").asString())
    }

    @Test
    fun refreshTokenContainsRefreshType() {
        val token = JWT.decode(generateRefreshToken(config, 42L))

        assertEquals("refresh", token.getClaim("type").asString())
    }

    @Test
    fun refreshVerificationRejectsAccessToken() {
        val service = AuthService(config, UserRepository())

        assertNull(service.verifyRefreshToken(generateAccessToken(config, 42L)))
    }

    @Test
    fun wrongIssuerAudienceOrSignatureIsRejected() {
        val service = AuthService(config, UserRepository())
        val invalidTokens = listOf(
            generateRefreshToken(config.copy(jwtIssuer = "wrong-issuer"), 42L),
            generateRefreshToken(config.copy(jwtAudience = "wrong-audience"), 42L),
            generateRefreshToken(config.copy(jwtSecret = "different-local-test-secret-at-least-32-chars"), 42L)
        )

        invalidTokens.forEach { token -> assertNull(service.verifyRefreshToken(token)) }
    }

    @Test
    fun expiredTokensAreRejected() {
        val expiredConfig = config.copy(jwtAccessExpireMinutes = -1, jwtRefreshExpireDays = -1)
        val verifier = JWT.require(Algorithm.HMAC256(config.jwtSecret))
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .build()

        assertFailsWith<JWTVerificationException> {
            verifier.verify(generateAccessToken(expiredConfig, 42L))
        }
        assertNull(AuthService(config, UserRepository()).verifyRefreshToken(generateRefreshToken(expiredConfig, 42L)))
    }

    private fun testConfig() = AppConfig(
        port = 8080,
        host = "127.0.0.1",
        postgresHost = "127.0.0.1",
        postgresPort = 5432,
        postgresDb = "test",
        postgresUser = "test",
        postgresPassword = "test",
        redisHost = "127.0.0.1",
        redisPort = 6379,
        redisPassword = null,
        minioEndpoint = "http://127.0.0.1:9000",
        minioAccessKey = "test",
        minioSecretKey = "test",
        minioBucket = "test",
        deepseekApiKey = "",
        deepseekBaseUrl = "http://127.0.0.1",
        deepseekModelChat = "test",
        deepseekModelVision = "test",
        deepseekTemperature = 0.0,
        deepseekMaxTokens = 1,
        jwtSecret = "unit-test-secret-that-is-at-least-32-characters",
        jwtIssuer = "unit-test-issuer",
        jwtAudience = "unit-test-audience",
        jwtAccessExpireMinutes = 60,
        jwtRefreshExpireDays = 30,
        smsProvider = "test",
        smsAccessKey = "",
        smsAccessSecret = "",
        smsSignName = "test",
        smsTemplateCode = "test",
        jpushAppKey = "",
        jpushMasterSecret = ""
    )
}
