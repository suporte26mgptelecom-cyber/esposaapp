package br.com.wellness.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.wellness.RestAlerts
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
    val guides = Codec.json.decodeFromString<List<ExerciseGuide>>(application.assets.open("exercise-guides.json").bufferedReader().use { it.readText() }).associateBy { it.id }
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
    var pendingRest by mutableStateOf<RestRequest?>(null)
        private set
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init {
        viewModelScope.launch {
            try { state = withContext(Dispatchers.IO) { file.read() }; syncAlert(requireNotNull(state)) }
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
        syncAlert(next)
    }
    private fun syncAlert(next: AppState) {
        runCatching { RestAlerts.sync(getApplication(), next.timer) }
            .onFailure { message = "Contagem salva; não foi possível agendar o aviso. Confira as permissões de descanso nos Ajustes." }
    }
    fun requestRest(request: RestRequest) = enqueue {
        val current = requireNotNull(state)
        if (engine.timerActive(current, System.currentTimeMillis())) pendingRest = request
        else persistRest(current, request, false)
    }
    private suspend fun persistRest(current: AppState, request: RestRequest, replace: Boolean) {
        val next = engine.beginRest(current, request, System.currentTimeMillis(), replace)
        withContext(Dispatchers.IO) { file.write(next) }
        state = next; syncAlert(next)
    }
    fun cancelRest() { pendingRest = null }
    fun replaceRest() {
        val request = pendingRest ?: return
        pendingRest = null
        enqueue { persistRest(requireNotNull(state), request, true) }
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
            state = restored; syncAlert(restored); fatal = null; message = "Backup restaurado."
        }
    }
}
