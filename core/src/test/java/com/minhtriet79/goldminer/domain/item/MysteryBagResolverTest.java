package com.minhtriet79.goldminer.domain.item;

import com.minhtriet79.goldminer.domain.item.MysteryBagOutcome.RewardType;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MysteryBagResolverTest {

    private static IntUnaryOperator sequence(Integer... values) {
        Queue<Integer> q = new ArrayDeque<>(List.of(values));
        return bound -> q.remove();
    }

    @Test
    void withoutCloverRollsStandardWeightsAndAllRewardBuckets() {
        // weight roll = 2 -> 6+2=8; w2 roll = 0 -> w2=1 (MONEY); cash roll = 349 -> $350
        MysteryBagOutcome money = MysteryBagResolver.resolve(false, sequence(2, 0, 349));
        assertEquals(RewardType.MONEY, money.rewardType);
        assertEquals(350, money.moneyAmount);
        assertEquals(8, money.weight);

        // w2 = 4 (roll 3) -> POWER
        MysteryBagOutcome power = MysteryBagResolver.resolve(false, sequence(0, 3));
        assertEquals(RewardType.POWER, power.rewardType);
        assertEquals(6, power.weight);

        // w2 = 5 (roll 4) -> DYNAMITE
        assertEquals(RewardType.DYNAMITE, MysteryBagResolver.resolve(false, sequence(1, 4)).rewardType);
        // w2 = 6 (roll 5) -> LASER
        assertEquals(RewardType.LASER, MysteryBagResolver.resolve(false, sequence(1, 5)).rewardType);
        // w2 = 7 (roll 6) -> SPIKE
        assertEquals(RewardType.SPIKE, MysteryBagResolver.resolve(false, sequence(1, 6)).rewardType);
        // w2 = 8 (roll 7) -> $800
        MysteryBagOutcome jackpot = MysteryBagResolver.resolve(false, sequence(3, 7));
        assertEquals(RewardType.MONEY, jackpot.rewardType);
        assertEquals(800, jackpot.moneyAmount);
        assertEquals(9, jackpot.weight);
    }

    @Test
    void withCloverRollsLighterWeightsAndFixedHighCashOrTools() {
        // roll=1 -> 1 (weight=1); w2=7 (roll 6) -> $700
        MysteryBagOutcome cash700 = MysteryBagResolver.resolve(true, sequence(1, 6));
        assertEquals(RewardType.MONEY, cash700.rewardType);
        assertEquals(700, cash700.moneyAmount);
        assertEquals(1, cash700.weight);

        // roll=0 -> weight = nextInt(7)+1 = 4+1 = 5; w2=2 (roll 1) -> POWER
        MysteryBagOutcome power = MysteryBagResolver.resolve(true, sequence(0, 4, 1));
        assertEquals(RewardType.POWER, power.rewardType);
        assertEquals(5, power.weight);

        // roll=2 -> weight=8; w2=3 (roll 2) -> DYNAMITE
        MysteryBagOutcome dyna = MysteryBagResolver.resolve(true, sequence(2, 2));
        assertEquals(RewardType.DYNAMITE, dyna.rewardType);
        assertEquals(8, dyna.weight);

        // w2=6 (roll 5) -> SPIKE
        assertEquals(RewardType.SPIKE, MysteryBagResolver.resolve(true, sequence(1, 5)).rewardType);
    }
}
