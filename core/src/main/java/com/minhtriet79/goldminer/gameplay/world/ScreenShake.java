package com.minhtriet79.goldminer.gameplay.world;

import com.badlogic.gdx.math.MathUtils;

/**
 * Zero-allocation camera shake helper for TNT and Dynamite blasts.
 */
public class ScreenShake {

    private float intensity;
    private float duration;
    private float elapsed;
    public float offsetX;
    public float offsetY;

    public void trigger(float intensity, float duration) {
        this.intensity = Math.max(this.intensity, intensity);
        this.duration = Math.max(this.duration - this.elapsed, duration);
        this.elapsed = 0f;
    }

    public void update(float dt) {
        if (elapsed >= duration || duration <= 0f) {
            offsetX = 0f;
            offsetY = 0f;
            intensity = 0f;
            return;
        }
        elapsed += dt;
        float progress = 1f - Math.min(1f, elapsed / duration);
        float mag = intensity * progress;
        offsetX = MathUtils.random(-mag, mag);
        offsetY = MathUtils.random(-mag, mag);
    }

    public void reset() {
        intensity = 0f;
        duration = 0f;
        elapsed = 0f;
        offsetX = 0f;
        offsetY = 0f;
    }
}
