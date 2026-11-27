package com.minhtriet79.goldminer.studio;

import com.minhtriet79.goldminer.gameplay.world.LevelConfig;
import com.minhtriet79.goldminer.studio.model.EntityPrefabCatalog;
import com.minhtriet79.goldminer.studio.model.LevelValidator;
import com.minhtriet79.goldminer.studio.model.LevelValidator.Severity;
import com.minhtriet79.goldminer.studio.model.LevelValidator.ValidationReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelValidatorTest {

    @Test
    void detectsEconomyErrorsOverlapsAndMissingAssets() {
        LevelConfig lvl = new LevelConfig();
        lvl.goal = 1000;
        lvl.items.add(EntityPrefabCatalog.createLevelSpawn(EntityPrefabCatalog.findByKey("gold_medium"), 120f, 80f, 0));
        lvl.items.add(EntityPrefabCatalog.createLevelSpawn(EntityPrefabCatalog.findByKey("gold_medium"), 121f, 80f, 1));
        lvl.items.add(EntityPrefabCatalog.createLevelSpawn(EntityPrefabCatalog.findByKey("diamond"), 5f, 220f, 2));

        ValidationReport report = LevelValidator.validate(lvl, key -> !"diamond".equals(key));
        assertTrue(report.hasErrors());
        assertEquals(1100, report.totalValue);
        assertTrue(report.issues.stream().anyMatch(i -> i.severity == Severity.ERROR && i.message.contains("missing") || i.message.contains("thiếu")));
        assertTrue(report.issues.stream().anyMatch(i -> i.severity == Severity.WARNING && i.message.contains("chồng")));
    }

    @Test
    void passesBalancedWellSpacedLevelWithoutErrors() {
        LevelConfig lvl = new LevelConfig();
        lvl.goal = 555;
        lvl.items.add(EntityPrefabCatalog.createLevelSpawn(EntityPrefabCatalog.findByKey("gold_large"), 120f, 70f, 0));
        lvl.items.add(EntityPrefabCatalog.createLevelSpawn(EntityPrefabCatalog.findByKey("diamond"), 300f, 60f, 1));

        ValidationReport report = LevelValidator.validate(lvl, key -> true);
        assertFalse(report.hasErrors());
        assertTrue(report.economyRatio >= 1.9f);
    }

    @Test
    void detectsInvalidCartRangeAndShortDuration() {
        LevelConfig lvl = new LevelConfig();
        lvl.goal = 200;
        lvl.durationSeconds = 8;
        lvl.cartRange.minX = 300f;
        lvl.cartRange.maxX = 150f;
        lvl.items.add(EntityPrefabCatalog.createLevelSpawn(EntityPrefabCatalog.findByKey("gold_large"), 200f, 80f, 0));

        ValidationReport report = LevelValidator.validate(lvl, key -> true);
        assertTrue(report.hasErrors());
        assertTrue(report.issues.stream().anyMatch(i -> i.message.contains("cartRange")));
        assertTrue(report.issues.stream().anyMatch(i -> i.message.contains("< 15s")));
    }
}
