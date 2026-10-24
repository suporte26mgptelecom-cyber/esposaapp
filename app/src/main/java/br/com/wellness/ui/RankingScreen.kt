package br.com.wellness.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.wellness.domain.*
import br.com.wellness.ui.theme.wellnessButtonColors

@Composable fun RankingScreen(progress: RankProgress, onHistory: () -> Unit) {
    LazyColumn(modifier = Modifier.testTag("ranking-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Ranking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Card {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SUA CONQUISTA ATUAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(progress.current?.displayName ?: "Em busca do primeiro rank", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${progress.completed} de 24 sessões diferentes concluídas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { progress.fraction }, color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth())
                Text(progress.description)
            }
        } }
        item { Button(colors = wellnessButtonColors(), onClick = onHistory, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Ver histórico") } }
        item { Text("Sua presença é a conquista. Repetir a mesma sessão mantém o histórico, mas não aumenta o ranking.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(PersonalRanking.ranks, key = { it.threshold }) { rank ->
            val unlocked = progress.completed >= rank.threshold
            OutlinedCard(modifier = Modifier.fillMaxWidth().testTag("rank-${rank.threshold}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rank.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${rank.threshold} sessões · ${if (unlocked) "✓ Desbloqueado" else "🔒 Bloqueado"}",
                        color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
