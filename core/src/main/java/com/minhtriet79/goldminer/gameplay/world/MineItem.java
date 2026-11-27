package com.minhtriet79.goldminer.gameplay.world;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.domain.item.ItemPricing;

/**
 * Runtime mine object with decoupled logical collision {@link Circle}, animation/patrol state,
 * and mutable resolved weight (avoiding allocations during gameplay).
 */
public class MineItem {

    public final int objectN;
    public final String type;
    public final String assetKey;
    public final int value;
    public final int weight;
    public final float radius;
    public final float drawWidth;
    public final float drawHeight;
    public final float scale;
    public final float moveSpeed;
    public final int zIndex;
    public final boolean animated;
    public final Circle bounds;

    public boolean active = true;
    public int resolvedWeight;
    public boolean facingRight;
    public float animTime;
    public float pauseTimer;
    public float turnTimer;

    public MineItem(LevelConfig.ItemSpawn spawn) {
        this.objectN = spawn.objectN;
        this.type = spawn.type;
        this.assetKey = spawn.assetKey;
        this.value = spawn.value;
        this.weight = spawn.weight;
        this.resolvedWeight = spawn.weight;
        float appliedScale = spawn.scale > 0f ? spawn.scale : 1.0f;
        this.scale = appliedScale;
        this.radius = spawn.radius * appliedScale;
        this.drawWidth = spawn.drawWidth * appliedScale;
        this.drawHeight = spawn.drawHeight * appliedScale;
        this.moveSpeed = spawn.moveSpeed;
        this.zIndex = spawn.zIndex;
        this.animated = spawn.animated;
        this.bounds = new Circle(spawn.x, spawn.y, this.radius);
        this.facingRight = MathUtils.randomBoolean();
        this.animTime = MathUtils.random(0f, 1.5f);
        this.pauseTimer = 0f;
        this.turnTimer = MathUtils.random(1.2f, 2.5f);
    }

    public void updateCreature(float dt) {
        if (!active || moveSpeed <= 0f) {
            return;
        }
        animTime += dt;

        // Bilby (21) hops in short bursts then pauses briefly; Diamond Bilby (22) & Tasmanian (27/28) patrol smoothly
        if (objectN == 21) {
            if (pauseTimer > 0f) {
                pauseTimer -= dt;
                return;
            }
            turnTimer -= dt;
            if (turnTimer <= 0f) {
                pauseTimer = MathUtils.random(0.28f, 0.78f);
                turnTimer = MathUtils.random(0.6f, 1.4f);
                if (MathUtils.randomBoolean(0.35f)) {
                    facingRight = !facingRight;
                }
                return;
            }
        } else if (objectN == 22) {
            turnTimer -= dt;
            if (turnTimer <= 0f) {
                turnTimer = 1.83f;
                if (MathUtils.randomBoolean(0.20f)) {
                    facingRight = !facingRight;
                }
            }
        }

        float dir = facingRight ? 1f : -1f;
        bounds.x += dir * moveSpeed * dt;
        float minX = Math.max(12f, drawWidth * 0.5f);
        float maxX = WorldBounds.WORLD_WIDTH - minX;
        if (bounds.x <= minX) {
            bounds.x = minX;
            facingRight = true;
        } else if (bounds.x >= maxX) {
            bounds.x = maxX;
            facingRight = false;
        }
    }

    public void knockBackLeft(float deltaUnits) {
        bounds.x = Math.max(14f, bounds.x - deltaUnits);
    }

    public boolean isTnt() {
        return objectN == 12 || "tnt_barrel".equals(type);
    }

    public boolean isMysteryBag() {
        return objectN == 11 || "mystery_bag".equals(type);
    }

    public boolean isGem() {
        return ItemPricing.isW1Gem(objectN);
    }

    public int getEffectiveWeight() {
        return resolvedWeight;
    }
}
