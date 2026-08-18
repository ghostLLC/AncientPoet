package com.ancientpoet.android.di

import com.ancientpoet.android.data.network.HttpClientFactory
import com.ancientpoet.android.data.session.SessionController
import com.ancientpoet.android.data.session.SharedPreferencesSessionStore
import com.ancientpoet.android.ui.screen.auth.LoginViewModel
import com.ancientpoet.android.ui.screen.community.CommunityViewModel
import com.ancientpoet.android.ui.screen.conversation.ConversationViewModel
import com.ancientpoet.android.ui.screen.home.HomeViewModel
import com.ancientpoet.android.ui.screen.map.MapViewModel
import com.ancientpoet.android.ui.screen.poet.PoetViewModel
import com.ancientpoet.android.ui.screen.poetry.PoetryViewModel
import com.ancientpoet.android.ui.session.SessionViewModel
import com.ancientpoet.shared.data.api.AncientPoetApi
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object AppModule {
    val modules = module {
        single { SharedPreferencesSessionStore(androidContext()) }
        single {
            SessionController(get<SharedPreferencesSessionStore>()) { get<HttpClient>() }
        }
        single<HttpClient> { HttpClientFactory.create(get()) }
        single { AncientPoetApi(get()) }

        viewModel { SessionViewModel(get()) }
        viewModel { LoginViewModel(get(), get()) }
        viewModel { HomeViewModel(get()) }
        viewModel { PoetViewModel(get()) }
        viewModel { ConversationViewModel(get()) }
        viewModel { MapViewModel(get()) }
        viewModel { PoetryViewModel(get()) }
        viewModel { CommunityViewModel(get()) }
    }
}
