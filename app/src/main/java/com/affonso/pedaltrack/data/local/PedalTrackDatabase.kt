package com.affonso.pedaltrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CyclingSessionEntity::class], version = 1, exportSchema = false)
@TypeConverters(InstantConverter::class)
abstract class PedalTrackDatabase : RoomDatabase() {
    abstract fun cyclingSessionDao(): CyclingSessionDao

    companion object {
        @Volatile private var INSTANCE: PedalTrackDatabase? = null

        fun getInstance(context: Context): PedalTrackDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PedalTrackDatabase::class.java,
                    "pedaltrack.db"
                ).build().also { INSTANCE = it }
            }
    }
}
