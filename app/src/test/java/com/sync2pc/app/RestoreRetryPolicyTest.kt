package com.sync2pc.app

import com.sync2pc.app.mirror.RestoreRetryPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Höchstens 5 automatische Versuche pro Wiederherstellung (0.11.8, DEC-146) */
class RestoreRetryPolicyTest {

    @Test fun exhaustedFromFifthAttempt() {
        assertFalse(RestoreRetryPolicy.exhausted(0))
        assertFalse(RestoreRetryPolicy.exhausted(4))
        assertTrue(RestoreRetryPolicy.exhausted(5))
        assertTrue(RestoreRetryPolicy.exhausted(6))
    }

    @Test fun bumpCountsPerRestoreId() {
        var set = emptySet<String>()
        for (i in 1..4) {
            val (next, n) = RestoreRetryPolicy.bump(set, 7)
            set = next
            assertEquals(i, n)
            assertFalse(RestoreRetryPolicy.exhausted(RestoreRetryPolicy.count(set, 7)))
        }
        val (next, n) = RestoreRetryPolicy.bump(set, 7)
        assertEquals(5, n)
        assertTrue(RestoreRetryPolicy.exhausted(RestoreRetryPolicy.count(next, 7)))
        // Eine andere Wiederherstellung ist unabhängig; genau ein Eintrag pro Kennung
        assertEquals(0, RestoreRetryPolicy.count(next, 8))
        assertEquals(1, next.count { it.substringBefore(':') == "7" })
    }

    @Test fun idPrefixDoesNotMix() {
        // Kennung 1 darf nicht mit 12 verwechselt werden
        val (set, _) = RestoreRetryPolicy.bump(setOf("12:3"), 1)
        assertEquals(3, RestoreRetryPolicy.count(set, 12))
        assertEquals(1, RestoreRetryPolicy.count(set, 1))
    }
}
