package com.minhtriet79.goldminer.domain.item;

import com.minhtriet79.goldminer.domain.item.MysteryBagOutcome.RewardType;

import java.util.function.IntUnaryOperator;

/**
 * Pure-Java Mystery Bag probability table extracted verbatim from Vegas DefineSprite_1884 / 2444 / 2684.
 * Accepts an {@link IntUnaryOperator} {@code nextInt(bound)} returning integers in {@code [0, bound - 1]}.
 */
public final class MysteryBagResolver {

    private MysteryBagResolver() {
    }

    public static MysteryBagOutcome resolve(boolean cloverBonus, IntUnaryOperator nextInt) {
        if (cloverBonus) {
            int weight = resolveCloverWeight(nextInt);
            int w2 = nextInt.applyAsInt(8) + 1;
            if (w2 <= 2) {
                return new MysteryBagOutcome(RewardType.POWER, 0, weight);
            } else if (w2 <= 4) {
                return new MysteryBagOutcome(RewardType.DYNAMITE, 0, weight);
            } else if (w2 <= 6) {
                return new MysteryBagOutcome(RewardType.SPIKE, 0, weight);
            }
            return new MysteryBagOutcome(RewardType.MONEY, 700, weight);
        }

        int weight = nextInt.applyAsInt(4) + 6;
        int w2 = nextInt.applyAsInt(8) + 1;
        if (w2 <= 3) {
            int cash = nextInt.applyAsInt(600) + 1;
            return new MysteryBagOutcome(RewardType.MONEY, cash, weight);
        } else if (w2 == 4) {
            return new MysteryBagOutcome(RewardType.POWER, 0, weight);
        } else if (w2 == 5) {
            return new MysteryBagOutcome(RewardType.DYNAMITE, 0, weight);
        } else if (w2 == 6) {
            return new MysteryBagOutcome(RewardType.LASER, 0, weight);
        } else if (w2 == 7) {
            return new MysteryBagOutcome(RewardType.SPIKE, 0, weight);
        }
        return new MysteryBagOutcome(RewardType.MONEY, 800, weight);
    }

    private static int resolveCloverWeight(IntUnaryOperator nextInt) {
        int roll = nextInt.applyAsInt(3) + 1;
        if (roll == 1) {
            return nextInt.applyAsInt(7) + 1;
        }
        if (roll == 2) {
            return 1;
        }
        return 8;
    }
}
