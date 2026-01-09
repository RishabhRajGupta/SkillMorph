
package com.example.skillmorph.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.skillmorph.data.local.entities.ChatEntity
import com.example.skillmorph.data.local.entities.GoalEntity
import com.example.skillmorph.data.local.entities.TaskEntity
import com.example.skillmorph.data.local.entities.KnowledgeChunkEntity
import androidx.room.TypeConverters

/**
 * The main database class for the app.
 */
@Database(
    entities = [GoalEntity::class, TaskEntity::class, ChatEntity::class, KnowledgeChunkEntity::class],
    version =5 ,
    exportSchema = false // We can set this to true in production if we need to export schemas.
)

@TypeConverters(Converters::class)
abstract class SkillMorphDatabase : RoomDatabase() {

    abstract fun skillMorphDao(): SkillMorphDao

    companion object {
        const val DATABASE_NAME = "skill_morph_db"
    }
}
