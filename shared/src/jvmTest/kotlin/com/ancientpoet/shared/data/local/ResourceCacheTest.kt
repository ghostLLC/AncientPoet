package com.ancientpoet.shared.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ancientpoet.shared.db.AncientPoetDb
import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class ResourceCacheTest {
    @Test fun draftSurvivesReopenAndOldAcknowledgementPreservesNewerDraft() = runTest {
        val file = Files.createTempFile("letter-cache-", ".db")
        try {
            val first = JdbcSqliteDriver("jdbc:sqlite:" + file)
            AncientPoetDb.Schema.create(first)
            SqliteResourceCache(AncientPoetDb(first)).saveDraft(1, 44, LetterDraft("request-one", "first letter"))
            first.close()
            val second = JdbcSqliteDriver("jdbc:sqlite:" + file)
            val cache = SqliteResourceCache(AncientPoetDb(second))
            assertEquals("request-one", cache.draft(1, 44)?.clientId)
            cache.saveDraft(1, 44, LetterDraft("request-two", "new draft"))
            cache.acknowledge(1, 44, "request-one")
            assertEquals("new draft", cache.draft(1, 44)?.text)
            cache.acknowledge(1, 44, "request-two")
            assertNull(cache.draft(1, 44))
            second.close()
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test fun accountClearCannotReadOrDeleteAnotherAccountsPrivateCache() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            AncientPoetDb.Schema.create(driver)
            val cache = SqliteResourceCache(AncientPoetDb(driver))
            cache.write(1, "conversations", "private", 0)
            cache.saveDraft(1, 44, LetterDraft("one", "private draft"))
            assertNull(cache.read(2, "conversations"))
            assertNull(cache.draft(2, 44))
            cache.write(2, "conversations", "second", 0)
            cache.write(0, "poets", "public", 0)
            cache.clear(1)
            assertNull(cache.read(1, "conversations"))
            assertNull(cache.draft(1, 44))
            assertEquals("second", cache.read(2, "conversations")?.payload)
            assertEquals("public", cache.read(0, "poets")?.payload)
        } finally {
            driver.close()
        }
    }
}
