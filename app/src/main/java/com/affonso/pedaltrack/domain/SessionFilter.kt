package com.affonso.pedaltrack.domain

object SessionFilter {
    fun loggable(
        healthConnectSessions: List<HealthConnectSession>,
        loggedIds: Set<String>
    ): List<HealthConnectSession> =
        healthConnectSessions
            .filterNot { it.healthConnectSessionId in loggedIds }
            .sortedByDescending { it.startTime }
}
