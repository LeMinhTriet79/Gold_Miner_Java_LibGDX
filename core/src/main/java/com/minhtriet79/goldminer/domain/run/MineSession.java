package com.minhtriet79.goldminer.domain.run;

/**
 * Pure-Java state of a single mining level run: Mine Money ($), Goal, countdown timer
 * with Vegas overtime, dynamite count, and temporary level buffs.
 */
public class MineSession {

    private int score;
    private int goal;
    private float timeRemaining;
    private int lastWholeSecond;
    private int dynamiteCount;
    private int minerStrength;
    private boolean cableBonus;
    private boolean superStrength;
    private boolean cloverBonus;
    private boolean finished;
    private boolean passed;

    public MineSession(int goal, float durationSec, int startingDynamite, int baseMinerStrength) {
        reset(goal, durationSec, startingDynamite, baseMinerStrength);
    }

    public void reset(int goal, float durationSec, int startingDynamite, int baseMinerStrength) {
        this.score = 0;
        this.goal = goal;
        this.timeRemaining = durationSec;
        this.lastWholeSecond = (int) Math.ceil(durationSec);
        this.dynamiteCount = startingDynamite;
        this.minerStrength = baseMinerStrength;
        this.cableBonus = false;
        this.superStrength = false;
        this.cloverBonus = false;
        this.finished = false;
        this.passed = false;
    }

    /**
     * Advances the countdown timer by {@code dt} seconds.
     * Implements Vegas overtime: if time reaches 0 while {@code isBusy} is true (reeling/animating),
     * level completion waits until {@code isBusy} becomes false.
     *
     * @return true when a countdown warning beep should trigger (each whole second <= warningSec).
     */
    public boolean tickTimer(float dt, boolean isBusy, int warningSec) {
        if (finished) {
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
            finished = true;
            passed = score >= goal;
        }
        return shouldBeep;
    }

    public void addScore(int delta) {
        score = Math.max(0, score + delta);
    }

    public boolean consumeDynamite() {
        if (dynamiteCount <= 0) {
            return false;
        }
        dynamiteCount--;
        return true;
    }

    public void addDynamite(int amount) {
        dynamiteCount += amount;
    }

    public void activateSuperStrength() {
        this.superStrength = true;
    }

    public void setCableBonus(boolean cableBonus) {
        this.cableBonus = cableBonus;
    }

    public void setCloverBonus(boolean cloverBonus) {
        this.cloverBonus = cloverBonus;
    }

    public void addStrength(int delta) {
        this.minerStrength += delta;
    }

    public int getScore() {
        return score;
    }

    public int getGoal() {
        return goal;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public int getDisplayedSeconds() {
        return Math.max(0, (int) Math.ceil(timeRemaining));
    }

    public int getDynamiteCount() {
        return dynamiteCount;
    }

    public int getMinerStrength() {
        return minerStrength;
    }

    public boolean hasCableBonus() {
        return cableBonus;
    }

    public boolean hasSuperStrength() {
        return superStrength;
    }

    public boolean hasCloverBonus() {
        return cloverBonus;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isPassed() {
        return passed;
    }
}
