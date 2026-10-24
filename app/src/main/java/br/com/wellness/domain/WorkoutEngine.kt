package br.com.wellness.domain

import java.util.UUID

class WorkoutEngine(val plan: Plan) {
    private val exercises = plan.sessions.flatMap { it.exercises }
    fun initial(): AppState {
        val shared = exercises.groupBy { it.originalProfile.key }.mapValues { (_, group) ->
            val ex = group.first()
            SharedValues(ex.originalProfile, group.firstNotNullOfOrNull { it.initialLoad }, ex.increment, ex.rest,
                group.flatMap { it.sets }.filter { it.initialReps != null }.associate { it.key to it.initialReps })
        }
        return AppState(sourceSha256 = plan.sourceSha256, shared = shared)
    }
    fun profile(state: AppState, ex: Exercise): Profile = state.profiles[ex.originalProfile.key] ?: ex.originalProfile
    fun values(state: AppState, ex: Exercise): SharedValues {
        val p = profile(state, ex)
        return state.shared[p.key] ?: SharedValues(p)
    }
    fun recordKey(state: AppState, session: Session, ex: Exercise, set: SetSpec): String =
        "${session.id}|${profile(state, ex).key}|${set.key}"
    fun record(state: AppState, session: Session, ex: Exercise, set: SetSpec): SetRecord =
        state.records[recordKey(state, session, ex, set)] ?: SetRecord()
    private fun update(state: AppState, ex: Exercise, change: (SharedValues) -> SharedValues): AppState {
        val before = values(state, ex)
        return state.copy(shared = state.shared + (before.profile.key to change(before)))
    }
    fun load(state: AppState, ex: Exercise, value: String?): AppState {
        require(Loads.valid(value)); return update(state, ex) { it.copy(load = value) }
    }
    fun reps(state: AppState, ex: Exercise, set: SetSpec, value: String?): AppState {
        require(Loads.valid(value)); return update(state, ex) { it.copy(reps = it.reps + (set.key to value)) }
    }
    fun configure(state: AppState, ex: Exercise, profile: Profile, increment: String?, rest: String?): AppState {
        require(profile.exerciseId == ex.id)
        require(profile.equipment in plan.equipmentOptions && profile.recording in plan.recordingOptions)
        require(profile.variant.length <= 80)
        require(Loads.valid(increment) && Loads.valid(rest))
        // No load/reps migration when changing machine, variation or registration unit.
        val existing = state.shared[profile.key] ?: SharedValues(profile)
        return state.copy(profiles = state.profiles + (ex.originalProfile.key to profile),
            shared = state.shared + (profile.key to existing.copy(increment = increment, rest = rest)))
    }
    fun mark(state: AppState, session: Session, ex: Exercise, set: SetSpec, done: Boolean): AppState {
        val key = recordKey(state, session, ex, set)
        return state.copy(records = state.records + (key to record(state, session, ex, set).copy(done = done)))
    }
    fun actualLoad(state: AppState, session: Session, ex: Exercise, set: SetSpec, value: String?): AppState {
        require(Loads.valid(value))
        return state.copy(records = state.records + (recordKey(state, session, ex, set) to
            record(state, session, ex, set).copy(actualLoad = value)))
    }
    fun complete(state: AppState, session: Session, now: Long = System.currentTimeMillis()): AppState {
        val snapshot = CompletedWorkout(UUID.randomUUID().toString(), session.id, session.title, session.week,
            session.phase, now, session.exercises.map { ex ->
                val v = values(state, ex)
                SnapshotExercise(ex.id, ex.name, v.profile, v.load, v.increment, v.rest, ex.video, ex.notes.toList(),
                    ex.sets.map { set ->
                        val record = record(state, session, ex, set)
                        val suggested = Loads.calculate(v.load, set.factor, v.increment)
                        SnapshotSet(set.copy(), suggested, record.actualLoad ?: suggested,
                            record.actualLoad != null, v.reps[set.key], record.done, when (WorkoutActions.restAction(session, ex, set)) {
                                RestAction.SERIES -> restSeconds(state, ex, set)
                                RestAction.NEXT_EXERCISE -> restSeconds(state, ex, null)
                                RestAction.FINISH -> null
                            })
                    }, if (ex.id != session.exercises.last().id) restSeconds(state, ex, null) else null)
            })
        return state.copy(history = state.history + snapshot)
    }
    fun restart(state: AppState, session: Session): AppState = state.copy(
        records = state.records.filterKeys { !it.startsWith("${session.id}|") })
    fun completedIds(state: AppState): Set<String> = state.history.map { it.sessionId }.filter { id -> plan.sessions.any { it.id == id } }.toSet()
    fun startSession(state: AppState, session: Session): AppState = state.copy(startedSessions = state.startedSessions + session.id)
    fun sessionStatus(state: AppState, session: Session): SessionStatus = when {
        session.id in completedIds(state) -> SessionStatus.COMPLETED
        session.id in state.startedSessions || state.records.any { (key, record) ->
            key.startsWith("${session.id}|") && (record.done || record.actualLoad != null)
        } -> SessionStatus.IN_PROGRESS
        else -> SessionStatus.PENDING
    }
    fun ranking(state: AppState): RankProgress = PersonalRanking.progress(completedIds(state).size)
    fun next(state: AppState): Session? = plan.sessions.firstOrNull { it.id !in completedIds(state) }
    fun remaining(timer: TimerState, now: Long): Long? = timer.deadline?.let { (it - now).coerceAtLeast(0) } ?: timer.remainingMs
    fun startTimer(state: AppState, seconds: String, label: String, now: Long): AppState {
        require(Loads.valid(seconds))
        val ms = Loads.parse(seconds)!!.multiply(java.math.BigDecimal(1000)).longValueExact()
        require(ms >= 0 && ms < Long.MAX_VALUE - now)
        return state.copy(timer = TimerState(deadline = now + ms, label = label))
    }
    fun restKey(state: AppState, ex: Exercise, set: SetSpec?): String =
        "${profile(state, ex).key}|${set?.key ?: "between"}"
    fun restSeconds(state: AppState, ex: Exercise, set: SetSpec?): Long =
        state.restPreferences[restKey(state, ex, set)] ?: if (set == null) {
            // The old exercise-level rest button maps to rest between exercises.
            runCatching { Loads.parse(values(state, ex).rest)?.toBigInteger()?.toString()?.toLongOrNull() }
                .getOrNull()?.takeIf { it <= (Long.MAX_VALUE - System.currentTimeMillis()) / 1000 } ?: 180L
        } else RestDurations.defaultSeconds(set.key.substringBefore(':'))
    fun restRequest(state: AppState, ex: Exercise, set: SetSpec?, seconds: Long? = null, save: Boolean = false) =
        RestRequest(restKey(state, ex, set), ex.name, set?.label ?: "Próximo exercício",
            seconds ?: restSeconds(state, ex, set), save)
    fun timerActive(state: AppState, now: Long): Boolean = (remaining(state.timer, now) ?: 0L) > 0
    fun beginRest(state: AppState, request: RestRequest, now: Long, replace: Boolean = false): AppState {
        require(!timerActive(state, now) || replace) { "Já existe um descanso ativo. Confirme a substituição." }
        require(request.seconds >= 0 && request.seconds <= (Long.MAX_VALUE - now) / 1000)
        return state.copy(timer = TimerState(now + request.seconds * 1000, label = "${request.exerciseName} · ${request.seriesLabel}",
            id = UUID.randomUUID().toString(), exerciseName = request.exerciseName, seriesLabel = request.seriesLabel),
            restPreferences = if (request.savePreference) state.restPreferences + (request.preferenceKey to request.seconds) else state.restPreferences)
    }
    fun pauseTimer(state: AppState, now: Long): AppState = state.copy(
        timer = state.timer.copy(deadline = null, remainingMs = remaining(state.timer, now)))
    fun resumeTimer(state: AppState, now: Long): AppState {
        val left = state.timer.remainingMs ?: return state
        return state.copy(timer = state.timer.copy(deadline = now + left, remainingMs = null))
    }
}
