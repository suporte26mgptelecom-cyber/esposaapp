package br.com.wellness

import android.app.Application
import android.content.*
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class TikTokVideosTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    private val plan get() = Codec.plan(context.assets.open("wellness.json").bufferedReader().use { it.readText() })
    private val videos get() = TikTokVideos(Codec.json.decodeFromString<List<TikTokVideo>>(context.assets.open("tiktok-videos.json").bufferedReader().use { it.readText() }))
    private val video get() = videos.entries.first()
    private val clipboard get() = context.getSystemService(ClipboardManager::class.java)
    private fun copied() = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    @Test fun allNineteenChoicesMatchExactlyAndApplyAcrossAllTwentyFourSessions() {
        val expected=mapOf(
            "cadeira-abdutora" to "ZSguYVXWq", "cadeira-flexora" to "ZSguYpLt9", "hack-squat" to "ZSgu2rJfJ",
            "cadeira-extensora" to "ZSgu22FfE", "leg-press-unilateral" to "ZSgu2Ypaq",
            "elevacao-pelvica-no-smith-maquina-ou-livre" to "ZSgu2vKYY", "leg-press-45o" to "ZSgujNwML",
            "stiff-unilateral" to "ZSgu6Ff9c", "terra-sumo" to "ZSgu61uNB", "abducao-de-quadril-na-polia" to "ZSgu6NrYG",
            "supino-inclinado-na-maquina" to "ZSgu6mvV5", "elevacao-lateral-com-halteres-sentada" to "ZSgu6D5Vo",
            "remada-articulada-unilateral" to "ZSgu6M4wm", "puxada-alta-com-pegada-neutra-ou-triangulo" to "ZSgu6U9pA",
            "desenvolvimento-na-maquina-articulada" to "ZSgu69kJx", "triceps-na-polia-alta-com-barra-reta-ou-v" to "ZSgurAVmj",
            "elevacao-lateral-com-halteres-sentada-90o" to "ZSgu6D5Vo", "remada-baixa-na-polia-com-barra-reta" to "ZSguM3Hea",
            "elevacao-lateral-na-polia" to "ZSguM3vTX"
        ).mapValues { "https://vt.tiktok.com/${it.value}/" }
        assertEquals(expected,videos.entries.associate { it.exerciseId to it.originalUrl })
        assertEquals(19,videos.entries.size);assertEquals(18,videos.entries.map { it.originalUrl }.toSet().size)
        assertTrue(videos.entries.all { it.resolvedUrl==null }) // All requests were blocked by proxy; none invented.
        val occurrences=plan.sessions.flatMap { it.exercises }
        assertEquals(130,occurrences.size)
        occurrences.forEach { ex -> assertEquals(expected.getValue(ex.id),videos.forExercise(ex)?.originalUrl) }
        assertEquals(19,ExerciseLibrary.catalog(plan).mapNotNull(videos::forExercise).size)
    }
    @Test fun sentLinksAreTheChosenVideosOnlyAndNeverTriggerBrowserClipboardOrHome() {
        val attempts=mutableListOf<Intent>()
        val wrapper=object:ContextWrapper(context) { override fun startActivity(intent:Intent) { attempts.add(intent) } }
        clipboard.setPrimaryClip(ClipData.newPlainText("Existente","não alterar"))
        for (entry in videos.entries) {
            attempts.clear()
            assertEquals(TikTokVideosOpener.Attempt.SENT_UNCONFIRMED,TikTokVideosOpener.openInTikTok(wrapper,entry))
            val intent=attempts.single()
            assertEquals(TikTokVideosOpener.packages.first(),intent.`package`)
            assertEquals(Intent.ACTION_VIEW,intent.action)
            assertEquals(entry.originalUrl,intent.dataString)
            assertEquals("não alterar",copied())
            assertNull(intent.data?.getQueryParameter("q"))
            assertNull(intent.data?.getQueryParameter("keyword"))
        }
    }
    @Test fun rejectedVideoReturnsOptionsWithoutImplicitFallbackEvenWhenBrowserExists() {
        addBrowser()
        val attempts=mutableListOf<Intent>()
        val wrapper=object:ContextWrapper(context) { override fun startActivity(intent:Intent) { attempts.add(intent);throw ActivityNotFoundException() } }
        clipboard.setPrimaryClip(ClipData.newPlainText("Existente","não alterar"))
        assertEquals(TikTokVideosOpener.Attempt.UNAVAILABLE,TikTokVideosOpener.openInTikTok(wrapper,video))
        assertEquals(2,attempts.size)
        assertTrue(attempts.all { it.`package` in TikTokVideosOpener.packages && it.dataString==video.originalUrl })
        assertEquals("não alterar",copied())
    }
    @Test fun resolvedVideoIsPreferredAndOriginalIsKeptForRetryAndCopy() {
        // Synthetic fixture only: this address is never embedded as a chosen video in the APK.
        val resolved=video.copy(resolvedUrl="https://www.tiktok.com/@fixture/video/1234567890123456789")
        TikTokVideos(listOf(resolved))
        val attempts=mutableListOf<Intent>()
        val wrapper=object:ContextWrapper(context) { override fun startActivity(intent:Intent) {
            attempts.add(intent);if(intent.dataString==resolved.resolvedUrl)throw ActivityNotFoundException()
        } }
        assertEquals(TikTokVideosOpener.Attempt.SENT_UNCONFIRMED,TikTokVideosOpener.openInTikTok(wrapper,resolved))
        assertEquals(listOf(resolved.resolvedUrl,resolved.resolvedUrl,resolved.originalUrl),attempts.map { it.dataString })
        assertTrue(TikTokVideosOpener.copyLink(context,resolved))
        assertEquals(resolved.originalUrl,copied())
    }
    private fun addBrowser() {
        val info=ResolveInfo().apply { activityInfo=ActivityInfo().apply { packageName="test.browser";name="Browser" } }
        shadowOf(context.packageManager).addResolveInfoForIntent(Intent(Intent.ACTION_VIEW,Uri.parse("https://example.org")).addCategory(Intent.CATEGORY_BROWSABLE),info)
    }
    @Test fun onlyTheManualBrowserActionStartsABrowserWithTheExactVideoLink() {
        addBrowser();val attempts=mutableListOf<Intent>()
        val wrapper=object:ContextWrapper(context) { override fun startActivity(intent:Intent) { attempts.add(intent) } }
        clipboard.setPrimaryClip(ClipData.newPlainText("Existente","não alterar"))
        assertTrue(TikTokVideosOpener.openInBrowser(wrapper,video))
        assertEquals("test.browser",attempts.single().`package`)
        assertEquals(video.originalUrl,attempts.single().dataString)
        assertEquals("não alterar",copied())
        attempts.clear()
        assertTrue(TikTokVideosOpener.copyLink(wrapper,video))
        assertEquals(video.originalUrl,copied())
        assertTrue(attempts.isEmpty())
    }
    @Test fun noBrowserOrBlockedTikTokReturnsFailureWithoutLaunchingSomethingElse() {
        val attempts=mutableListOf<Intent>()
        val wrapper=object:ContextWrapper(context) { override fun startActivity(intent:Intent) { attempts.add(intent);throw SecurityException() } }
        assertFalse(TikTokVideosOpener.openInBrowser(wrapper,video));assertTrue(attempts.isEmpty())
        assertEquals(TikTokVideosOpener.Attempt.UNAVAILABLE,TikTokVideosOpener.openInTikTok(wrapper,video))
        assertTrue(attempts.all { it.`package` in TikTokVideosOpener.packages })
    }
    @Test fun catalogKeepsSeatedVariationsSeparateDespiteTheirDeliberatelyIdenticalLinks() {
        val variations=ExerciseLibrary.search(plan,"lateral com halteres sentada")
        assertEquals(2,variations.size);assertNotEquals(variations[0].id,variations[1].id)
        assertNotEquals(videos.forExercise(variations[0])?.exerciseId,videos.forExercise(variations[1])?.exerciseId)
        assertEquals(videos.forExercise(variations[0])?.originalUrl,videos.forExercise(variations[1])?.originalUrl)
        assertNotEquals(ExerciseLibrary.youtubeUrl(variations[0]),ExerciseLibrary.youtubeUrl(variations[1]))
    }
    @Test fun configurationRejectsSearchHomepageAndForeignRedirects() {
        for (url in listOf("https://www.google.com/search?q=video","https://www.tiktok.com/search?q=treino","https://www.tiktok.com/","not a link")) {
            assertThrows(Exception::class.java) { TikTokVideos(listOf(video.copy(originalUrl=url))) }
            assertThrows(Exception::class.java) { TikTokVideos(listOf(video.copy(resolvedUrl=url))) }
        }
        assertThrows(Exception::class.java) { TikTokVideos(listOf(video,video)) }
    }
}
