package com.minhtriet79.goldminer.studio.model;

import java.util.List;

/**
 * Pure-Java math utilities for Gold Miner Studio grid snapping, aspect fitting,
 * level target calculations, and bounding boxes.
 */
public final class StudioMath {

    public static final float GRID_STEP = 5.0f;
    public static final float AUTO_GOAL_RATIO = 0.40f;

    private StudioMath() {
    }

    public static float snapToGrid(float value, float step, boolean enabled) {
        if (!enabled || step <= 0f) {
            return Math.round(value * 10f) / 10f;
        }
        return Math.round(value / step) * step;
    }

    public static int computeTotalLevelValue(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (int v : values) {
            if (v > 0) {
                sum += v;
            }
        }
        return sum;
    }

    public static int computeAutoGoal(int levelValue) {
        return Math.max(100, Math.round(levelValue * AUTO_GOAL_RATIO));
    }

    /**
     * Computes centered rectangle {@code [x, y, width, height]} matching {@code targetAspect}
     * inside {@code (areaX, areaY, areaW, areaH)}.
     */
    public static float[] fitAspectRect(float areaX, float areaY, float areaW, float areaH, float targetAspect) {
        if (areaW <= 0f || areaH <= 0f || targetAspect <= 0f) {
            return new float[] {areaX, areaY, 0f, 0f};
        }
        float currentAspect = areaW / areaH;
        float w;
        float h;
        if (currentAspect > targetAspect) {
            h = areaH;
            w = h * targetAspect;
        } else {
            w = areaW;
            h = w / targetAspect;
        }
        float x = areaX + (areaW - w) * 0.5f;
        float y = areaY + (areaH - h) * 0.5f;
        return new float[] {x, y, w, h};
    }

    /**
     * Computes union bounding box {@code [minX, minY, maxX, maxY]} across multiple {@code [x0, y0, x1, y1]} boxes.
     */
    public static int[] computeUnionBbox(List<int[]> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int[] b : boxes) {
            if (b == null || b.length < 4) {
                continue;
            }
            minX = Math.min(minX, b[0]);
            minY = Math.min(minY, b[1]);
            maxX = Math.max(maxX, b[2]);
            maxY = Math.max(maxY, b[3]);
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }
        return new int[] {minX, minY, maxX, maxY};
    }
}
