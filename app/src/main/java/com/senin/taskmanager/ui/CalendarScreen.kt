package com.senin.taskmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.senin.taskmanager.data.Task
import com.senin.taskmanager.data.TaskPriority
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddTask: (LocalDate) -> Unit,
    onSettings: () -> Unit,
    viewModel: TaskViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val tasksForSelected by viewModel.getTasksForDate(selectedDate).collectAsState(initial = emptyList())

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    LaunchedEffect(Unit) { viewModel.rolloverOverdue() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 Görev Takvimi") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Ayarlar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddTask(selectedDate) }) {
                Icon(Icons.Default.Add, "Görev Ekle")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                MonthCalendar(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    allTasks = allTasks,
                    onDateSelected = { viewModel.setSelectedDate(it) },
                    onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
                )
            }

            item {
                val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))
                Text(
                    text = selectedDate.format(fmt) + " Görevleri",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val important = tasksForSelected.filter { it.priority == TaskPriority.IMPORTANT }
            val other = tasksForSelected.filter { it.priority == TaskPriority.OTHER }

            if (important.isNotEmpty()) {
                item { SectionLabel("⭐ Önemli") }
                items(important) { TaskRow(task = it, viewModel = viewModel) }
            }
            if (other.isNotEmpty()) {
                item { SectionLabel("📌 Diğer") }
                items(other) { TaskRow(task = it, viewModel = viewModel) }
            }
            if (tasksForSelected.isEmpty()) {
                item {
                    Text(
                        "Bu gün için görev yok",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MonthCalendar(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    allTasks: List<Task>,
    onDateSelected: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val taskDates = allTasks.map { it.dueDate }.toSet()
    val today = LocalDate.now()

    Column(modifier = Modifier.padding(8.dp)) {
        // Ay başlığı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ArrowBack, "Önceki") }
            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("tr"))
                    .replaceFirstChar { it.uppercase() } + " " + currentMonth.year,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) { Icon(Icons.Default.ArrowForward, "Sonraki") }
        }

        // Gün başlıkları
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Günler
        val firstDay = currentMonth.atDay(1)
        val startOffset = (firstDay.dayOfWeek.value - 1) // Pazartesi = 0
        val daysInMonth = currentMonth.lengthOfMonth()
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startOffset + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val date = currentMonth.atDay(dayNum)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val hasTasks = taskDates.contains(date.toString())

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasTasks) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
fun TaskRow(task: Task, viewModel: TaskViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { viewModel.toggleComplete(task) }
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.dueTime != null) {
                        Text(
                            "🕐 ${task.dueTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        freqLabel(task.frequency.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = { viewModel.deleteTask(task) }) {
                Icon(Icons.Default.Delete, "Sil", tint = Color.Red)
            }
        }
    }
}

fun freqLabel(name: String) = when (name) {
    "ONCE"         -> "Tek seferlik"
    "DAILY"        -> "Her gün"
    "EVERY_2_DAYS" -> "2 günde bir"
    "EVERY_3_DAYS" -> "3 günde bir"
    "WEEKLY"       -> "Haftada bir"
    else           -> ""
}
