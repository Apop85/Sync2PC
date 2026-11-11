package com.sync2pc.app

import com.sync2pc.app.sync.SkipFollowUp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Begrenzung des Folgelaufs nach übersprungenen Dateien (SYNC-20261111-173) */
class SkipFollowUpTest {
    private val hour = SkipFollowUp.MIN_INTERVAL_MS

    @Test fun nothingDeferredPlansNothing() = assertFalse(SkipFollowUp.mayPlan(0, 10 * hour, 0))

    @Test fun firstTimePlans() = assertTrue(SkipFollowUp.mayPlan(2, 10 * hour, 0))

    @Test fun atMostOnePerHour() {
        val last = 10 * hour
        assertFalse(SkipFollowUp.mayPlan(1, last + 5 * 60_000, last))
        assertFalse(SkipFollowUp.mayPlan(1, last + hour - 1, last))
        assertTrue(SkipFollowUp.mayPlan(1, last + hour, last))
    }

    @Test fun clockMovedBackwardsDoesNotBlockForever() = assertTrue(SkipFollowUp.mayPlan(1, 5 * hour, 10 * hour))
}
