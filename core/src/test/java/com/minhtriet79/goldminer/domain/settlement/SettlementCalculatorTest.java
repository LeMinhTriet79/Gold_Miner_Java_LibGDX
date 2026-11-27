package com.minhtriet79.goldminer.domain.settlement;

import com.minhtriet79.goldminer.domain.run.RunSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementCalculatorTest {

    @Test
    void zeroProgressRunAwardsZeroOfficialCoins() {
        RunSession run = new RunSession();
        SettlementSummary summary = SettlementCalculator.settle(run, false);
        assertEquals(0, summary.totalAwardedCoins);
        assertFalse(summary.completedWorld1);
        assertTrue(run.isSettled());
    }

    @Test
    void world1ClearRunConvertsMoneyPlusClearBonusesAndIsIdempotent() {
        RunSession run = new RunSession();
        for (int lv = 1; lv <= 6; lv++) {
            run.prepareLevel(lv, 2000);
            run.addMineMoney(1000);
            run.finishLevel(true);
        }

        // $6,000 / 10 = 600 Xu; 6 * 25 = 150 Xu; W1 clear bonus = 200 Xu -> 950 Xu total
        SettlementSummary first = SettlementCalculator.settle(run, true);
        assertEquals(6000, first.finalMineMoney);
        assertEquals(6, first.levelsCleared);
        assertTrue(first.completedWorld1);
        assertEquals(600, first.convertedCoins);
        assertEquals(350, first.clearBonusCoins);
        assertEquals(950, first.totalAwardedCoins);

        // Idempotent guard: second settle must award 0
        SettlementSummary second = SettlementCalculator.settle(run, true);
        assertEquals(0, second.totalAwardedCoins);
    }
}
