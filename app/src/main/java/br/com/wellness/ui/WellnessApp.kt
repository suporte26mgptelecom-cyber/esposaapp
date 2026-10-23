@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package br.com.wellness.ui

import android.content.Intent
import android.media.ToneGenerator
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.wellness.domain.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun String?.shown() = this?.replace('.', ',') ?: "—"
private fun date(time: Long) = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(time))

@Composable fun WellnessApp(vm: WellnessViewModel) {
    val state = vm.state
    var page by rememberSaveable { mutableStateOf("inicio") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var snapshotId by rememberSaveable { mutableStateOf<String?>(null) }
    var completeDialog by remember { mutableStateOf(false) }
    var restartDialog by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::export) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::inspectImport) }
    val session = vm.plan.sessions.firstOrNull { it.id == selected }
    val snapshot = state?.history?.firstOrNull { it.id == snapshotId }
    BackHandler(selected != null || snapshotId != null || page != "inicio") {
        when { snapshotId != null -> snapshotId = null; selected != null -> selected = null; else -> page = "inicio" }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Column { Text("Wellness", fontWeight = FontWeight.Bold)
                Text(if (vm.pending > 0) "Salvando…" else "Seu treino, no seu ritmo", style = MaterialTheme.typography.labelMedium) } },
                navigationIcon = { if (selected != null || snapshotId != null) TextButton(onClick = { selected = null; snapshotId = null }) { Text("Voltar") } })
        },
        bottomBar = { if (selected == null && snapshotId == null) NavigationBar {
            listOf("inicio" to "Início", "historico" to "Histórico", "ajustes" to "Ajustes").forEach { (id, title) ->
                NavigationBarItem(selected = page == id, onClick = { page = id }, icon = { Text(when(id) { "inicio" -> "◉"; "historico" -> "◷"; else -> "⚙" }) }, label = { Text(title) })
            }
        } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            vm.message?.let { msg -> Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(msg, Modifier.weight(1f)); TextButton(onClick = vm::dismissMessage) { Text("Fechar") }
                }
            } }
            if (state == null) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (vm.fatal == null) CircularProgressIndicator() else {
                        Text(vm.fatal!!); Button(onClick = { importer.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }) { Text("Importar backup") }
                    }
                }
            } else {
                TimerBar(state, vm)
                when {
                    snapshot != null -> SnapshotScreen(snapshot)
                    session != null -> WorkoutScreen(session, state, vm,
                        onComplete = { completeDialog = true }, onRestart = { restartDialog = true })
                    page == "historico" -> HistoryScreen(state) { snapshotId = it }
                    page == "ajustes" -> SettingsScreen(vm, state,
                        onExport = { export.launch("wellness-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}.json") },
                        onImport = { importer.launch(arrayOf("application/json", "text/*", "application/octet-stream")) })
                    else -> HomeScreen(vm, state) { selected = it }
                }
            }
        }
    }
    if (completeDialog && session != null && state != null) {
        val all = session.exercises.sumOf { it.sets.size }
        val done = session.exercises.sumOf { ex -> ex.sets.count { vm.engine.record(state, session, ex, it).done } }
        AlertDialog(onDismissRequest = { completeDialog = false }, title = { Text("Concluir ${session.id}?") },
            text = { Text("$done de $all séries marcadas. Uma cópia das cargas, repetições e séries feitas será guardada no histórico, incluindo campos vazios. Alterações futuras não modificarão essa cópia.") },
            confirmButton = { TextButton(enabled = vm.pending == 0, onClick = { vm.edit { vm.engine.complete(it, session) }; completeDialog = false; selected = null; page = "historico" }) { Text("Concluir e guardar") } },
            dismissButton = { TextButton(onClick = { completeDialog = false }) { Text("Continuar treino") } })
    }
    if (restartDialog && session != null) AlertDialog(onDismissRequest = { restartDialog = false },
        title = { Text("Registrar novamente?") }, text = { Text("As marcações e cargas específicas desta sessão serão limpas. Os valores compartilhados e o histórico serão preservados.") },
        confirmButton = { TextButton(onClick = { vm.edit { vm.engine.restart(it, session) }; restartDialog = false }) { Text("Novo registro") } },
        dismissButton = { TextButton(onClick = { restartDialog = false }) { Text("Cancelar") } })
    if (vm.pendingBackup != null) AlertDialog(onDismissRequest = vm::cancelImport, title = { Text("Restaurar backup?") },
        text = { Text("O arquivo é válido e pertence a esta planilha. Ele substituirá TODOS os registros atuais, incluindo o histórico e os perfis. Exporte os dados atuais antes, se quiser guardá-los.") },
        confirmButton = { TextButton(onClick = vm::confirmImport) { Text("Substituir e restaurar") } },
        dismissButton = { TextButton(onClick = vm::cancelImport) { Text("Cancelar") } })
}

