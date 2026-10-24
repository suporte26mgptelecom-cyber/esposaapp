package br.com.wellness

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.com.wellness.domain.TikTokVideo

object TikTokVideosOpener {
    val packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    enum class Attempt { SENT_UNCONFIRMED, UNAVAILABLE }

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }

    /** Dispatch to TikTok only. This does not prove which screen the external app displays. */
    fun openInTikTok(context: Context, video: TikTokVideo): Attempt {
        for (url in video.openingUrls) for (name in packages) {
            if (launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addCategory(Intent.CATEGORY_BROWSABLE).setPackage(name))) return Attempt.SENT_UNCONFIRMED
        }
        return Attempt.UNAVAILABLE
    }

    /** Called only from the user's explicit browser action, never from openInTikTok. */
    fun openInBrowser(context: Context, video: TikTokVideo): Boolean {
        val browsers = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org")).addCategory(Intent.CATEGORY_BROWSABLE), 0)
            .map { it.activityInfo.packageName }.distinct().filter { it !in packages && it !in listOf("com.google.android.youtube", "com.google.android.googlequicksearchbox") }
        for (name in browsers) {
            if (launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(video.browserUrl))
                    .addCategory(Intent.CATEGORY_BROWSABLE).setPackage(name))) return true
        }
        return false
    }

    /** Preserve the user's exact original link on the clipboard, only after the Copy link tap. */
    fun copyLink(context: Context, video: TikTokVideo): Boolean = try {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Vídeo do exercício no TikTok", video.originalUrl))
        true
    } catch (_: SecurityException) { false }
}
