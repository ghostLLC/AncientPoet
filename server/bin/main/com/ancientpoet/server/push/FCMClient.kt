package com.ancientpoet.server.push

// Replaced by JPushClient for domestic push notification delivery.
// This file kept as a no-op stub for backward-compatible DI wiring.
class FCMClient {
    suspend fun send(userId: Long, title: String, body: String, data: Map<String, String>) {}
}
