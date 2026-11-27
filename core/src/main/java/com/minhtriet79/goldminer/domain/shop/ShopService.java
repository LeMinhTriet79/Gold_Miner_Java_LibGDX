package com.minhtriet79.goldminer.domain.shop;

import com.minhtriet79.goldminer.domain.run.RunSession;

/**
 * Pure-Java shop purchase validator & buff applier.
 */
public final class ShopService {

    public enum PurchaseResult {
        SUCCESS,
        EMPTY_SLOT,
        ALREADY_PURCHASED,
        INSUFFICIENT_FUNDS
    }

    private ShopService() {
    }

    public static PurchaseResult purchase(RunSession session, ShopOffer offer) {
        if (offer == null || offer.itemType == null) {
            return PurchaseResult.EMPTY_SLOT;
        }
        if (offer.purchased) {
            return PurchaseResult.ALREADY_PURCHASED;
        }
        if (session.getMineMoney() < offer.price) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        session.spendMineMoney(offer.price);
        offer.purchased = true;

        switch (offer.itemType) {
            case STRENGTH_DRINK -> session.addStrength(2);
            case OIL_CAN -> session.activateOilCan();
            case LUCKY_CLOVER -> session.setCloverBonus(true);
            case DYNAMITE_STICK -> session.addDynamite(1);
            case POCKET_WATCH -> session.activatePocketWatch();
            case GEM_POLISH -> session.setDiamondBonus(true);
            case TITANIUM_CABLE -> session.setCableBonus(true);
        }
        return PurchaseResult.SUCCESS;
    }
}
