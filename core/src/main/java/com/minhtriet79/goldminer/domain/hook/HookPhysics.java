package com.minhtriet79.goldminer.domain.hook;

/**
 * Pure-Java mathematical model of Gold Miner Vegas hook swinging, cable breaking,
 * and weight-based retraction speeds. Zero libGDX dependencies.
 */
public final class HookPhysics {

    private HookPhysics() {
    }

    /**
     * Computes sinusoidal pendulum angle in degrees within [-maxAngleDeg, +maxAngleDeg].
     * Matches Vegas DefineSprite_2864 (period ~5.556s, ease-in-out at +-80 deg).
     */
    public static float computeSwingAngleDeg(
        float elapsedSec,
        float maxAngleDeg,
        float periodSec,
        float phaseOffsetRad
    ) {
        double omega = (2.0 * Math.PI) / periodSec;
        return (float) (maxAngleDeg * Math.sin(omega * elapsedSec + phaseOffsetRad));
    }

    /**
     * Checks Vegas cable-break rule (DefineSprite_2863):
     * breaks when minerStrength - weight <= 0 without Titanium Cable or Super Strength.
     */
    public static boolean isCableBreak(
        int minerStrength,
        int weight,
        boolean cableBonus,
        boolean superStrength
    ) {
        return (minerStrength - weight) <= 0 && !cableBonus && !superStrength;
    }

    /**
     * Computes retraction speed in world units/sec using Vegas formula:
     * v = speedPerStrengthUnit * (minerStrength - weight), or fastRetractSpeed when empty/blasted/super.
     * With Titanium Cable (cableBonus), overloaded items clamp down to weight=9 -> min 1 strength unit.
     */
    public static float computeRetractSpeed(
        int minerStrength,
        int weight,
        boolean cableBonus,
        boolean superStrength,
        boolean emptyOrBlasted,
        float speedPerStrengthUnit,
        float fastRetractSpeed
    ) {
        if (emptyOrBlasted || superStrength) {
            return fastRetractSpeed;
        }
        int delta = minerStrength - weight;
        if (delta <= 0) {
            if (cableBonus) {
                int clampedDelta = Math.max(1, minerStrength - 9);
                return speedPerStrengthUnit * clampedDelta;
            }
            return 0f;
        }
        return speedPerStrengthUnit * delta;
    }
}
