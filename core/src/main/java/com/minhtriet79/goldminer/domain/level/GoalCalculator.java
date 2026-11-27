package com.minhtriet79.goldminer.domain.level;

/**
 * Pure-Java implementation of Gold Miner Vegas dynamic goal progression ({@code setGoal()}).
 * Defaults match Story Mode Easy difficulty from VEGAS_EXTRACTED_SPECS.md.
 */
public final class GoalCalculator {

    public static final int DEFAULT_LEVEL_1_GOAL = 555;
    public static final float EASY_START_GOAL_CHANGER = 0.38f;
    public static final float EASY_GOAL_CHANGER_ADD_ON = 0.01f;
    public static final float EASY_GOAL_CHANGER_MAX = 0.42f;
    public static final float EASY_GOAL_OVER_PERCENT = 0.38f;

    public static final class GoalResult {
        public final int goal;
        public final float goalChanger;

        public GoalResult(int goal, float goalChanger) {
            this.goal = goal;
            this.goalChanger = goalChanger;
        }
    }

    private GoalCalculator() {
    }

    public static GoalResult computeEasyGoal(int level, int prevGoal, int lastScore, int levelValue, float currentChanger) {
        return computeNextGoal(
            level,
            prevGoal,
            lastScore,
            levelValue,
            currentChanger,
            EASY_GOAL_CHANGER_ADD_ON,
            EASY_GOAL_CHANGER_MAX,
            EASY_GOAL_OVER_PERCENT
        );
    }

    public static GoalResult computeNextGoal(
        int level,
        int prevGoal,
        int lastScore,
        int levelValue,
        float currentChanger,
        float addOn,
        float maxChanger,
        float overPercent
    ) {
        if (level <= 1 || prevGoal <= 0) {
            float l1Changer = Math.min(maxChanger, currentChanger + addOn);
            int l1Goal = levelValue > 0 ? Math.max(DEFAULT_LEVEL_1_GOAL, (int) (levelValue * l1Changer)) : DEFAULT_LEVEL_1_GOAL;
            if (level == 1 && prevGoal <= 0) {
                l1Goal = DEFAULT_LEVEL_1_GOAL;
            }
            return new GoalResult(l1Goal, l1Changer);
        }

        int baseGoal = prevGoal;
        if (lastScore >= prevGoal) {
            baseGoal = (int) (prevGoal + (lastScore - prevGoal) * overPercent);
        }

        float nextChanger = currentChanger;
        if (nextChanger < maxChanger) {
            nextChanger = Math.min(maxChanger, nextChanger + addOn);
        }

        int nextGoal = (int) (baseGoal + levelValue * nextChanger);
        return new GoalResult(nextGoal, nextChanger);
    }
}
