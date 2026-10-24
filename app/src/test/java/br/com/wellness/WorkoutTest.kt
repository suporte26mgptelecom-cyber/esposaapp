package br.com.wellness

import br.com.wellness.data.*
import br.com.wellness.domain.*
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WorkoutTest {
    private val plan = Codec.plan(File("src/main/assets/wellness.json").readText())
    private val engine = WorkoutEngine(plan)
    private fun ex(session: String, name: String) = plan.sessions.first { it.id == session }.exercises.first { it.name == name }
    private fun set(ex: Exercise, key: String) = ex.sets.first { it.key == key }

    @Serializable data class Case(val sheet: String, val cell: String, val load: String?, val increment: String?, val factor: String, val expected: String?)
    @Test fun everyWorkbookFormulaMatchesIndependentFixtures() {
        val cases = Codec.json.decodeFromString<List<Case>>(File("src/test/resources/formula-cases.json").readText())
        assertEquals(594 * 7, cases.size)
        cases.forEach { c -> assertEquals("${c.sheet}!${c.cell}: WS=${c.load}, inc=${c.increment}", c.expected, Loads.calculate(c.load, c.factor, c.increment)) }
    }
    @Test fun calendarAndApprovedAdaptationsPreserved() {
        assertEquals(24, plan.sessions.size)
        assertEquals(594, plan.sessions.sumOf { it.exercises.sumOf { e -> e.sets.size } })
        plan.sessions.forEachIndexed { i, s ->
            assertEquals(i / 3 + 1, s.week)
            assertEquals("A${i % 3 + 1}", s.code)
            assertEquals(listOf("Segunda", "Quarta", "Sexta")[i % 3], s.day)
            assertEquals(i % 2 == 0, s.title.startsWith("Pernas"))
            assertFalse(s.exercises.any { it.name.contains("Step", true) || it.name.contains("Búlgaro", true) || it.name.contains("Coice", true) })
        }
        assertEquals("1.08", set(ex("S4 A2", "Terra sumô"), "Top Set:1").factor)
        assertEquals("108", Loads.calculate("100", "1.08", null))
        assertEquals("75", Loads.calculate("100", "0.75", null))
    }
    @Test fun originalFiveKgAndEmptyInputsArePreserved() {
        val abd = ex("S1 A1", "Cadeira abdutora")
        val state = engine.initial()
        assertEquals("5", engine.values(state, abd).load)
        assertEquals("1.5", Loads.calculate("5", abd.sets.first().factor, null))
        assertEquals("3.5", Loads.calculate("5", abd.sets[1].factor, null))
        assertEquals("4.25", Loads.calculate("5", abd.sets[2].factor, null))
        assertEquals(1, state.shared.values.count { it.load != null })
        assertNull(Loads.calculate(null, "0.85", "2.5"))
        assertEquals("0", Loads.calculate("0", "0.85", "2.5"))
        assertEquals("16.2", Loads.calculate("15", "1.08", "0"))
        assertEquals("7.5", Loads.calculate("10", "0.85", "2.5"))
        assertEquals("4.25", Loads.calculate("5", "0.85", "0.01"))
    }
    @Test fun loadsAndRepsSynchronizeByProfileAndSemanticSetAcrossAllSessions() {
        val occurrences = plan.sessions.flatMap { it.exercises }
        for (source in occurrences) {
            var state = engine.load(engine.initial(), source, "42.5")
            for (spec in source.sets) state = engine.reps(state, source, spec, "9")
            for (target in occurrences) {
                val v = engine.values(state, target)
                if (target.originalProfile == source.originalProfile) {
                    assertEquals("42.5", v.load)
                    target.sets.forEach { spec -> assertEquals(if (source.sets.any { it.key == spec.key }) "9" else null, v.reps[spec.key]) }
                } else assertNotEquals("42.5", v.load)
            }
        }
    }
    @Test fun workingSetDoesNotFollowRowPositionAndVariantsNeverMix() {
        val first = ex("S1 A1", "Cadeira abdutora")
        val later = ex("S8 A2", "Cadeira abdutora")
        assertNotEquals(first.sets.indexOf(set(first, "Working Set:1")), later.sets.indexOf(set(later, "Working Set:1")))
        val state = engine.reps(engine.initial(), first, set(first, "Working Set:1"), "0")
        assertEquals("0", engine.values(state, later).reps["Working Set:1"])
        assertNull(engine.values(state, later).reps["Working Set:2"])
        val pulley = ex("S1 A2", "Remada articulada unilateral")
        val machine = ex("S2 A1", "Remada articulada unilateral")
        val updated = engine.load(state, pulley, "30")
        assertNull(engine.values(updated, machine).load)
        val seated = ex("S1 A2", "Elevação lateral com halteres sentada")
        val ninety = ex("S2 A1", "Elevação lateral com halteres sentada (90º)")
        assertNull(engine.values(engine.load(state, seated, "12"), ninety).load)
    }
    @Test fun changingEquipmentOrRegistrationCreatesSeparateProfileAndRestoresOldOne() {
        val ex = ex("S1 A1", "Elevação pélvica no smith, máquina ou livre")
        val other = ex("S8 A2", ex.name)
        var state = engine.load(engine.initial(), ex, "50")
        val newProfile = Profile(ex.id, "Máquina", "Visor / pilha", "Máquina azul")
        state = engine.configure(state, ex, newProfile, "2.5", "90")
        assertNull(engine.values(state, ex).load)
        assertEquals(newProfile, engine.profile(state, other))
        state = engine.load(state, other, "70")
        assertEquals("70", engine.values(state, ex).load)
        state = engine.configure(state, other, ex.originalProfile, null, null)
        assertEquals("50", engine.values(state, ex).load)
        assertNull(engine.values(state, ex).increment)
    }
    @Test fun marksAndActualLoadsAreSessionSpecificAndZeroIsExplicit() {
        val s = plan.sessions.first(); val ex = s.exercises.first(); val spec = ex.sets.first()
        var state = engine.mark(engine.initial(), s, ex, spec, true)
        state = engine.actualLoad(state, s, ex, spec, "0")
        val later = plan.sessions[2]
        assertFalse(engine.record(state, later, later.exercises.first(), spec).done)
        assertNull(engine.record(state, later, later.exercises.first(), spec).actualLoad)
        state = engine.complete(state, s, 100)
        assertEquals("0", state.history.first().exercises.first().sets.first().usedLoad)
        assertTrue(state.history.first().exercises.first().sets.first().explicitLoad)
        assertEquals("S1 A2", engine.next(state)?.id)
    }
    @Test fun persistedSnapshotsSurviveSharedEditsAndRepeatedCompletion() {
        val dir = Files.createTempDirectory("wellness-test").toFile()
        try {
            val f = File(dir, "state.json"); val store = StateFile(f, plan)
            val s = plan.sessions.first(); val ex = s.exercises.first(); val spec = set(ex, "Working Set:1")
            var state = engine.reps(engine.initial(), ex, spec, "12")
            state = engine.complete(state, s, 12345)
            val snapshot = state.history.single()
            state = engine.load(state, ex, "20")
            state = engine.reps(state, ex, spec, "6")
            store.write(state)
            assertEquals(state, StateFile(f, plan).read())
            assertEquals(snapshot, store.read().history.single())
            assertEquals("5", snapshot.exercises.first().workLoad)
            assertEquals("12", snapshot.exercises.first().sets.first { it.spec.key == "Working Set:1" }.reps)
            state = engine.complete(engine.restart(state, s), s, 12346)
            store.write(state)
            assertEquals(2, store.read().history.size)
            assertEquals(1, engine.completedIds(state).size)
            assertEquals(snapshot, state.history.first())
            assertFalse(File(dir, "state.json.pending").exists())
        } finally { dir.deleteRecursively() }
    }
    @Test fun backupRoundTripAndInvalidImportNeverOverwriteCurrentFile() {
        val dir = Files.createTempDirectory("wellness-backup").toFile()
        try {
            val file = File(dir, "records.json"); val storage = StateFile(file, plan)
            val ex = plan.sessions.first().exercises.first()
            val state = engine.load(engine.initial(), ex, "0")
            storage.write(state)
            val backup = Codec.encode(state)
            storage.write(engine.load(state, ex, null))
            assertEquals(state, storage.import(backup))
            for (bad in listOf("{bad", backup.replace(plan.sourceSha256, "another-plan"), backup.replace("\"load\": \"0\"", "\"load\": \"-1\""), backup.replace("\"format\": 3", "\"format\": 99"))) {
                assertThrows(Exception::class.java) { storage.import(bad) }
                assertEquals(state, storage.read())
            }
            file.writeText("interrupted-invalid-file")
            assertThrows(Exception::class.java) { storage.read() }
            assertEquals("interrupted-invalid-file", file.readText())
        } finally { dir.deleteRecursively() }
    }
    @Test fun timerPersistsDeadlineAndPauseResumeWorksWithoutLiveProcess() {
        val state = engine.startTimer(engine.initial(), "90", "Hack", 1000)
        val restored = Codec.decode(Codec.encode(state), plan)
        assertEquals(60000L, engine.remaining(restored.timer, 31000))
        val paused = engine.pauseTimer(restored, 31000)
        assertEquals(60000L, engine.remaining(paused.timer, 500000))
        val resumed = engine.resumeTimer(paused, 100000)
        assertEquals(30000L, engine.remaining(resumed.timer, 130000))
        assertEquals(0L, engine.remaining(resumed.timer, 200000))
    }
}
