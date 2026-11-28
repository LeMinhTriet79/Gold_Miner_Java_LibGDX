package com.minhtriet79.goldminer.domain.run;

import com.minhtriet79.goldminer.domain.hook.ClawType;
import com.minhtriet79.goldminer.domain.item.ItemPricing;
import com.minhtriet79.goldminer.domain.level.GoalCalculator;

/**
 * Pure-Java multi-level World 1 (Levels 1..6) campaign run state.
 * Tracks cumulative Mine Money ($), dynamic Goal, persistent dynamite count,
 * per-level shop/bag buffs, 4-Gem combo tracker, and settlement guard.
 */
public class RunSession {

    public static final int MAX_W1_LEVEL = 6;
    public static final int BASE_MINER_STRENGTH = 10;
    public static final int BASE_STARTING_DYNAMITE = 3;
    public static final int EXTRA_TIME_SECONDS = 15;
    public static final int ALL_GEMS_MASK = 0b1111;

    private int currentLevel = 1;
    private int mineMoney = 0;
    private int goal = GoalCalculator.DEFAULT_LEVEL_1_GOAL;
    private int lastScore = 0;
    private float goalChanger = GoalCalculator.EASY_START_GOAL_CHANGER;
    private int dynamiteCount = BASE_STARTING_DYNAMITE;
    private int levelsCleared = 0;
    private boolean settled = false;

    // Temporary per-level buffs (cleared by resetLevelBuffs() after each level ends; dynamite persists)
    private int minerStrength = BASE_MINER_STRENGTH;
    private float cartSpeedMultiplier = 1.0f;
    private boolean cloverBonus;
    private int extraTimeSeconds;
    private boolean diamondBonus;
    private boolean cableBonus;
    private boolean superStrength;
    private ClawType activeClaw = ClawType.NORMAL;
    private int collectedGemsMask;

    // Per-level countdown timer (with Vegas overtime)
    private float timeRemaining = 60f;
    private int lastWholeSecond = 60;
    private boolean levelFinished;
    private boolean levelPassed;

    public RunSession() {
        startNewRun();
    }

    public void startNewRun() {
        currentLevel = 1;
        mineMoney = 0;
        goal = GoalCalculator.DEFAULT_LEVEL_1_GOAL;
        lastScore = 0;
        goalChanger = GoalCalculator.EASY_START_GOAL_CHANGER;
        dynamiteCount = BASE_STARTING_DYNAMITE;
        levelsCleared = 0;
        settled = false;
        resetLevelBuffs();
    }

    /**
     * Configures {@code currentLevel} and computes Vegas dynamic goal from previous score + {@code levelValue}.
     */
    public void prepareLevel(int levelNumber, int levelValue) {
        this.currentLevel = Math.max(1, Math.min(MAX_W1_LEVEL, levelNumber));
        this.collectedGemsMask = 0;
        this.levelFinished = false;
        this.levelPassed = false;
        if (this.currentLevel == 1 && levelsCleared == 0 && mineMoney == 0) {
            this.goal = GoalCalculator.DEFAULT_LEVEL_1_GOAL;
            this.goalChanger = 0.39f;
            resetLevelTimer(60);
            return;
        }
        int prevGoal = (this.currentLevel == 1) ? 0 : this.goal;
        GoalCalculator.GoalResult result = GoalCalculator.computeEasyGoal(
            this.currentLevel,
            prevGoal,
            this.lastScore,
            levelValue,
            this.goalChanger
        );
        int nextGoal = result.goal;
        this.goal = nextGoal;
        this.goalChanger = result.goalChanger;
        resetLevelTimer(60 + extraTimeSeconds);
    }

    /**
     * Returns the canonical World 1 Australia map target ($) for levels 1..6.
     */
    public static int mapTargetForLevel(int levelNumber) {
        return switch (Math.max(1, Math.min(MAX_W1_LEVEL, levelNumber))) {
            case 1 -> 555;
            case 2 -> 1150;
            case 3 -> 1900;
            case 4 -> 2800;
            case 5 -> 3800;
            default -> 5000;
        };
    }

