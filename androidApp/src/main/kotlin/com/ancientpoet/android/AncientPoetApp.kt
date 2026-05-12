package com.ancientpoet.android

import android.app.Application
import com.ancientpoet.android.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AncientPoetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AncientPoetApp)
            modules(AppModule.modules)
        }
    }
}
