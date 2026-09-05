package com.ancientpoet.android.data.session

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.ancientpoet.shared.auth.AuthTokens
import com.ancientpoet.shared.auth.SessionStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The encryption key never leaves Android Keystore; session files are excluded from backup. */
@SuppressLint("ApplySharedPref") // Already on Dispatchers.IO; a saved session must survive process death before returning.
class SharedPreferencesSessionStore(context: Context) : SessionStore {
    private val preferences = context.getSharedPreferences("auth_session", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
            )
        }.generateKey()
    }
    override suspend fun load(): AuthTokens? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val encrypted = preferences.getString("sealed_session", null)
            if (encrypted != null) {
                try {
                    val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                    val saved = Json.decodeFromString<StoredSession>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString())
                    AuthTokens(saved.access, saved.refresh, saved.userId)
                } catch (_: Exception) {
                    preferences.edit().clear().commit()
                    null
                }
            } else {
                val access = preferences.getString("access_token", null)
                val refresh = preferences.getString("refresh_token", null)
                val user = preferences.getLong("user_id", -1)
                if (access != null && refresh != null && user > 0) {
                    AuthTokens(access, refresh, user).also { saveLocked(it) }
                } else {
                    null
                }
            }
        }
    }
    override suspend fun save(tokens: AuthTokens) = withContext(Dispatchers.IO) {
        mutex.withLock { saveLocked(tokens) }
    }
    private fun saveLocked(tokens: AuthTokens) {
        val text = Json.encodeToString(StoredSession.serializer(), StoredSession(tokens.accessToken, tokens.refreshToken, tokens.userId))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = Base64.encodeToString(cipher.iv + cipher.doFinal(text.encodeToByteArray()), Base64.NO_WRAP)
        check(preferences.edit().clear().putString("sealed_session", encrypted).commit()) { "Session persistence failed" }
    }
    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(preferences.edit().clear().commit())
            Unit
        }
    }

    @Serializable private data class StoredSession(val access: String, val refresh: String, val userId: Long)
    companion object {
        private const val KEY_ALIAS = "ancientpoet.session.v1"
    }
}
