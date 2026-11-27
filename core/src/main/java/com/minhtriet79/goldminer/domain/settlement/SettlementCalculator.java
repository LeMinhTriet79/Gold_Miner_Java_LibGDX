package com.minhtriet79.goldminer.domain.settlement;

import com.minhtriet79.goldminer.domain.run.RunSession;

/**
 * Pure-Java settlement converter: turns temporary Mine Money ($) and cleared levels
 * into persistent Official Currency (Xu) exactly once per completed/ended run.
 */
public final class SettlementCalculator {

    public static final int MINE_MONEY_PER_COIN = 10;
    public static final int COINS_PER_CLEARED_LEVEL = 25;
    public static final int WORLD1_CLEAR_BONUS_COINS = 200;

    private SettlementCalculator() {
    }

    public static SettlementSummary settle(RunSession session, boolean completedWorld1) {
        if (session == null || session.isSettled()) {
            return new SettlementSummary(0, 0, false, 0, 0);
        }
        session.markSettled();

        int money = Math.max(0, session.getMineMoney());
        int cleared = Math.max(0, session.getLevelsCleared());
        if (money == 0 && cleared == 0 && !completedWorld1) {
            return new SettlementSummary(0, 0, false, 0, 0);
        }

        int fromMoney = money / MINE_MONEY_PER_COIN;
        int clearBonus = cleared * COINS_PER_CLEARED_LEVEL + (completedWorld1 ? WORLD1_CLEAR_BONUS_COINS : 0);
        return new SettlementSummary(money, cleared, completedWorld1, fromMoney, clearBonus);
    }
}
