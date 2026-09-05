package com.ancientpoet.android.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.ancientpoet.android.MainActivity
import com.ancientpoet.android.R
import com.ancientpoet.shared.contract.ConversationResponse
import com.ancientpoet.shared.data.api.ApiResult
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ArrivalNotifications {
    private const val WORK = "letter-arrival-check"
    const val CHANNEL = "letter_arrival_quiet"
    const val ID = 1201
    fun permitted(context: Context): Boolean = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun configure(context: Context, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "回信抵达", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "提醒你阅读已抵达的回信，通知中不展示正文"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                }
            )
        }
        val manager = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<ArrivalWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            manager.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            manager.cancelUniqueWork(WORK)
            NotificationManagerCompat.from(context).cancel(ID)
        }
    }
}

/** Opt-in periodic checks. Android may defer work; this is not an exact-time push service. */
class ArrivalWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters),
    KoinComponent {
    private val repository: AppRepository by inject()
    private val preferences: ReadingPreferences by inject()
    override suspend fun doWork(): Result {
        if (!preferences.options.value.notifications || !ArrivalNotifications.permitted(applicationContext)) return Result.success()
        val owner = repository.owner()
        if (owner == 0L) return Result.success()
        val response = repository.fetch<List<ConversationResponse>>("conversations")
        if (response !is ApiResult.Success) return Result.retry()
        val newest = response.value.mapNotNull { it.latestUnreadMessageId }.maxOrNull() ?: return Result.success()
        val prefs = applicationContext.getSharedPreferences("arrival_receipts", Context.MODE_PRIVATE)
        val key = "last_" + owner
        if (newest <= prefs.getLong(key, 0L) || repository.owner() != owner || !preferences.options.value.notifications) return Result.success()
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, ArrivalNotifications.CHANNEL)
            .setSmallIcon(R.drawable.ic_letter).setContentTitle("有回信抵达")
            .setContentText("打开鸿雁，读一封来自远方的信。")
            .setContentIntent(intent).setAutoCancel(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(ArrivalNotifications.ID, notification)
            prefs.edit().putLong(key, newest).apply()
        } catch (_: SecurityException) { /* Permission may be withdrawn while a request is in flight. */ }
        return Result.success()
    }
}
