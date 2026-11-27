package com.minhtriet79.goldminer.domain.shop;

/**
 * World 1 (Australia) shop items from Vegas DefineSprite_1568.
 */
public enum ShopItemType {
    STRENGTH_DRINK("Strength Drink", "Tang luc keo +2 cho man sau.", "shop_strength"),
    OIL_CAN("Oil Can", "Tang toc do xe mo gap 2.5x cho man sau.", "shop_oil"),
    LUCKY_CLOVER("Lucky Clover", "Tang co hoi ra qua xin va nhe trong Tui Bi An.", "shop_clover"),
    DYNAMITE_STICK("Dynamite Stick", "+1 thanh thuoc no (giu qua cac man).", "shop_dynamite"),
    POCKET_WATCH("Pocket Watch", "Cong them +15 giay cho man tiep theo.", "shop_watch"),
    GEM_POLISH("Gem Polish", "Tang gia Kim cuong ($900) va Bilby Kim cuong ($910).", "shop_polish"),
    TITANIUM_CABLE("Titanium Cable", "Chong dut day khi keo vat sieu nang.", "shop_cable");

    public final String displayName;
    public final String description;
    public final String buttonKeyPrefix;

    ShopItemType(String displayName, String description, String buttonKeyPrefix) {
        this.displayName = displayName;
        this.description = description;
        this.buttonKeyPrefix = buttonKeyPrefix;
    }
}
