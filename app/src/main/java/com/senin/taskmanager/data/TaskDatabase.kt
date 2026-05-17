package com.senin.taskmanager.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Belirli bir günün görevleri
    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isDeleted = 0 ORDER BY priority ASC, dueTime ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    // Aylık görünüm için tüm görevler
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY dueDate ASC, dueTime ASC")
    fun getAllTasks(): Flow<List<Task>>

    // Tekrarlı görev listesi (ayarlar)
    @Query("SELECT * FROM tasks WHERE frequency != 'ONCE' AND isDeleted = 0 ORDER BY frequency ASC")
    fun getRecurringTasks(): Flow<List<Task>>

    // Bildirim için: bugün belirli saatteki görevler
    @Query("SELECT * FROM tasks WHERE dueDate = :date AND dueTime = :time AND isDeleted = 0 AND isCompleted = 0")
    suspend fun getTasksForNotification(date: String, time: String): List<Task>

    // Tamamlanmamış geçmiş görevler (rollover için)
    @Query("SELECT * FROM tasks WHERE dueDate < :today AND isCompleted = 0 AND isDeleted = 0 AND frequency != 'ONCE'")
    suspend fun getOverdueTasks(today: String): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Query("UPDATE tasks SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Int)
}

class Converters {
    @TypeConverter fun fromFreq(v: String) = TaskFrequency.valueOf(v)
    @TypeConverter fun toFreq(f: TaskFrequency) = f.name
    @TypeConverter fun fromPrio(v: String) = TaskPriority.valueOf(v)
    @TypeConverter fun toPrio(p: TaskPriority) = p.name
}

@Database(entities = [Task::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    companion object {
        @Volatile private var INSTANCE: TaskDatabase? = null
        fun getDatabase(context: Context) = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context, TaskDatabase::class.java, "task_db")
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }
}
