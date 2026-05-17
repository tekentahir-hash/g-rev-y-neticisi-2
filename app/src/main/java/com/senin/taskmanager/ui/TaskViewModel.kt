package com.senin.taskmanager.ui

import android.app.Application
import androidx.lifecycle.*
import com.senin.taskmanager.data.*
import com.senin.taskmanager.notification.scheduleNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TaskDatabase.getDatabase(application).taskDao()
    private val app = application

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    fun setSelectedDate(date: LocalDate) { _selectedDate.value = date }

    fun getTasksForDate(date: LocalDate) =
        dao.getTasksForDate(date.toString())

    val allTasks = dao.getAllTasks()
    val recurringTasks = dao.getRecurringTasks()

    fun addTask(
        title: String,
        desc: String,
        freq: TaskFrequency,
        prio: TaskPriority,
        dueDate: LocalDate,
        dueTime: String?
    ) {
        viewModelScope.launch {
            val dayOfWeek = if (freq == TaskFrequency.WEEKLY) dueDate.dayOfWeek.value else null
            val id = dao.insertTask(
                Task(
                    title = title,
                    description = desc,
                    frequency = freq,
                    priority = prio,
                    dueDate = dueDate.toString(),
                    dueTime = dueTime,
                    dayOfWeek = dayOfWeek
                )
            )
            // Bildirim planla
            if (dueTime != null) {
                scheduleNotification(app, id.toInt(), title, dueDate.toString(), dueTime)
            }
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            if (!task.isCompleted) {
                // Tamamlandı olarak işaretle
                dao.updateTask(task.copy(isCompleted = true))
                // Tekrarlı ise bir sonraki görevi oluştur
                if (task.frequency != TaskFrequency.ONCE) {
                    val nextDate = nextDueDate(task)
                    val newTask = task.copy(
                        id = 0,
                        dueDate = nextDate.toString(),
                        isCompleted = false
                    )
                    val newId = dao.insertTask(newTask)
                    if (task.dueTime != null) {
                        scheduleNotification(app, newId.toInt(), task.title, nextDate.toString(), task.dueTime)
                    }
                }
            } else {
                // Geri al
                dao.updateTask(task.copy(isCompleted = false))
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.softDelete(task.id) }
    }

    fun rolloverOverdue() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val overdue = dao.getOverdueTasks(today.toString())
            overdue.forEach { task ->
                val next = nextDueDate(task, today)
                dao.updateTask(task.copy(dueDate = next.toString()))
            }
        }
    }

    private fun nextDueDate(task: Task, from: LocalDate = LocalDate.now()): LocalDate {
        return when (task.frequency) {
            TaskFrequency.DAILY        -> from.plusDays(1)
            TaskFrequency.EVERY_2_DAYS -> from.plusDays(2)
            TaskFrequency.EVERY_3_DAYS -> from.plusDays(3)
            TaskFrequency.WEEKLY -> {
                val dow = task.dayOfWeek ?: from.dayOfWeek.value
                var next = from.plusDays(1)
                while (next.dayOfWeek.value != dow) next = next.plusDays(1)
                next
            }
            TaskFrequency.ONCE -> from
        }
    }
}
