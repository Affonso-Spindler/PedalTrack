package com.affonso.pedaltrack.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CyclingSessionDaoTest {
    private lateinit var db: PedalTrackDatabase
    private lateinit var dao: CyclingSessionDao

    private fun entity(hcId: String, km: Double = 10.0) = CyclingSessionEntity(
        healthConnectSessionId = hcId,
        startTime = Instant.parse("2026-08-01T10:00:00Z"),
        endTime = Instant.parse("2026-08-01T10:30:00Z"),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130,
        km = km,
        carga = "media",
        createdAt = Instant.parse("2026-08-01T10:31:00Z")
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PedalTrackDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.cyclingSessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveSession() = runBlocking {
        dao.insert(entity("hc-1"))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("hc-1", all[0].healthConnectSessionId)
    }

    @Test
    fun duplicateHealthConnectIdThrows() = runBlocking {
        dao.insert(entity("hc-1"))
        var threw = false
        try {
            dao.insert(entity("hc-1", km = 20.0))
        } catch (e: Exception) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun getAllHealthConnectIdsReturnsLoggedIds() = runBlocking {
        dao.insert(entity("hc-1"))
        dao.insert(entity("hc-2"))

        assertEquals(setOf("hc-1", "hc-2"), dao.getAllHealthConnectIds().toSet())
    }

    @Test
    fun deleteByIdRemovesSession() = runBlocking {
        val id = dao.insert(entity("hc-1"))
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }
}
