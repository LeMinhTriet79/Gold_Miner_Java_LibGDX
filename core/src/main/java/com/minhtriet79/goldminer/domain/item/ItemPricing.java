package com.minhtriet79.goldminer.domain.item;

/**
 * Pure-Java item value and collector bonus helpers matching Gold Miner Vegas rules.
 */
public final class ItemPricing {

    public static final int DIAMOND_OBJECT_N = 13;
    public static final int DIAMOND_BILBY_OBJECT_N = 22;
    public static final int GEM_FIRST_OBJECT_N = 23;
    public static final int GEM_LAST_OBJECT_N = 26;

    public static final int DIAMOND_POLISHED_VALUE = 900;
    public static final int DIAMOND_BILBY_POLISHED_VALUE = 910;
    public static final int GEM_COMBO_BONUS = 2000;

    private ItemPricing() {
    }

    /**
     * Resolves delivered cash value applying World 1 Gem Polish buff.
     */
    public static int resolveEffectiveValue(int objectN, int baseValue, boolean diamondBonus) {
        if (!diamondBonus) {
            return baseValue;
        }
        if (objectN == DIAMOND_OBJECT_N) {
            return DIAMOND_POLISHED_VALUE;
        }
        if (objectN == DIAMOND_BILBY_OBJECT_N) {
            return DIAMOND_BILBY_POLISHED_VALUE;
        }
        return baseValue;
    }

    public static boolean isW1Gem(int objectN) {
        return objectN >= GEM_FIRST_OBJECT_N && objectN <= GEM_LAST_OBJECT_N;
    }

    public static int gemBitIndex(int objectN) {
        return isW1Gem(objectN) ? (objectN - GEM_FIRST_OBJECT_N) : -1;
    }
}
