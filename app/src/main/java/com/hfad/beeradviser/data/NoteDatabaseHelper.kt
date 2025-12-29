package com.hfad.beeradviser.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Note(
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val photoUri: String? = null,
    var id: Long = -1
)

class NoteDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 3
        const val TABLE_NOTES = "notes"
        const val COLUMN_ID = "_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_PHOTO_URI = "photo_uri"

        private const val SQL_CREATE_NOTES =
            "CREATE TABLE $TABLE_NOTES (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COLUMN_TITLE TEXT NOT NULL," +
                    "$COLUMN_CONTENT TEXT NOT NULL," +
                    "$COLUMN_TIMESTAMP INTEGER NOT NULL," +
                    "$COLUMN_PHOTO_URI TEXT" +
                    ")"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_NOTES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_PHOTO_URI TEXT DEFAULT NULL")
        }
    }

    val allNotes: List<Note>
        get() {
            val notes = mutableListOf<Note>()
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NOTES,
                arrayOf(
                    COLUMN_ID,
                    COLUMN_TITLE,
                    COLUMN_CONTENT,
                    COLUMN_TIMESTAMP,
                    COLUMN_PHOTO_URI
                ),
                null, null, null, null, "$COLUMN_TIMESTAMP DESC", null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                        val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                        val content = it.getString(it.getColumnIndexOrThrow(COLUMN_CONTENT))
                        val timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                        val photoUri = it.getString(it.getColumnIndexOrThrow(COLUMN_PHOTO_URI))
                        notes.add(Note(title, content, timestamp, photoUri, id))
                    } while (it.moveToNext())
                }
            }
            db.close()
            return notes
        }


    fun insertNote(note: Note): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_TIMESTAMP, note.timestamp)
            put(COLUMN_PHOTO_URI, note.photoUri)
        }
        val newRowId = db.insert(TABLE_NOTES, null, values)
        db.close()
        return newRowId
    }

    fun getNoteById(id: Long): Note? {
        val db = this.readableDatabase
        var note: Note? = null

        val cursor = db.query(
            TABLE_NOTES,
            arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT, COLUMN_TIMESTAMP, COLUMN_PHOTO_URI),
            "$COLUMN_ID = ?", // 查詢條件
            arrayOf(id.toString()),
            null, null, null, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val noteId = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                val content = it.getString(it.getColumnIndexOrThrow(COLUMN_CONTENT))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val photoUri = it.getString(it.getColumnIndexOrThrow(COLUMN_PHOTO_URI))

                note = Note(title, content, timestamp, photoUri, noteId)
            }
        }
        db.close()
        return note
    }
}