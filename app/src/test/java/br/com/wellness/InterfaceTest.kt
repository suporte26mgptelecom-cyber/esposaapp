package br.com.wellness

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import br.com.wellness.data.*
import br.com.wellness.domain.*
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
@Config(sdk = [34], application = InterfaceTest.FixtureApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InterfaceTest {
    class FixtureApplication : Application() {
        override fun onCreate() {
            super.onCreate()
            val plan = Codec.plan(assets.open("wellness.json").bufferedReader().use { it.readText() })
            val engine = WorkoutEngine(plan); val s = plan.sessions.first(); val ex = s.exercises.first()
            var state = engine.load(engine.initial(),ex,"27.5")
            state = engine.actualLoad(state,s,ex,ex.sets.first(),"0")
            state = engine.complete(state,s,100)
            // Seed previous schema so navigation opens migrated data with a saved history.
            File(filesDir,"wellness-v1.json").writeText(Codec.encode(state.copy(format=1)))
            getSharedPreferences("wellness-ui",Context.MODE_PRIVATE).edit().putBoolean("notification-asked",true).commit()
        }
    }
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        val folder = File("build/reports/screenshots").apply { mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(folder,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }
    @Test fun navigationSearchWorkoutAndAdjustableTimerPreserveLegacyData() {
        compose.waitUntil(30000) { compose.onAllNodesWithText("Bem-vinda, Nanda").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("WELLNESS").assertIsDisplayed()
        compose.onNodeWithText("Seu treino com segurança").assertDoesNotExist()
        compose.onNodeWithContentDescription("Logotipo Wellness").assertIsDisplayed()
        compose.onNodeWithText("Bem-vinda, Nanda").assertIsDisplayed()
        capture("inicio")
        compose.onNodeWithText("Exercícios").performClick()
        compose.onNodeWithText("Buscar por nome").performTextInput("sumo")
        compose.onNodeWithText("Terra sumô").assertIsDisplayed()
        compose.onNodeWithText("YouTube").performScrollTo().assertExists()
        compose.onNodeWithText("TikTok").assertIsDisplayed()
        capture("exercicios-busca")
        compose.onAllNodesWithText("Ranking")[0].performClick()
        compose.onNodeWithTag("ranking-list").performScrollToNode(hasText("Ver histórico"))
        compose.onNodeWithText("Ver histórico").performScrollTo().performClick()
        compose.onNodeWithText("Seu histórico").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Seu cuidado, guardado").assertIsDisplayed()
        compose.onNodeWithText("Início").performClick()
        // Open first session (already concluded) to exercise migrated zero-kg record.
        compose.onNodeWithText("Ver treinos").performClick()
        compose.onNodeWithTag("sessions-list").performScrollToNode(hasText("A1 · Segunda"))
        compose.onNodeWithText("A1 · Segunda").performClick()
        compose.onAllNodesWithText("Aparelho e carga")[0].performScrollTo().performClick()
        compose.onNodeWithText("Perfil do aparelho").assertIsDisplayed()
        compose.onNodeWithText("Salvar perfil").performClick()
        compose.onAllNodesWithText("Dica de execução")[0].performScrollTo().assertExists()
        compose.onAllNodesWithText("Sobre o cálculo ▾")[0].performScrollTo().performClick()
        compose.onNodeWithText("Aquecimento: 30% da WS, referência da PURGE; o PDF não define percentual.").assertExists()
        compose.onAllNodesWithText("Kg usados").assertCountEquals(0)
        compose.onAllNodesWithText("Descansar · 0:40")[0].performScrollTo().assertExists()
        // Restrict to the first exercise's card to disambiguate equal per-series controls.
        compose.onAllNodesWithText("Ajustar tempo")[0].performScrollTo().performClick()
        compose.onNodeWithText("Minutos").performTextReplacement("1")
        compose.onNodeWithText("Segundos").performTextReplacement("5")
        compose.onNodeWithText("Salvar e iniciar").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithTag("rest-banner").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("rest-banner").assertIsDisplayed()
        capture("treino-descanso")
        compose.onNodeWithText("Pausar").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Continuar").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Descansar · 1:05").performScrollTo().performClick()
        compose.onNodeWithText("Substituir descanso atual?").assertIsDisplayed()
        compose.onNodeWithText("Manter atual").performClick()
        compose.onNodeWithText("Continuar").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Pausar").fetchSemanticsNodes().isNotEmpty() }
        // Scrolling must not displace the banner.
        compose.onAllNodesWithText("Descansar · 1:30")[0].performScrollTo()
        compose.onNodeWithTag("rest-banner").assertIsDisplayed()
        compose.onNodeWithText("Encerrar").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithTag("rest-banner").fetchSemanticsNodes().isEmpty() }
        val context = RuntimeEnvironment.getApplication()
        val plan = Codec.plan(context.assets.open("wellness.json").bufferedReader().use { it.readText() })
        val persisted = StateFile(File(context.filesDir,"wellness-v1.json"),plan).read()
        assertEquals("0",persisted.records.values.single().actualLoad)
        assertEquals("0",persisted.history.single().exercises.first().sets.first().usedLoad)
        assertEquals("27.5",WorkoutEngine(plan).values(persisted,plan.sessions.first().exercises.first()).load)
        assertTrue(persisted.restPreferences.values.contains(65L))
    }
}
