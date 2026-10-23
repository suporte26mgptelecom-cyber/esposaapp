package br.com.wellness

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import br.com.wellness.domain.Exercise
import br.com.wellness.domain.ExerciseLibrary

object VideoSearch {
    private val packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    fun open(context: Context, exercise: Exercise) {
        val uri = Uri.parse(ExerciseLibrary.videoUrl(exercise))
        fun launch(intent: Intent): Boolean = try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }
        for (name in packages) {
            if (launch(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(name))) return
        }
        // Resolve generic HTTPS browsers, then send the identical query URL explicitly to one.
        // This avoids a default TikTok handler swallowing the browser fallback.
        val browsers = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org")).addCategory(Intent.CATEGORY_BROWSABLE), 0)
            .map { it.activityInfo.packageName }.distinct().filter { it !in packages }
        for (browser in browsers) {
            if (launch(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(browser))) return
        }
        Toast.makeText(context, "Não foi possível abrir a busca. Instale ou habilite um navegador.", Toast.LENGTH_LONG).show()
    }
}
