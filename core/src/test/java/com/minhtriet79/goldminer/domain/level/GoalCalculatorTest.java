package com.minhtriet79.goldminer.domain.level;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalCalculatorTest {

    @Test
    void levelOneUsesCanonicalDefault555Goal() {
        GoalCalculator.GoalResult result = GoalCalculator.computeEasyGoal(1, 0, 0, 1455, 0.38f);
        assertEquals(555, result.goal);
        assertEquals(0.39f, result.goalChanger, 0.0001f);
    }

    @Test
    void subsequentLevelIncorporatesSurplusAndIncrementsGoalChangerUpToMax() {
        GoalCalculator.GoalResult l2 = GoalCalculator.computeEasyGoal(2, 555, 1055, 2589, 0.39f);
        // base = 555 + (1055 - 555) * 0.38 = 745; changer = 0.40; goal = 745 + int(2589 * 0.40) = 1780
        assertEquals(0.40f, l2.goalChanger, 0.0001f);
        assertEquals((int) ((int) (555 + 500 * 0.38f) + 2589 * 0.40f), l2.goal);

        GoalCalculator.GoalResult capped = GoalCalculator.computeEasyGoal(6, 5000, 5000, 2412, 0.42f);
        assertEquals(0.42f, capped.goalChanger, 0.0001f);
        assertTrue(capped.goal > 5000);
    }
}
