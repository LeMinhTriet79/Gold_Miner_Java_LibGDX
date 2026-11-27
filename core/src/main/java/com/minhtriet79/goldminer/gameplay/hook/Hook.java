package com.minhtriet79.goldminer.gameplay.hook;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.domain.hook.HookPhysics;
import com.minhtriet79.goldminer.domain.run.RunSession;
import com.minhtriet79.goldminer.gameplay.world.MineItem;

/**
 * Zero-allocation Vegas hook entity managing sinusoidal swing, extension, circle hitbox,
 * weight-based retraction, cable break, and flying dynamite release.
 */
public class Hook {

    private static final float BRAKE_ANIM_DURATION = 36f / 18f;

    private final HookConfig config;
    private final Vector2 pivot = new Vector2();
    private final Vector2 tip = new Vector2();
    private final Circle tipCircle = new Circle();

    private HookState state = HookState.SWINGING;
    private float stateElapsed;
    private float angleDeg;
    private float cableLength;
    private float swingTime;
    private float brakeTimer;
    private boolean fastRetractOverride;
    private boolean dynamiteInFlight;
    private MineItem attachedItem;

    public Hook(HookConfig config, float pivotX, float pivotY) {
        this.config = config;
        reset(pivotX, pivotY);
    }

    public void reset(float pivotX, float pivotY) {
        this.state = HookState.SWINGING;
        this.stateElapsed = 0f;
        this.swingTime = 0f;
        this.brakeTimer = 0f;
        this.angleDeg = 0f;
        this.cableLength = config.restCableLength;
        this.fastRetractOverride = false;
        this.dynamiteInFlight = false;
        this.attachedItem = null;
        this.pivot.set(pivotX, pivotY);
        this.tipCircle.radius = config.hookTipRadius;
        updateTipPosition();
    }

    public void setPivotX(float pivotX) {
        this.pivot.x = pivotX;
        updateTipPosition();
    }

    public boolean launch() {
        if (state != HookState.SWINGING) {
            return false;
        }
        state = HookState.EXTENDING;
        stateElapsed = 0f;
        fastRetractOverride = false;
        dynamiteInFlight = false;
        attachedItem = null;
        return true;
    }

    /**
     * Attaches a grabbed item or snaps cable if overloaded without Cable/Strength buffs.
     *
     * @return true if cable broke (item lost, braking animation plays), false if grabbed cleanly.
     */
    public boolean attachItem(MineItem item, RunSession session) {
        int effWeight = item.getEffectiveWeight();
        boolean breaks = HookPhysics.isCableBreak(
            session.getMinerStrength(),
            effWeight,
            session.hasCableBonus(),
            session.hasSuperStrength()
        );
        item.active = false;

        if (breaks) {
            attachedItem = null;
            state = HookState.BRAKING;
            stateElapsed = 0f;
            brakeTimer = BRAKE_ANIM_DURATION;
            return true;
        }

        attachedItem = item;
        fastRetractOverride = false;
        dynamiteInFlight = false;
        state = HookState.RETRACTING;
        stateElapsed = 0f;
        return false;
    }

    public void startEmptyRetract() {
        attachedItem = null;
        fastRetractOverride = true;
        dynamiteInFlight = false;
        state = HookState.RETRACTING;
        stateElapsed = 0f;
    }

    public boolean canReceiveDynamite() {
        return state == HookState.RETRACTING && attachedItem != null && !dynamiteInFlight;
    }

    public void markDynamiteInFlight(boolean inFlight) {
        this.dynamiteInFlight = inFlight;
    }

    /**
     * Destroys the currently reeled item when the flying dynamite reaches the hook tip.
     */
    public MineItem blastHeldItem() {
        dynamiteInFlight = false;
        if (state != HookState.RETRACTING || attachedItem == null) {
            return null;
        }
        MineItem blasted = attachedItem;
        attachedItem = null;
        fastRetractOverride = true;
        return blasted;
    }

    /**
     * Advances hook physics for {@code dt} seconds.
     *
     * @return item delivered back to the cart on this frame, or null.
     */
    public MineItem update(float dt, RunSession session) {
        stateElapsed += dt;
        switch (state) {
            case SWINGING -> {
                swingTime += dt;
                angleDeg = HookPhysics.computeSwingAngleDeg(
                    swingTime,
                    config.swingMaxAngleDeg,
                    config.swingPeriodSec,
                    0f
                );
                cableLength = config.restCableLength;
                updateTipPosition();
                return null;
            }
            case EXTENDING -> {
                cableLength += config.extendSpeed * dt;
                updateTipPosition();
                if (cableLength >= config.maxCableLength
                    || tip.x <= 0f
                    || tip.x >= WorldBounds.WORLD_WIDTH
                    || tip.y <= 0f) {
                    startEmptyRetract();
                }
                return null;
            }
            case BRAKING -> {
                brakeTimer -= dt;
                if (brakeTimer <= 0f) {
                    brakeTimer = 0f;
                    fastRetractOverride = true;
                    state = HookState.RETRACTING;
                    stateElapsed = 0f;
                }
                return null;
            }
            case RETRACTING -> {
                int weight = attachedItem != null ? attachedItem.getEffectiveWeight() : -1;
                boolean emptyOrBlasted = attachedItem == null || fastRetractOverride;
                float speed = HookPhysics.computeRetractSpeed(
                    session.getMinerStrength(),
                    weight,
                    session.hasCableBonus(),
                    session.hasSuperStrength(),
                    emptyOrBlasted,
                    config.retractSpeedPerStrengthUnit,
                    config.emptyRetractSpeed
                );
                cableLength -= speed * dt;
                if (cableLength <= config.restCableLength) {
                    cableLength = config.restCableLength;
                    updateTipPosition();
                    MineItem delivered = attachedItem;
                    attachedItem = null;
                    fastRetractOverride = false;
                    dynamiteInFlight = false;
                    state = HookState.SWINGING;
                    stateElapsed = 0f;
                    return delivered;
                }
                updateTipPosition();
                return null;
            }
        }
        return null;
    }

    private void updateTipPosition() {
        float rad = angleDeg * MathUtils.degreesToRadians;
        tip.set(
            pivot.x + MathUtils.sin(rad) * cableLength,
            pivot.y - MathUtils.cos(rad) * cableLength
        );
        tipCircle.setPosition(tip.x, tip.y);
    }

    public HookState getState() {
        return state;
    }

    public float getStateElapsed() {
        return stateElapsed;
    }

    public Vector2 getPivot() {
        return pivot;
    }

    public Vector2 getTip() {
        return tip;
    }

    public Circle getTipCircle() {
        return tipCircle;
    }

    public float getAngleDeg() {
        return angleDeg;
    }

    public float getCableLength() {
        return cableLength;
    }

    public MineItem getAttachedItem() {
        return attachedItem;
    }

    public boolean isBusy() {
        return state != HookState.SWINGING;
    }
}
