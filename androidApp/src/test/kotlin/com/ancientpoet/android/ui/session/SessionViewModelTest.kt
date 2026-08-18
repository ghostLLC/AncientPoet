package com.ancientpoet.android.ui.session

import com.ancientpoet.android.data.session.SessionController
import com.ancientpoet.shared.auth.AuthTokens
import com.ancientpoet.shared.auth.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.yield
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @Test
    fun logoutThatArrivesDuringInitialRestoreCannotBeOverwrittenByStaleLoad() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val store = LogoutDuringFirstLoadStore(AuthTokens("access", "refresh", 1))
            val controller = SessionController(store)
            store.controller = controller

            val viewModel = SessionViewModel(controller)
            advanceUntilIdle()

            assertEquals(SessionStatus.Anonymous, viewModel.status.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class LogoutDuringFirstLoadStore(
        private val initial: AuthTokens,
    ) : SessionStore {
        lateinit var controller: SessionController
        private var firstLoad = true
        private var value: AuthTokens? = initial

        override suspend fun load(): AuthTokens? {
            if (firstLoad) {
                firstLoad = false
                CoroutineScope(coroutineContext).launch { controller.logout() }
                yield()
                return initial
            }
            return value
        }

        override suspend fun save(tokens: AuthTokens) {
            value = tokens
        }

        override suspend fun clear() {
            value = null
        }
    }
}
