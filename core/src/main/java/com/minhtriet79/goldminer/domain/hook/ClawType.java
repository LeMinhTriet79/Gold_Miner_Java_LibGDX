package com.minhtriet79.goldminer.domain.hook;

/**
 * The 4 claw types in Gold Miner Vegas (DefineSprite_2823 / 2826 / 2830).
 * Pure Java, zero libGDX imports.
 */
public enum ClawType {
    NORMAL(1),
    SPIKE(2),
    LASER(3),
    SUPER(4);

    public final int id;

    ClawType(int id) {
        this.id = id;
    }

    /**
     * Spike Claw (claw == 2) shatters rocks, bones, and walls directly without reeling them in.
     * Matches Vegas DefineSprite_2863: objectN in [8..10], [14..18], 31, [35..36], [60..62].
     */
    public static boolean isShatteredBySpike(int objectN) {
        return (objectN >= 8 && objectN <= 10)
            || (objectN >= 14 && objectN <= 18)
            || objectN == 31
            || (objectN >= 35 && objectN <= 36)
            || (objectN >= 60 && objectN <= 62);
    }

    /**
     * Spike Claw (claw == 2) knocks Bilby (21) and Tasmanian Devil (27..28) back without grabbing.
     */
    public static boolean isKnockedBackBySpike(int objectN) {
        return objectN == 21 || (objectN >= 27 && objectN <= 28);
    }
}
