package br.com.wellness.domain

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

@Serializable data class Plan(
    val sourceSha256: String, val notes: List<String>,
    val equipmentOptions: List<String>, val recordingOptions: List<String>, val sessions: List<Session>
)
@Serializable data class Session(
    val id: String, val week: Int, val code: String, val day: String, val title: String,
    val heading: String, val pdfPage: Int, val completed: Boolean, val exercises: List<Exercise>
) {
    val phase: String get() { val first = ((week - 1) / 2) * 2 + 1; return "$first–${first + 1}" }
}
@Serializable data class Exercise(
    val id: String, val name: String, val equipment: String, val recording: String,
    val initialLoad: String?, val increment: String?, val rest: String?,
    val video: String, val notes: List<String>, val sets: List<SetSpec>
) {
    val originalProfile get() = Profile(id, equipment, recording)
}
@Serializable data class SetSpec(
    val key: String, val label: String, val factor: String, val range: String,
    val initialReps: String?, val cell: String
)
@Serializable data class Profile(
    val exerciseId: String, val equipment: String, val recording: String, val variant: String = ""
) {
    // Length-prefixed components avoid collisions, including user-entered separators.
    val key: String get() = digest(listOf(exerciseId, equipment, recording, variant).joinToString("") { "${it.length}:$it" })
}
fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

@Serializable data class SharedValues(
    val profile: Profile, val load: String? = null, val increment: String? = null,
    val rest: String? = null, val reps: Map<String, String?> = emptyMap()
)
@Serializable data class SetRecord(val done: Boolean = false, val actualLoad: String? = null)
@Serializable data class TimerState(val deadline: Long? = null, val remainingMs: Long? = null, val label: String = "")
@Serializable data class SnapshotSet(
    val spec: SetSpec, val suggestedLoad: String?, val usedLoad: String?,
    val explicitLoad: Boolean, val reps: String?, val done: Boolean
)
@Serializable data class SnapshotExercise(
    val id: String, val name: String, val profile: Profile, val workLoad: String?,
    val increment: String?, val rest: String?, val video: String, val notes: List<String>,
    val sets: List<SnapshotSet>
)
@Serializable data class CompletedWorkout(
    val id: String, val sessionId: String, val title: String, val week: Int,
    val phase: String, val finishedAt: Long, val exercises: List<SnapshotExercise>
)
@Serializable data class AppState(
    val format: Int = 1, val sourceSha256: String,
    val shared: Map<String, SharedValues> = emptyMap(),
    // One assignment for each original exercise/equipment/registration group, across all 24 sessions.
    val profiles: Map<String, Profile> = emptyMap(),
    val records: Map<String, SetRecord> = emptyMap(),
    val history: List<CompletedWorkout> = emptyList(),
    val timer: TimerState = TimerState()
)

object Loads {
    fun parse(value: String?): BigDecimal? = value?.takeIf { it.isNotBlank() }?.replace(',', '.')?.toBigDecimalOrNull()
    fun valid(value: String?): Boolean = value == null || (value.length <= 40 &&
        value.matches(Regex("[0-9]+([.,][0-9]*)?|[.,][0-9]+")) && parse(value)?.signum() != -1)
    fun canonical(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
    fun calculate(workLoad: String?, factor: String, increment: String?): String? {
        val load = parse(workLoad) ?: return null
        require(load.signum() >= 0)
        val exact = load.multiply(BigDecimal(factor))
        val step = parse(increment)
        require(step == null || step.signum() >= 0)
        return canonical(if (step == null || step.signum() == 0) exact
            else exact.divide(step, 0, RoundingMode.DOWN).multiply(step))
    }
}
