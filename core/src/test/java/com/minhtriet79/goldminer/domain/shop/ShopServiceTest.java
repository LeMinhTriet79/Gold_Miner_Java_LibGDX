package com.minhtriet79.goldminer.domain.shop;

import com.minhtriet79.goldminer.domain.run.RunSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopServiceTest {

    @Test
    void w1ShopGeneratesFiveSlotsAndAppliesPurchasedBuffsAndDynamite() {
        // Always return 0 -> triggers Strength Drink, Lucky Clover, Dynamite, Pocket Watch, Gem Polish
        List<ShopOffer> offers = VegasW1ShopGenerator.generateOffers(2, false, bound -> 0);
        assertEquals(5, offers.size());
        assertEquals(ShopItemType.STRENGTH_DRINK, offers.get(0).itemType);
        assertEquals(50, offers.get(0).price);
        assertEquals(ShopItemType.DYNAMITE_STICK, offers.get(2).itemType);
        assertEquals(5, offers.get(2).price); // 0 + 1 + 2*2

        RunSession run = new RunSession();
        run.addMineMoney(500);

        assertEquals(ShopService.PurchaseResult.SUCCESS, ShopService.purchase(run, offers.get(0)));
        assertEquals(450, run.getMineMoney());
        assertEquals(12, run.getMinerStrength());
        assertTrue(offers.get(0).purchased);
        assertEquals(ShopService.PurchaseResult.ALREADY_PURCHASED, ShopService.purchase(run, offers.get(0)));

        assertEquals(ShopService.PurchaseResult.SUCCESS, ShopService.purchase(run, offers.get(2)));
        assertEquals(4, run.getDynamiteCount());
    }

    @Test
    void purchaseRejectedWhenInsufficientFundsOrEmptySlot() {
        RunSession run = new RunSession(); // 0 Mine Money
        ShopOffer expensive = new ShopOffer(1, ShopItemType.TITANIUM_CABLE, 400);
        ShopOffer empty = new ShopOffer(2, null, 0);

        assertEquals(ShopService.PurchaseResult.INSUFFICIENT_FUNDS, ShopService.purchase(run, expensive));
        assertFalse(expensive.purchased);
        assertEquals(ShopService.PurchaseResult.EMPTY_SLOT, ShopService.purchase(run, empty));

        assertNotNull(ShopItemType.GEM_POLISH.description);
    }
}
