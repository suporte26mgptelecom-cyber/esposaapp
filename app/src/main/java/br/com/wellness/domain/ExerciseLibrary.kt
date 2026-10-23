package br.com.wellness.domain

import kotlinx.serialization.Serializable
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
    fun executionNotes(ex: Exercise): List<String> = ex.notes.flatMap { note ->
        note.split(Regex("(?<=\\.)\\s+"))
    }.filter { line ->
        !listOf("aquecimento:", "purge", "carga", "quilos", "equipamento ao comparar").any { line.contains(it, true) }
    }.filter { it.isNotBlank() }
    fun calculationNotes(ex: Exercise): List<String> = ex.notes.flatMap { it.split(Regex("(?<=\\.)\\s+")) }
        .filter { line -> listOf("aquecimento:", "purge", "carga", "quilos", "equipamento ao comparar", "PDF não define").any { line.contains(it, true) } }
}
