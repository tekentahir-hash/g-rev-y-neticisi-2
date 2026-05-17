package com.senin.taskmanager.ui

import android.app.Application
import androidx.lifecycle.*
import com.senin.taskmanager.data.*
import com.senin.taskmanager.notification.scheduleNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TaskDatabase.getDatabase(application).taskDao()
    private val app = application

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    fun setSelectedDate(date: LocalDate) { _selectedDate.value = date }

    fun getTasksForDate(date: LocalDate) = dao.getTasksForDate(date.toString())

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

            if (freq == TaskFrequency.ONCE) {
                // Tek seferlik: sadece o güne ekle
                val id = dao.insertTask(
                    Task(
                        title = title, description = desc, frequency = freq,
                        priority = prio, dueDate = dueDate.toString(),
                        dueTime = dueTime, dayOfWeek = dayOfWeek
                    )
                )
                if (dueTime != null) scheduleNotification(app, id.toInt(), title, dueDate.toString(), dueTime)
            } else {
                // Tekrarlı: eklendiği andan itibaren 365 gün ileriye tüm tarihlere otomatik ekle
                var current = dueDate
                val endDate = dueDate.plusDays(365)
                while (!current.isAfter(endDate)) {
                    val id = dao.insertTask(
                        Task(
                            title = title, description = desc, frequency = freq,
                            priority = prio, dueDate = current.toString(),
                            dueTime = dueTime, dayOfWeek = dayOfWeek
                        )
                    )
                    if (dueTime != null) scheduleNotification(app, id.toInt(), title, current.toString(), dueTime)
                    current = nextDate(current, freq)
                }
            }
        }
    }

    // Tik atılınca tamamlandı/geri al — görev silinmez, üstü çizilir
    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    // Sil butonuna basılınca gerçekten sil
    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.softDelete(task.id) }
    }

    // Uygulama açılışında: dünden kalan tamamlanmamış görevleri bugüne taşı
    fun rolloverOverdue() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val overdue = dao.getOverdueTasks(today.toString())
            overdue.forEach { task ->
                dao.updateTask(task.copy(dueDate = today.toString()))
            }
        }
    }

    private fun nextDate(from: LocalDate, freq: TaskFrequency): LocalDate = when (freq) {
        TaskFrequency.DAILY        -> from.plusDays(1)
        TaskFrequency.EVERY_2_DAYS -> from.plusDays(2)
        TaskFrequency.EVERY_3_DAYS -> from.plusDays(3)
        TaskFrequency.WEEKLY       -> from.plusDays(7)
        TaskFrequency.ONCE         -> from
    }
}
