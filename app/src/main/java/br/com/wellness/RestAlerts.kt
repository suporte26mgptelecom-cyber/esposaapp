package br.com.wellness

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import br.com.wellness.domain.TimerState
import br.com.wellness.domain.digest

/** OS alarm survives screen locking and process death. Data file remains owned by the ViewModel. */
object RestAlerts {
    private const val CHANNEL = "wellness-rest-v1"
    private const val NOTICE = 2401
    private fun prefs(c: Context) = c.getSharedPreferences("wellness-rest-alerts", Context.MODE_PRIVATE)
    private fun id(t: TimerState) = t.id.ifEmpty { digest("${t.label}|${t.deadline}|${t.remainingMs}") }
    private fun alarm(c: Context) = c.getSystemService(AlarmManager::class.java)
    private fun pending(c: Context) = PendingIntent.getBroadcast(c, NOTICE,
        Intent(c, RestAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun exactAllowed(c: Context) = Build.VERSION.SDK_INT < 31 || alarm(c).canScheduleExactAlarms()
    fun notificationsAllowed(c: Context): Boolean = (Build.VERSION.SDK_INT < 33 ||
        c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        c.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    fun channel(c: Context) {
        val manager = c.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Descanso concluído", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos do temporizador de descanso"
            enableVibration(true)
        })
    }
    @Synchronized fun sync(c: Context, timer: TimerState) {
        channel(c)
        val p = prefs(c); val timerId = id(timer); val deadline = timer.deadline
        if (deadline == null) {
            alarm(c).cancel(pending(c)); p.edit().remove("active").remove("deadline").apply()
            c.getSystemService(NotificationManager::class.java).cancel(NOTICE)
            return
        }
        val changed = p.getString("active", null) != timerId || p.getLong("deadline", -1) != deadline
        if (changed) {
            alarm(c).cancel(pending(c))
            c.getSystemService(NotificationManager::class.java).cancel(NOTICE)
            p.edit().putString("active", timerId).putLong("deadline", deadline).putString("label", timer.label).apply()
        }
        if (deadline <= System.currentTimeMillis()) { deliver(c); return }
        if (!changed) return
        try {
            if (exactAllowed(c)) alarm(c).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pending(c))
            else alarm(c).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pending(c))
        } catch (_: SecurityException) {
            alarm(c).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pending(c))
        }
    }
    @Synchronized fun deliver(c: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(c); val active = p.getString("active", null) ?: return
        val deadline = p.getLong("deadline", Long.MAX_VALUE)
        // Ignore stale broadcasts after replacement or pause; delivery is once per timer.
        if (now < deadline || p.getString("delivered", null) == active) return
        p.edit().putString("delivered", active).apply()
        if (!notificationsAllowed(c)) return
        channel(c)
        val open = PendingIntent.getActivity(c, NOTICE, Intent(c, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notice = Notification.Builder(c, CHANNEL).setSmallIcon(R.drawable.ic_rest)
            .setContentTitle("Descanso concluído ✓").setContentText(p.getString("label", "Hora da próxima série"))
            .setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM).build()
        try { c.getSystemService(NotificationManager::class.java).notify(NOTICE, notice) } catch (_: SecurityException) { /* UI still shows completion. */ }
    }
    fun notificationSettings(c: Context) {
        c.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName))
    }
    fun exactSettings(c: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            try { c.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${c.packageName}"))) }
            catch (_: ActivityNotFoundException) { c.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${c.packageName}"))) }
        }
    }
    /** Re-establish the deadline if the user grants exact-alarm access while the app is open. */
    fun reschedule(c: Context, timer: TimerState) {
        prefs(c).edit().remove("deadline").apply(); sync(c, timer)
    }
}

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { RestAlerts.deliver(context) }
}
