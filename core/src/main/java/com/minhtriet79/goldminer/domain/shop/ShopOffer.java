package com.minhtriet79.goldminer.domain.shop;

/**
 * One shop slot generated for the between-level store.
 */
public class ShopOffer {

    public final int slotIndex;
    public final ShopItemType itemType;
    public final int price;
    public boolean purchased;

    public ShopOffer(int slotIndex, ShopItemType itemType, int price) {
        this.slotIndex = slotIndex;
        this.itemType = itemType;
        this.price = price;
        this.purchased = itemType == null;
    }

    public boolean isAvailable() {
        return itemType != null && !purchased;
    }
}
