package uk.co.jasonmarston.build.utility

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampTest {
    @Test
    fun `formats timestamp using expected UTC pattern`() {
        val fixedClock = Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC)

        assertEquals("20260102030405", timestamp(fixedClock))
    }
}