@Composable private fun HomeScreen(vm: WellnessViewModel, state: AppState, open: (String) -> Unit) {
    val completed = vm.engine.completedIds(state)
    val next = vm.engine.next(state)
    LazyColumn(modifier = Modifier.testTag("home-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Vamos cuidar de você.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SEU PROGRESSO", style = MaterialTheme.typography.labelLarge)
                Text("${completed.size} de 24 sessões", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { completed.size / 24f }, modifier = Modifier.fillMaxWidth())
                Text("8 semanas · segunda, quarta e sexta")
            }
        } }
        item { if (next != null) Card {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PRÓXIMO TREINO", style = MaterialTheme.typography.labelLarge)
                Text(next.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Semana ${next.week} · ${next.code} · ${next.day}")
                Button(onClick = { open(next.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Começar treino") }
            }
        } else Text("24 sessões concluídas! Seu histórico está guardado.", style = MaterialTheme.typography.titleLarge) }
        for (week in 1..8) {
            item { Text("Semana $week", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(vm.plan.sessions.filter { it.week == week }, key = { it.id }) { s ->
                OutlinedCard(onClick = { open(s.id) }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("${s.code} · ${s.day}", style = MaterialTheme.typography.labelLarge); Text(s.title, style = MaterialTheme.typography.titleMedium) }
                        Text(if (s.id in completed) "Concluído ✓" else "Abrir →", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable private fun WorkoutScreen(session: Session, state: AppState, vm: WellnessViewModel, onComplete: () -> Unit, onRestart: () -> Unit) {
    val done = session.exercises.sumOf { ex -> ex.sets.count { vm.engine.record(state, session, ex, it).done } }
    val total = session.exercises.sumOf { it.sets.size }
    LazyColumn(modifier = Modifier.testTag("workout-list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SEMANA ${session.week} · ${session.code} · ${session.day.uppercase()}", style = MaterialTheme.typography.labelLarge)
                Text(session.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Fase ${session.phase} · $done / $total séries feitas")
                LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
                Text("Carga de trabalho e reps são compartilhadas com o mesmo exercício e perfil em todas as semanas. As marcas de feita pertencem a esta sessão.", style = MaterialTheme.typography.bodySmall)
                if (session.id in vm.engine.completedIds(state)) {
                    Text("Já existe uma conclusão no histórico. Esta tela mostra os valores compartilhados atuais.")
                    OutlinedButton(onClick = onRestart) { Text("Registrar novamente") }
                }
            }
        }
        items(session.exercises, key = { it.id }) { ex -> ExerciseCard(session, ex, state, vm) }
        item { Button(onClick = onComplete, enabled = vm.pending == 0, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Concluir sessão") } }
        item { Text("Salvamento automático no aparelho. Os vídeos abrem em outro aplicativo e precisam de internet.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun ExerciseCard(session: Session, ex: Exercise, state: AppState, vm: WellnessViewModel) {
    val v = vm.engine.values(state, ex)
    var configuring by remember { mutableStateOf(false) }
    var restDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ex.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${v.profile.equipment} · ${v.profile.recording}${v.profile.variant.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { configuring = true }) { Text("Aparelho e carga") }
                TextButton(onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ex.video))) }
                    catch (_: Exception) { android.widget.Toast.makeText(context, "Nenhum aplicativo disponível para abrir o vídeo.", android.widget.Toast.LENGTH_LONG).show() }
                }) { Text("Ver vídeo ↗") }
            }
            NumericField(v.load, "Carga de trabalho (kg)", Modifier.fillMaxWidth(), "WS-${session.id}-${v.profile.key}") { n -> vm.edit { vm.engine.load(it, ex, n) } }
            Text("${if (v.increment == null || Loads.parse(v.increment)?.signum() == 0) "Cálculo exato" else "Arredonda para baixo a cada ${v.increment.shown()} kg"}. ${v.profile.recording}. Sem divisão por lado ou desconto da barra.", style = MaterialTheme.typography.bodySmall)
            ex.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            ex.sets.forEach { set ->
                val record = vm.engine.record(state, session, ex, set)
                val suggested = Loads.calculate(v.load, set.factor, v.increment)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(set.label, fontWeight = FontWeight.SemiBold)
                            Text("Meta: ${set.range} reps · ${Loads.canonical(java.math.BigDecimal(set.factor).multiply(java.math.BigDecimal(100)))}% WS", style = MaterialTheme.typography.bodySmall)
                        }
                        Checkbox(checked = record.done, onCheckedChange = { checked -> vm.edit { vm.engine.mark(it, session, ex, set, checked) } },
                            modifier = Modifier.semantics { contentDescription = "Feita ${ex.name} ${set.label}" })
                    }
                    Text("Carga calculada: ${suggested.shown()} kg", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumericField(record.actualLoad, "Kg usados", Modifier.weight(1f), "kg-${session.id}-${v.profile.key}-${set.key}", placeholder = suggested.shown()) { n -> vm.edit { vm.engine.actualLoad(it, session, ex, set, n) } }
                        NumericField(v.reps[set.key], "Reps feitas", Modifier.weight(1f), "reps-${session.id}-${v.profile.key}-${set.key}") { n -> vm.edit { vm.engine.reps(it, ex, set, n) } }
                    }
                }
            }
            Text("Kg usados vazio: utiliza a carga calculada. Se diferente, informe o que usou só nesta série/sessão. Reps vazias não significam zero.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                if (v.rest == null) restDialog = true
                else vm.edit { vm.engine.startTimer(it, v.rest, ex.name, System.currentTimeMillis()) }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (v.rest == null) "Definir descanso" else "Descansar · ${v.rest.shown()} s") }
        }
    }
    if (configuring) ProfileDialog(ex, state, vm) { configuring = false }
    if (restDialog) RestDialog(onDismiss = { restDialog = false }) { seconds ->
        vm.edit { current ->
            val values = vm.engine.values(current, ex)
            val configured = vm.engine.configure(current, ex, values.profile, values.increment, seconds)
            vm.engine.startTimer(configured, seconds, ex.name, System.currentTimeMillis())
        }; restDialog = false
    }
}

