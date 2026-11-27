package com.minhtriet79.goldminer.domain.item;

/**
 * Outcome rolled when grabbing a Mystery Bag (Grab Bag / Treasure Chest / Vegas Bag).
 */
public final class MysteryBagOutcome {

    public enum RewardType {
        MONEY,
        POWER,
        DYNAMITE,
        LASER,
        SPIKE
    }

    public final RewardType rewardType;
    public final int moneyAmount;
    public final int weight;

    public MysteryBagOutcome(RewardType rewardType, int moneyAmount, int weight) {
        this.rewardType = rewardType;
        this.moneyAmount = moneyAmount;
        this.weight = weight;
    }
}
