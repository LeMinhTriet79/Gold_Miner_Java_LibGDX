package com.minhtriet79.goldminer.gameplay.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;

/**
 * Pooled floating feedback text supporting both vertical popups ("BOOM!", "TOO HEAVY!")
 * and Vegas fly-to-TOTAL cash animation (rises briefly above cart then lerps smoothly to HUD TOTAL sign).
 */
public class FloatingText {

    public boolean active;
    public String text = "";
    public float x;
    public float y;
    public float startX;
    public float startY;
    public float targetX;
    public float targetY;
    public boolean flyToTarget;
    public float holdDuration;
    public float elapsed;
    public float duration;
    public float scale = 1.0f;
    public final Color color = new Color(Color.GOLD);

    public void spawn(String text, float x, float y, Color color, float duration) {
        this.active = true;
        this.text = text;
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.flyToTarget = false;
        this.holdDuration = duration;
        this.elapsed = 0f;
        this.duration = duration;
        this.scale = 1.0f;
        this.color.set(color);
    }

    public void spawnFlyToTarget(
        String text,
        float startX,
        float startY,
        float targetX,
        float targetY,
        Color color,
        float holdDuration,
        float flyDuration
    ) {
        this.active = true;
        this.text = text;
        this.x = startX;
        this.y = startY;
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.flyToTarget = true;
        this.holdDuration = holdDuration;
        this.elapsed = 0f;
        this.duration = holdDuration + flyDuration;
        this.scale = 1.15f;
        this.color.set(color);
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        elapsed += dt;
        if (elapsed >= duration) {
            active = false;
            return;
        }

        if (!flyToTarget) {
            y += 20f * dt;
            return;
        }

        if (elapsed <= holdDuration) {
            x = startX;
            y = startY + (elapsed / holdDuration) * 14f;
            scale = 1.15f;
        } else {
            float flyAlpha = (elapsed - holdDuration) / Math.max(0.01f, duration - holdDuration);
            float smooth = Interpolation.pow2In.apply(Math.min(1f, flyAlpha));
            float baseRiseY = startY + 14f;
            x = startX + (targetX - startX) * smooth;
            y = baseRiseY + (targetY - baseRiseY) * smooth;
            scale = 1.15f - 0.30f * smooth;
        }
    }
}
