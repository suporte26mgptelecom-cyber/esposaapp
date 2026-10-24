package br.com.wellness

import android.app.Application
import android.content.Context
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import br.com.wellness.data.*
import br.com.wellness.domain.*
import br.com.wellness.ui.WellnessApp
import br.com.wellness.ui.WellnessViewModel
import br.com.wellness.ui.theme.WellnessTheme
import androidx.activity.compose.setContent
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w393dp-h852dp-xhdpi",application=NavigationV13Test.LegacyApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationV13Test {
    class LegacyApplication: Application() {
        override fun onCreate() {
            super.onCreate()
            val plan=Codec.plan(assets.open("wellness.json").bufferedReader().use { it.readText() });val engine=WorkoutEngine(plan)
            var state=engine.initial();val ex=plan.sessions[5].exercises.first()
            state=engine.load(state,ex,"31.5")
            state=engine.reps(state,ex,ex.sets.first(),"0")
            state=engine.actualLoad(state,plan.sessions[5],ex,ex.sets.first(),"0")
            state=state.copy(restPreferences=mapOf(engine.restKey(state,ex,null) to 210L,engine.restKey(state,ex,ex.sets.last()) to 95L))
            repeat(5) { state=engine.complete(state,plan.sessions[it],it.toLong()) }
            val fields=Codec.json.parseToJsonElement(Codec.encode(state)).jsonObject.toMutableMap()
            fields["format"]=JsonPrimitive(2);fields.remove("startedSessions");fields.remove("celebratedRank")
            File(filesDir,"wellness-v1.json").writeText(JsonObject(fields).toString())
            getSharedPreferences("wellness-ui",Context.MODE_PRIVATE).edit().putBoolean("notification-asked",true).commit()
        }
    }
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun oldConclusionsRankingSessionsRestAndFinalizationWorkAtLargeFontSize() {
        RuntimeEnvironment.setFontScale(1.6f)
        compose.activityRule.scenario.recreate()
        val vm=ViewModelProvider(compose.activity,ViewModelProvider.AndroidViewModelFactory(compose.activity.application))[WellnessViewModel::class.java]
        compose.activity.setContent { WellnessTheme { WellnessApp(vm) } }
        assertEquals(1.6f,compose.activity.resources.configuration.fontScale)
        compose.waitUntil(60000) { compose.onAllNodesWithText("Bem-vinda, Nanda").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithText("Novo rank conquistado!").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Seu treino com segurança").assertDoesNotExist()
        compose.onNodeWithContentDescription("Logotipo Wellness").assertIsDisplayed()
        val title=compose.onNodeWithText("WELLNESS").getUnclippedBoundsInRoot()
        val logo=compose.onNodeWithContentDescription("Logotipo Wellness").getUnclippedBoundsInRoot()
        assertEquals((title.left.value+title.right.value)/2,(logo.left.value+logo.right.value)/2,1f)
        compose.onNodeWithText("Semana 1").assertDoesNotExist()
        compose.onAllNodesWithText("Ranking")[0].performClick()
        compose.onNodeWithTag("ranking-list").performScrollToIndex(1)
        compose.onNodeWithTag("ranking-list").performScrollToNode(hasText("2 de 3 treinos — falta 1 para desbloquear Voltei por Milagre"))
        compose.onNodeWithText("2 de 3 treinos — falta 1 para desbloquear Voltei por Milagre").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ranking-list").performScrollToNode(hasText("Ver histórico"))
        compose.onNodeWithText("Ver histórico").performScrollTo().performClick()
        compose.onNodeWithText("Seu histórico").assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Início").performClick()
        compose.onNodeWithText("Ver treinos").performClick()
        compose.onNodeWithTag("sessions-list").performScrollToNode(hasTestTag("session-S1 A1"))
        compose.onNodeWithTag("session-S1 A1").assert(hasText("✓ Concluído"))
        compose.onNodeWithTag("sessions-list").performScrollToNode(hasTestTag("session-S2 A3"))
        // Old per-session zero-kg record identifies a workout already in progress, independent of date.
        compose.onNodeWithTag("session-S2 A3").assert(hasText("▶ Em andamento")).performClick()
        val plan=vm.plan;val session=plan.sessions[5];val first=session.exercises.first();val last=session.exercises.last()
        compose.onNodeWithContentDescription("Abrir vídeo de ${first.name} no YouTube").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Buscar como fazer ${first.name} no TikTok").assertIsDisplayed()
        compose.onNodeWithTag("rest-controls-${first.id}-between").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("rest-controls-${first.id}-${first.sets.last().key}").assertDoesNotExist()
        compose.onNodeWithText("Descansar para o próximo exercício · 3:30").performScrollTo().assertIsDisplayed()
        // With large fonts, each control gets its own line; neither label is clipped or truncated.
        val start=compose.onNodeWithText("Descansar para o próximo exercício · 3:30").getUnclippedBoundsInRoot()
        val adjust=compose.onNodeWithTag("rest-controls-${first.id}-between").onChildren().filter(hasClickAction())[1].getUnclippedBoundsInRoot()
        assertTrue(adjust.top >= start.bottom)
        val layouts=mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Descansar para o próximo exercício · 3:30").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout=layouts.single()
        assertFalse(layout.hasVisualOverflow)
        for (line in 1 until layout.lineCount) {
            val lineStart=layout.getLineStart(line)
            assertTrue("No word should split at line $line",layout.layoutInput.text.text[lineStart-1].isWhitespace())
        }
        compose.onNodeWithTag("workout-list").performScrollToNode(hasTestTag("finish-workout"))
        compose.onNodeWithTag("rest-controls-${last.id}-between").assertDoesNotExist()
        compose.onNodeWithTag("rest-controls-${last.id}-${last.sets.last().key}").assertDoesNotExist()
        compose.onNodeWithText("Finalizar treino").performScrollTo().performClick()
        compose.onNodeWithText("Concluir e guardar").assertIsDisplayed().assertIsEnabled().performClick()
        // Robolectric executes the test on its main thread: drain Handler callbacks from the real IO dispatcher.
        compose.waitUntil(30000) { shadowOf(Looper.getMainLooper()).idle(); vm.state?.history?.size==6 }
        assertEquals(6,vm.state?.celebratedRank)
        if (compose.onAllNodesWithText("Novo rank conquistado!").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithTag("sessions-list").performScrollToNode(hasTestTag("session-S2 A3"))
        compose.onNodeWithTag("session-S2 A3").assert(hasText("✓ Concluído"))
        compose.onNodeWithText("WELLNESS").assertIsDisplayed()
        val context=RuntimeEnvironment.getApplication()
        val file=StateFile(File(context.filesDir,"wellness-v1.json"),plan)
        val persisted=file.read()
        assertEquals(6,engineCount(persisted,plan))
        assertEquals(6,persisted.celebratedRank)
        assertEquals("0",persisted.records.values.single().actualLoad)
        assertEquals(210L,vm.engine.restSeconds(persisted,first,null))
        assertEquals(95L,vm.engine.restSeconds(persisted,first,first.sets.last()))
        assertEquals(0L,persisted.history.first().finishedAt)
        // Fresh ViewModel reads persisted acknowledgment; navigating/reopening cannot celebrate again.
        val store=ViewModelStore()
        val reopened=ViewModelProvider(store,ViewModelProvider.AndroidViewModelFactory(context))[WellnessViewModel::class.java]
        compose.waitUntil(30000) { shadowOf(Looper.getMainLooper()).idle(); reopened.state!=null }
        assertNull(reopened.rankCelebration)
        assertEquals(persisted,reopened.state)
        store.clear()
    }
    private fun engineCount(state:AppState,plan:Plan)=WorkoutEngine(plan).ranking(state).completed
}
