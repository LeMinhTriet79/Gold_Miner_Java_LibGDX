package com.minhtriet79.goldminer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.minhtriet79.goldminer.GoldMinerGame;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.assets.ThemeConfig;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.domain.hook.ClawType;
import com.minhtriet79.goldminer.domain.item.ItemPricing;
import com.minhtriet79.goldminer.domain.item.MysteryBagOutcome;
import com.minhtriet79.goldminer.domain.item.MysteryBagResolver;
import com.minhtriet79.goldminer.domain.run.RunSession;
import com.minhtriet79.goldminer.gameplay.hook.Hook;
import com.minhtriet79.goldminer.gameplay.hook.HookConfig;
import com.minhtriet79.goldminer.gameplay.hook.HookState;
import com.minhtriet79.goldminer.gameplay.ui.VegasButton;
import com.minhtriet79.goldminer.gameplay.world.ExplosionFx;
import com.minhtriet79.goldminer.gameplay.world.FloatingText;
import com.minhtriet79.goldminer.gameplay.world.FlyingDynamite;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig;
import com.minhtriet79.goldminer.gameplay.world.MineItem;
import com.minhtriet79.goldminer.gameplay.world.ScreenShake;
import com.minhtriet79.goldminer.studio.StudioMode;
import com.minhtriet79.goldminer.studio.io.StudioFileHelper;

import java.util.Comparator;

/**
 * Playable World 1 (Australia, Levels 1..6) Vegas mining screen with forensically verified
 * Miner & 4-Claw states, authentic HUD buttons, Dynamite icons, creature patrol AI,
 * TNT chain blasts, flying dynamite, fly-to-TOTAL cash effects, audio, and F1 Debug bar.
 */
public class PlayScreen implements Screen {

    private static final float CART_DRAW_WIDTH = 42f;
    private static final float CART_DRAW_HEIGHT = 25.5f;
    private static final float CABLE_THICKNESS = 1.4f;
    private static final float THROW_ANIM_DURATION = 9f / 18f;
    private static final float STRENGTH_ANIM_DURATION = 10f / 18f;
    private static final float CLAW_WORLD_SCALE = 0.56f;
    private static final float TOTAL_SIGN_CENTER_X = 366f;
    private static final float TOTAL_SIGN_CENTER_Y = 247f;

    private static final int MAX_EXPLOSIONS = 24;
    private static final int MAX_FLOATING_TEXTS = 24;

    private final GoldMinerGame game;
    private final String levelFileName;
    private final Runnable backToStudioAction;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Json json = new Json();

    private final HookConfig hookConfig;
    private final LevelConfig levelConfig;
    private final Hook hook;
    private final Array<MineItem> items = new Array<>(36);
    private final int[] blastQueue = new int[64];
    private final boolean levelHasGems;

    private final ExplosionFx[] fxPool = new ExplosionFx[MAX_EXPLOSIONS];
    private final FloatingText[] floatingTextPool = new FloatingText[MAX_FLOATING_TEXTS];
    private final FlyingDynamite flyingDynamite = new FlyingDynamite();
    private final ScreenShake screenShake = new ScreenShake();

    // Zero-allocation scratch objects & cached TextureRegions
    private final Vector3 touchPoint = new Vector3();
    private final Vector2 cableDelta = new Vector2();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final TextureRegion[][] clawRegions = new TextureRegion[4][3];

    // Authentic Vegas Top-HUD Buttons (adjusted 16:9 layout without bottom toolbar)
    private final VegasButton pauseButton;
    private final VegasButton soundButton;
    private final VegasButton menuButton;
    private final VegasButton earlyNextButton;
    private final Rectangle backToStudioRect = new Rectangle(4f, 255f, 96f, 12f);

    // Debug strip hitboxes (when toggled on via F1)
    private final Rectangle dbgNextRect = new Rectangle(6f, 2f, 82f, 12f);
    private final Rectangle dbgCycleRect = new Rectangle(94f, 2f, 76f, 12f);
    private final Rectangle dbgRichRect = new Rectangle(176f, 2f, 94f, 12f);
    private final Rectangle dbgTimeRect = new Rectangle(276f, 2f, 74f, 12f);

    private float cartX;
    private float cartMinX;
    private float cartMaxX;
    private float cartClickTargetX;
    private boolean cartMovingToClick;
    private float cartSfxTimer;
    private int cartSfxStep;

    private String activeMinerAnimKey = AssetKey.MINER_IDLE;
    private float minerAnimTime;
    private float fxTime;
    private float throwAnimTimer;
    private float strengthAnimTimer;
    private float timerPulseTimer;
    private boolean paused;
    private boolean transitionTriggered;
    private boolean debugBarVisible;
    private int earnedThisLevel;

    private long retractSoundId = -1L;
    private MysteryBagOutcome pendingMysteryOutcome;
    private String mysteryBannerText = "";
    private float mysteryBannerTimer;

    // Cached HUD strings (only re-formatted on change)
    private int cachedGoal = -1;
    private int cachedScore = -1;
    private int cachedSeconds = -1;
    private String goalText = "$555";
    private String scoreText = "$0";
    private String timerText = "60";
    private final String levelBadgeText;

    public PlayScreen(GoldMinerGame game, int levelNumber) {
        this(game, levelNumber, "level_" + levelNumber + ".json", null);
    }

    public PlayScreen(GoldMinerGame game, int levelNumber, String levelFileName, Runnable backToStudioAction) {
        this.game = game;
        this.levelFileName = (levelFileName == null || levelFileName.isEmpty())
            ? ("level_" + levelNumber + ".json")
            : levelFileName;
        this.backToStudioAction = backToStudioAction;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT, camera);
        this.viewport.apply(true);

        this.hookConfig = json.fromJson(HookConfig.class, StudioFileHelper.resolveHookConfigFile());
        this.levelConfig = json.fromJson(
            LevelConfig.class,
            StudioFileHelper.resolveLevelFile(this.levelFileName)
        );

