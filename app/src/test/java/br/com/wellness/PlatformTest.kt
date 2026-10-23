package br.com.wellness

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import br.com.wellness.data.Codec
import br.com.wellness.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlatformTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    @Test fun tiktokFailureFallsBackToBrowserWithIdenticalSearchUrl() {
        val plan = Codec.plan(context.assets.open("wellness.json").bufferedReader().use { it.readText() })
        val exercise = ExerciseLibrary.catalog(plan).first { it.name == "Terra sumô" }
        val browser = ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName="test.browser"; name="Browser" } }
        shadowOf(context.packageManager).addResolveInfoForIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org")).addCategory(Intent.CATEGORY_BROWSABLE), browser)
        val attempts = mutableListOf<Intent>()
        val wrapper = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                attempts.add(intent)
                if (intent.`package` != "test.browser") throw ActivityNotFoundException()
            }
        }
        VideoSearch.open(wrapper,exercise)
        assertEquals(listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "test.browser"),attempts.map { it.`package` })
        assertEquals(setOf(ExerciseLibrary.videoUrl(exercise)),attempts.map { it.dataString }.toSet())
        assertEquals("como fazer Terra sumô",attempts.last().data!!.getQueryParameter("q"))
        val accepted = mutableListOf<Intent>()
        VideoSearch.open(object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { accepted.add(intent) }
        }, exercise)
        assertEquals(1,accepted.size)
        assertEquals("com.zhiliaoapp.musically",accepted.single().`package`)
    }
    @Test fun alarmsAreReplacedCancelledOnPauseAndDeliverOneNotification() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(AlarmManager::class.java)
        val alarms = shadowOf(manager)
        val notices = shadowOf(context.getSystemService(NotificationManager::class.java))
        context.getSharedPreferences("wellness-rest-alerts",Context.MODE_PRIVATE).edit().clear().commit()
        val first = TimerState(deadline=System.currentTimeMillis()+40000, id="first",label="Hack squat · Aquecimento")
        RestAlerts.sync(context,first)
        assertEquals(1,alarms.scheduledAlarms.size)
        assertEquals(first.deadline,alarms.scheduledAlarms.single().triggerAtTime)
        RestAlerts.sync(context,first)
        assertEquals(1,alarms.scheduledAlarms.size)
        RestAlerts.sync(context,first.copy(deadline=null,remainingMs=30000))
        assertEquals(0,alarms.scheduledAlarms.size)
        val second = first.copy(id="second",deadline=System.currentTimeMillis()+20000)
        RestAlerts.sync(context,second)
        assertEquals(1,alarms.scheduledAlarms.size)
        RestAlerts.deliver(context, second.deadline!! + 1)
        assertEquals(1,notices.size())
        RestAlerts.deliver(context, second.deadline!! + 1)
        assertEquals(1,notices.size())
        assertEquals("Descanso concluído ✓",notices.allNotifications.single().extras.getCharSequence("android.title"))
        RestAlerts.sync(context,TimerState())
        assertEquals(0,notices.size())
        assertEquals(0,alarms.scheduledAlarms.size)
    }
}