    /**
     * Configures a standalone World Map stage starting from {@code mineMoney = 0} with a fixed target
     * while preserving owned dynamite sticks and pre-level shop buffs.
     */
    public void prepareMapLevel(int levelNumber, int fixedGoal, int durationSec) {
        this.currentLevel = Math.max(1, Math.min(MAX_W1_LEVEL, levelNumber));
        this.mineMoney = 0;
        this.lastScore = 0;
        this.collectedGemsMask = 0;
        this.settled = false;
        this.goal = Math.max(50, fixedGoal);
        resetLevelTimer(Math.max(10, durationSec) + extraTimeSeconds);
    }

    /**
     * Continues seamlessly from {@link com.minhtriet79.goldminer.screens.ShopScreen} into the next map level,
     * preserving remaining {@code mineMoney}, {@code dynamiteCount}, and all purchased Shop buffs.
     */
    public void prepareContinueMapLevel(int levelNumber, int fixedGoal, int durationSec) {
        this.currentLevel = Math.max(1, Math.min(MAX_W1_LEVEL, levelNumber));
        this.collectedGemsMask = 0;
        this.settled = false;
        this.goal = Math.max(50, fixedGoal);
        resetLevelTimer(Math.max(10, durationSec) + extraTimeSeconds);
    }

    /**
     * Configures an immediate Studio Play Test run preserving the exact edited goal and timer.
     */
    public void prepareCustomPlayTest(int levelNumber, int customGoal, int customDurationSec) {
        this.currentLevel = Math.max(1, levelNumber);
        this.collectedGemsMask = 0;
        this.goal = Math.max(50, customGoal);
        resetLevelTimer(Math.max(10, customDurationSec) + extraTimeSeconds);
    }

    public void resetLevelTimer(int durationSec) {
        this.timeRemaining = durationSec;
        this.lastWholeSecond = Math.max(0, durationSec);
        this.levelFinished = false;
        this.levelPassed = false;
    }

    /**
     * Advances the active level countdown timer with Vegas overtime while hook/animations are busy.
     *
     * @return true when a countdown warning beep should trigger (each whole second <= warningSec).
     */
    public boolean tickTimer(float dt, boolean isBusy, int warningSec) {
        if (levelFinished) {
            return false;
        }
        boolean shouldBeep = false;
        if (timeRemaining > 0f) {
            timeRemaining = Math.max(0f, timeRemaining - dt);
            int currentWhole = (int) Math.ceil(timeRemaining);
            if (currentWhole < lastWholeSecond) {
                lastWholeSecond = currentWhole;
                if (currentWhole >= 1 && currentWhole <= warningSec) {
                    shouldBeep = true;
                }
            }
        }
        if (timeRemaining <= 0f && !isBusy) {
            levelFinished = true;
            levelPassed = mineMoney >= goal;
        }
        return shouldBeep;
    }

    public void forceTimeRemaining(float seconds) {
        this.timeRemaining = Math.max(0f, seconds);
        this.lastWholeSecond = (int) Math.ceil(this.timeRemaining);
    }

