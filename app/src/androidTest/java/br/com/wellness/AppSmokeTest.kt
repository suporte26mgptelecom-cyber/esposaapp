package br.com.wellness

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import br.com.wellness.data.Codec
import br.com.wellness.domain.WorkoutEngine
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class AppSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun editsSurviveActivityCloseAndAppearInAnotherSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, "wellness-v1.json")
        file.delete()
        val plan = Codec.plan(context.assets.open("wellness.json").bufferedReader().use { it.readText() })
        val ex = plan.sessions.first().exercises.first()
        val engine = WorkoutEngine(plan)
        val key = ex.originalProfile.key
        var activity = ActivityScenario.launch(MainActivity::class.java)
        try {
            compose.waitUntil(60000) { compose.onAllNodesWithText("Começar treino").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Começar treino").performScrollTo().performClick()
            compose.onNodeWithContentDescription("WS-S1 A1-$key").performScrollTo().performTextReplacement("27,5")
            compose.waitUntil(60000) {
                runCatching { engine.values(Codec.decode(file.readText(), plan), ex).load == "27.5" }.getOrDefault(false)
            }
            compose.onNodeWithContentDescription("Feita ${ex.name} Aquecimento").performScrollTo().performClick()
            compose.waitUntil(60000) {
                runCatching { engine.record(Codec.decode(file.readText(), plan), plan.sessions.first(), ex, ex.sets.first()).done }.getOrDefault(false)
            }
            activity.close()
            activity = ActivityScenario.launch(MainActivity::class.java)
            compose.waitUntil(60000) { compose.onAllNodesWithText("Começar treino").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Começar treino").performScrollTo().performClick()
            compose.onNodeWithContentDescription("WS-S1 A1-$key").assertTextContains("27,5")
            compose.onNodeWithContentDescription("Feita ${ex.name} Aquecimento").assertIsOn()
            compose.onNodeWithText("Voltar").performClick()
            compose.onNodeWithTag("home-list").performScrollToNode(hasText("A3 · Sexta"))
            compose.onNodeWithText("A3 · Sexta").performClick()
            compose.onNodeWithContentDescription("WS-S1 A3-$key").assertTextContains("27,5")
            compose.onNodeWithContentDescription("Feita ${ex.name} Aquecimento").assertIsOff()
        } finally { activity.close() }
    }
}
