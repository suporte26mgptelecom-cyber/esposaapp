package br.com.wellness

import br.com.wellness.data.*
import br.com.wellness.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RankingTest {
    private val plan = Codec.plan(File("src/main/assets/wellness.json").readText())
    private val engine = WorkoutEngine(plan)
    @Test fun everyThresholdAndTheCountsBetweenThresholdsMatchTheRequestedRanks() {
        val titles = listOf("Só Vim Conhecer","Voltei por Milagre","Diva do Aquecimento","Movida a Café e Ódio","A Inimiga do Descanso","Fiscal de Halter","Dona da Academia","Lenda da Maromba")
        assertEquals(titles,PersonalRanking.ranks.map { it.title })
        for (count in 0..24) {
            val progress = PersonalRanking.progress(count)
            val index = count / 3 - 1
            assertEquals(if (index < 0) null else titles[index],progress.current?.title)
            assertEquals(if (count == 24) null else (count / 3 + 1) * 3,progress.next?.threshold)
            assertEquals(if (count == 24) 3 else count % 3,progress.inStep)
            assertEquals(if (count == 24) 0 else 3 - count % 3,progress.remaining)
        }
        assertEquals("2 de 3 treinos — falta 1 para desbloquear Voltei por Milagre",PersonalRanking.progress(5).description)
        assertEquals("Todos os ranks conquistados",PersonalRanking.progress(24).description)
        assertEquals(1f,PersonalRanking.progress(24).fraction)
    }
    @Test fun repeatedCompletionsDoNotCountExtraAndDatesDoNotMatter() {
        var state = engine.initial()
        repeat(10) { state = engine.complete(state,plan.sessions.first(),it.toLong()) }
        assertEquals(10,state.history.size)
        assertEquals(1,engine.ranking(state).completed)
        assertNull(engine.ranking(state).current)
        for (session in plan.sessions.drop(1).reversed()) state = engine.complete(state,session,0)
        assertEquals(24,engine.ranking(state).completed)
        assertEquals("Lenda da Maromba",engine.ranking(state).current?.title)
        assertEquals(33,state.history.size)
    }
    @Test fun oldFormatTwoMigratesAllRecordsPreferencesAndCountsHistoricalConclusions() {
        val s = plan.sessions.first(); val ex = s.exercises.first();val set=ex.sets.first()
        var before = engine.load(engine.initial(),ex,"35.5")
        before = engine.reps(before,ex,set,"0")
        before = engine.actualLoad(before,s,ex,set,"0")
        before = engine.mark(before,s,ex,set,true)
        before = engine.beginRest(before,engine.restRequest(before,ex,set,65,true),1000)
        before = engine.startSession(before,plan.sessions[6])
        repeat(5) { before = engine.complete(before,plan.sessions[it],it.toLong()) }
        val fields = Codec.json.parseToJsonElement(Codec.encode(before)).jsonObject.toMutableMap()
        fields["format"] = JsonPrimitive(2); fields.remove("startedSessions");fields.remove("celebratedRank")
        val legacy = JsonObject(fields).toString()
        val folder = Files.createTempDirectory("v13-migration").toFile()
        try {
            val file=File(folder,"wellness-v1.json");file.writeText(legacy)
            val storage=StateFile(file,plan);val migrated=storage.read()
            assertEquals(3,migrated.format);assertEquals(legacy,file.readText())
            assertEquals(before.history,migrated.history);assertEquals(before.records,migrated.records)
            assertEquals(before.shared,migrated.shared);assertEquals(before.profiles,migrated.profiles)
            assertEquals(before.restPreferences,migrated.restPreferences);assertEquals(before.timer,migrated.timer)
            assertEquals(5,engine.ranking(migrated).completed)
            assertEquals("Só Vim Conhecer",engine.ranking(migrated).current?.title)
            val claimed=PersonalRanking.acknowledge(engine.startSession(migrated,plan.sessions[6]))
            storage.write(claimed)
            assertEquals(claimed,storage.read())
            assertEquals(claimed,storage.import(Codec.encode(claimed)))
            assertNull(PersonalRanking.unannounced(storage.read()))
            assertEquals(SessionStatus.IN_PROGRESS,engine.sessionStatus(storage.read(),plan.sessions[6]))
        } finally { folder.deleteRecursively() }
    }
    @Test fun celebrationIsClaimedOncePersistsAcrossReopenAndUnlocksOnlyAtNewMilestones() {
        var state=engine.initial()
        for (i in 0..23) {
            state=engine.complete(state,plan.sessions[i],i.toLong())
            val expected=if ((i+1)%3==0) i+1 else null
            assertEquals(expected,PersonalRanking.unannounced(state)?.threshold)
            state=PersonalRanking.acknowledge(state)
            assertNull(PersonalRanking.unannounced(Codec.decode(Codec.encode(state),plan)))
            state=engine.complete(state,plan.sessions[i],1000)
            assertNull(PersonalRanking.unannounced(state))
        }
        assertEquals(24,state.celebratedRank)
        assertEquals(48,state.history.size)
    }
    @Test fun statusesPersistWithoutDependingOnCalendarOrSharedWorkLoads() {
        val first=plan.sessions.first();val other=plan.sessions[2];val ex=first.exercises.first()
        var state=engine.load(engine.initial(),ex,"40")
        assertEquals(SessionStatus.PENDING,engine.sessionStatus(state,first))
        assertEquals(SessionStatus.PENDING,engine.sessionStatus(state,other))
        state=engine.startSession(state,first)
        assertEquals(SessionStatus.IN_PROGRESS,engine.sessionStatus(state,first))
        state=Codec.decode(Codec.encode(state),plan)
        assertEquals(SessionStatus.IN_PROGRESS,engine.sessionStatus(state,first))
        assertEquals(SessionStatus.PENDING,engine.sessionStatus(state,other))
        state=engine.complete(state,first,0)
        assertEquals(SessionStatus.COMPLETED,engine.sessionStatus(state,first))
        state=engine.restart(state,first)
        assertEquals(SessionStatus.COMPLETED,engine.sessionStatus(state,first))
        val legacy=engine.mark(engine.initial(),first,ex,ex.sets.first(),true)
        assertEquals(SessionStatus.IN_PROGRESS,engine.sessionStatus(legacy,first))
        assertEquals(SessionStatus.PENDING,engine.sessionStatus(legacy,other))
    }
    @Test fun finalSeriesHasExactlyOneTransitionAndLastExerciseOnlyFinishes() {
        var series=0;var transitions=0;var finishes=0
        for (s in plan.sessions) for (ex in s.exercises) for (set in ex.sets) {
            when(WorkoutActions.restAction(s,ex,set)) {
                RestAction.SERIES -> {series++;assertNotEquals(ex.sets.last().key,set.key)}
                RestAction.NEXT_EXERCISE -> {transitions++;assertEquals(ex.sets.last().key,set.key);assertNotEquals(s.exercises.last().id,ex.id)}
                RestAction.FINISH -> {finishes++;assertEquals(s.exercises.last().id,ex.id);assertEquals(ex.sets.last().key,set.key)}
            }
        }
        assertEquals(464,series);assertEquals(106,transitions);assertEquals(24,finishes)
        val ex=plan.sessions.first().exercises.first();val last=ex.sets.last()
        var state=engine.initial()
        state=state.copy(restPreferences=mapOf(engine.restKey(state,ex,last) to 95L,engine.restKey(state,ex,null) to 210L))
        val roundTrip=Codec.decode(Codec.encode(state),plan)
        assertEquals(95L,engine.restSeconds(roundTrip,ex,last))
        assertEquals(210L,engine.restSeconds(roundTrip,ex,null))
        val completed=engine.complete(roundTrip,plan.sessions.first(),0).history.single()
        assertEquals(210L,completed.exercises.first().sets.last().restSeconds)
        assertEquals(210L,completed.exercises.first().betweenRestSeconds)
        assertNull(completed.exercises.last().sets.last().restSeconds)
        assertNull(completed.exercises.last().betweenRestSeconds)
        assertEquals(95L,engine.restSeconds(roundTrip,ex,last))
    }
    @Test fun youtubeLinksRemainExactlyThoseOfTheWorkbookWithNoInventedFallback() {
        for (ex in plan.sessions.flatMap { it.exercises }) assertEquals(ex.video,ExerciseLibrary.youtubeUrl(ex))
        val ex=plan.sessions.first().exercises.first()
        for (missing in listOf("", " ", "https://www.youtube.com", "https://www.youtube.com/results?search_query=treino", "https://example.org/video", "not a link"))
            assertNull(ExerciseLibrary.youtubeUrl(ex.copy(video=missing)))
        val seated=ExerciseLibrary.search(plan,"lateral com halteres sentada")
        assertEquals(2,seated.size);assertNotEquals(ExerciseLibrary.youtubeUrl(seated[0]),ExerciseLibrary.youtubeUrl(seated[1]))
    }
    @Test fun titlesUppercaseOnlyForDisplayAndPreserveAccentsAndIdentifiers() {
        assertEquals("PERNAS/GLÚTEOS I","Pernas/glúteos I".workoutTitle())
        val encoded=Codec.json.encodeToString(Plan.serializer(),plan)
        plan.sessions.forEach { assertEquals(it.title.uppercase(java.util.Locale.forLanguageTag("pt-BR")),it.title.workoutTitle()) }
        assertEquals(plan,Codec.plan(encoded))
        assertEquals("S1 A1",plan.sessions.first().id)
    }
    @Test fun invalidRankingAndSessionPreferencesAreRejectedBeforeReplacingSavedData() {
        val folder=Files.createTempDirectory("rank-invalid").toFile()
        try {
            val store=StateFile(File(folder,"state.json"),plan);val state=engine.initial();store.write(state)
            for (bad in listOf(state.copy(celebratedRank=3),state.copy(celebratedRank=2),state.copy(startedSessions=setOf("other")))) {
                assertThrows(Exception::class.java) { store.import(Codec.encode(bad)) }
                assertEquals(state,store.read())
            }
        } finally { folder.deleteRecursively() }
    }
}
