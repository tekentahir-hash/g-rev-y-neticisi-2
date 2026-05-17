package com.senin.taskmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senin.taskmanager.data.Task
import com.senin.taskmanager.data.TaskFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: TaskViewModel = viewModel()
) {
    val recurring by viewModel.recurringTasks.collectAsState(initial = emptyList())

    val weekly     = recurring.filter { it.frequency == TaskFrequency.WEEKLY }
    val every3     = recurring.filter { it.frequency == TaskFrequency.EVERY_3_DAYS }
    val every2     = recurring.filter { it.frequency == TaskFrequency.EVERY_2_DAYS }
    val daily      = recurring.filter { it.frequency == TaskFrequency.DAILY }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Tekrarlı Görevler") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (weekly.isNotEmpty()) {
                item { GroupHeader("📅 Haftalık") }
                items(weekly) { RecurringTaskItem(it, viewModel) }
            }
            if (every3.isNotEmpty()) {
                item { GroupHeader("🔄 3 Günde Bir") }
                items(every3) { RecurringTaskItem(it, viewModel) }
            }
            if (every2.isNotEmpty()) {
                item { GroupHeader("🔄 2 Günde Bir") }
                items(every2) { RecurringTaskItem(it, viewModel) }
            }
            if (daily.isNotEmpty()) {
                item { GroupHeader("☀️ Günlük") }
                items(daily) { RecurringTaskItem(it, viewModel) }
            }
            if (recurring.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Henüz tekrarlı görev yok.\nAna ekrandan görev ekleyebilirsin.", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun RecurringTaskItem(task: Task, viewModel: TaskViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.dueTime != null) {
                        Text("🕐 ${task.dueTime}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    if (task.frequency == TaskFrequency.WEEKLY && task.dayOfWeek != null) {
                        Text("Her ${getDayName(task.dayOfWeek)}", style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray)
                    }
                }
            }
            IconButton(onClick = { viewModel.deleteTask(task) }) {
                Icon(Icons.Default.Delete, "Sil", tint = Color.Red)
            }
        }
    }
}