    public void finishImmediatelyAsPass() {
        if (mineMoney < goal) {
            mineMoney = goal;
        }
        this.timeRemaining = 0f;
        this.levelFinished = true;
        this.levelPassed = true;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public int getDisplayedSeconds() {
        return Math.max(0, (int) Math.ceil(timeRemaining));
    }

    public boolean isLevelFinished() {
        return levelFinished;
    }

    public boolean isLevelPassed() {
        return levelPassed;
    }

    /**
     * Marks level completion at timer end (or early Next Level). Saves {@code lastScore}
     * and clears temporary per-level buffs while keeping persistent dynamite sticks.
     */
    public void finishLevel(boolean passed) {
        this.lastScore = this.mineMoney;
        if (passed && levelsCleared < currentLevel) {
            levelsCleared = currentLevel;
        }
        resetLevelBuffs();
    }

    /**
     * Clears temporary buffs between levels; strictly preserves {@code dynamiteCount} and {@code mineMoney}.
     */
    public void resetLevelBuffs() {
        minerStrength = BASE_MINER_STRENGTH;
        cartSpeedMultiplier = 1.0f;
        cloverBonus = false;
        extraTimeSeconds = 0;
        diamondBonus = false;
        cableBonus = false;
        superStrength = false;
        activeClaw = ClawType.NORMAL;
        collectedGemsMask = 0;
    }

    public void addMineMoney(int amount) {
        mineMoney = Math.max(0, mineMoney + amount);
    }

    public void spendMineMoney(int amount) {
        mineMoney = Math.max(0, mineMoney - amount);
    }

    public void addDynamite(int amount) {
        dynamiteCount = Math.max(0, dynamiteCount + amount);
    }

    public boolean consumeDynamite() {
        if (dynamiteCount <= 0) {
            return false;
        }
        dynamiteCount--;
        return true;
    }

    public void addStrength(int delta) {
        minerStrength += delta;
    }

    public void activateOilCan() {
        cartSpeedMultiplier = 2.5f;
    }

    public void activatePocketWatch() {
        extraTimeSeconds = EXTRA_TIME_SECONDS;
    }

    public void activateSuperStrength() {
        superStrength = true;
        activeClaw = ClawType.SUPER;
    }

    public void setCloverBonus(boolean cloverBonus) {
        this.cloverBonus = cloverBonus;
    }

    public void setDiamondBonus(boolean diamondBonus) {
        this.diamondBonus = diamondBonus;
    }

    public void setCableBonus(boolean cableBonus) {
        this.cableBonus = cableBonus;
    }

    public void setActiveClaw(ClawType activeClaw) {
        this.activeClaw = activeClaw != null ? activeClaw : ClawType.NORMAL;
    }

    /**
     * Records one of the 4 World 1 Gems (objectN 23..26).
     *
     * @return true if this gem completed the 4-Gem Combo set (caller awards +$2,000).
     */
    public boolean recordGemCollected(int objectN) {
        int bitIdx = ItemPricing.gemBitIndex(objectN);
        if (bitIdx < 0) {
            return false;
        }
        int prevMask = collectedGemsMask;
        collectedGemsMask |= (1 << bitIdx);
        return prevMask != ALL_GEMS_MASK && collectedGemsMask == ALL_GEMS_MASK;
    }

    public boolean isGemCollected(int objectN) {
        int bitIdx = ItemPricing.gemBitIndex(objectN);
        return bitIdx >= 0 && (collectedGemsMask & (1 << bitIdx)) != 0;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getMineMoney() {
        return mineMoney;
    }

    public int getGoal() {
        return goal;
    }

    public void setGoalForDebug(int goal) {
        this.goal = goal;
    }

    public int getLastScore() {
        return lastScore;
    }

    public int getDynamiteCount() {
        return dynamiteCount;
    }

    public int getLevelsCleared() {
        return levelsCleared;
    }

    public int getMinerStrength() {
        return minerStrength;
    }

    public float getCartSpeedMultiplier() {
        return cartSpeedMultiplier;
    }

    public boolean hasCloverBonus() {
        return cloverBonus;
    }

    public int getExtraTimeSeconds() {
        return extraTimeSeconds;
    }

    public boolean hasDiamondBonus() {
        return diamondBonus;
    }

    public boolean hasCableBonus() {
        return cableBonus;
    }

    public boolean hasSuperStrength() {
        return superStrength;
    }

    public ClawType getActiveClaw() {
        return activeClaw;
    }

    public int getCollectedGemsMask() {
        return collectedGemsMask;
    }

    public boolean isSettled() {
        return settled;
    }

    public void markSettled() {
        this.settled = true;
    }

    public boolean isGoalMet() {
        return mineMoney >= goal;
    }
}
