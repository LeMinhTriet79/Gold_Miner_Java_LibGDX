package com.minhtriet79.goldminer.gameplay.world;

import com.minhtriet79.goldminer.domain.WorldBounds;

/**
 * Reusable flying dynamite stick matching Vegas DefineSprite_2825:
 * chases the hook tip using pos += (tip - pos) * 0.5 per 18-FPS frame before detonating.
 */
public class FlyingDynamite {

    private static final float HIT_RADIUS_SQ = 7.5f * 7.5f;

    public boolean active;
    public float x;
    public float y;
    public float elapsed;

    public void reset() {
        active = false;
        elapsed = 0f;
    }

    public void launch(float startX, float startY) {
        this.active = true;
        this.x = startX;
        this.y = startY;
        this.elapsed = 0f;
    }

    /**
     * Advances dynamite along the cable toward {@code (tipX, tipY)}.
     *
     * @return true on the frame it reaches the hook tip and detonates.
     */
    public boolean update(float dt, float tipX, float tipY) {
        if (!active) {
            return false;
        }
        elapsed += dt;
        float chaseAlpha = 1f - (float) Math.pow(0.5, dt * WorldBounds.VEGAS_NATIVE_FPS);
        x += (tipX - x) * chaseAlpha;
        y += (tipY - y) * chaseAlpha;

        float dx = tipX - x;
        float dy = tipY - y;
        if (dx * dx + dy * dy <= HIT_RADIUS_SQ || elapsed >= 0.45f) {
            active = false;
            return true;
        }
        return false;
    }
}