@Composable private fun NumericField(value: String?, label: String, modifier: Modifier = Modifier, identity: String = label, placeholder: String = "", changed: (String?) -> Unit) {
    var text by rememberSaveable(identity) { mutableStateOf(value?.replace('.', ',').orEmpty()) }
    var focused by remember { mutableStateOf(false) }
    // Do not overwrite an in-flight keystroke with an older queued disk write.
    LaunchedEffect(value) { if (!focused) text = value?.replace('.', ',').orEmpty() }
    val focus = LocalFocusManager.current
    OutlinedTextField(value = text, onValueChange = { next ->
        if (next.isEmpty() || Loads.valid(next)) { text = next; changed(next.replace(',', '.').ifEmpty { null }) }
    }, label = { Text(label) }, placeholder = { Text(placeholder) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = modifier.heightIn(min = 60.dp).semantics { contentDescription = identity }
            .onFocusChanged { focused = it.isFocused })
}

@Composable private fun ProfileDialog(ex: Exercise, state: AppState, vm: WellnessViewModel, close: () -> Unit) {
    val current = vm.engine.values(state, ex)
    var equipment by remember { mutableStateOf(current.profile.equipment) }
    var recording by remember { mutableStateOf(current.profile.recording) }
    var variant by remember { mutableStateOf(current.profile.variant) }
    var increment by remember { mutableStateOf(current.increment) }
    var rest by remember { mutableStateOf(current.rest) }
    val profile = Profile(ex.id, equipment, recording, variant.trim())
    LaunchedEffect(profile.key) {
        val existing = state.shared[profile.key]
        increment = existing?.increment; rest = existing?.rest
    }
    AlertDialog(onDismissRequest = close, title = { Text("Perfil do aparelho") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Vale em todas as ocorrências com a mesma configuração original deste exercício. Perfis diferentes guardam cargas e reps separadas.")
                Choice("Equipamento", equipment, vm.plan.equipmentOptions) { equipment = it }
                Choice("Como registrar", recording, vm.plan.recordingOptions) { recording = it }
                OutlinedTextField(variant, { if (it.length <= 80) variant = it }, label = { Text("Identificação / variação") },
                    placeholder = { Text("Ex.: máquina azul, smith ou livre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Use nomes diferentes para aparelhos ou variações diferentes. Trocar perfil não copia os quilos do anterior. Escolher o perfil anterior recupera seus valores.", style = MaterialTheme.typography.bodySmall)
                NumericField(increment, "Incremento (kg)", identity = "increment-${profile.key}") { increment = it }
                Text("Vazio ou zero: cálculo exato. Maior que zero: arredonda para baixo, por série.", style = MaterialTheme.typography.bodySmall)
                NumericField(rest, "Descanso (segundos)", identity = "rest-${profile.key}") { rest = it }
                Text("A planilha não especifica descanso; escolha o seu. Vazio permanece sem duração definida.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { vm.edit { vm.engine.configure(it, ex, profile, increment, rest) }; close() }) { Text("Salvar perfil") } },
        dismissButton = { TextButton(onClick = close) { Text("Cancelar") } })
}

@Composable private fun Choice(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $value ▾") }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable private fun RestDialog(onDismiss: () -> Unit, onStart: (String) -> Unit) {
    var seconds by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Seu descanso") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A planilha não define uma duração. Informe o tempo em segundos.")
            NumericField(seconds, "Segundos") { seconds = it }
        }
    }, confirmButton = { TextButton(enabled = seconds != null, onClick = { seconds?.let(onStart) }) { Text("Iniciar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable private fun TimerBar(state: AppState, vm: WellnessViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val timer = state.timer
    LaunchedEffect(timer.deadline) {
        if (timer.deadline != null) {
            while (true) {
                now = System.currentTimeMillis()
                if (now >= timer.deadline) {
                    val tone = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70) }.getOrNull()
                    try {
                        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
                        delay(300)
                    } finally { tone?.release() }
                    vm.edit { current -> if (current.timer.deadline == timer.deadline)
                        current.copy(timer = TimerState(remainingMs = 0, label = timer.label)) else current }
                    break
                }
                delay(250)
            }
        }
    }
    val left = vm.engine.remaining(timer, now) ?: return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            val seconds = (left + 999) / 1000
            Text(if (left == 0L) "Descanso concluído" else "Descanso · ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}", fontWeight = FontWeight.Bold)
            Text(timer.label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Row {
                if (left > 0) TextButton(onClick = { vm.edit { if (it.timer.deadline != null) vm.engine.pauseTimer(it, System.currentTimeMillis()) else vm.engine.resumeTimer(it, System.currentTimeMillis()) } }) { Text(if (timer.deadline != null) "Pausar" else "Continuar") }
                TextButton(onClick = { vm.edit { it.copy(timer = TimerState()) } }) { Text("Fechar") }
            }
        }
    }
}

@Composable private fun HistoryScreen(state: AppState, open: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Seu histórico", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("Cada conclusão guarda uma cópia das cargas e repetições usadas. Os valores aqui não mudam quando você edita outros treinos.") }
        if (state.history.isEmpty()) item { Text("Seu primeiro treino concluído aparecerá aqui.") }
        items(state.history.asReversed(), key = { it.id }) { h ->
            OutlinedCard(onClick = { open(h.id) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${h.sessionId} · ${h.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(date(h.finishedAt))
                    Text("${h.exercises.sumOf { e -> e.sets.count { it.done } }} séries marcadas · Ver registro →", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable private fun SnapshotScreen(h: CompletedWorkout) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Registro concluído", style = MaterialTheme.typography.labelLarge)
            Text("${h.sessionId} · ${h.title}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${date(h.finishedAt)} · fase ${h.phase}")
            Text("Cópia preservada · somente leitura", style = MaterialTheme.typography.bodySmall)
        }
        items(h.exercises, key = { it.id }) { ex ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(ex.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${ex.profile.equipment} · ${ex.profile.recording} · ${ex.profile.variant}")
                    Text("Carga de trabalho: ${ex.workLoad.shown()} kg · incremento: ${ex.increment.shown()} kg · descanso: ${ex.rest.shown()} s")
                    ex.sets.forEach { set ->
                        HorizontalDivider()
                        Text("${if (set.done) "✓" else "○"} ${set.spec.label} · meta ${set.spec.range}", fontWeight = FontWeight.SemiBold)
                        Text("${set.usedLoad.shown()} kg · ${set.reps.shown()} reps${if (set.explicitLoad) " · carga informada" else " · carga calculada"}")
                    }
                }
            }
        }
    }
}

@Composable private fun SettingsScreen(vm: WellnessViewModel, state: AppState, onExport: () -> Unit, onImport: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Seu cuidado, guardado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("Tudo fica neste aparelho, sem conta. Exporte um backup antes de trocar de celular ou desinstalar o aplicativo.") }
        item { Button(onClick = onExport, enabled = vm.pending == 0, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Exportar backup") } }
        item { OutlinedButton(onClick = onImport, enabled = vm.pending == 0, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Importar backup") } }
        item { Text("O backup inclui valores compartilhados, configurações, marcações, temporizador e ${state.history.size} conclusões. A importação valida o arquivo e pede confirmação antes de substituir os dados.", style = MaterialTheme.typography.bodySmall) }
        item { Text("Sobre seu plano", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { Text("ACADEMIAWELLNESS.xlsx · 25 abas · 24 sessões. As configurações e fórmulas vêm do arquivo. Para mudar aparelho, forma de registrar, incremento ou descanso, use ‘Aparelho e carga’ no exercício.") }
        item { Text("Os vídeos precisam de internet. O restante funciona offline. O descanso usa o horário de término, inclusive ao fechar a tela; o aviso sonoro é dado com o aplicativo aberto. Não há serviço de alarme em segundo plano.") }
        items(vm.plan.notes) { Text(it, style = MaterialTheme.typography.bodyMedium) }
        item { Text("Fonte SHA-256: ${vm.plan.sourceSha256}", style = MaterialTheme.typography.labelSmall) }
    }
}
