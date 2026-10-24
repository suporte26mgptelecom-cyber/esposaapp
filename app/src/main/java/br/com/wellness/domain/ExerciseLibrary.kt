package br.com.wellness.domain

import kotlinx.serialization.Serializable
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

@Serializable data class ExerciseGuide(val id: String, val tip: String, val sources: List<String>)
object ExerciseLibrary {
    fun catalog(plan: Plan): List<Exercise> = plan.sessions.flatMap { it.exercises }.distinctBy { it.id }
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    fun search(plan: Plan, query: String): List<Exercise> = catalog(plan).filter {
        normalize(it.name).contains(normalize(query).trim())
    }
    fun videoQuery(ex: Exercise) = "como fazer ${ex.name}"
    fun videoUrl(ex: Exercise): String = "https://www.tiktok.com/search?q=" +
        URLEncoder.encode(videoQuery(ex), "UTF-8").replace("+", "%20")
    fun youtubeUrl(ex: Exercise): String? = runCatching {
        val value = ex.video.trim(); val uri = URI(value)
        val host = uri.host?.lowercase(Locale.ROOT)
        val videoPath = when (host) {
            "youtu.be" -> uri.path.orEmpty().trim('/').isNotEmpty()
            "www.youtube.com", "youtube.com", "m.youtube.com" ->
                (uri.path == "/watch" && uri.rawQuery.orEmpty().split('&').any { it.startsWith("v=") && it.length > 2 }) ||
                    (uri.path.orEmpty().startsWith("/shorts/") && uri.path.removePrefix("/shorts/").isNotBlank())
            else -> false
        }
        value.takeIf { uri.scheme in listOf("https", "http") && videoPath }
    }.getOrNull()
    fun executionNotes(ex: Exercise): List<String> = ex.notes.flatMap { note ->
        note.split(Regex("(?<=\\.)\\s+"))
    }.filter { line ->
        !listOf("aquecimento:", "purge", "carga", "quilos", "equipamento ao comparar").any { line.contains(it, true) }
    }.filter { it.isNotBlank() }
    fun calculationNotes(ex: Exercise): List<String> = ex.notes.flatMap { it.split(Regex("(?<=\\.)\\s+")) }
        .filter { line -> listOf("aquecimento:", "purge", "carga", "quilos", "equipamento ao comparar", "PDF não define").any { line.contains(it, true) } }
}
