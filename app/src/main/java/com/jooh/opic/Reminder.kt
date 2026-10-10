package com.jooh.opic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jooh.opic.core.common.StudyLanguages
import com.jooh.opic.core.common.nextReminderAt
import com.jooh.opic.core.common.reminderText
import com.jooh.opic.feature.analysis.loadStats
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 학습 알림 설정 (TASK 34). hour = null이면 꺼짐. 네트워크 사용 없음. */
object ReminderSettings {
    private const val PREFS = "opic_reminder"
    private const val WORK = "daily-reminder"
    const val CHANNEL = "study-reminder"
    val HOURS = listOf(8, 12, 19, 21, 22)

    fun hour(context: Context): Int? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("hour", -1).takeIf { it >= 0 }

    fun set(context: Context, hour: Int?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("hour", hour ?: -1).apply()
        if (hour == null) WorkManager.getInstance(context).cancelUniqueWork(WORK) else schedule(context, hour)
    }

    /** 다음 알림 1회만 예약하고, 알림을 보낸 뒤 다음 날 것을 다시 예약한다. 같은 이름 작업은 새 예약으로 바꾼다. */
    fun schedule(context: Context, hour: Int, afterCurrent: Boolean = false) {
        val now = LocalDateTime.now()
        val delay = Duration.between(now, nextReminderAt(now, LocalTime.of(hour, 0)))
        val policy = if (afterCurrent) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, policy,
            OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay).build())
    }

    fun canNotify(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OpicApplication
        val hour = ReminderSettings.hour(app) ?: return Result.success()
        runCatching {
            val today = LocalDate.now().toEpochDay()
            val stats = loadStats(app.database, StudyLanguages.EN.code, today)
            val nextDay = app.database.wordDao().dayStats(StudyLanguages.EN.code, 40).firstOrNull { it.mastered < it.total }?.day
            reminderText(stats.week.last().count > 0, nextDay, stats.grammarDue)?.let { notify(app, it) }
        }
        ReminderSettings.schedule(app, hour, afterCurrent = true)
        return Result.success()
    }

    private fun notify(context: Context, text: String) {
        if (!ReminderSettings.canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(ReminderSettings.CHANNEL, "학습 알림", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(1, NotificationCompat.Builder(context, ReminderSettings.CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("OPIc 공부할 시간").setContentText(text)
            .setContentIntent(open).setAutoCancel(true).build())
    }
}
