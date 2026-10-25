package com.wecker.app.alarm

import com.wecker.app.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmSchedulerTest {

    private val zone = ZoneId.systemDefault()
    private fun millisOf(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone).toInstant().toEpochMilli()

    private val now = LocalDateTime.of(2026, 9, 28, 10, 0) // Montag

    @Test
    fun `einmalig heute - Zeit noch kommend`() {
        val alarm = Alarm(id = 1, hour = 8, minute = 0)
        // 8:00 liegt vor 10:00 → morgen
        assertEquals(millisOf(now.plusDays(1).withHour(8)), AlarmScheduler.nextTriggerMillis(alarm, now))
    }

    @Test
    fun `einmalig heute - Zeit vorbei`() {
        val alarm = Alarm(id = 1, hour = 22, minute = 30)
        assertEquals(millisOf(now.withHour(22).withMinute(30)), AlarmScheduler.nextTriggerMillis(alarm, now))
    }

    @Test
    fun `konkretes Datum in der Zukunft`() {
        val alarm = Alarm(id = 1, hour = 8, minute = 0, dateEpochDay = LocalDate.of(2028, 10, 9).toEpochDay())
        assertEquals(
            millisOf(LocalDateTime.of(2028, 10, 9, 8, 0)),
            AlarmScheduler.nextTriggerMillis(alarm, now)
        )
    }

    @Test
    fun `konkretes Datum in der Vergangenheit liefert null`() {
        val alarm = Alarm(id = 1, hour = 8, minute = 0, dateEpochDay = LocalDate.of(2020, 1, 1).toEpochDay())
        assertNull(AlarmScheduler.nextTriggerMillis(alarm, now))
    }

    @Test
    fun `konkretes Datum heute mit vergangener Uhrzeit liefert null`() {
        val alarm = Alarm(id = 1, hour = 8, minute = 0, dateEpochDay = now.toLocalDate().toEpochDay())
        assertNull(AlarmScheduler.nextTriggerMillis(alarm, now))
    }

    @Test
    fun `konkretes Datum heute mit kommender Uhrzeit`() {
        val alarm = Alarm(id = 1, hour = 18, minute = 0, dateEpochDay = now.toLocalDate().toEpochDay())
        assertEquals(millisOf(now.withHour(18)), AlarmScheduler.nextTriggerMillis(alarm, now))
    }

    @Test
    fun `wochentage - nächster passender Tag`() {
        // Bit 0 = Montag … Bit 6 = Sonntag. 28.09.2026 ist Montag.
        val freitag = Alarm(id = 1, hour = 7, minute = 30, daysMask = 0b0010000)
        assertEquals(millisOf(now.plusDays(4).withHour(7).withMinute(30)), AlarmScheduler.nextTriggerMillis(freitag, now))
    }

    @Test
    fun `wochentage - heute falls Zeit noch kommend`() {
        val montag = Alarm(id = 1, hour = 23, minute = 0, daysMask = 0b0000001)
        assertEquals(millisOf(now.withHour(23)), AlarmScheduler.nextTriggerMillis(montag, now))
    }

    @Test
    fun `datum hat vorrang vor wochentagen`() {
        val alarm = Alarm(
            id = 1, hour = 8, minute = 0,
            daysMask = 0b1111111,
            dateEpochDay = LocalDate.of(2027, 3, 15).toEpochDay()
        )
        assertEquals(
            millisOf(LocalDateTime.of(2027, 3, 15, 8, 0)),
            AlarmScheduler.nextTriggerMillis(alarm, now)
        )
    }

    @Test
    fun `skip laesst das naechste Vorkommen ausfallen`() {
        // Montag 10:00, Wecker montags 23:00 → ohne Skip heute, mit Skip erst nächste Woche.
        val alarm = Alarm(
            id = 1, hour = 23, minute = 0,
            daysMask = 0b0000001,
            skipEpochDay = now.toLocalDate().toEpochDay()
        )
        assertEquals(
            millisOf(now.plusWeeks(1).withHour(23).withMinute(0)),
            AlarmScheduler.nextTriggerMillis(alarm, now)
        )
    }

    @Test
    fun `skip bei taeglichem Wecker springt auf den naechsten Tag`() {
        val alarm = Alarm(
            id = 1, hour = 23, minute = 0,
            daysMask = 0b1111111,
            skipEpochDay = now.toLocalDate().toEpochDay()
        )
        assertEquals(
            millisOf(now.plusDays(1).withHour(23)),
            AlarmScheduler.nextTriggerMillis(alarm, now)
        )
    }

    @Test
    fun `skip an einem anderen Tag aendert nichts`() {
        val alarm = Alarm(
            id = 1, hour = 23, minute = 0,
            daysMask = 0b1111111,
            skipEpochDay = now.toLocalDate().plusDays(1).toEpochDay()
        )
        assertEquals(
            millisOf(now.withHour(23)),
            AlarmScheduler.nextTriggerMillis(alarm, now)
        )
    }

    @Test
    fun `skip bei einmaligem Wecker liefert null`() {
        val alarm = Alarm(
            id = 1, hour = 23, minute = 0,
            skipEpochDay = now.toLocalDate().toEpochDay()
        )
        assertNull(AlarmScheduler.nextTriggerMillis(alarm, now))
    }
}
