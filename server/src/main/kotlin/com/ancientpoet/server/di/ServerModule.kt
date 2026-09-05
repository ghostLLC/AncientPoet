package com.ancientpoet.server.di

import com.ancientpoet.server.ai.*
import com.ancientpoet.server.config.AppConfig
import com.ancientpoet.server.push.*
import com.ancientpoet.server.repository.*
import com.ancientpoet.server.scheduler.MessageDeliveryScheduler
import com.ancientpoet.server.service.*
import org.koin.dsl.module

object ServerModule {
    fun module(config: AppConfig) = module {
        single { config }
        single { UserRepository() }
        single { PoetRepository() }
        single { CityRepository() }
        single { ConversationRepository() }
        single { MessageRepository() }
        single { DeepSeekClient(get()) }
        single { TranslationService(get(), get()) }
        single { ContextManager(get()) }
        single { JPushClient(get()) }
        single { PushNotificationService(get()) }
        single { MessageDeliveryScheduler(get(), get(), get(), get(), get(), get(), get()) }
        single { AuthService(get(), get()) }
        single { PoetLocationService(get()) }
        single { MovementService(get(), get()) }
        single { UserService(get(), get(), get()) }
        single { StorylineService(get(), get(), get()) }
        single { ConversationService(get(), get(), get(), get()) }
        single { MessageService(get(), get(), get(), get(), get(), get(), get(), get()) }
        single<ReadinessChecker> { InfrastructureReadinessChecker(get()) }
    }
}
