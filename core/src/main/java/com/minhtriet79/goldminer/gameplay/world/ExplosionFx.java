package com.minhtriet79.goldminer.gameplay.world;

/**
 * Pooled effect slot (supports explosion fireballs and smoke clouds) with zero runtime allocations.
 */
public class ExplosionFx {

    public boolean active;
    public String animKey;
    public float x;
    public float y;
    public float elapsed;
    public float duration;

    public void trigger(String animKey, float x, float y, float duration) {
        this.active = true;
        this.animKey = animKey;
        this.x = x;
        this.y = y;
        this.elapsed = 0f;
        this.duration = duration;
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        elapsed += dt;
        if (elapsed >= duration) {
            active = false;
        }
    }
}
