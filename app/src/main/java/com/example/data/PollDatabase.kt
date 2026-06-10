package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        FollowEntity::class,
        PollEntity::class,
        PollOptionEntity::class,
        PollVoteEntity::class,
        PollLikeEntity::class,
        PollBookmarkEntity::class,
        PollReactionEntity::class,
        CommentEntity::class,
        CommentLikeEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PollDatabase : RoomDatabase() {
    abstract fun pollDao(): PollDao

    companion object {
        @Volatile
        private var INSTANCE: PollDatabase? = null

        fun getDatabase(context: Context): PollDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PollDatabase::class.java,
                    "linkpoll_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
