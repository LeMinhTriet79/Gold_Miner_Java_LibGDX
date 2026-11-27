package com.minhtriet79.goldminer.studio;

import com.minhtriet79.goldminer.gameplay.world.LevelConfig.ItemSpawn;
import com.minhtriet79.goldminer.studio.model.EntityPrefabCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityPrefabCatalogTest {

    @Test
    void catalogHasAllCorePrefabsAndValidTrackRanges() {
        assertFalse(EntityPrefabCatalog.getAll().isEmpty());
        for (EntityPrefabCatalog.PrefabDef def : EntityPrefabCatalog.getAll()) {
            assertNotNull(def.key);
            assertTrue(def.radius > 0f);
            assertTrue(def.drawWidth > 0f);
            assertTrue(def.drawHeight > 0f);
        }

        ItemSpawn gold = EntityPrefabCatalog.createLevelSpawn(EntityPrefabCatalog.findByKey("gold_large"), 100f, 50f, 2);
        assertEquals(500, gold.value);
        assertEquals(9, gold.weight);
        assertEquals(2, gold.zIndex);

        float[] track2 = EntityPrefabCatalog.getTrackRange(2);
        assertEquals(155f, track2[0], 0.01f);
        assertEquals(371f, track2[1], 0.01f);
    }
}
