package com.minhtriet79.goldminer.studio;

import com.minhtriet79.goldminer.studio.model.CanvasCamera;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasCameraTest {

    @Test
    void roundTripLocalAndWorldMatchesWithinSubPixelPrecision() {
        CanvasCamera cam = new CanvasCamera();
        cam.setViewportSize(730f, 620f);
        cam.panByLocalDelta(42f, -18f);
        cam.zoomAtLocalPoint(365f, 310f, 1.5f);

        float[] local = new float[2];
        float[] world = new float[2];
        cam.worldToLocal(240f, 135f, local);
        cam.localToWorld(local[0], local[1], world);

        assertEquals(240f, world[0], 0.001f);
        assertEquals(135f, world[1], 0.001f);
    }

    @Test
    void zoomAroundCursorKeepsWorldPointFixedUnderCursor() {
        CanvasCamera cam = new CanvasCamera();
        cam.setViewportSize(800f, 450f);

        float cursorX = 520f;
        float cursorY = 190f;
        float[] before = new float[2];
        float[] after = new float[2];

        cam.localToWorld(cursorX, cursorY, before);
        cam.zoomAtLocalPoint(cursorX, cursorY, 1.8f);
        cam.localToWorld(cursorX, cursorY, after);

        assertEquals(before[0], after[0], 0.002f);
        assertEquals(before[1], after[1], 0.002f);
    }

    @Test
    void isLocalInsideWorldRespectsStageBounds() {
        CanvasCamera cam = new CanvasCamera();
        cam.setViewportSize(600f, 400f);

        float[] origin = new float[2];
        cam.getWorldOriginInLocal(origin);

        assertTrue(cam.isLocalInsideWorld(origin[0] + 10f, origin[1] + 10f));
        assertFalse(cam.isLocalInsideWorld(origin[0] - 5f, origin[1] + 10f));
    }
}
