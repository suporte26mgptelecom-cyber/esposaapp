package br.com.wellness.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.wellness.data.*
import br.com.wellness.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class WellnessViewModel(application: Application) : AndroidViewModel(application) {
    val plan = Codec.plan(application.assets.open("wellness.json").bufferedReader().use { it.readText() })
    val engine = WorkoutEngine(plan)
    private val file = StateFile(File(application.filesDir, "wellness-v1.json"), plan)
    var state by mutableStateOf<AppState?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var fatal by mutableStateOf<String?>(null)
        private set
    var pending by mutableIntStateOf(0)
        private set
    var pendingBackup by mutableStateOf<String?>(null)
        private set
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init {
        viewModelScope.launch {
            try { state = withContext(Dispatchers.IO) { file.read() } }
            catch (e: Exception) { fatal = "Não foi possível ler os registros. O arquivo foi preservado. Importe um backup válido. ${e.message.orEmpty()}" }
            for (job in queue) {
                try { job() }
                catch (e: Exception) { message = "Operação não concluída: ${e.message ?: "erro ao acessar os dados"}" }
                finally { pending-- }
            }
        }
    }
    private fun enqueue(action: suspend () -> Unit) { pending++; queue.trySend(action) }
    fun edit(change: (AppState) -> AppState) = enqueue {
        val next = change(requireNotNull(state))
        withContext(Dispatchers.IO) { file.write(next) }
        state = next
    }
    fun dismissMessage() { message = null }
    fun export(uri: Uri) = enqueue {
        val text = Codec.encode(requireNotNull(state))
        withContext(Dispatchers.IO) {
            requireNotNull(getApplication<Application>().contentResolver.openOutputStream(uri, "wt")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
        message = "Backup exportado. Guarde este arquivo em um lugar seguro."
    }
    fun inspectImport(uri: Uri) = enqueue {
        val text = withContext(Dispatchers.IO) {
            requireNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).use { input ->
                val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    require(output.size() + n <= Codec.MAX_BYTES) { "Backup maior que 16 MB." }
                    output.write(buffer, 0, n)
                }
                output.toString("UTF-8")
            }
        }
        withContext(Dispatchers.IO) { Codec.decode(text, plan) }
        pendingBackup = text
    }
    fun cancelImport() { pendingBackup = null }
    fun confirmImport() {
        val text = pendingBackup ?: return
        pendingBackup = null
        enqueue {
            val restored = withContext(Dispatchers.IO) { file.import(text) }
            state = restored; fatal = null; message = "Backup restaurado."
        }
    }
}
