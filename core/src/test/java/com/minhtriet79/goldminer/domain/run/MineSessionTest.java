package com.minhtriet79.goldminer.domain.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MineSessionTest {

    @Test
    void timerEmitsBeepOnlyWithinWarningWindow() {
        MineSession session = new MineSession(555, 7.0f, 3, 10);

        assertFalse(session.tickTimer(1.0f, false, 5)); // 6s
        assertTrue(session.tickTimer(1.0f, false, 5));  // 5s -> beep
        assertFalse(session.tickTimer(0.2f, false, 5)); // still 5s displayed
        assertTrue(session.tickTimer(0.9f, false, 5));  // 4s -> beep
    }

    @Test
    void overtimeWaitsUntilBusyEndsAndEvaluatesPassOrFail() {
        MineSession session = new MineSession(555, 1.0f, 3, 10);
        session.addScore(500);

        // Time hits 0 while busy reeling
        session.tickTimer(1.5f, true, 5);
        assertFalse(session.isFinished());

        // Player finishes reeling a $100 gold nugget during overtime
        session.addScore(100);
        session.tickTimer(0.016f, false, 5);
        assertTrue(session.isFinished());
        assertTrue(session.isPassed());
    }

    @Test
    void dynamiteConsumptionStopsAtZero() {
        MineSession session = new MineSession(555, 60f, 1, 10);
        assertTrue(session.consumeDynamite());
        assertEquals(0, session.getDynamiteCount());
        assertFalse(session.consumeDynamite());
    }
}
