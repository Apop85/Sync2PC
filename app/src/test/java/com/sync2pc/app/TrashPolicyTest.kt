package com.sync2pc.app

import com.sync2pc.app.mirror.TrashPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TrashPolicyTest {
    @Test fun olderFoldersExpire() {
        assertTrue(TrashPolicy.isExpired("2026-09-30", "2026-10-02"))
        assertFalse(TrashPolicy.isExpired("2026-10-02", "2026-10-02")) // Stichtag selbst bleibt
        assertFalse(TrashPolicy.isExpired("2026-10-15", "2026-10-02"))
    }

    @Test fun otherNamesNeverExpire() {
        assertFalse(TrashPolicy.isExpired("Dokumente", "2026-10-02"))
        assertFalse(TrashPolicy.isExpired("2026-1-5", "2026-10-02"))
        assertFalse(TrashPolicy.isExpired("", "2026-10-02"))
    }

    @Test fun cutoffIsDaysBack() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 1, 12, 0, 0) }.timeInMillis
        assertEquals("2026-10-02", TrashPolicy.cutoff(30, now))
    }
}
