package com.minhtriet79.goldminer.domain.hook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HookPhysicsTest {

    @Test
    void swingAngleFollowsSinusoidalCurveWithinBounds() {
        float period = 5.556f;
        assertEquals(0f, HookPhysics.computeSwingAngleDeg(0f, 80f, period, 0f), 0.01f);
        assertEquals(80f, HookPhysics.computeSwingAngleDeg(period * 0.25f, 80f, period, 0f), 0.01f);
        assertEquals(0f, HookPhysics.computeSwingAngleDeg(period * 0.5f, 80f, period, 0f), 0.01f);
        assertEquals(-80f, HookPhysics.computeSwingAngleDeg(period * 0.75f, 80f, period, 0f), 0.01f);
    }

    @Test
    void cableBreakTriggersWhenWeightMeetsOrExceedsStrengthWithoutBuffs() {
        assertFalse(HookPhysics.isCableBreak(10, 9, false, false));
        assertTrue(HookPhysics.isCableBreak(10, 10, false, false));
        assertTrue(HookPhysics.isCableBreak(10, 11, false, false));
        assertFalse(HookPhysics.isCableBreak(10, 11, true, false));
        assertFalse(HookPhysics.isCableBreak(10, 12, false, true));
    }

    @Test
    void retractSpeedMatchesVegasWeightFormula() {
        float perUnit = 15.14f;
        float fast = 242.2f;

        assertEquals(fast, HookPhysics.computeRetractSpeed(10, 9, false, false, true, perUnit, fast), 0.01f);
        assertEquals(fast, HookPhysics.computeRetractSpeed(10, 11, false, true, false, perUnit, fast), 0.01f);

        // Small gold w=3 -> 15.14 * (10 - 3) = 105.98
        assertEquals(105.98f, HookPhysics.computeRetractSpeed(10, 3, false, false, false, perUnit, fast), 0.01f);
        // Large gold w=9 -> 15.14 * 1
        assertEquals(15.14f, HookPhysics.computeRetractSpeed(10, 9, false, false, false, perUnit, fast), 0.01f);
        // Mega gold w=11 without cableBonus -> 0 (breaks)
        assertEquals(0f, HookPhysics.computeRetractSpeed(10, 11, false, false, false, perUnit, fast), 0.01f);
        // Mega gold w=11 with Titanium Cable -> clamped to 1 step
        assertEquals(15.14f, HookPhysics.computeRetractSpeed(10, 11, true, false, false, perUnit, fast), 0.01f);
    }
}
