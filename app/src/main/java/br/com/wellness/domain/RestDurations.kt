package br.com.wellness.domain

/** Keys use exercise profile plus semantic series type/number, never sheet row position. */
data class RestRequest(
    val preferenceKey: String, val exerciseName: String, val seriesLabel: String,
    val seconds: Long, val savePreference: Boolean = false
)
object RestDurations {
    fun defaultSeconds(type: String): Long = when (type.lowercase().trim()) {
        "aquecimento" -> 40
        "feeder set" -> 90
        "working set", "top set", "back off", "back off set" -> 180
        "rest pause", "rest-pause" -> 20
        else -> error("Tipo de série sem descanso definido: $type")
    }
    fun display(seconds: Long): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    fun parse(minutes: String, seconds: String): Long? = runCatching {
        if (!minutes.matches(Regex("[0-9]+")) || !seconds.matches(Regex("[0-9]+"))) return null
        val m = minutes.toLong(); val s = seconds.toLong()
        if (s !in 0..59 || m < 0) return null
        Math.addExact(Math.multiplyExact(m, 60), s).also {
            if (it > (Long.MAX_VALUE - System.currentTimeMillis()) / 1000) return null
        }
    }.getOrNull()
}
