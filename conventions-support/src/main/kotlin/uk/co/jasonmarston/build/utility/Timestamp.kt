package uk.co.jasonmarston.build.utility

import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val TS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

/**
 * Returns the current timestamp in UTC formatted as yyyyMMddHHmmss.
 *
 * @param clock the [Clock] to use for the current time (defaults to [Clock.systemUTC())
 * @return the formatted timestamp string
 */
@Suppress("unused")
fun timestamp(clock: Clock = Clock.systemUTC()): String = LocalDateTime.now(clock).format(TS_FORMAT)