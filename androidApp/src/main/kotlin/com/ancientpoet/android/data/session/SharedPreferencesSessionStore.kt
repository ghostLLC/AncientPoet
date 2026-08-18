package com.ancientpoet.android.data.session

import android.content.Context
import com.ancientpoet.shared.auth.AuthTokens
import com.ancientpoet.shared.auth.SessionStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedPreferencesSessionStore(context: Context) : SessionStore {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun load(): AuthTokens? = mutex.withLock {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)
        if (accessToken == null || refreshToken == null || !preferences.contains(KEY_USER_ID)) {
            null
        } else {
            AuthTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = preferences.getLong(KEY_USER_ID, INVALID_USER_ID),
            )
        }
    }

    override suspend fun save(tokens: AuthTokens) {
        mutex.withLock {
            preferences.edit()
                .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
                .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
                .putLong(KEY_USER_ID, tokens.userId)
                .commit()
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            preferences.edit().clear().commit()
        }
    }

    private companion object {
        const val FILE_NAME = "auth_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val INVALID_USER_ID = Long.MIN_VALUE
    }
}
