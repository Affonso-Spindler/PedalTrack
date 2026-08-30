package com.affonso.pedaltrack.data.local

import androidx.room.TypeConverter
import java.time.Instant

class InstantConverter {
    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()
}
