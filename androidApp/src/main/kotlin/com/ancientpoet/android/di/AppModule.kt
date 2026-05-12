package com.ancientpoet.android.di

import com.ancientpoet.android.ui.screen.auth.LoginViewModel
import com.ancientpoet.android.ui.screen.conversation.ConversationViewModel
import com.ancientpoet.android.ui.screen.home.HomeViewModel
import com.ancientpoet.android.ui.screen.map.MapViewModel
import com.ancientpoet.android.ui.screen.poet.PoetViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object AppModule {
    val modules = module {
        single {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
                }
                install(Logging)
            }
        }

        viewModel { LoginViewModel(get()) }
        viewModel { HomeViewModel(get()) }
        viewModel { PoetViewModel(get()) }
        viewModel { ConversationViewModel(get()) }
        viewModel { MapViewModel(get()) }
    }
}
