@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package br.com.wellness.ui

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.os.Build
import br.com.wellness.RestAlerts
import br.com.wellness.VideoSearch
import br.com.wellness.TikTokVideosOpener
import br.com.wellness.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import br.com.wellness.ui.theme.wellnessOutlinedButtonColors
import br.com.wellness.ui.theme.wellnessButtonColors
import br.com.wellness.ui.theme.wellnessTextButtonColors
import br.com.wellness.ui.theme.wellnessTextFieldColors
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
    fun openSession(id: String) {
        val target = vm.plan.sessions.first { it.id == id }
        vm.edit { vm.engine.startSession(it, target) }
        selected = id
    }
    fun goBack() {
        when {
            snapshotId != null -> snapshotId = null
            selected != null -> selected = null
            page == "historico" -> page = "ranking"
            else -> page = "inicio"
        }
    }
    val childPage = page == "treinos" || page == "historico"
    val navPage = when(page) { "treinos" -> "inicio"; "historico" -> "ranking"; else -> page }
    BackHandler(selected != null || snapshotId != null || page != "inicio") { goBack() }
    Scaffold(
        topBar = {
            // Intrinsic height, including system insets: large fonts never scroll beneath the header.
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(vertical = 8.dp)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 52.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("WELLNESS", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Image(painterResource(R.drawable.ic_wellness), contentDescription = "Logotipo Wellness", modifier = Modifier.size(28.dp))
                    }
                    if (selected != null || snapshotId != null || childPage) IconButton(onClick = { goBack() }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Voltar")
                    }
                }
            }
        },
        bottomBar = { if (selected == null && snapshotId == null) NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
            listOf("inicio" to "Início", "exercicios" to "Exercícios", "ranking" to "Ranking", "ajustes" to "Ajustes").forEach { (id, title) ->
                NavigationBarItem(colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ), selected = navPage == id, onClick = { page = id }, icon = { Text(when(id) { "inicio" -> "◉"; "ranking" -> "♕"; "exercicios" -> "▤"; else -> "⚙" } + if (navPage == id) " ✓" else "") }, label = { Text(title, maxLines = 1) })
            }
        } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            vm.message?.let { msg -> Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(msg, Modifier.weight(1f)); TextButton(colors = wellnessTextButtonColors(), onClick = vm::dismissMessage) { Text("Fechar") }
                }
            } }
            if (state == null) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (vm.fatal == null) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outline) else {
                        Text(vm.fatal!!); Button(colors = wellnessButtonColors(), onClick = { importer.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }) { Text(if (vm.pending > 0) "Salvando…" else "Importar backup") }
                    }
                }
            } else {
                if (vm.pending > 0) Text("Salvando…", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelSmall)
                if (session != null) TimerBar(state, vm)
                Box(Modifier.weight(1f).fillMaxWidth()) { when {
                    snapshot != null -> SnapshotScreen(snapshot)
                    session != null -> WorkoutScreen(session, state, vm,
                        onComplete = { completeDialog = true }, onRestart = { restartDialog = true })
                    page == "treinos" -> SessionsScreen(vm, state, ::openSession)
                    page == "ranking" -> RankingScreen(vm.engine.ranking(state)) { page = "historico" }
                    page == "exercicios" -> ExercisesScreen(vm)
                    page == "historico" -> HistoryScreen(state) { snapshotId = it }
                    page == "ajustes" -> SettingsScreen(vm, state,
                        onExport = { export.launch("wellness-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}.json") },
                        onImport = { importer.launch(arrayOf("application/json", "text/*", "application/octet-stream")) })
                    else -> HomeScreen(vm, state, ::openSession) { page = "treinos" }
                } }
            }
        }
    }
    val celebration = vm.rankCelebration
    LaunchedEffect(celebration) {
        if (celebration != null) { delay(5000); vm.dismissRankCelebration() }
    }
    if (celebration != null) AlertDialog(onDismissRequest = vm::dismissRankCelebration,
        title = { Text("Novo rank conquistado!") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(celebration.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${celebration.threshold} sessões diferentes concluídas. Mais uma conquista sua, Nanda!")
        } },
        confirmButton = { TextButton(colors = wellnessTextButtonColors(), onClick = vm::dismissRankCelebration) { Text("Continuar") } })
    vm.pendingRest?.let { request ->
        AlertDialog(onDismissRequest = vm::cancelRest, title = { Text("Substituir descanso atual?") },
            text = { Text("Já existe um descanso em andamento ou pausado. Iniciar ${request.exerciseName} · ${request.seriesLabel} encerrará o atual.") },
            confirmButton = { TextButton(colors = wellnessTextButtonColors(), onClick = vm::replaceRest) { Text("Substituir e iniciar") } },
            dismissButton = { TextButton(colors = wellnessTextButtonColors(), onClick = vm::cancelRest) { Text("Manter atual") } })
    }
    if (completeDialog && session != null && state != null) {
        val all = session.exercises.sumOf { it.sets.size }
        val done = session.exercises.sumOf { ex -> ex.sets.count { vm.engine.record(state, session, ex, it).done } }
        AlertDialog(onDismissRequest = { completeDialog = false }, title = { Text("Concluir ${session.id}?") },
            text = { Text("$done de $all séries marcadas. Uma cópia das cargas, repetições e séries feitas será guardada no histórico, incluindo campos vazios. Alterações futuras não modificarão essa cópia.", modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(colors = wellnessTextButtonColors(), enabled = vm.pending == 0, onClick = { vm.edit { vm.engine.complete(it, session) }; completeDialog = false; selected = null; page = "treinos" }) { Text(if (vm.pending > 0) "Salvando…" else "Concluir e guardar") } },
            dismissButton = { TextButton(colors = wellnessTextButtonColors(), onClick = { completeDialog = false }) { Text("Continuar treino") } })
    }
    if (restartDialog && session != null) AlertDialog(onDismissRequest = { restartDialog = false },
        title = { Text("Registrar novamente?") }, text = { Text("As marcações e cargas específicas desta sessão serão limpas. Os valores compartilhados e o histórico serão preservados.") },
        confirmButton = { TextButton(colors = wellnessTextButtonColors(), onClick = { vm.edit { vm.engine.restart(it, session) }; restartDialog = false }) { Text("Novo registro") } },
        dismissButton = { TextButton(colors = wellnessTextButtonColors(), onClick = { restartDialog = false }) { Text("Cancelar") } })
    if (vm.pendingBackup != null) AlertDialog(onDismissRequest = vm::cancelImport, title = { Text("Restaurar backup?") },
        text = { Text("O arquivo é válido e pertence a esta planilha. Ele substituirá TODOS os registros atuais, incluindo o histórico e os perfis. Exporte os dados atuais antes, se quiser guardá-los.") },
        confirmButton = { TextButton(colors = wellnessTextButtonColors(), onClick = vm::confirmImport) { Text("Substituir e restaurar") } },
        dismissButton = { TextButton(colors = wellnessTextButtonColors(), onClick = vm::cancelImport) { Text("Cancelar") } })
}

