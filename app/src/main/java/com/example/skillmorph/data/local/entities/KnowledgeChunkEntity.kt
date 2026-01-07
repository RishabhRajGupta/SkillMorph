package com.example.skillmorph.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * The 4th Pillar: Knowledge Shelf.
 * This table uses FTS4 (Full-Text Search 4) which is a special SQLite module.
 * It allows us to search through massive amounts of text instantly.
 */
@Fts4
@Entity(tableName = "knowledge_chunks")
data class KnowledgeChunkEntity(
    // ARCHITECT NOTE: FTS4 tables always use a specific 'rowid' as the primary key.
    @PrimaryKey(autoGenerate = true)
    val rowid: Int = 0,

    val content: String, // The actual text (e.g., 500 words from a PDF)
    val source: String,  // Where did this come from? (e.g., "Python_Manual.pdf")
    val tags: String,    // Keywords for filtering (e.g., "coding, basics")

    // --- API HOOKS (For your coworker) ---
    // ARCHITECT NOTE: Leave these for the API guy.
    // He will use 'lastUpdated' to sync with the cloud.
    val lastUpdated: Long = System.currentTimeMillis()
)