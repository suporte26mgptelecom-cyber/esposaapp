package br.com.wellness.domain

import java.util.Locale

fun String.workoutTitle(): String = uppercase(Locale.forLanguageTag("pt-BR"))
enum class SessionStatus(val label: String) {
    PENDING("○ Pendente"), IN_PROGRESS("▶ Em andamento"), COMPLETED("✓ Concluído")
}
data class PersonalRank(val threshold: Int, val emoji: String, val title: String) {
    val displayName get() = "$emoji $title"
}
data class RankProgress(val completed: Int, val current: PersonalRank?, val next: PersonalRank?) {
    val inStep: Int get() = if (next == null) 3 else completed - (current?.threshold ?: 0)
    val remaining: Int get() = next?.let { it.threshold - completed } ?: 0
    val fraction: Float get() = inStep / 3f
    val description: String get() = next?.let {
        "$inStep de 3 treinos — ${if (remaining == 1) "falta 1" else "faltam $remaining"} para desbloquear ${it.title}"
    } ?: "Todos os ranks conquistados"
}
object PersonalRanking {
    val ranks = listOf(
        PersonalRank(3, "🫣", "Só Vim Conhecer"),
        PersonalRank(6, "😂", "Voltei por Milagre"),
        PersonalRank(9, "💅", "Diva do Aquecimento"),
        PersonalRank(12, "☕", "Movida a Café e Ódio"),
        PersonalRank(15, "🔥", "A Inimiga do Descanso"),
        PersonalRank(18, "🏋️‍♀️", "Fiscal de Halter"),
        PersonalRank(21, "👑", "Dona da Academia"),
        PersonalRank(24, "🏆", "Lenda da Maromba")
    )
    fun progress(count: Int): RankProgress {
        val completed = count.coerceIn(0,24)
        return RankProgress(completed, ranks.lastOrNull { completed >= it.threshold }, ranks.firstOrNull { completed < it.threshold })
    }
    fun unannounced(state: AppState): PersonalRank? = progress(state.history.map { it.sessionId }.toSet().size).current
        ?.takeIf { it.threshold > state.celebratedRank }
    fun acknowledge(state: AppState): AppState = state.copy(celebratedRank =
        maxOf(state.celebratedRank, unannounced(state)?.threshold ?: 0))
}

/** Last series replaces its own rest with the transition, never both. The final exercise has no transition. */
enum class RestAction { SERIES, NEXT_EXERCISE, FINISH }
object WorkoutActions {
    fun restAction(session: Session, ex: Exercise, set: SetSpec): RestAction = when {
        set.key != ex.sets.last().key -> RestAction.SERIES
        ex.id != session.exercises.last().id -> RestAction.NEXT_EXERCISE
        else -> RestAction.FINISH
    }
}
