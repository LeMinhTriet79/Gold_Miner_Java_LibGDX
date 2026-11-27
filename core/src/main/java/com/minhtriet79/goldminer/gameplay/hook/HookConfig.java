package com.minhtriet79.goldminer.gameplay.hook;

import com.minhtriet79.goldminer.domain.WorldBounds;

/**
 * Data-driven hook & cart tuning loaded from {@code assets/data/hook_config.json}.
 */
public class HookConfig {

    public float swingMaxAngleDeg = 80f;
    public float swingPeriodSec = 5.556f;
    public float extendSpeed = 106.6f;
    public float emptyRetractSpeed = 242.2f;
    public float retractSpeedPerStrengthUnit = 15.14f;
    public int baseMinerStrength = 10;
    public float cartSpeed = 24.3f;
    public float hookTipRadius = WorldBounds.DEFAULT_HOOK_TIP_RADIUS;
    public float restCableLength = 12.0f;
    public float maxCableLength = 270.0f;
    public float tntBlastRadius = 40.5f;
    public int countdownWarningSec = 5;
    public int defaultDurationSec = 60;
    public int startingDynamite = 3;

    public HookConfig() {
    }
}
