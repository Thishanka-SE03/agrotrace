package com.lnbti.agrotrace.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: Int, // 1-7
    val title: String,
    val summary: String,
    val rawJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY timestamp DESC")
    suspend fun getAll(): List<DocumentEntity>

    @Insert
    suspend fun insert(doc: DocumentEntity): Long

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): DocumentEntity?

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: Int): Int
}

@Database(entities = [DocumentEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agrotrace_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
