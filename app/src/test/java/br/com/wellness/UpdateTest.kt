package br.com.wellness

import br.com.wellness.domain.*
import br.com.wellness.data.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.file.Files

class UpdateTest {
    private val plan = Codec.plan(File("src/main/assets/wellness.json").readText())
    private val engine = WorkoutEngine(plan)
    private val session = plan.sessions.first()
    private val ex = session.exercises.first()
    @Test fun catalogPreservesDistinctVariationsAndSearchIgnoresAccents() {
        val catalog = ExerciseLibrary.catalog(plan)
        assertEquals(19, catalog.size)
        assertEquals(catalog.size, catalog.map { it.id }.toSet().size)
        assertEquals(2, ExerciseLibrary.search(plan, "lateral com halteres sentada").size)
        assertEquals(listOf("Terra sumô"), ExerciseLibrary.search(plan, " SUMO ").map { it.name })
        assertEquals(19, ExerciseLibrary.search(plan, "").size)
        assertTrue(ExerciseLibrary.search(plan, "inexistente").isEmpty())
    }
    @Test fun everyVideoUrlContainsOnlyTheExactExerciseSearchWithEncodedUtf8() {
        ExerciseLibrary.catalog(plan).forEach { ex ->
            val url = ExerciseLibrary.videoUrl(ex)
            val uri = URI(url)
            assertEquals("https", uri.scheme)
            assertEquals("www.tiktok.com", uri.host)
            assertEquals("/search", uri.path)
            assertEquals("como fazer ${ex.name}", URLDecoder.decode(uri.rawQuery.removePrefix("q="), "UTF-8"))
            assertFalse(url.contains(" ")); assertFalse(url.contains("+"))
        }
    }
    @Test fun guidesCoverEveryExerciseAndCalculationNotesAreSeparate() {
        val guides = Codec.json.decodeFromString<List<ExerciseGuide>>(File("src/main/assets/exercise-guides.json").readText())
        assertEquals(ExerciseLibrary.catalog(plan).map { it.id }.toSet(), guides.map { it.id }.toSet())
        assertEquals(19, guides.size)
        assertTrue(guides.all { it.tip.isNotBlank() && it.sources.isNotEmpty() })
        assertTrue(ExerciseLibrary.executionNotes(ex).any { it.contains("Tronco levemente") })
        assertFalse(ExerciseLibrary.executionNotes(ex).any { it.contains("PURGE") || it.contains("percentual") })
        assertTrue(ExerciseLibrary.calculationNotes(ex).any { it.contains("PURGE") })
        plan.sessions.flatMap { it.exercises }.forEach { e ->
            val sentences = e.notes.flatMap { it.split(Regex("(?<=\\.)\\s+")) }.filter { it.isNotBlank() }
            assertEquals(sentences.toSet(), (ExerciseLibrary.executionNotes(e) + ExerciseLibrary.calculationNotes(e)).toSet())
        }
    }
    @Test fun allSeriesHaveTheRequestedDefaultRestTimes() {
        val expected = mapOf("Aquecimento" to 40L, "Feeder Set" to 90L, "Working Set" to 180L, "Rest Pause" to 20L, "Top Set" to 180L, "Back Off" to 180L)
        expected.forEach { (type, seconds) -> assertEquals(seconds, RestDurations.defaultSeconds(type)) }
        val initial = engine.initial()
        plan.sessions.flatMap { it.exercises }.forEach { e -> e.sets.forEach { s ->
            assertEquals(expected.getValue(s.key.substringBefore(':')), engine.restSeconds(initial, e, s))
        } }
        assertEquals(180L, engine.restSeconds(initial, ex, null))
        assertEquals("1:30", RestDurations.display(90))
        assertEquals("0:40", RestDurations.display(40))
        assertEquals(90L, RestDurations.parse("1", "30"))
        assertEquals(0L, RestDurations.parse("0", "0"))
        for ((m,s) in listOf("" to "20", "1" to "", "-1" to "0", "1" to "60", "1.5" to "0", "99999999999999999999" to "0")) assertNull(RestDurations.parse(m,s))
    }
    @Test fun restsUseSemanticSeriesKeysAcrossAllSessionsAndStayInTheirOwnProfile() {
        var state = engine.initial()
        val working = ex.sets.first { it.key == "Working Set:1" }
        val request = engine.restRequest(state, ex, working, 125, true)
        state = engine.beginRest(state, request, 1000)
        val later = plan.sessions.flatMap { it.exercises }.filter { it.originalProfile == ex.originalProfile }
        later.forEach { e ->
            e.sets.forEach { set -> assertEquals(if (set.key == working.key) 125L else RestDurations.defaultSeconds(set.key.substringBefore(':')), engine.restSeconds(state, e, set)) }
            assertEquals(180L, engine.restSeconds(state, e, null))
        }
        val p = Profile(ex.id, ex.equipment, ex.recording, "outro aparelho")
        val different = engine.configure(state, ex, p, null, null)
        assertEquals(180L, engine.restSeconds(different, ex, working))
        val restored = engine.configure(different, ex, ex.originalProfile, null, null)
        assertEquals(125L, engine.restSeconds(restored, ex, working))
        val disk = Files.createTempDirectory("rests").toFile()
        try {
            val storage = StateFile(File(disk,"state.json"), plan)
            storage.write(state)
            assertEquals(state, storage.read())
            assertEquals(state, storage.import(Codec.encode(state)))
        } finally { disk.deleteRecursively() }
    }
    @Test fun activeOrPausedTimerRequiresReplacementAndMarkDoesNotStartRest() {
        val initial = engine.initial()
        val first = ex.sets.first()
        assertEquals(TimerState(), engine.mark(initial, session, ex, first, true).timer)
        val request = engine.restRequest(initial, ex, first)
        var state = engine.beginRest(initial, request, 1000)
        assertEquals(40000L, engine.remaining(state.timer, 1000))
        assertFalse(engine.record(state, session, ex, first).done)
        assertThrows(IllegalArgumentException::class.java) { engine.beginRest(state, request, 2000) }
        state = engine.pauseTimer(state, 2000)
        assertEquals(39000L, engine.remaining(state.timer, 500000))
        assertThrows(IllegalArgumentException::class.java) { engine.beginRest(state, request, 500000) }
        val replaced = engine.beginRest(state, request.copy(seconds=20), 500000, true)
        assertNotEquals(state.timer.id, replaced.timer.id)
        assertEquals(20000L, engine.remaining(replaced.timer, 500000))
        assertEquals(0L, engine.remaining(Codec.decode(Codec.encode(replaced), plan).timer, 600000))
        val next = engine.beginRest(replaced, engine.restRequest(replaced, ex, null), 600000)
        assertEquals("Próximo exercício", next.timer.seriesLabel)
        assertEquals(initial.records, next.records)
        assertEquals(initial.history, next.history)
    }
    @Test fun legacyV1FileMigratesWithExplicitZeroLoadsAndImmutableHistoryIntact() {
        var before = engine.load(engine.initial(), ex, "27.5")
        before = engine.reps(before, ex, ex.sets.first(), "0")
        before = engine.actualLoad(before, session, ex, ex.sets.first(), "0")
        before = engine.mark(before, session, ex, ex.sets.first(), true)
        before = engine.configure(before, ex, ex.originalProfile, "2.5", "95")
        before = engine.complete(before, session, 100)
        before = engine.startTimer(before,"90","Descanso antigo",1000)
        // Construct the exact old schema: absent additive fields in root, timer and snapshots.
        val root = Codec.json.parseToJsonElement(Codec.encode(before)).jsonObject.toMutableMap()
        root["format"] = JsonPrimitive(1); root.remove("restPreferences"); root.remove("startedSessions"); root.remove("celebratedRank")
        root["timer"] = JsonObject(root.getValue("timer").jsonObject.filterKeys { it in setOf("deadline", "remainingMs", "label") })
        root["history"] = JsonArray(root.getValue("history").jsonArray.map { h ->
            JsonObject(h.jsonObject.toMutableMap().apply {
                put("exercises", JsonArray(getValue("exercises").jsonArray.map { e ->
                    JsonObject(e.jsonObject.filterKeys { it != "betweenRestSeconds" }.toMutableMap().apply {
                        put("sets", JsonArray(getValue("sets").jsonArray.map { set -> JsonObject(set.jsonObject.filterKeys { it != "restSeconds" }) }))
                    })
                }))
            })
        })
        val legacy = JsonObject(root).toString()
        val disk = Files.createTempDirectory("legacy").toFile()
        try {
            val file = File(disk,"wellness-v1.json"); file.writeText(legacy)
            val storage = StateFile(file,plan); val migrated = storage.read()
            assertEquals(3,migrated.format)
            assertEquals(legacy,file.readText()) // read never deletes/replaces user's file
            assertEquals(before.shared,migrated.shared); assertEquals(before.profiles,migrated.profiles)
            assertEquals(before.records,migrated.records)
            assertEquals("0",engine.record(migrated,session,ex,ex.sets.first()).actualLoad)
            assertEquals("0",migrated.history.single().exercises.first().sets.first().usedLoad)
            assertTrue(migrated.history.single().exercises.first().sets.first().explicitLoad)
            assertEquals(95L,engine.restSeconds(migrated,ex,null))
            assertEquals(40L,engine.restSeconds(migrated,ex,ex.sets.first()))
            val history = migrated.history
            val edited = engine.reps(engine.load(migrated,ex,"50"),ex,ex.sets.first(),"8")
            storage.write(edited)
            assertEquals(edited, storage.read())
            assertEquals(history,storage.read().history)
            assertEquals(edited,storage.import(Codec.encode(edited)))
            assertEquals(history,storage.read().history)
        } finally { disk.deleteRecursively() }
    }
    @Test fun restSettingsDoNotRewriteACompletedSnapshot() {
        var state = engine.complete(engine.initial(),session,100)
        val snapshot = state.history.single()
        state = engine.beginRest(state,engine.restRequest(state,ex,ex.sets.first(),55,true),1000)
        assertEquals(snapshot,state.history.single())
        assertEquals(40L,snapshot.exercises.first().sets.first().restSeconds)
        assertEquals(55L,engine.restSeconds(state,ex,ex.sets.first()))
        assertThrows(IllegalArgumentException::class.java) { Codec.decode(Codec.encode(state.copy(restPreferences=mapOf("bad|Working Set:1" to 20L))),plan) }
    }
}
