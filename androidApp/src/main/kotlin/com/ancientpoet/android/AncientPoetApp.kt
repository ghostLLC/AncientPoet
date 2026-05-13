package com.ancientpoet.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.ancientpoet.android.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AncientPoetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { androidContext(this@AncientPoetApp); modules(AppModule.modules) }

        // JPush (极光推送): uncomment after adding jpush SDK dependency + JPUSH_APPKEY in build.gradle.kts
        // JPushInterface.setDebugMode(false)
        // JPushInterface.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("letter_arrival", "书信到达", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "当诗人的回信到达时通知" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
