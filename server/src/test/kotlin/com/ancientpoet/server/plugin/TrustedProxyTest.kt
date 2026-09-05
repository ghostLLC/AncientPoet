package com.ancientpoet.server.plugin
import kotlin.test.*

class TrustedProxyTest {
    @Test fun externalClientsCannotChooseTheirRateLimitAddress() {
        assertEquals("203.0.113.5", trustedClientAddress("203.0.113.5", "198.51.100.10", listOf("127.0.0.1")))
        assertEquals("127.0.0.1", trustedClientAddress("127.0.0.1", "198.51.100.10", emptyList()))
    }

    @Test fun onlyConfiguredPeersCanForwardASingleAddress() {
        assertEquals("198.51.100.10", trustedClientAddress("127.0.0.1", "198.51.100.10", listOf("127.0.0.1")))
        assertEquals("127.0.0.1", trustedClientAddress("127.0.0.1", "1.2.3.4, 5.6.7.8", listOf("127.0.0.1")))
        assertEquals("127.0.0.1", trustedClientAddress("127.0.0.1", "attacker.example", listOf("127.0.0.1")))
    }
}
