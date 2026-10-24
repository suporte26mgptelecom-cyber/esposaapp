package br.com.wellness

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import br.com.wellness.ui.WellnessViewModel
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=InterfaceTest.FixtureApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TikTokUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun catalogAndWorkoutUseChosenVideosAndCopyOnlyAfterExplicitTapWithoutChangingRecords() {
        val context=RuntimeEnvironment.getApplication()
        shadowOf(context).checkActivities(false) // Model accepted dispatch only; no real TikTok is installed.
        val clipboard=context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Existente","não alterar"))
        compose.waitUntil(30000) { compose.onAllNodesWithText("Bem-vinda, Nanda").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Exercícios").performClick()
        compose.onNodeWithText("Buscar por nome").performTextInput("sumo")
        compose.onNodeWithContentDescription("Abrir vídeo de Terra sumô no TikTok").performScrollTo().performClick()
        compose.onNodeWithText("Vídeo no TikTok").assertIsDisplayed()
        compose.onNodeWithText("https://vt.tiktok.com/ZSgu61uNB/").assertIsDisplayed()
        assertEquals("não alterar",clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        val attempt=shadowOf(context).nextStartedActivity
        assertEquals(TikTokVideosOpener.packages.first(),attempt.`package`)
        assertEquals("https://vt.tiktok.com/ZSgu61uNB/",attempt.dataString)
        compose.onNodeWithText("Copiar link").performClick()
        assertEquals("https://vt.tiktok.com/ZSgu61uNB/",clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertNull(shadowOf(context).nextStartedActivity)
        compose.onNodeWithText("Início").performClick()
        compose.onNodeWithTag("home-list").performScrollToNode(hasText("Começar treino"))
        compose.onNodeWithText("Começar treino").performScrollTo().performClick()
        val vm=ViewModelProvider(compose.activity,ViewModelProvider.AndroidViewModelFactory(compose.activity.application))[WellnessViewModel::class.java]
        compose.waitUntil(30000) { shadowOf(Looper.getMainLooper()).idle();vm.pending==0 }
        val ex=vm.engine.next(requireNotNull(vm.state))!!.exercises.first()
        val link=vm.tikTokVideos.forExercise(ex)!!.originalUrl
        val file=File(context.filesDir,"wellness-v1.json");val saved=file.readText()
        compose.onNodeWithContentDescription("Abrir vídeo de ${ex.name} no TikTok").performScrollTo().performClick()
        compose.onNodeWithText("Vídeo no TikTok").assertIsDisplayed()
        compose.onNodeWithText(link).assertIsDisplayed()
        val workoutAttempt=shadowOf(context).nextStartedActivity
        assertEquals(TikTokVideosOpener.packages.first(),workoutAttempt.`package`)
        assertEquals(link,workoutAttempt.dataString)
        assertEquals("https://vt.tiktok.com/ZSgu61uNB/",clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        compose.onNodeWithText("Fechar").performClick()
        assertEquals(saved,file.readText())
        assertNull(shadowOf(context).nextStartedActivity)
    }
    @Test fun unavailableTikTokOffersBrowserAndCopyButStartsBrowserOnlyAfterTap() {
        val context=RuntimeEnvironment.getApplication()
        shadowOf(context).checkActivities(true)
        val clipboard=context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Existente","não alterar"))
        compose.waitUntil(30000) { compose.onAllNodesWithText("Bem-vinda, Nanda").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Exercícios").performClick()
        compose.onNodeWithText("Buscar por nome").performTextInput("sumo")
        val file=File(context.filesDir,"wellness-v1.json");val saved=file.readText()
        compose.onNodeWithContentDescription("Abrir vídeo de Terra sumô no TikTok").performScrollTo().performClick()
        compose.onNodeWithText("Vídeo no TikTok").assertIsDisplayed()
        compose.onNodeWithText("Abrir no navegador").assertIsDisplayed()
        compose.onNodeWithText("Copiar link").assertIsDisplayed()
        assertEquals("não alterar",clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        var recorded=shadowOf(context).nextStartedActivity
        while(recorded!=null) { assertTrue(recorded.`package` in TikTokVideosOpener.packages);recorded=shadowOf(context).nextStartedActivity }
        val info=ResolveInfo().apply { activityInfo=ActivityInfo().apply { packageName="test.browser";name="Browser" } }
        shadowOf(context.packageManager).addResolveInfoForIntent(Intent(Intent.ACTION_VIEW,Uri.parse("https://example.org")).addCategory(Intent.CATEGORY_BROWSABLE),info)
        shadowOf(context).checkActivities(false) // Model a browser handling the explicitly requested open.
        compose.onNodeWithText("Abrir no navegador").performClick()
        val browser=shadowOf(context).nextStartedActivity
        assertEquals("test.browser",browser.`package`)
        assertEquals("https://vt.tiktok.com/ZSgu61uNB/",browser.dataString)
        assertEquals("não alterar",clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertEquals(saved,file.readText())
    }
}
