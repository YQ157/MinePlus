package com.cumtb.mineplus.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

class TimeStatusUtilsTest {

    @Test
    fun `upcoming when far before start`() {
        val status = computeCourseTimeStatus(
            now = LocalTime.of(7, 0),
            start = "08:00",
            end = "09:00",
            startingSoonWithin = Duration.ofMinutes(10)
        )
        assertEquals(CourseTimeStatus.UPCOMING, status)
    }

    @Test
    fun `starting soon when within threshold`() {
        val status = computeCourseTimeStatus(
            now = LocalTime.of(7, 55),
            start = "08:00",
            end = "09:00",
            startingSoonWithin = Duration.ofMinutes(10)
        )
        assertEquals(CourseTimeStatus.STARTING_SOON, status)
    }

    @Test
    fun `in progress when between start and end`() {
        val status = computeCourseTimeStatus(
            now = LocalTime.of(8, 0),
            start = "08:00",
            end = "09:00"
        )
        assertEquals(CourseTimeStatus.IN_PROGRESS, status)
    }

    @Test
    fun `finished when after end`() {
        val status = computeCourseTimeStatus(
            now = LocalTime.of(9, 0),
            start = "08:00",
            end = "09:00"
        )
        assertEquals(CourseTimeStatus.FINISHED, status)
    }

    @Test
    fun `unknown when time parse fails`() {
        val status = computeCourseTimeStatus(
            now = LocalTime.of(9, 0),
            start = "bad",
            end = "09:00"
        )
        assertEquals(CourseTimeStatus.UNKNOWN, status)
    }

    @Test
    fun `starting soon in default is within 30 minutes`() {
        val status = computeCourseTimeStatus(
            now = LocalTime.of(7, 31),
            start = "08:00",
            end = "09:00"
        )
        assertEquals(CourseTimeStatus.STARTING_SOON, status)
    }

    @Test
    fun `list-level starting soon only when none in progress`() {
        val now = LocalTime.of(8, 10)
        val statuses = computeTodayCourseTimeStatuses(
            now = now,
            courses = listOf(
                "08:00" to "09:00", // in progress
                "08:20" to "09:20"  // also in progress based on time range
            )
        )

        // No STARTING_SOON should be present
        assertEquals(false, statuses.any { it == CourseTimeStatus.STARTING_SOON })
        assertEquals(true, statuses.any { it == CourseTimeStatus.IN_PROGRESS })
    }

    @Test
    fun `list-level starting soon picks nearest upcoming within 30 minutes`() {
        val now = LocalTime.of(7, 40)
        val statuses = computeTodayCourseTimeStatuses(
            now = now,
            courses = listOf(
                "08:20" to "09:00", // 40 min away - should NOT starting soon
                "08:05" to "09:00", // 25 min away - should starting soon
                "08:10" to "09:00"  // 30 min away - eligible, but not nearest
            )
        )
        assertEquals(CourseTimeStatus.UPCOMING, statuses[0])
        assertEquals(CourseTimeStatus.STARTING_SOON, statuses[1])
        assertEquals(CourseTimeStatus.UPCOMING, statuses[2])
    }
}
