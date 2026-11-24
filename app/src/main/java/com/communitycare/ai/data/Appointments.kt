package com.communitycare.ai.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

fun appointmentInstant(value: String, zone: ZoneId): Instant {
    val local = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
        .withResolverStyle(ResolverStyle.STRICT))
    require(zone.rules.getValidOffsets(local).isNotEmpty()) { "Time does not exist in this timezone" }
    return local.atZone(zone).toInstant()
}
