package manoellribeiro.dev.martp.core.extensions

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class CalendarExtensionsTest {

    private lateinit var originalDefaultLocale: Locale

    @BeforeEach
    fun setup() {
        // formatDateHour reads Locale.getDefault(), so the default locale is pinned to keep
        // the digits and the calendar system the same on every machine
        originalDefaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalDefaultLocale)
    }

    // formatDateHour formats in the default time zone, so every calendar is created in the
    // default time zone (GregorianCalendar does it) to get the same text in any machine
    private fun createFakeCalendar(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int
    ): Calendar = GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute)

    @Test
    fun `if calendar has an afternoon date should return it as day month year and 24 hour time`() {
        // Given
        val fakeCalendar = createFakeCalendar(2026, Calendar.JANUARY, 15, 14, 5)

        // When
        val result = fakeCalendar.formatDateHour()

        // Then
        assertEquals("15/01/2026 14:05", result)
    }

    @Test
    fun `if calendar has single digit day month hour and minute should pad them with zeros`() {
        // Given
        val fakeCalendar = createFakeCalendar(2025, Calendar.MARCH, 5, 9, 7)

        // When
        val result = fakeCalendar.formatDateHour()

        // Then
        assertEquals("05/03/2025 09:07", result)
    }

    @Test
    fun `if calendar is at midnight should return the hour as 00`() {
        // Given
        val fakeCalendar = createFakeCalendar(2024, Calendar.JUNE, 10, 0, 0)

        // When
        val result = fakeCalendar.formatDateHour()

        // Then
        assertEquals("10/06/2024 00:00", result)
    }

    @Test
    fun `if calendar is at the last minute of the year should return the date without rolling over`() {
        // Given
        val fakeCalendar = createFakeCalendar(2023, Calendar.DECEMBER, 31, 23, 59)

        // When
        val result = fakeCalendar.formatDateHour()

        // Then
        assertEquals("31/12/2023 23:59", result)
    }

    @Test
    fun `if calendar has seconds and milliseconds should leave them out of the text`() {
        // Given
        val fakeCalendar = createFakeCalendar(2022, Calendar.AUGUST, 21, 18, 42)
        fakeCalendar.set(Calendar.SECOND, 59)
        fakeCalendar.set(Calendar.MILLISECOND, 999)

        // When
        val result = fakeCalendar.formatDateHour()

        // Then
        assertEquals("21/08/2022 18:42", result)
    }

    @Test
    fun `if calendar is on the leap day should return the 29th of February`() {
        // Given
        val fakeCalendar = createFakeCalendar(2024, Calendar.FEBRUARY, 29, 12, 30)

        // When
        val result = fakeCalendar.formatDateHour()

        // Then
        assertEquals("29/02/2024 12:30", result)
    }
}
