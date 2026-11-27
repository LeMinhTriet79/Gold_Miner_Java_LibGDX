package com.minhtriet79.goldminer.domain.settlement;

/**
 * Immutable result of settling a World 1 run from Mine Money ($) into Official Currency (Xu).
 */
public final class SettlementSummary {

    public final int finalMineMoney;
    public final int levelsCleared;
    public final boolean completedWorld1;
    public final int convertedCoins;
    public final int clearBonusCoins;
    public final int totalAwardedCoins;

    public SettlementSummary(
        int finalMineMoney,
        int levelsCleared,
        boolean completedWorld1,
        int convertedCoins,
        int clearBonusCoins
    ) {
        this.finalMineMoney = finalMineMoney;
        this.levelsCleared = levelsCleared;
        this.completedWorld1 = completedWorld1;
        this.convertedCoins = convertedCoins;
        this.clearBonusCoins = clearBonusCoins;
        this.totalAwardedCoins = convertedCoins + clearBonusCoins;
    }
}