@Composable private fun HomeScreen(vm: WellnessViewModel, state: AppState, open: (String) -> Unit, onAll: () -> Unit) {
    val completed = vm.engine.completedIds(state)
    val next = vm.engine.next(state)
    Column(Modifier.fillMaxSize()) {
    LazyColumn(modifier = Modifier.weight(1f).testTag("home-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Bem-vinda, Nanda", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SEU PROGRESSO", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                Text("${completed.size} de 24 sessões", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outline, progress = { completed.size / 24f }, modifier = Modifier.fillMaxWidth())
                Text("8 semanas · segunda, quarta e sexta", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } }
        item { if (next != null) Card {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PRÓXIMO TREINO", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                Text(next.title.workoutTitle(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Semana ${next.week} · ${next.code} · ${next.day}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(colors = wellnessButtonColors(), onClick = { open(next.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Começar treino") }
            }
        } else Text("24 sessões concluídas! Seu histórico está guardado.", style = MaterialTheme.typography.titleLarge) }
    }
    OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = onAll, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = 56.dp)) { Text("Ver treinos") }
    }
}

@Composable private fun SessionsScreen(vm: WellnessViewModel, state: AppState, open: (String) -> Unit) {
    LazyColumn(modifier = Modifier.testTag("sessions-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Seus treinos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("24 sessões · conclua no seu ritmo. Os dias indicam a sequência do plano; não bloqueiam nem concluem treinos.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        for (week in 1..8) {
            item { Text("Semana $week", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(vm.plan.sessions.filter { it.week == week }, key = { it.id }) { target ->
                val status = vm.engine.sessionStatus(state, target)
                val done = status == SessionStatus.COMPLETED
                val background = if (done) colorResource(R.color.wellness_completed) else MaterialTheme.colorScheme.surface
                val border = if (done) background else if (status == SessionStatus.IN_PROGRESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                OutlinedCard(onClick = { open(target.id) }, modifier = Modifier.fillMaxWidth().testTag("session-${target.id}"),
                    colors = CardDefaults.outlinedCardColors(containerColor = background, contentColor = MaterialTheme.colorScheme.onSurface), border = BorderStroke(1.dp,border)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${target.code} · ${target.day}", style = MaterialTheme.typography.labelLarge, color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(target.title.workoutTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(status.label, fontWeight = FontWeight.SemiBold)
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
                Text("SEMANA ${session.week} · ${session.code} · ${session.day.uppercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                Text(session.title.workoutTitle(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Fase ${session.phase} · $done / $total séries feitas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outline, progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
                Text("Carga de trabalho e reps são compartilhadas com o mesmo exercício e perfil em todas as semanas. As marcas de feita pertencem a esta sessão.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (session.id in vm.engine.completedIds(state)) {
                    Text("Já existe uma conclusão no histórico. Esta tela mostra os valores compartilhados atuais.")
                    OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = onRestart) { Text("Registrar novamente") }
                }
            }
        }
        items(session.exercises, key = { it.id }) { ex -> ExerciseCard(session, ex, state, vm, onComplete) }
        item { Text("Salvamento automático no aparelho. Os vídeos abrem em outro aplicativo e precisam de internet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun VideoButtons(ex: Exercise, vm: WellnessViewModel) {
    val context = LocalContext.current
    val youtube = ExerciseLibrary.youtubeUrl(ex)
    val tikTokVideo = vm.tikTokVideos.forExercise(ex)
    var tikTokAttempt by rememberSaveable(ex.id) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("COMO FAZER", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(colors = wellnessOutlinedButtonColors(), enabled = youtube != null,
            onClick = { VideoSearch.openYouTube(context, ex) }, contentPadding = PaddingValues(12.dp),
            modifier = Modifier.weight(1f).heightIn(min = 80.dp).semantics { contentDescription = if (youtube == null) "YouTube: sem vídeo cadastrado para ${ex.name}" else "Abrir vídeo de ${ex.name} no YouTube" }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_youtube), contentDescription = null, tint = colorResource(R.color.wellness_button), modifier = Modifier.size(32.dp))
                Text("YouTube", style = MaterialTheme.typography.labelSmall, color = if (youtube == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                if (youtube == null) Text("Sem vídeo cadastrado", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
        OutlinedButton(colors = wellnessOutlinedButtonColors(), enabled = tikTokVideo != null, onClick = { tikTokVideo?.let { tikTokAttempt = TikTokVideosOpener.openInTikTok(context, it).name } }, contentPadding = PaddingValues(12.dp),
            modifier = Modifier.weight(1f).heightIn(min = 80.dp).semantics { contentDescription = if (tikTokVideo == null) "TikTok: sem vídeo cadastrado para ${ex.name}" else "Abrir vídeo de ${ex.name} no TikTok" }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_tiktok), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                Text("TikTok", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    }
    if (tikTokAttempt != null && tikTokVideo != null) AlertDialog(onDismissRequest = { tikTokAttempt = null },
        title = { Text("Vídeo no TikTok") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ex.name, fontWeight = FontWeight.SemiBold)
            Text(if (tikTokAttempt == TikTokVideosOpener.Attempt.SENT_UNCONFIRMED.name)
                "Se o vídeo escolhido não apareceu no TikTok, você pode abrir o link no navegador ou copiá-lo."
                else "Não foi possível abrir o vídeo no TikTok. Escolha como deseja abrir ou guardar o link.")
            Text(tikTokVideo.originalUrl, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(colors = wellnessButtonColors(), onClick = {
                if (TikTokVideosOpener.openInBrowser(context, tikTokVideo)) tikTokAttempt = null
                else android.widget.Toast.makeText(context, "Nenhum navegador disponível. Use ‘Copiar link’.", android.widget.Toast.LENGTH_LONG).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("Abrir no navegador") }
            OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = {
                val copied = TikTokVideosOpener.copyLink(context, tikTokVideo)
                android.widget.Toast.makeText(context, if (copied) "Link copiado." else "Não foi possível copiar o link.", android.widget.Toast.LENGTH_LONG).show()
                if (copied) tikTokAttempt = null
            }, modifier = Modifier.fillMaxWidth()) { Text("Copiar link") }
            TextButton(colors = wellnessTextButtonColors(), onClick = { tikTokAttempt = null }, modifier = Modifier.align(Alignment.End)) { Text("Fechar") }
        } })
}

@Composable private fun ExecutionTip(ex: Exercise, vm: WellnessViewModel, catalog: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Dica de execução", fontWeight = FontWeight.SemiBold)
        val notes = if (catalog) vm.plan.sessions.flatMap { it.exercises }.filter { it.id == ex.id }
            .flatMap(ExerciseLibrary::executionNotes).distinct() else ExerciseLibrary.executionNotes(ex)
        notes.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(vm.guides.getValue(ex.id).tip, style = MaterialTheme.typography.bodyMedium)
        Text("Se sentir dor, interrompa o movimento e peça orientação.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun ExercisesScreen(vm: WellnessViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val exercises = remember(query) { ExerciseLibrary.search(vm.plan, query) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Exercícios", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar por nome") }, singleLine = true,
            colors = wellnessTextFieldColors(), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
        LazyColumn(modifier = Modifier.testTag("exercises-list"), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("${exercises.size} exercícios · variações com nomes próprios preservadas", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            if (exercises.isEmpty()) item { Text("Nenhum exercício encontrado.") }
            items(exercises, key = { it.id }) { ex -> Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(ex.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    ExecutionTip(ex, vm, catalog = true)
                    VideoButtons(ex, vm)
                }
            } }
        }
    }
}

@Composable private fun ExerciseCard(session: Session, ex: Exercise, state: AppState, vm: WellnessViewModel, onComplete: () -> Unit) {
    val v = vm.engine.values(state, ex)
    var configuring by remember { mutableStateOf(false) }
    var calculation by rememberSaveable { mutableStateOf(false) }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ex.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = { configuring = true }) { Text("Aparelho e carga") }
            VideoButtons(ex, vm)
            NumericField(v.load, "Carga de trabalho (kg)", Modifier.fillMaxWidth(), "WS-${session.id}-${v.profile.key}") { n -> vm.edit { vm.engine.load(it, ex, n) } }
            ExecutionTip(ex, vm)
            TextButton(colors = wellnessTextButtonColors(), onClick = { calculation = !calculation }) { Text("Sobre o cálculo ${if (calculation) "▴" else "▾"}") }
            if (calculation) {
                Text("${if (v.increment == null || Loads.parse(v.increment)?.signum() == 0) "Cálculo exato" else "Arredonda para baixo a cada ${v.increment.shown()} kg"}. ${v.profile.recording}. Sem divisão por lado ou desconto da barra.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                ExerciseLibrary.calculationNotes(ex).forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
            ex.sets.forEach { set ->
                val record = vm.engine.record(state, session, ex, set)
                val suggested = Loads.calculate(v.load, set.factor, v.increment)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(set.label + if (record.done) " · Feita ✓" else "", fontWeight = FontWeight.SemiBold)
                            Text("Meta: ${set.range} reps · ${Loads.canonical(java.math.BigDecimal(set.factor).multiply(java.math.BigDecimal(100)))}% WS", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Checkbox(checked = record.done, onCheckedChange = { checked -> vm.edit { vm.engine.mark(it, session, ex, set, checked) } },
                            modifier = Modifier.semantics { contentDescription = "Feita ${ex.name} ${set.label}" })
                    }
                    Text("Carga calculada: ${suggested.shown()} kg", style = MaterialTheme.typography.titleMedium)
                    NumericField(v.reps[set.key], "Reps feitas", Modifier.fillMaxWidth(), "reps-${session.id}-${v.profile.key}-${set.key}") { n -> vm.edit { vm.engine.reps(it, ex, set, n) } }
                    when (WorkoutActions.restAction(session, ex, set)) {
                        RestAction.SERIES -> RestControls(ex, set, state, vm)
                        RestAction.NEXT_EXERCISE -> RestControls(ex, null, state, vm)
                        RestAction.FINISH -> Button(colors = wellnessButtonColors(), onClick = onComplete, enabled = vm.pending == 0,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("finish-workout")) { Text(if (vm.pending > 0) "Salvando…" else "Finalizar treino") }
                    }
                }
            }
            Text("Reps vazias não significam zero. Marcar uma série não inicia o descanso.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (configuring) ProfileDialog(ex, state, vm) { configuring = false }
}

@Composable private fun RestControls(ex: Exercise, set: SetSpec?, state: AppState, vm: WellnessViewModel) {
    var adjusting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val seconds = vm.engine.restSeconds(state, ex, set)
    fun start(duration: Long = seconds, save: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33 && !RestAlerts.notificationsAllowed(context)) {
            val prefs = context.getSharedPreferences("wellness-ui", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("notification-asked", false)) {
                prefs.edit().putBoolean("notification-asked", true).apply()
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        vm.requestRest(vm.engine.restRequest(state, ex, set, duration, save))
    }
    val startLabel = "${if (set == null) "Descansar para o próximo exercício" else "Descansar"} · ${RestDurations.display(seconds)}"
    val textStyle = MaterialTheme.typography.labelLarge.copy(lineBreak = LineBreak.Simple, hyphens = Hyphens.None)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    @Composable fun StartButton(modifier: Modifier) {
        Button(colors = wellnessButtonColors(), onClick = { start() }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp), modifier = modifier.heightIn(min = 52.dp)) {
            Text(startLabel, textAlign = TextAlign.Center, style = textStyle)
        }
    }
    @Composable fun AdjustButton(modifier: Modifier) {
        OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = { adjusting = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp), modifier = modifier.heightIn(min = 52.dp)) {
            Text("Ajustar tempo", textAlign = TextAlign.Center, style = textStyle)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("rest-controls-${ex.id}-${set?.key ?: "between"}")) {
        val startWidth = measurer.measure(startLabel, textStyle, softWrap = false).size.width
        val adjustWidth = measurer.measure("Ajustar tempo", textStyle, softWrap = false).size.width
        val rowFits = 2 * maxOf(startWidth, adjustWidth) + with(density) { 64.dp.toPx() } <= with(density) { maxWidth.toPx() }
        if (rowFits) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StartButton(Modifier.weight(1f)); AdjustButton(Modifier.weight(1f))
        } else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StartButton(Modifier.fillMaxWidth()); AdjustButton(Modifier.fillMaxWidth())
        }
    }
    if (adjusting) RestAdjustDialog(seconds, set?.label ?: "Próximo exercício", onDismiss = { adjusting = false }) {
        start(it, true); adjusting = false
    }
}

@Composable private fun NumericField(value: String?, label: String, modifier: Modifier = Modifier, identity: String = label, placeholder: String = "", changed: (String?) -> Unit) {
    var text by rememberSaveable(identity) { mutableStateOf(value?.replace('.', ',').orEmpty()) }
    var focused by remember { mutableStateOf(false) }
    // Do not overwrite an in-flight keystroke with an older queued disk write.
    LaunchedEffect(value) { if (!focused) text = value?.replace('.', ',').orEmpty() }
    val focus = LocalFocusManager.current
    OutlinedTextField(colors = wellnessTextFieldColors(), value = text, onValueChange = { next ->
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
                OutlinedTextField(value = variant, onValueChange = { if (it.length <= 80) variant = it }, colors = wellnessTextFieldColors(), label = { Text("Identificação / variação") },
                    placeholder = { Text("Ex.: máquina azul, smith ou livre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Use nomes diferentes para aparelhos ou variações diferentes. Trocar perfil não copia os quilos do anterior. Escolher o perfil anterior recupera seus valores.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                NumericField(increment, "Incremento (kg)", identity = "increment-${profile.key}") { increment = it }
                Text("Vazio ou zero: cálculo exato. Maior que zero: arredonda para baixo, por série.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text("Para mudar o descanso, use ‘Ajustar tempo’ abaixo da série ou ao final do exercício.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(colors = wellnessTextButtonColors(), onClick = { vm.edit { vm.engine.configure(it, ex, profile, increment, rest) }; close() }) { Text("Salvar perfil") } },
        dismissButton = { TextButton(colors = wellnessTextButtonColors(), onClick = close) { Text("Cancelar") } })
}

@Composable private fun Choice(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $value ▾") }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(
                if (option == value) "$option ✓" else option,
                color = if (option == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable private fun RestAdjustDialog(initial: Long, label: String, onDismiss: () -> Unit, onStart: (Long) -> Unit) {
    var minutes by remember { mutableStateOf((initial / 60).toString()) }
    var seconds by remember { mutableStateOf((initial % 60).toString()) }
    val duration = RestDurations.parse(minutes, seconds)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ajustar tempo") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label)
            Text("A preferência será salva para esta série do mesmo exercício e perfil, em todas as semanas.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = minutes, onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) minutes = it }, label = { Text("Minutos") }, colors = wellnessTextFieldColors(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = seconds, onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) seconds = it }, label = { Text("Segundos") }, colors = wellnessTextFieldColors(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            if (duration == null) Text("Preencha os minutos e os segundos (de 0 a 59).", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, confirmButton = { TextButton(colors = wellnessTextButtonColors(), enabled = duration != null, onClick = { duration?.let(onStart) }) { Text("Salvar e iniciar") } },
        dismissButton = { TextButton(colors = wellnessTextButtonColors(), onClick = onDismiss) { Text("Cancelar") } })
}

@Composable private fun TimerBar(state: AppState, vm: WellnessViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val timer = state.timer
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(timer, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Refresh against the deadline immediately on return from background.
            RestAlerts.reschedule(context, timer)
            while (true) {
                now = System.currentTimeMillis()
                if (timer.deadline != null && now >= timer.deadline) { RestAlerts.deliver(context); break }
                delay(250)
            }
        }
    }
    val left = vm.engine.remaining(timer, now) ?: return
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = Modifier.testTag("rest-banner")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(if (left == 0L) "Descanso concluído ✓" else "Descanso · ${RestDurations.display((left + 999) / 1000)}${if (timer.deadline == null) " · Pausado" else ""}", fontWeight = FontWeight.Bold)
            Text(timer.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Row {
                if (left > 0) TextButton(colors = wellnessTextButtonColors(), onClick = { vm.edit { if (it.timer.deadline != null) vm.engine.pauseTimer(it, System.currentTimeMillis()) else vm.engine.resumeTimer(it, System.currentTimeMillis()) } }) { Text(if (timer.deadline != null) "Pausar" else "Continuar") }
                TextButton(colors = wellnessTextButtonColors(), onClick = { vm.edit { it.copy(timer = TimerState()) } }) { Text("Encerrar") }
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
                    Text("${h.sessionId} · ${h.title.workoutTitle()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(date(h.finishedAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${h.exercises.sumOf { e -> e.sets.count { it.done } }} séries marcadas · Ver registro →", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable private fun SnapshotScreen(h: CompletedWorkout) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Registro concluído", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Text("${h.sessionId} · ${h.title.workoutTitle()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${date(h.finishedAt)} · fase ${h.phase}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Cópia preservada · somente leitura", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        items(h.exercises, key = { it.id }) { ex ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(ex.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Carga de trabalho: ${ex.workLoad.shown()} kg · incremento: ${ex.increment.shown()} kg · descanso: ${ex.rest.shown()} s", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ex.sets.forEach { set ->
                        HorizontalDivider()
                        Text("${if (set.done) "✓" else "○"} ${set.spec.label} · meta ${set.spec.range}", fontWeight = FontWeight.SemiBold)
                        Text("${set.usedLoad.shown()} kg · ${set.reps.shown()} reps${if (set.explicitLoad) " · carga informada anteriormente" else " · carga calculada"}")
                        set.restSeconds?.let { Text("Descanso: ${RestDurations.display(it)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable private fun SettingsScreen(vm: WellnessViewModel, state: AppState, onExport: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Seu cuidado, guardado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("Tudo fica neste aparelho, sem conta. Exporte um backup antes de trocar de celular ou desinstalar o aplicativo.") }
        item { Button(colors = wellnessButtonColors(), onClick = onExport, enabled = vm.pending == 0, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(if (vm.pending > 0) "Salvando…" else "Exportar backup") } }
        item { OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = onImport, enabled = vm.pending == 0, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(if (vm.pending > 0) "Salvando…" else "Importar backup") } }
        item { Text("O backup inclui valores compartilhados, configurações, marcações, sessões em andamento, conquistas, temporizador e ${state.history.size} conclusões. A importação valida o arquivo e pede confirmação antes de substituir os dados.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        item { Text("Sobre seu plano", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { Text("ACADEMIAWELLNESS.xlsx · 25 abas · 24 sessões. As configurações e fórmulas vêm do arquivo. Para mudar aparelho, forma de registrar ou incremento, use ‘Aparelho e carga’. O descanso tem um ajuste próprio por série e entre exercícios.") }
        item { Text("Avisos de descanso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { Text("A contagem usa um horário de término e continua correta com a tela bloqueada. O Android controla som e vibração pelo canal ‘Descanso concluído’, pelo volume e pelo modo Não perturbe. Sem permissão de notificação, o aviso aparece ao voltar ao treino. Sem alarmes precisos, o Android pode atrasar o aviso durante a economia de bateria.") }
        item { OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = { RestAlerts.notificationSettings(context) }) { Text("Configurar notificações, som e vibração") } }
        if (Build.VERSION.SDK_INT >= 31) item { OutlinedButton(colors = wellnessOutlinedButtonColors(), onClick = { RestAlerts.exactSettings(context) }) { Text("Permitir alarmes precisos") } }
        item { Text("YouTube abre o vídeo do plano; TikTok abre o vídeo escolhido para o exercício. Os vídeos precisam de internet. O restante funciona offline. As dicas complementam a planilha; peça orientação presencial para ajustar o aparelho ao seu corpo.") }
        items(vm.plan.notes) { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        item { Text("Fonte SHA-256: ${vm.plan.sourceSha256}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
    }
}
