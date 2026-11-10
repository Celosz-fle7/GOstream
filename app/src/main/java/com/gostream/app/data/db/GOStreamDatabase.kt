package com.gostream.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.gostream.app.data.model.VideoItem

data class HistoryItem(
    val subjectId: String,
    val title: String,
    val posterUrl: String,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

class GOStreamDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS watch_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject_id TEXT UNIQUE,
                title TEXT,
                poster_url TEXT,
                episode_number INTEGER,
                position_ms INTEGER,
                duration_ms INTEGER,
                updated_at INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject_id TEXT UNIQUE,
                title TEXT,
                poster_url TEXT,
                created_at INTEGER
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS watch_history")
        db.execSQL("DROP TABLE IF EXISTS favorites")
        onCreate(db)
    }

    fun saveProgress(subjectId: String, title: String, posterUrl: String, episodeNumber: Int, positionMs: Long, durationMs: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("subject_id", subjectId)
            put("title", title)
            put("poster_url", posterUrl)
            put("episode_number", episodeNumber)
            put("position_ms", positionMs)
            put("duration_ms", durationMs)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("watch_history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getHistoryList(): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val cursor = db.query("watch_history", null, null, null, null, null, "updated_at DESC", "30")

        cursor.use {
            while (it.moveToNext()) {
                val subjectId = it.getString(it.getColumnIndexOrThrow("subject_id"))
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val posterUrl = it.getString(it.getColumnIndexOrThrow("poster_url"))
                val epNum = it.getInt(it.getColumnIndexOrThrow("episode_number"))
                val posMs = it.getLong(it.getColumnIndexOrThrow("position_ms"))
                val durMs = it.getLong(it.getColumnIndexOrThrow("duration_ms"))
                val updated = it.getLong(it.getColumnIndexOrThrow("updated_at"))

                list.add(HistoryItem(subjectId, title, posterUrl, epNum, posMs, durMs, updated))
            }
        }
        return list
    }

    companion object {
        private const val DATABASE_NAME = "gostream_local.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: GOStreamDatabase? = null

        fun getInstance(context: Context): GOStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GOStreamDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
