package com.minhtriet79.goldminer.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldBoundsTest {

    @Test
    void worldDimensionsAndMaximizedMineAreaMatchAdjustedLayout() {
        assertEquals(480f, WorldBounds.WORLD_WIDTH, 0.001f);
        assertEquals(270f, WorldBounds.WORLD_HEIGHT, 0.001f);
        assertEquals(0f, WorldBounds.MINE_BOTTOM_Y, 0.001f);
        assertEquals(192f, WorldBounds.MINE_TOP_Y, 0.001f);
        assertEquals(44f, WorldBounds.HUD_TOP_HEIGHT, 0.001f);
    }

    @Test
    void hookTipRadiusAndFpsConversionMatchVegasSpecs() {
        assertEquals(5.0f, WorldBounds.DEFAULT_HOOK_TIP_RADIUS, 0.001f);
        assertEquals(236.88f, WorldBounds.toPerSecond(13.16f), 0.01f);
    }

    @Test
    void circlesOverlapDetectsInsideTangentAndOutsideCases() {
        float hookR = WorldBounds.DEFAULT_HOOK_TIP_RADIUS;
        float itemR = 10f;

        assertTrue(WorldBounds.circlesOverlap(100f, 100f, hookR, 110f, 100f, itemR));
        assertTrue(WorldBounds.circlesOverlap(100f, 100f, hookR, 115f, 100f, itemR));
        assertFalse(WorldBounds.circlesOverlap(100f, 100f, hookR, 115.1f, 100f, itemR));
    }
}
