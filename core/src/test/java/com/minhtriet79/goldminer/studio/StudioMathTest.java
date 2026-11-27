package com.minhtriet79.goldminer.studio;

import com.minhtriet79.goldminer.studio.model.StudioMath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudioMathTest {

    @Test
    void snapToGridSnapsWhenEnabledAndPreservesPrecisionWhenDisabled() {
        assertEquals(15f, StudioMath.snapToGrid(13.2f, 5f, true), 0.001f);
        assertEquals(13.2f, StudioMath.snapToGrid(13.2f, 5f, false), 0.001f);
    }

    @Test
    void autoGoalAndLevelValueSumCalculateAccurately() {
        int total = StudioMath.computeTotalLevelValue(List.of(500, 250, 600, 100));
        assertEquals(1450, total);
        assertEquals(580, StudioMath.computeAutoGoal(total));
    }

    @Test
    void fitAspectRectCentersSixteenByNineCanvasInsideWideRegion() {
        float[] rect = StudioMath.fitAspectRect(100f, 50f, 800f, 400f, 480f / 270f);
        // height fits 400 -> width = 400 * 16/9 = 711.11, centered at x = 100 + (800 - 711.11)/2
        assertEquals(400f, rect[3], 0.01f);
        assertEquals(711.11f, rect[2], 0.05f);
        assertEquals(144.44f, rect[0], 0.05f);
        assertEquals(50f, rect[1], 0.01f);
    }
}