        RunSession run = game.getRunSession();
        if (backToStudioAction != null) {
            int customDur = levelConfig.durationSeconds > 0 ? levelConfig.durationSeconds : hookConfig.defaultDurationSec;
            run.prepareCustomPlayTest(levelNumber, levelConfig.goal, customDur);
        } else {
            run.prepareLevel(levelNumber, levelConfig.levelValue);
            if (levelConfig.durationSeconds > 0 && levelConfig.durationSeconds != 60) {
                run.resetLevelTimer(levelConfig.durationSeconds + run.getExtraTimeSeconds());
            }
        }
        this.levelBadgeText = "LVL " + levelNumber + "/" + RunSession.MAX_W1_LEVEL;

        this.hook = new Hook(hookConfig, WorldBounds.WORLD_WIDTH * 0.5f, WorldBounds.HOOK_PIVOT_Y);

        for (int i = 0; i < MAX_EXPLOSIONS; i++) {
            fxPool[i] = new ExplosionFx();
        }
        for (int i = 0; i < MAX_FLOATING_TEXTS; i++) {
            floatingTextPool[i] = new FloatingText();
        }

        Assets assets = game.getAssets();
        for (int c = 0; c < 4; c++) {
            int clawId = c + 1;
            clawRegions[c][0] = new TextureRegion(assets.getTexture(AssetKey.clawKey(clawId, "closed")));
            clawRegions[c][1] = new TextureRegion(assets.getTexture(AssetKey.clawKey(clawId, "open")));
            clawRegions[c][2] = new TextureRegion(assets.getTexture(AssetKey.clawKey(clawId, "retract")));
        }

        this.pauseButton = new VegasButton(
            112f, 254f, 38f, 12f,
            AssetKey.BTN_PAUSE_UP, AssetKey.BTN_PAUSE_OVER, AssetKey.BTN_PAUSE_DOWN
        );
        this.soundButton = new VegasButton(
            152f, 254f, 16f, 12f,
            AssetKey.BTN_SOUND_ON_UP, AssetKey.BTN_SOUND_ON_OVER, AssetKey.BTN_SOUND_ON_DOWN
        );
        this.menuButton = new VegasButton(
            170f, 254f, 54f, 12f,
            AssetKey.BTN_MENU_UP, AssetKey.BTN_MENU_OVER, AssetKey.BTN_MENU_DOWN
        );
        this.earlyNextButton = new VegasButton(
            200f, 237f, 58f, 13f,
            AssetKey.BTN_NEXT_UP, AssetKey.BTN_NEXT_OVER, AssetKey.BTN_NEXT_DOWN
        );
        this.earlyNextButton.visible = false;
        syncSoundButtonTexture();

        boolean gemsPresent = false;
        for (int i = 0; i < levelConfig.items.size; i++) {
            MineItem item = new MineItem(levelConfig.items.get(i));
            if (item.isGem()) {
                gemsPresent = true;
            }
            items.add(item);
        }
        this.levelHasGems = gemsPresent;
        this.items.sort(Comparator.comparingInt(m -> m.zIndex));

