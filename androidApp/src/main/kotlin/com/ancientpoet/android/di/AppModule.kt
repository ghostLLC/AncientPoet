package com.ancientpoet.android.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ancientpoet.android.data.AppRepository
import com.ancientpoet.android.data.ReadingPreferences
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
import com.ancientpoet.shared.data.local.ResourceCache
import com.ancientpoet.shared.data.local.SqliteResourceCache
import com.ancientpoet.shared.db.AncientPoetDb
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object AppModule {
    val modules = module {
        single { SharedPreferencesSessionStore(androidContext()) }
        single { ReadingPreferences(androidContext()) }
        single {
            SessionController(get<SharedPreferencesSessionStore>()) { get<HttpClient>() }
        }
        single<HttpClient> { HttpClientFactory.create(get()) }
        single { AncientPoetApi(get()) }
        single<ResourceCache> { SqliteResourceCache(AncientPoetDb(AndroidSqliteDriver(AncientPoetDb.Schema, androidContext(), "reading-cache-v1.db"))) }
        single { AppRepository(get(), get(), get<SessionController>()) }

        viewModel { SessionViewModel(get()) }
        viewModel { LoginViewModel(get(), get()) }
        viewModel { HomeViewModel(get<AppRepository>()) }
        viewModel { PoetViewModel(get<AppRepository>()) }
        viewModel { ConversationViewModel(get<AppRepository>()) }
        viewModel { MapViewModel(get<AppRepository>()) }
        viewModel { PoetryViewModel(get<AppRepository>()) }
        viewModel { CommunityViewModel(get()) }
    }
}
