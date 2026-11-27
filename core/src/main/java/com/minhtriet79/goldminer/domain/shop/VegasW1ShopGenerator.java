package com.minhtriet79.goldminer.domain.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Pure-Java World 1 (Australia) 5-slot shop inventory & price generator matching Vegas DefineSprite_1568.
 * {@code nextInt.applyAsInt(bound)} returns an integer uniformly in {@code [0, bound - 1]}.
 */
public final class VegasW1ShopGenerator {

    private VegasW1ShopGenerator() {
    }

    public static List<ShopOffer> generateOffers(int upcomingLevel, boolean hardDifficulty, IntUnaryOperator nextInt) {
        List<ShopOffer> offers = new ArrayList<>(5);
        offers.add(generateSlot1(hardDifficulty, nextInt));
        offers.add(generateSlot2(nextInt));
        offers.add(generateSlot3(upcomingLevel, hardDifficulty, nextInt));
        offers.add(generateSlot4(hardDifficulty, nextInt));
        offers.add(generateSlot5(nextInt));
        return offers;
    }

    private static ShopOffer generateSlot1(boolean hardDifficulty, IntUnaryOperator nextInt) {
        int roll = nextInt.applyAsInt(3);
        if (roll == 0) {
            int price = hardDifficulty ? (nextInt.applyAsInt(600) + 1) : (nextInt.applyAsInt(300) + 50);
            return new ShopOffer(1, ShopItemType.STRENGTH_DRINK, price);
        }
        if (roll == 1) {
            int price = nextInt.applyAsInt(300) + 25;
            return new ShopOffer(1, ShopItemType.OIL_CAN, price);
        }
        return new ShopOffer(1, null, 0);
    }

    private static ShopOffer generateSlot2(IntUnaryOperator nextInt) {
        int roll = nextInt.applyAsInt(10);
        if (roll < 6) {
            int price = nextInt.applyAsInt(100) + 19;
            return new ShopOffer(2, ShopItemType.LUCKY_CLOVER, price);
        }
        return new ShopOffer(2, null, 0);
    }

    private static ShopOffer generateSlot3(int upcomingLevel, boolean hardDifficulty, IntUnaryOperator nextInt) {
        int price = hardDifficulty
            ? (nextInt.applyAsInt(600) + 1)
            : (nextInt.applyAsInt(300) + 1 + Math.max(1, upcomingLevel) * 2);
        return new ShopOffer(3, ShopItemType.DYNAMITE_STICK, price);
    }

    private static ShopOffer generateSlot4(boolean hardDifficulty, IntUnaryOperator nextInt) {
        int roll = nextInt.applyAsInt(10);
        if (roll < 6) {
            int price = hardDifficulty ? (nextInt.applyAsInt(1000) + 150) : (nextInt.applyAsInt(500) + 150);
            return new ShopOffer(4, ShopItemType.POCKET_WATCH, price);
        }
        return new ShopOffer(4, null, 0);
    }

    private static ShopOffer generateSlot5(IntUnaryOperator nextInt) {
        int roll = nextInt.applyAsInt(10);
        if (roll < 3) {
            int price = nextInt.applyAsInt(600) + 300;
            return new ShopOffer(5, ShopItemType.GEM_POLISH, price);
        }
        if (roll < 7) {
            int price = nextInt.applyAsInt(800) + 10;
            return new ShopOffer(5, ShopItemType.TITANIUM_CABLE, price);
        }
        return new ShopOffer(5, null, 0);
    }
}