        initCartAndHook();
        syncCachedHudStrings();
    }

    private void initCartAndHook() {
        cartMinX = levelConfig.cartRange != null ? levelConfig.cartRange.minX : WorldBounds.CART_MIN_X;
        cartMaxX = levelConfig.cartRange != null ? levelConfig.cartRange.maxX : WorldBounds.CART_MAX_X;
        cartX = MathUtils.clamp(WorldBounds.WORLD_WIDTH * 0.5f, cartMinX, cartMaxX);
        cartMovingToClick = false;
        hook.reset(cartX, WorldBounds.HOOK_PIVOT_Y);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
        game.playMusic(AssetKey.BGM_W1, 0.42f);
    }

    @Override
    public void render(float rawDt) {
        float dt = Math.min(rawDt, 0.05f);
        if (handleInput() || transitionTriggered) {
            return;
        }

        if (!paused) {
            updateGameplay(dt);
        }

        if (!transitionTriggered) {
            renderWorld();
        }
    }

    private boolean handleInput() {
        RunSession run = game.getRunSession();

        if (backToStudioAction != null && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            transitionTriggered = true;
            stopRetractSound();
            backToStudioAction.run();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F10)) {
            transitionTriggered = true;
            stopRetractSound();
            if (backToStudioAction != null) {
                backToStudioAction.run();
            } else {
                game.openStudio(StudioMode.LEVEL_MODE, levelFileName);
            }
            return true;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            debugBarVisible = !debugBarVisible;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            triggerDebugWinToShop();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            triggerDebugNextLevelDirect();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            triggerDebugCashAndTnt();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            run.forceTimeRemaining(6f);
            syncCachedHudStrings();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && !run.isLevelFinished()) {
            togglePause();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            game.toggleMuted();
            syncSoundButtonTexture();
            if (game.isMuted()) {
                stopRetractSound();
            }
        }

        if (Gdx.input.justTouched()) {
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            viewport.unproject(touchPoint);

            if (backToStudioAction != null && backToStudioRect.contains(touchPoint.x, touchPoint.y)) {
                transitionTriggered = true;
                stopRetractSound();
                backToStudioAction.run();
                return true;
            }

            if (debugBarVisible) {
                if (dbgNextRect.contains(touchPoint.x, touchPoint.y)) {
                    triggerDebugWinToShop();
                    return true;
                }
                if (dbgCycleRect.contains(touchPoint.x, touchPoint.y)) {
                    triggerDebugNextLevelDirect();
                    return true;
                }
                if (dbgRichRect.contains(touchPoint.x, touchPoint.y)) {
                    triggerDebugCashAndTnt();
                    return false;
                }
                if (dbgTimeRect.contains(touchPoint.x, touchPoint.y)) {
                    run.forceTimeRemaining(6f);
                    syncCachedHudStrings();
                    return false;
                }
            }

            if (pauseButton.contains(touchPoint.x, touchPoint.y)) {
                if (!run.isLevelFinished()) {
                    togglePause();
                }
                return false;
            }
            if (soundButton.contains(touchPoint.x, touchPoint.y)) {
                game.toggleMuted();
                syncSoundButtonTexture();
                if (game.isMuted()) {
                    stopRetractSound();
                }
                return false;
            }
            if (menuButton.contains(touchPoint.x, touchPoint.y)) {
                transitionTriggered = true;
                stopRetractSound();
                game.showSettlement(false);
                return true;
            }
            if (earlyNextButton.contains(touchPoint.x, touchPoint.y)) {
                finishEarlyAsPass();
                return true;
            }

            if (!paused && !run.isLevelFinished()) {
                handleWorldPointerDown(touchPoint.x, touchPoint.y);
            }
        }
        return false;
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            stopRetractSound();
        } else if (hook.getState() == HookState.RETRACTING) {
            MineItem held = hook.getAttachedItem();
            startRetractSound(held != null ? held.getEffectiveWeight() : 0);
        }
    }

    private void syncSoundButtonTexture() {
        if (game.isMuted()) {
            soundButton.setKeys(AssetKey.BTN_SOUND_OFF, AssetKey.BTN_SOUND_OFF, AssetKey.BTN_SOUND_OFF);
        } else {
            soundButton.setKeys(AssetKey.BTN_SOUND_ON_UP, AssetKey.BTN_SOUND_ON_OVER, AssetKey.BTN_SOUND_ON_DOWN);
        }
    }

    private void handleWorldPointerDown(float worldX, float worldY) {
        if (worldY >= WorldBounds.MINE_BOTTOM_Y && worldY < WorldBounds.MINE_TOP_Y) {
            cartMovingToClick = false;
            tryLaunchHook();
        } else if (worldY >= WorldBounds.MINE_TOP_Y && worldY < WorldBounds.HUD_BOTTOM_Y) {
            if (hook.getState() == HookState.SWINGING) {
                cartClickTargetX = MathUtils.clamp(worldX, cartMinX, cartMaxX);
                cartMovingToClick = true;
            }
        }
    }

    private void finishEarlyAsPass() {
        RunSession run = game.getRunSession();
        if (!run.isGoalMet() || transitionTriggered) {
            return;
        }
        transitionTriggered = true;
        stopRetractSound();
        game.showLevelResult(true, earnedThisLevel);
    }

    private void triggerDebugWinToShop() {
        transitionTriggered = true;
        stopRetractSound();
        RunSession run = game.getRunSession();
        int added = Math.max(0, run.getGoal() - run.getMineMoney());
        earnedThisLevel += added;
        run.finishImmediatelyAsPass();
        game.showLevelResult(true, earnedThisLevel);
    }

    private void triggerDebugNextLevelDirect() {
        transitionTriggered = true;
        stopRetractSound();
        int next = (game.getRunSession().getCurrentLevel() % RunSession.MAX_W1_LEVEL) + 1;
        game.startLevel(next);
    }

    private void triggerDebugCashAndTnt() {
        RunSession run = game.getRunSession();
        run.addMineMoney(1000);
        run.addDynamite(3);
        earnedThisLevel += 1000;
        spawnFloatingText("+$1000 / +3 TNT", cartX, WorldBounds.TRACK_Y + 34f, Color.CYAN, 1.0f);
        syncCachedHudStrings();
    }

    private void updateGameplay(float dt) {
        RunSession run = game.getRunSession();
        fxTime += dt;
        screenShake.update(dt);

        if (throwAnimTimer > 0f) {
            throwAnimTimer = Math.max(0f, throwAnimTimer - dt);
        }
        if (strengthAnimTimer > 0f) {
            strengthAnimTimer = Math.max(0f, strengthAnimTimer - dt);
        }
        if (timerPulseTimer > 0f) {
            timerPulseTimer = Math.max(0f, timerPulseTimer - dt);
        }
        if (mysteryBannerTimer > 0f) {
            mysteryBannerTimer = Math.max(0f, mysteryBannerTimer - dt);
        }

        for (int i = 0; i < MAX_EXPLOSIONS; i++) {
            fxPool[i].update(dt);
        }
        for (int i = 0; i < MAX_FLOATING_TEXTS; i++) {
            floatingTextPool[i].update(dt);
        }

        // Update patrol creatures
        for (int i = 0; i < items.size; i++) {
            MineItem item = items.get(i);
            if (!item.active) {
                continue;
            }
            item.updateCreature(dt);
            if (item.objectN == 27 || item.objectN == 28) {
                checkTasmanianDestroyNearby(item);
            }
        }

        // Update in-flight dynamite chasing hook tip
        if (flyingDynamite.active && flyingDynamite.update(dt, hook.getTip().x, hook.getTip().y)) {
            MineItem blasted = hook.blastHeldItem();
            if (blasted != null) {
                pendingMysteryOutcome = null;
                spawnExplosionAndSmoke(hook.getTip().x, hook.getTip().y);
                screenShake.trigger(3.8f, 0.28f);
                spawnFloatingText("BOOM!", hook.getTip().x, hook.getTip().y + 10f, Color.ORANGE, 0.85f);
                playSound(AssetKey.SFX_EXPLOSION, 0.85f);
            }
        }

        if (run.isLevelFinished()) {
            transitionTriggered = true;
            stopRetractSound();
            game.showLevelResult(run.isLevelPassed(), earnedThisLevel);
            return;
        }

        updateCartMovement(dt, run);
        handleActionKeys();

        HookState prevState = hook.getState();
        MineItem delivered = hook.update(dt, run);

        if (hook.getState() == HookState.EXTENDING) {
            checkHookCollisions(run);
        }

        if (prevState == HookState.RETRACTING && hook.getState() != HookState.RETRACTING) {
            stopRetractSound();
        }

        if (delivered != null) {
            onItemDelivered(delivered, run);
        }

        boolean shouldBeep = run.tickTimer(dt, hook.isBusy() || flyingDynamite.active, hookConfig.countdownWarningSec);
        if (shouldBeep) {
            timerPulseTimer = 0.45f;
            playSound(AssetKey.SFX_COUNTDOWN_BEEP, 0.75f);
        }

        earlyNextButton.visible = run.isGoalMet();
        advanceMinerAnimationState(dt);
        syncCachedHudStrings();
    }

    private void checkTasmanianDestroyNearby(MineItem devil) {
        for (int i = 0; i < items.size; i++) {
            MineItem other = items.get(i);
            if (!other.active || other == devil || other.moveSpeed > 0f) {
                continue;
            }
            if (WorldBounds.circlesOverlap(devil.bounds.x, devil.bounds.y, devil.radius, other.bounds.x, other.bounds.y, other.radius)) {
                other.active = false;
                spawnSmoke(other.bounds.x, other.bounds.y);
                return;
            }
        }
    }

    private void advanceMinerAnimationState(float dt) {
        String desiredKey = resolveMinerAnimationKey();
        if (!desiredKey.equals(activeMinerAnimKey)) {
            activeMinerAnimKey = desiredKey;
            minerAnimTime = 0f;
        } else {
            minerAnimTime += dt;
        }
    }

    private String resolveMinerAnimationKey() {
        if (throwAnimTimer > 0f) {
            return AssetKey.MINER_THROW;
        }
        if (strengthAnimTimer > 0f) {
            return AssetKey.MINER_STRENGTH;
        }
        return switch (hook.getState()) {
            case SWINGING -> AssetKey.MINER_IDLE;
            case EXTENDING -> AssetKey.MINER_DOWN;
            case RETRACTING -> AssetKey.MINER_UP;
            case BRAKING -> AssetKey.MINER_BRAKE;
        };
    }

    private void updateCartMovement(float dt, RunSession run) {
        if (hook.getState() != HookState.SWINGING) {
            cartMovingToClick = false;
            return;
        }

        float speed = hookConfig.cartSpeed * run.getCartSpeedMultiplier();
        float moveDir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveDir -= 1f;
            cartMovingToClick = false;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveDir += 1f;
            cartMovingToClick = false;
        }

        float prevCartX = cartX;
        if (moveDir != 0f) {
            cartX = MathUtils.clamp(cartX + moveDir * speed * dt, cartMinX, cartMaxX);
        } else if (cartMovingToClick) {
            float diff = cartClickTargetX - cartX;
            float step = speed * dt;
            if (Math.abs(diff) <= step) {
                cartX = cartClickTargetX;
                cartMovingToClick = false;
            } else {
                cartX += Math.signum(diff) * step;
            }
        }

        if (Math.abs(cartX - prevCartX) > 0.001f) {
            cartSfxTimer -= dt;
            if (cartSfxTimer <= 0f) {
                cartSfxTimer = 0.28f;
                playCartRollSfx();
            }
        } else {
            cartSfxTimer = 0f;
        }

        hook.setPivotX(cartX);
    }

    private void playCartRollSfx() {
        String key = switch (cartSfxStep % 3) {
            case 0 -> AssetKey.SFX_CART_1;
            case 1 -> AssetKey.SFX_CART_2;
            default -> AssetKey.SFX_CART_3;
        };
        cartSfxStep++;
        playSound(key, 0.30f);
    }

    private void handleActionKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.S)
            || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            tryLaunchHook();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            tryThrowDynamite();
        }
    }

    private void tryLaunchHook() {
        if (hook.launch()) {
            cartMovingToClick = false;
            playSound(AssetKey.SFX_HOOK_EXTEND, 0.65f);
        }
    }

    private void tryThrowDynamite() {
        if (!hook.canReceiveDynamite()) {
            return;
        }
        if (!game.getRunSession().consumeDynamite()) {
            return;
        }
        hook.markDynamiteInFlight(true);
        throwAnimTimer = THROW_ANIM_DURATION;
        flyingDynamite.launch(cartX, WorldBounds.TRACK_Y + 20f);
    }

    private void checkHookCollisions(RunSession run) {
        Vector2 tip = hook.getTip();
        float tipRadius = hook.getTipCircle().radius;

        for (int i = 0; i < items.size; i++) {
            MineItem item = items.get(i);
            if (!item.active) {
                continue;
            }
            if (WorldBounds.circlesOverlap(tip.x, tip.y, tipRadius, item.bounds.x, item.bounds.y, item.radius)) {
                onHookHitItem(item, run);
                return;
            }
        }
    }

    private void onHookHitItem(MineItem item, RunSession run) {
        if (item.isTnt()) {
            item.active = false;
            triggerExplosionChain(item.bounds.x, item.bounds.y);
            hook.startEmptyRetract();
            startRetractSound(0);
            return;
        }

        // Spike Claw rules (DefineSprite_2863): smash rocks/bones on contact & keep extending; knock back creatures
        if (run.getActiveClaw() == ClawType.SPIKE) {
            if (ClawType.isShatteredBySpike(item.objectN)) {
                item.active = false;
                spawnSmoke(item.bounds.x, item.bounds.y);
                spawnFloatingText("VO NAT!", item.bounds.x, item.bounds.y + 6f, Color.LIGHT_GRAY, 0.75f);
                playSound(AssetKey.SFX_HEAVY_GRAB, 0.55f);
                return;
            }
            if (ClawType.isKnockedBackBySpike(item.objectN)) {
                item.knockBackLeft(9f);
                spawnSmoke(item.bounds.x, item.bounds.y);
                playSound(AssetKey.SFX_HEAVY_GRAB, 0.45f);
                return;
            }
        }

        if (item.isMysteryBag()) {
            pendingMysteryOutcome = MysteryBagResolver.resolve(run.hasCloverBonus(), bound -> MathUtils.random(bound - 1));
            item.resolvedWeight = pendingMysteryOutcome.weight;
        } else {
            pendingMysteryOutcome = null;
            item.resolvedWeight = item.weight;
        }

        boolean broke = hook.attachItem(item, run);
        if (broke) {
            pendingMysteryOutcome = null;
            spawnSmoke(cartX, WorldBounds.TRACK_Y + 18f);
            playSound(AssetKey.SFX_CABLE_BRAKE, 0.85f);
        } else {
            if (item.getEffectiveWeight() >= 8) {
                playSound(AssetKey.SFX_HEAVY_GRAB, 0.7f);
            }
            startRetractSound(item.getEffectiveWeight());
        }
    }

    private void triggerExplosionChain(float originX, float originY) {
        int queueHead = 0;
        int queueTail = 0;

        spawnExplosionAndSmoke(originX, originY);
        screenShake.trigger(4.2f, 0.36f);
        playSound(AssetKey.SFX_EXPLOSION, 0.9f);
        queueTail = enqueueExplosionSweep(originX, originY, hookConfig.tntBlastRadius, queueTail);

        while (queueHead < queueTail) {
            int tntIndex = blastQueue[queueHead++];
            MineItem chainedTnt = items.get(tntIndex);
            spawnExplosionAndSmoke(chainedTnt.bounds.x, chainedTnt.bounds.y);
            playSound(AssetKey.SFX_EXPLOSION, 0.85f);
            queueTail = enqueueExplosionSweep(
                chainedTnt.bounds.x,
                chainedTnt.bounds.y,
                hookConfig.tntBlastRadius,
                queueTail
            );
        }
    }

    private int enqueueExplosionSweep(float cx, float cy, float blastRadius, int queueTail) {
        for (int i = 0; i < items.size; i++) {
            MineItem target = items.get(i);
            if (!target.active) {
                continue;
            }
            if (WorldBounds.circlesOverlap(cx, cy, blastRadius, target.bounds.x, target.bounds.y, target.radius)) {
                target.active = false;
                spawnSmoke(target.bounds.x, target.bounds.y);
                if (target.isTnt() && queueTail < blastQueue.length) {
                    blastQueue[queueTail++] = i;
                }
            }
        }
        return queueTail;
    }

    private void onItemDelivered(MineItem item, RunSession run) {
        if (item.isMysteryBag() && pendingMysteryOutcome != null) {
            MysteryBagOutcome outcome = pendingMysteryOutcome;
            pendingMysteryOutcome = null;
            switch (outcome.rewardType) {
                case MONEY -> {
                    awardMoney(outcome.moneyAmount, AssetKey.SFX_COIN);
                    showMysteryBanner("TUI BI AN: +$" + outcome.moneyAmount);
                }
                case DYNAMITE -> {
                    run.addDynamite(1);
                    spawnFloatingText("+1 THUOC NO!", cartX, WorldBounds.TRACK_Y + 34f, Color.ORANGE, 1.15f);
                    showMysteryBanner("TUI BI AN: +1 THUOC NO!");
                    playSound(AssetKey.SFX_TALLY, 0.8f);
                }
                case POWER -> {
                    run.activateSuperStrength();
                    strengthAnimTimer = STRENGTH_ANIM_DURATION;
                    spawnFloatingText("SIEU SUC MANH!", cartX, WorldBounds.TRACK_Y + 34f, Color.CYAN, 1.25f);
                    showMysteryBanner("TUI BI AN: SIEU SUC MANH + SUPER CLAW!");
                    playSound(AssetKey.SFX_GEM_COMBO, 0.85f);
                }
                case LASER -> {
                    run.setActiveClaw(ClawType.LASER);
                    spawnFloatingText("NGOAM LASER!", cartX, WorldBounds.TRACK_Y + 34f, Color.SCARLET, 1.2f);
                    showMysteryBanner("TUI BI AN: TIA NGAM LASER CLAW!");
                    playSound(AssetKey.SFX_TALLY, 0.8f);
                }
                case SPIKE -> {
                    run.setActiveClaw(ClawType.SPIKE);
                    spawnFloatingText("NGOAM GAI!", cartX, WorldBounds.TRACK_Y + 34f, Color.LIME, 1.2f);
                    showMysteryBanner("TUI BI AN: NGOAM GAI PHA DA!");
                    playSound(AssetKey.SFX_TALLY, 0.8f);
                }
            }
            return;
        }

        int value = ItemPricing.resolveEffectiveValue(item.objectN, item.value, run.hasDiamondBonus());
        if (item.isGem()) {
            boolean comboCompleted = run.recordGemCollected(item.objectN);
            awardMoney(value, AssetKey.SFX_GEM_HIT);
            if (comboCompleted) {
                run.addMineMoney(ItemPricing.GEM_COMBO_BONUS);
                earnedThisLevel += ItemPricing.GEM_COMBO_BONUS;
                spawnFlyToTotalText("+$2000 GEM COMBO!", cartX, WorldBounds.TRACK_Y + 46f, Color.MAGENTA);
                showMysteryBanner("DU BO 4 DA QUY: +$2,000 COMBO!");
                playSound(AssetKey.SFX_GEM_COMBO, 0.95f);
            }
            return;
        }

        if (value > 0) {
            awardMoney(value, AssetKey.SFX_COIN);
        }
    }

    private void awardMoney(int amount, String sfxKey) {
        game.getRunSession().addMineMoney(amount);
        earnedThisLevel += amount;
        spawnFlyToTotalText("+$" + amount, cartX, WorldBounds.TRACK_Y + 32f, Color.GOLD);
        playSound(sfxKey, 0.8f);
    }

    private void showMysteryBanner(String text) {
        mysteryBannerText = text;
        mysteryBannerTimer = 2.0f;
    }

    private void spawnExplosionAndSmoke(float x, float y) {
        spawnFx(AssetKey.FX_EXPLOSION, x, y);
        spawnSmoke(x, y);
    }

    private void spawnSmoke(float x, float y) {
        spawnFx(AssetKey.FX_SMOKE, x, y);
    }

    private void spawnFx(String animKey, float x, float y) {
        ThemeConfig.AnimationDef def = game.getAssets().getAnimationDef(animKey);
        float duration = def.frameDuration * def.frames.size;
        for (int i = 0; i < MAX_EXPLOSIONS; i++) {
            if (!fxPool[i].active) {
                fxPool[i].trigger(animKey, x, y, duration);
                return;
            }
        }
    }

    private void spawnFloatingText(String text, float x, float y, Color color, float duration) {
        for (int i = 0; i < MAX_FLOATING_TEXTS; i++) {
            if (!floatingTextPool[i].active) {
                floatingTextPool[i].spawn(text, x, y, color, duration);
                return;
            }
        }
    }

    private void spawnFlyToTotalText(String text, float x, float y, Color color) {
        for (int i = 0; i < MAX_FLOATING_TEXTS; i++) {
            if (!floatingTextPool[i].active) {
                floatingTextPool[i].spawnFlyToTarget(
                    text, x, y, TOTAL_SIGN_CENTER_X, TOTAL_SIGN_CENTER_Y, color, 0.35f, 0.55f
                );
                return;
            }
        }
    }

    private void syncCachedHudStrings() {
        RunSession run = game.getRunSession();
        if (cachedGoal != run.getGoal()) {
            cachedGoal = run.getGoal();
            goalText = "$" + cachedGoal;
        }
        if (cachedScore != run.getMineMoney()) {
            cachedScore = run.getMineMoney();
            scoreText = "$" + cachedScore;
        }
        int secs = run.getDisplayedSeconds();
        if (cachedSeconds != secs) {
            cachedSeconds = secs;
            timerText = Integer.toString(secs);
        }
    }

    private void playSound(String assetKey, float volume) {
        if (game.isMuted()) {
            return;
        }
        game.getAssets().getSound(assetKey).play(volume);
    }

    private void startRetractSound(int weight) {
        if (game.isMuted()) {
            return;
        }
        stopRetractSound();
        Sound sound = game.getAssets().getSound(AssetKey.SFX_HOOK_RETRACT);
        retractSoundId = sound.loop(0.45f);
        float pitch = MathUtils.clamp(1.12f - Math.max(0, weight) * 0.055f, 0.55f, 1.12f);
        sound.setPitch(retractSoundId, pitch);
    }

    private void stopRetractSound() {
        if (retractSoundId != -1L) {
            game.getAssets().getSound(AssetKey.SFX_HOOK_RETRACT).stop(retractSoundId);
            retractSoundId = -1L;
        }
    }

    private void renderWorld() {
        ScreenUtils.clear(0.08f, 0.06f, 0.04f, 1f);
        camera.position.set(WorldBounds.WORLD_WIDTH * 0.5f + screenShake.offsetX, WorldBounds.WORLD_HEIGHT * 0.5f + screenShake.offsetY, 0f);
        camera.update();
        viewport.apply();

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        RunSession run = game.getRunSession();

        Texture sky = assets.getTexture(AssetKey.BG_SKY_W1);
        Texture cave = assets.getTexture(AssetKey.BG_CAVE_W1);
        Texture mine = assets.getTexture(AssetKey.BG_MINE_W1);
        Texture track = assets.getTexture(AssetKey.trackKey(levelConfig.trackN));
        Texture cart = assets.getTexture(AssetKey.CART_STANDARD);

        Animation<TextureRegion> minerAnim = assets.getAnimation(activeMinerAnimKey);
        ThemeConfig.AnimationDef minerDef = assets.getAnimationDef(activeMinerAnimKey);
        TextureRegion minerFrame = minerAnim.getKeyFrame(minerAnimTime, minerDef.isLooping());

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 1. Backgrounds & level-authentic Track
        batch.draw(mine, 0f, WorldBounds.MINE_BOTTOM_Y, WorldBounds.WORLD_WIDTH, WorldBounds.MINE_TOP_Y);
        batch.draw(cave, 0f, WorldBounds.MINE_TOP_Y, WorldBounds.WORLD_WIDTH, WorldBounds.HUD_BOTTOM_Y - WorldBounds.MINE_TOP_Y);
        batch.draw(track, 0f, WorldBounds.TRACK_Y - 1.5f, WorldBounds.WORLD_WIDTH, 32f);
        batch.draw(sky, 0f, WorldBounds.HUD_BOTTOM_Y, WorldBounds.WORLD_WIDTH, WorldBounds.HUD_TOP_HEIGHT);

        // 2. Mine items & animated creatures
        for (int i = 0; i < items.size; i++) {
            MineItem item = items.get(i);
            if (!item.active) {
                continue;
            }
            if (item.animated) {
                Animation<TextureRegion> anim = assets.getAnimation(item.assetKey);
                TextureRegion frame = anim.getKeyFrame(item.animTime, true);
                batch.draw(
                    frame,
                    item.bounds.x - item.drawWidth * 0.5f,
                    item.bounds.y - item.drawHeight * 0.5f,
                    item.drawWidth * 0.5f,
                    item.drawHeight * 0.5f,
                    item.drawWidth,
                    item.drawHeight,
                    item.facingRight ? -1f : 1f,
                    1f,
                    0f
                );
            } else {
                Texture tex = assets.getTexture(item.assetKey);
                batch.draw(
                    tex,
                    item.bounds.x - item.drawWidth * 0.5f,
                    item.bounds.y - item.drawHeight * 0.5f,
                    item.drawWidth,
                    item.drawHeight
                );
            }
        }

        // 3. Miner (spool-anchored proportionally) & Cart
        float minerDrawX = cartX - minerDef.drawWidth * minerDef.anchorX;
        float minerDrawY = WorldBounds.TRACK_Y + 11f - minerDef.drawHeight * minerDef.anchorY;
        batch.draw(minerFrame, minerDrawX, minerDrawY, minerDef.drawWidth, minerDef.drawHeight);

        batch.draw(
            cart,
            cartX - CART_DRAW_WIDTH * 0.5f,
            WorldBounds.TRACK_Y + 2f,
            CART_DRAW_WIDTH,
            CART_DRAW_HEIGHT
        );

        // 4. Cable, Laser sightline, Held item, Claw, and Flying Dynamite
        drawCableAndHook(batch, assets, run);
        if (flyingDynamite.active) {
            Animation<TextureRegion> dynAnim = assets.getAnimation(AssetKey.FX_DYNAMITE_FLY);
            ThemeConfig.AnimationDef dynDef = assets.getAnimationDef(AssetKey.FX_DYNAMITE_FLY);
            TextureRegion dynFrame = dynAnim.getKeyFrame(fxTime, true);
            batch.draw(
                dynFrame,
                flyingDynamite.x - dynDef.drawWidth * 0.5f,
                flyingDynamite.y - dynDef.drawHeight * 0.5f,
                dynDef.drawWidth,
                dynDef.drawHeight
            );
        }

        // 5. Explosions & Smoke pools
        for (int i = 0; i < MAX_EXPLOSIONS; i++) {
            ExplosionFx fx = fxPool[i];
            if (!fx.active) {
                continue;
            }
            Animation<TextureRegion> fxAnim = assets.getAnimation(fx.animKey);
            ThemeConfig.AnimationDef fxDef = assets.getAnimationDef(fx.animKey);
            TextureRegion fxFrame = fxAnim.getKeyFrame(fx.elapsed, false);
            batch.draw(
                fxFrame,
                fx.x - fxDef.drawWidth * fxDef.anchorX,
                fx.y - fxDef.drawHeight * fxDef.anchorY,
                fxDef.drawWidth,
                fxDef.drawHeight
            );
        }

        // 6. Top HUD overlays (stabilized without shake)
        drawTopHud(batch, assets, run);

        // 7. Floating Cash & Banners
        drawFloatingTexts(batch);
        if (mysteryBannerTimer > 0f) {
            drawMysteryBanner(batch);
        }

        // 8. Pause & Debug Strip
        if (paused) {
            drawPauseOverlay(batch);
        }
        if (debugBarVisible) {
            drawDebugBar(batch);
        }

        batch.end();
    }

    private void drawCableAndHook(SpriteBatch batch, Assets assets, RunSession run) {
        Vector2 pivot = hook.getPivot();
        Vector2 tip = hook.getTip();
        cableDelta.set(tip.x - pivot.x, tip.y - pivot.y);
        float len = cableDelta.len();
        Texture white = game.getWhitePixel();

        // Laser sightline when Laser Claw is active and swinging
        if (run.getActiveClaw() == ClawType.LASER && hook.getState() == HookState.SWINGING) {
            float beamLen = 285f;
            batch.setColor(1f, 0.15f, 0.12f, 0.52f);
            batch.draw(
                white,
                tip.x - 0.6f,
                tip.y - beamLen,
                0.6f,
                beamLen,
                1.2f,
                beamLen,
                1f,
                1f,
                hook.getAngleDeg(),
                0,
                0,
                1,
                1,
                false,
                false
            );
            batch.setColor(Color.WHITE);
        }

        // Braided cable line
        batch.setColor(0.14f, 0.12f, 0.10f, 1f);
        batch.draw(
            white,
            pivot.x - CABLE_THICKNESS * 0.5f,
            pivot.y - len,
            CABLE_THICKNESS * 0.5f,
            len,
            CABLE_THICKNESS,
            len,
            1f,
            1f,
            hook.getAngleDeg(),
            0,
            0,
            1,
            1,
            false,
            false
        );
        batch.setColor(Color.WHITE);

        // Held item tucked tightly in claw jaws along rope direction
        MineItem held = hook.getAttachedItem();
        if (held != null) {
            float rad = hook.getAngleDeg() * MathUtils.degreesToRadians;
            float offset = 4.5f + held.drawHeight * 0.32f;
            float itemCx = tip.x + MathUtils.sin(rad) * offset;
            float itemCy = tip.y - MathUtils.cos(rad) * offset;
            if (held.animated) {
                Animation<TextureRegion> anim = assets.getAnimation(held.assetKey);
                TextureRegion frame = anim.getKeyFrame(fxTime, true);
                batch.draw(
                    frame,
                    itemCx - held.drawWidth * 0.5f,
                    itemCy - held.drawHeight * 0.5f,
                    held.drawWidth,
                    held.drawHeight
                );
            } else {
                Texture heldTex = assets.getTexture(held.assetKey);
                batch.draw(
                    heldTex,
                    itemCx - held.drawWidth * 0.5f,
                    itemCy - held.drawHeight * 0.5f,
                    held.drawWidth,
                    held.drawHeight
                );
            }
        }

        // Exact Claw state texture pivoted cleanly at top-center (cable attachment point)
        int clawIdx = Math.max(0, Math.min(3, run.getActiveClaw().id - 1));
        int stateIdx = switch (hook.getState()) {
            case SWINGING -> 0;
            case EXTENDING -> 1;
            case RETRACTING, BRAKING -> 2;
        };
        TextureRegion clawRegion = clawRegions[clawIdx][stateIdx];
        float clawW = clawRegion.getRegionWidth() * CLAW_WORLD_SCALE;
        float clawH = clawRegion.getRegionHeight() * CLAW_WORLD_SCALE;
        batch.draw(
            clawRegion,
            tip.x - clawW * 0.5f,
            tip.y - clawH,
            clawW * 0.5f,
            clawH,
            clawW,
            clawH,
            1f,
            1f,
            hook.getAngleDeg()
        );
    }

    private void drawTopHud(SpriteBatch batch, Assets assets, RunSession run) {
        Texture white = game.getWhitePixel();
        BitmapFont hudFont = game.getHudFont();
        BitmapFont smallFont = game.getSmallFont();

        // Mask baked "60" inside TIMER black circle on bg_sky_w1
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(white, 411f, 240f, 18f, 14f);
        batch.setColor(Color.WHITE);

        if (backToStudioAction != null) {
            batch.setColor(0.08f, 0.14f, 0.25f, 0.90f);
            batch.draw(white, backToStudioRect.x, backToStudioRect.y, backToStudioRect.width, backToStudioRect.height);
            batch.setColor(Color.WHITE);
            smallFont.setColor(Color.CYAN);
            smallFont.draw(batch, "<- BACK TO STUDIO", backToStudioRect.x, backToStudioRect.y + 8.5f, backToStudioRect.width, Align.center, false);
        }

        // Authentic Vegas Buttons
        pauseButton.draw(batch, assets, viewport, touchPoint);
        soundButton.draw(batch, assets, viewport, touchPoint);
        menuButton.draw(batch, assets, viewport, touchPoint);
        earlyNextButton.draw(batch, assets, viewport, touchPoint);

        // Muted visual slash indicator
        if (game.isMuted()) {
            batch.setColor(0.95f, 0.15f, 0.15f, 0.95f);
            batch.draw(white, soundButton.bounds.x + 2f, soundButton.bounds.y + 5.5f, 0f, 0.5f, 13f, 1.3f, 1f, 1f, 35f, 0, 0, 1, 1, false, false);
            batch.setColor(Color.WHITE);
        }

        // Level badge (LVL X/6) & Real Dynamite Stick icons neatly in sky band
        batch.setColor(0.20f, 0.12f, 0.04f, 0.85f);
        batch.draw(white, 112f, 237f, 36f, 12f);
        batch.setColor(Color.WHITE);
        smallFont.setColor(Color.GOLD);
        smallFont.draw(batch, levelBadgeText, 112f, 245.5f, 36f, Align.center, false);

        Texture dynIcon = assets.getTexture(AssetKey.UI_DYNAMITE_ICON);
        int dynCount = run.getDynamiteCount();
        int visibleSticks = Math.min(6, dynCount);
        for (int s = 0; s < visibleSticks; s++) {
            batch.draw(dynIcon, 151f + s * 7.5f, 236.5f, 6.5f, 12.5f);
        }
        if (dynCount > 6) {
            smallFont.setColor(Color.ORANGE);
            smallFont.draw(batch, "x" + dynCount, 195f, 245f);
        }

        // 4-Gem Collector HUD strip on levels containing Gems (Level 6)
        if (levelHasGems) {
            drawGemTracker(batch, assets, run);
        }

        // TARGET sign
        hudFont.setColor(0.25f, 0.10f, 0.02f, 1f);
        hudFont.draw(batch, goalText, 272f, 250f, 52f, Align.center, false);

        // TOTAL sign (green highlight once target reached)
        hudFont.setColor(run.isGoalMet() ? new Color(0.05f, 0.52f, 0.06f, 1f) : new Color(0.25f, 0.10f, 0.02f, 1f));
        hudFont.draw(batch, scoreText, 342f, 250f, 48f, Align.center, false);

        // TIMER circle with pulsing red scale at <= 5s
        int secs = run.getDisplayedSeconds();
        if (secs <= hookConfig.countdownWarningSec) {
            float pulse = 1.0f + (timerPulseTimer / 0.45f) * 0.35f;
            hudFont.getData().setScale(0.50f * pulse);
            hudFont.setColor(1f, 0.20f, 0.18f, 1f);
            hudFont.draw(batch, timerText, 408f, 250f + (pulse - 1f) * 3f, 24f, Align.center, false);
            hudFont.getData().setScale(0.50f);
        } else {
            hudFont.setColor(0.20f, 1.0f, 0.25f, 1f);
            hudFont.draw(batch, timerText, 408f, 250f, 24f, Align.center, false);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawGemTracker(SpriteBatch batch, Assets assets, RunSession run) {
        Texture white = game.getWhitePixel();
        batch.setColor(0.08f, 0.06f, 0.04f, 0.65f);
        batch.draw(white, 434f, 248f, 42f, 16f);

        for (int g = 0; g < 4; g++) {
            int objN = 23 + g;
            String key = switch (g) {
                case 0 -> AssetKey.GEM_1;
                case 1 -> AssetKey.GEM_2;
                case 2 -> AssetKey.GEM_3;
                default -> AssetKey.GEM_4;
            };
            boolean hasGem = run.isGemCollected(objN);
            batch.setColor(1f, 1f, 1f, hasGem ? 1f : 0.24f);
            batch.draw(assets.getTexture(key), 436f + g * 9.8f, 251f, 8.5f, 10f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawFloatingTexts(SpriteBatch batch) {
        BitmapFont floatingFont = game.getFloatingFont();
        for (int i = 0; i < MAX_FLOATING_TEXTS; i++) {
            FloatingText ft = floatingTextPool[i];
            if (!ft.active) {
                continue;
            }
            floatingFont.getData().setScale(0.50f * ft.scale);
            floatingFont.setColor(ft.color);
            glyphLayout.setText(floatingFont, ft.text);
            floatingFont.draw(batch, glyphLayout, ft.x - glyphLayout.width * 0.5f, ft.y);
        }
        floatingFont.getData().setScale(0.50f);
    }

    private void drawMysteryBanner(SpriteBatch batch) {
        Texture white = game.getWhitePixel();
        BitmapFont small = game.getSmallFont();
        batch.setColor(0.10f, 0.06f, 0.02f, 0.86f);
        batch.draw(white, 130f, 210f, 220f, 15f);
        batch.setColor(Color.WHITE);
        small.setColor(Color.GOLD);
        small.draw(batch, mysteryBannerText, 130f, 220f, 220f, Align.center, false);
    }

    private void drawPauseOverlay(SpriteBatch batch) {
        Texture white = game.getWhitePixel();
        BitmapFont banner = game.getBannerFont();
        BitmapFont hud = game.getHudFont();
        batch.setColor(0f, 0f, 0f, 0.58f);
        batch.draw(white, 0f, 0f, WorldBounds.WORLD_WIDTH, WorldBounds.HUD_BOTTOM_Y);
        batch.setColor(Color.WHITE);
        banner.setColor(Color.GOLD);
        banner.draw(batch, "TAM DUNG", 0f, 135f, WorldBounds.WORLD_WIDTH, Align.center, false);
        hud.setColor(Color.WHITE);
        hud.draw(batch, "Bam PAUSE hoac phim P de tiep tuc", 0f, 104f, WorldBounds.WORLD_WIDTH, Align.center, false);
    }

    private void drawDebugBar(SpriteBatch batch) {
        Texture white = game.getWhitePixel();
        BitmapFont small = game.getSmallFont();

        batch.setColor(0.05f, 0.05f, 0.08f, 0.88f);
        batch.draw(white, 0f, 0f, WorldBounds.WORLD_WIDTH, 16f);

        drawDbgBtn(batch, small, dbgNextRect, "F2: NEXT>>Shop");
        drawDbgBtn(batch, small, dbgCycleRect, "F3: LVL +1");
        drawDbgBtn(batch, small, dbgRichRect, "F4: +$1K/+3TNT");
        drawDbgBtn(batch, small, dbgTimeRect, "F5: TIME=6s");

        small.setColor(Color.LIGHT_GRAY);
        small.draw(batch, "F1: An Debug", 358f, 11f, 118f, Align.right, false);
        batch.setColor(Color.WHITE);
    }

    private void drawDbgBtn(SpriteBatch batch, BitmapFont small, Rectangle rect, String text) {
        Texture white = game.getWhitePixel();
        batch.setColor(0.22f, 0.25f, 0.34f, 0.95f);
        batch.draw(white, rect.x, rect.y, rect.width, rect.height);
        batch.setColor(Color.WHITE);
        small.setColor(Color.YELLOW);
        small.draw(batch, text, rect.x, rect.y + 9f, rect.width, Align.center, false);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        paused = true;
        stopRetractSound();
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        stopRetractSound();
    }

    @Override
    public void dispose() {
        stopRetractSound();
    }
}
