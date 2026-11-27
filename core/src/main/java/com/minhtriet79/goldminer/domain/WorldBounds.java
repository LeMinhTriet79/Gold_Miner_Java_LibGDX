package com.minhtriet79.goldminer.domain;

/**
 * Pure Java definition of the virtual 16:9 world layout (480x270) and Vegas FPS conversion helpers.
 * Adjusted layout: removes the 22.5u bottom toolbar of the original 4:3 game so the underground mine
 * spans the full depth from y=0 up to y=192, while HUD controls (Pause/Sound/Menu) live in the top band.
 */
public final class WorldBounds {

    public static final float WORLD_WIDTH = 480f;
    public static final float WORLD_HEIGHT = 270f;

    /** Top HUD + sky band [226, 270] (height 44u). */
    public static final float HUD_BOTTOM_Y = 226f;
    public static final float HUD_TOP_HEIGHT = WORLD_HEIGHT - HUD_BOTTOM_Y;

    /** Track & miner deck band [192, 226]. */
    public static final float MINE_BOTTOM_Y = 0f;
    public static final float MINE_TOP_Y = 192f;
    public static final float TRACK_Y = 194f;
    public static final float HOOK_PIVOT_Y = 194f;

    public static final float CART_MIN_X = 24f;
    public static final float CART_MAX_X = 456f;

    /** Collision radius of the hook tip itself (never a 0px point). */
    public static final float DEFAULT_HOOK_TIP_RADIUS = 5.0f;

    /** Original Gold Miner Vegas Flash timeline FPS verified from timerMov (18 frames/sec). */
    public static final float VEGAS_NATIVE_FPS = 18f;

    /** Uniform vertical scale factor from 800x600 Vegas coordinates to 480x270 world units. */
    public static final float VEGAS_WORLD_SCALE = WORLD_HEIGHT / 600f;

    private WorldBounds() {
    }

    /**
     * Converts a per-frame speed in the original 18 FPS Vegas game to per-second speed for delta-time updates.
     */
    public static float toPerSecond(float valuePerFrame) {
        return valuePerFrame * VEGAS_NATIVE_FPS;
    }

    /**
     * Zero-allocation circle-circle overlap check used by hook tip and mine item hitboxes.
     */
    public static boolean circlesOverlap(float x1, float y1, float r1, float x2, float y2, float r2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float radiusSum = r1 + r2;
        return dx * dx + dy * dy <= radiusSum * radiusSum;
    }
}
