package com.sync2pc.app

import com.sync2pc.app.data.NumberInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SYNC-20261110-114 P7 / -115 P8: kein stiller Zwischenwert, kein stilles Korrigieren */
class NumberInputTest {
    @Test fun emptyOrNonNumericIsNeverAValue() {
        assertNull(NumberInput.parse("", 0, 3650))
        assertNull(NumberInput.parse(" ", 0, 3650))
        assertNull(NumberInput.parse("abc", 0, 3650))
        assertFalse(NumberInput.isValid("", 15, 1440))
    }

    @Test fun trashDaysBounds() {
        assertEquals(0, NumberInput.parse("0", 0, 3650))
        assertEquals(30, NumberInput.parse("30", 0, 3650))
        assertEquals(3650, NumberInput.parse("3650", 0, 3650))
        assertNull(NumberInput.parse("3651", 0, 3650))
    }

    @Test fun typingIntermediateValuesDoesNotYieldTheWrongNumber() {
        // "30" Zeichen für Zeichen löschen: "3" ist formal gültig (der Benutzer sieht aber "3" im Feld, nie ein leeres Feld mit Wert 3)
        assertEquals(3, NumberInput.parse("3", 0, 3650))
        assertNull(NumberInput.parse("", 0, 3650))
    }

    @Test fun pollMinutesMinimum() {
        assertNull(NumberInput.parse("5", 15, 1440))
        assertNull(NumberInput.parse("14", 15, 1440))
        assertEquals(15, NumberInput.parse("15", 15, 1440))
        assertEquals(1440, NumberInput.parse("1440", 15, 1440))
        assertNull(NumberInput.parse("1441", 15, 1440))
    }

    @Test fun guardPercentBounds() {
        assertNull(NumberInput.parse("0", 1, 100))
        assertEquals(1, NumberInput.parse("1", 1, 100))
        assertEquals(100, NumberInput.parse("100", 1, 100))
        assertNull(NumberInput.parse("150", 1, 100))
        assertNull(NumberInput.parse("2", 5, 100))
    }

    @Test fun filterKeepsOnlyDigitsAndLimitsLength() {
        assertEquals("1234", NumberInput.filter("12a3-4.5", 4))
        assertEquals("", NumberInput.filter("abc", 4))
        assertTrue(NumberInput.filter("12345", 3).length == 3)
    }
}
