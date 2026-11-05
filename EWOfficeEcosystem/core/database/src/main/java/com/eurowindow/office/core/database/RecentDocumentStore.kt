package com.eurowindow.office.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class RecentDocumentRecord(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val lastOpenedEpochMs: Long
)

/**
 * Lightweight, zero-reflection SQLite store for recent document metadata.
 * Uses an explicit per-application database name (`ew_documents.db` vs `ew_sheets.db`)
 * to guarantee strict process and storage isolation between EW Documents and EW Sheets.
 */
class RecentDocumentStore(
    context: Context,
    val databaseFileName: String
) : SQLiteOpenHelper(context.applicationContext, databaseFileName, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recent_documents (
                uri_string TEXT PRIMARY KEY NOT NULL,
                display_name TEXT NOT NULL,
                mime_type TEXT NOT NULL,
                last_opened_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS recent_documents")
        onCreate(db)
    }

    fun upsertRecent(record: RecentDocumentRecord) {
        writableDatabase.execSQL(
            """
            INSERT OR REPLACE INTO recent_documents (uri_string, display_name, mime_type, last_opened_ms)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(record.uriString, record.displayName, record.mimeType, record.lastOpenedEpochMs)
        )
    }

    fun listRecent(limit: Int = 20): List<RecentDocumentRecord> {
        val result = mutableListOf<RecentDocumentRecord>()
        readableDatabase.rawQuery(
            "SELECT uri_string, display_name, mime_type, last_opened_ms FROM recent_documents ORDER BY last_opened_ms DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    RecentDocumentRecord(
                        uriString = cursor.getString(0),
                        displayName = cursor.getString(1),
                        mimeType = cursor.getString(2),
                        lastOpenedEpochMs = cursor.getLong(3)
                    )
                )
            }
        }
        return result
    }
}
