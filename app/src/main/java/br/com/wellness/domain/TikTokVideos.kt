package br.com.wellness.domain

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable data class TikTokVideo(val exerciseId: String, val originalUrl: String, val resolvedUrl: String? = null) {
    val openingUrls: List<String> get() = listOfNotNull(resolvedUrl, originalUrl).distinct()
    val browserUrl: String get() = resolvedUrl ?: originalUrl
}
class TikTokVideos(val entries: List<TikTokVideo>) {
    init {
        require(entries.map { it.exerciseId }.toSet().size == entries.size)
        entries.forEach { video ->
            val original = URI(video.originalUrl)
            require(original.scheme == "https" && original.host in setOf("vt.tiktok.com", "vm.tiktok.com", "www.tiktok.com", "tiktok.com"))
            require(if (original.host in setOf("vt.tiktok.com", "vm.tiktok.com"))
                original.rawQuery == null && original.path.orEmpty().matches(Regex("/[a-zA-Z0-9]+/?"))
                else original.path.orEmpty().matches(Regex("/@[^/]+/video/[0-9]+/?")))
            video.resolvedUrl?.let { url ->
                val uri = URI(url)
                require(uri.scheme == "https" && uri.host in setOf("www.tiktok.com", "tiktok.com", "m.tiktok.com"))
                require(uri.path.orEmpty().matches(Regex("/@[^/]+/video/[0-9]+/?")))
            }
        }
    }
    private val byExercise = entries.associateBy { it.exerciseId }
    fun forExercise(ex: Exercise): TikTokVideo? = byExercise[ex.id]
}
