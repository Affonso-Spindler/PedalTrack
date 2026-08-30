package com.affonso.pedaltrack.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SessionFilterTest {

    private fun session(id: String, start: Instant) = HealthConnectSession(
        healthConnectSessionId = id,
        startTime = start,
        endTime = start.plusSeconds(1800),
        durationMin = 30,
        calories = 250.0,
        avgHeartRate = 130
    )

    @Test
    fun `excludes sessions already logged`() {
        val sessions = listOf(
            session("hc-1", Instant.parse("2026-08-01T10:00:00Z")),
            session("hc-2", Instant.parse("2026-08-02T10:00:00Z"))
        )

        val result = SessionFilter.loggable(sessions, loggedIds = setOf("hc-1"))

        assertEquals(listOf("hc-2"), result.map { it.healthConnectSessionId })
    }

    @Test
    fun `sorts remaining sessions from most recent to oldest`() {
        val sessions = listOf(
            session("hc-1", Instant.parse("2026-08-01T10:00:00Z")),
            session("hc-2", Instant.parse("2026-08-03T10:00:00Z")),
            session("hc-3", Instant.parse("2026-08-02T10:00:00Z"))
        )

        val result = SessionFilter.loggable(sessions, loggedIds = emptySet())

        assertEquals(listOf("hc-2", "hc-3", "hc-1"), result.map { it.healthConnectSessionId })
    }
}
