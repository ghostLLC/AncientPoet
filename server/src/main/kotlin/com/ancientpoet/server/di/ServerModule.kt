package com.ancientpoet.server.di

import com.ancientpoet.server.ai.ContextManager
import com.ancientpoet.server.ai.DeepSeekClient
import com.ancientpoet.server.ai.TranslationService
import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.push.FCMClient
import com.ancientpoet.server.push.PushNotificationService
import com.ancientpoet.server.repository.CommunityRepository
import com.ancientpoet.server.repository.ConversationRepository
import com.ancientpoet.server.repository.MessageRepository
import com.ancientpoet.server.repository.PoetRepository
import com.ancientpoet.server.repository.UserRepository
import com.ancientpoet.server.scheduler.MessageDeliveryScheduler
import com.ancientpoet.server.service.*
import org.koin.dsl.module

object ServerModule {
    fun module(config: AppConfig) = module {
        // Config
        single { config }

        // Repositories
        single { UserRepository() }
        single { PoetRepository() }
        single { ConversationRepository() }
        single { MessageRepository() }

        // AI
        single { DeepSeekClient(get()) }
        single { TranslationService(get(), get()) }
        single { ContextManager(get()) }

        // Scheduler & Push
        single { FCMClient() }
        single { PushNotificationService(get()) }
        single { MessageDeliveryScheduler(get(), get(), get()) }

        // Services
        single { AuthService(get(), get()) }
        single { PoetLocationService(get()) }
        single { UserService(get()) }
        single { StorylineService(get(), get(), get()) }
        single { MovementService(get()) }
        single { ConversationService(get(), get(), get()) }
        single { MessageService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        single { CommunityRepository() }
        single { CommunityService(get(), get()) }
    }
}
