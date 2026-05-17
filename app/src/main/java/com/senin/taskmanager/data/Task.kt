package com.senin.taskmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskFrequency {
    ONCE,          // Tek seferlik
    DAILY,         // Her gün
    EVERY_2_DAYS,  // 2 günde bir
    EVERY_3_DAYS,  // 3 günde bir
    WEEKLY         // Haftada bir (aynı gün)
}

enum class TaskPriority { IMPORTANT, OTHER }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val frequency: TaskFrequency,
    val priority: TaskPriority,
    val dueDate: String = java.time.LocalDate.now().toString(),   // yyyy-MM-dd
    val dueTime: String? = null,                                   // HH:mm (opsiyonel)
    val dayOfWeek: Int? = null,                                    // haftalık için: 1=Pzt..7=Paz
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false
)
