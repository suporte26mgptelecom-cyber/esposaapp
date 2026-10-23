package br.com.wellness.data

import br.com.wellness.domain.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object Codec {
    val json = Json { encodeDefaults = true; prettyPrint = true }
    const val MAX_BYTES = 16 * 1024 * 1024
    fun plan(text: String): Plan = json.decodeFromString(text)
    fun encode(state: AppState): String = json.encodeToString(state)
    fun decode(text: String, plan: Plan): AppState {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup maior que 16 MB." }
        val state = json.decodeFromString<AppState>(text)
        require(state.format in 1..2) { "Versão de backup incompatível." }
        require(state.sourceSha256 == plan.sourceSha256) { "Backup de outra planilha." }
        val exercises = plan.sessions.flatMap { it.exercises }
        val originals = exercises.map { it.originalProfile.key }.toSet()
        val sessions = plan.sessions.associateBy { it.id }
        fun profile(p: Profile) {
            require(exercises.any { it.id == p.exerciseId })
            require(p.equipment in plan.equipmentOptions && p.recording in plan.recordingOptions)
            require(p.variant.length <= 80)
        }
        fun number(n: String?) { require(Loads.valid(n)) { "Valor numérico inválido no backup." } }
        state.shared.forEach { (key, v) ->
            profile(v.profile); require(key == v.profile.key)
            number(v.load); number(v.increment); number(v.rest)
            val validKeys = exercises.filter { it.id == v.profile.exerciseId }.flatMap { it.sets }.map { it.key }.toSet()
            require(v.reps.keys.all { it in validKeys }); v.reps.values.forEach(::number)
        }
        state.profiles.forEach { (key, p) ->
            profile(p); require(key in originals && state.shared.containsKey(p.key))
            require(exercises.any { it.originalProfile.key == key && it.id == p.exerciseId })
        }
        state.records.forEach { (key, v) ->
            val parts = key.split('|'); require(parts.size == 3)
            val session = sessions.getValue(parts[0]); val shared = state.shared.getValue(parts[1])
            require(session.exercises.any { it.id == shared.profile.exerciseId && it.sets.any { set -> set.key == parts[2] } })
            number(v.actualLoad)
        }
        require(state.history.map { it.id }.toSet().size == state.history.size)
        state.history.forEach { h ->
            val session = sessions.getValue(h.sessionId)
            require(h.id.isNotBlank() && h.finishedAt >= 0 && h.week == session.week && h.phase == session.phase)
            require(h.title == session.title && h.exercises.map { it.id } == session.exercises.map { it.id })
            h.exercises.zip(session.exercises).forEach { (ex, source) ->
                profile(ex.profile); require(ex.profile.exerciseId == ex.id && ex.name == source.name)
                require(ex.betweenRestSeconds == null || ex.betweenRestSeconds >= 0)
                number(ex.workLoad); number(ex.increment); number(ex.rest)
                require(ex.sets.map { it.spec } == source.sets)
                ex.sets.forEach { set ->
                    require(set.restSeconds == null || set.restSeconds >= 0)
                    number(set.suggestedLoad); number(set.usedLoad); number(set.reps)
                    require(set.suggestedLoad == Loads.calculate(ex.workLoad, set.spec.factor, ex.increment))
                    if (!set.explicitLoad) require(set.usedLoad == set.suggestedLoad)
                }
            }
        }
        require(state.timer.deadline == null || state.timer.remainingMs == null)
        require(state.timer.deadline == null || state.timer.deadline >= 0)
        require(state.timer.remainingMs == null || state.timer.remainingMs >= 0)
        require(state.timer.label.length <= 300)
        require(state.timer.exerciseName.length <= 200 && state.timer.seriesLabel.length <= 100 && state.timer.id.length <= 100)
        state.restPreferences.forEach { (key, seconds) ->
            val parts = key.split('|'); require(parts.size == 2)
            val v = requireNotNull(state.shared[parts[0]]) { "Perfil de descanso inexistente." }
            val specs = exercises.filter { it.id == v.profile.exerciseId }.flatMap { it.sets }.map { it.key }
            require(parts[1] == "between" || parts[1] in specs)
            require(seconds >= 0 && seconds <= (Long.MAX_VALUE - System.currentTimeMillis()) / 1000)
        }
        // Additive migration: all existing values, history and legacy actualLoad stay intact.
        return state.copy(format = 2)
    }
}

/** Write+fsync+atomic rename: an interrupted write never replaces the previous complete file. */
class StateFile(private val file: File, private val plan: Plan) {
    fun read(): AppState = if (file.exists()) Codec.decode(file.readText(Charsets.UTF_8), plan) else WorkoutEngine(plan).initial()
    fun write(state: AppState) {
        val text = Codec.encode(state)
        require(text.toByteArray(Charsets.UTF_8).size <= Codec.MAX_BYTES) { "Armazenamento excedeu 16 MB. Exporte seus registros." }
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.pending")
        FileOutputStream(temp).use { output -> output.write(text.toByteArray(Charsets.UTF_8)); output.fd.sync() }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
    // Validate all data BEFORE replacing the current state. Import is an explicit full restore, never a merge.
    fun import(text: String): AppState = Codec.decode(text, plan).also(::write)
}
