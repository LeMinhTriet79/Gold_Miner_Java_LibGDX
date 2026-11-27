package com.minhtriet79.goldminer.domain.run;

import com.minhtriet79.goldminer.domain.hook.ClawType;
import com.minhtriet79.goldminer.domain.item.ItemPricing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunSessionTest {

    @Test
    void temporaryBuffsResetBetweenLevelsWhileDynamiteAndMoneyPersist() {
        RunSession run = new RunSession();
        run.prepareLevel(1, 1455);
        assertEquals(555, run.getGoal());
        assertEquals(3, run.getDynamiteCount());

        run.addMineMoney(900);
        run.addDynamite(2); // now 5
        run.addStrength(2);
        run.activateOilCan();
        run.activatePocketWatch();
        run.activateSuperStrength();
        run.setCableBonus(true);
        run.setCloverBonus(true);
        run.setDiamondBonus(true);

        run.finishLevel(true);

        // Mine money, cleared level, and dynamite sticks must survive; all buffs reset
        assertEquals(900, run.getMineMoney());
        assertEquals(1, run.getLevelsCleared());
        assertEquals(5, run.getDynamiteCount());
        assertEquals(RunSession.BASE_MINER_STRENGTH, run.getMinerStrength());
        assertEquals(1.0f, run.getCartSpeedMultiplier(), 0.0001f);
        assertEquals(0, run.getExtraTimeSeconds());
        assertEquals(ClawType.NORMAL, run.getActiveClaw());
        assertFalse(run.hasSuperStrength());
        assertFalse(run.hasCableBonus());
        assertFalse(run.hasCloverBonus());
        assertFalse(run.hasDiamondBonus());
    }

    @Test
    void fourGemComboTriggersExactlyOnceWhenAllFourGemsCollected() {
        RunSession run = new RunSession();
        assertFalse(run.recordGemCollected(23));
        assertFalse(run.recordGemCollected(24));
        assertFalse(run.recordGemCollected(25));
        assertTrue(run.recordGemCollected(26));
        assertFalse(run.recordGemCollected(26)); // Already completed
        assertEquals(2000, ItemPricing.GEM_COMBO_BONUS);
    }

    @Test
    void diamondPolishAndSpikeClawRulesMatchVegasSpecs() {
        assertEquals(600, ItemPricing.resolveEffectiveValue(13, 600, false));
        assertEquals(900, ItemPricing.resolveEffectiveValue(13, 600, true));
        assertEquals(910, ItemPricing.resolveEffectiveValue(22, 610, true));

        assertTrue(ClawType.isShatteredBySpike(8));
        assertTrue(ClawType.isShatteredBySpike(14));
        assertFalse(ClawType.isShatteredBySpike(5));
        assertTrue(ClawType.isKnockedBackBySpike(21));
        assertTrue(ClawType.isKnockedBackBySpike(27));
    }
}
