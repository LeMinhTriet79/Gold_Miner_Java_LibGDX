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

    private static final float VEGAS_W = 800f;
    private static final float VEGAS_H = 600f;
    private static final float HOLLOW_LEFT = 12f;
    private static final float HOLLOW_RIGHT = 788f;
    private static final float HOLLOW_BOTTOM = 52f;
    private static final float HOLLOW_TOP = 588f;
    private static final float MINE_SCREEN_BOTTOM = 52f;
    private static final float MINE_SCREEN_TOP = 408f;
    private static final float SX = VEGAS_W / WorldBounds.WORLD_WIDTH;
    private static final float SY = (MINE_SCREEN_TOP - MINE_SCREEN_BOTTOM) / WorldBounds.MINE_TOP_Y;

    private static final float CART_DRAW_WIDTH = 42f;
    private static final float CART_DRAW_HEIGHT = 25.5f;
    private static final float CABLE_THICKNESS = 2.2f;
    private static final float THROW_ANIM_DURATION = 9f / 18f;
    private static final float STRENGTH_ANIM_DURATION = 10f / 18f;
    private static final float CLAW_WORLD_SCALE = 0.56f;
    private static final float TOTAL_SIGN_CENTER_X = 616f / SX;
    private static final float TOTAL_SIGN_CENTER_Y = (552f - MINE_SCREEN_BOTTOM) / SY;
    private static final float MINER_CART_DRAW_H = 88f;
    private static final float MINER_CART_DRAW_W = 66f;
    private static final float MINER_CART_BOTTOM_Y = 414f;
    private static final float WINCH_OFFSET_Y = MINER_CART_DRAW_H * 0.42f;
    private static final Color COL_TOTAL_GOAL_MET = new Color(0.05f, 0.52f, 0.06f, 1f);
    private static final Color COL_TOTAL_NORMAL = new Color(0.25f, 0.10f, 0.02f, 1f);
    private static final Color COL_TNT_FLASH = new Color(1.0f, 0.34f, 0.08f, 1f);
    private static final Color COL_CART_BADGE_BG = new Color(0.18f, 0.05f, 0.02f, 0.88f);
    private static final Color COL_SPARKLE_GOLD = new Color(1.0f, 0.92f, 0.32f, 1f);
    private static final Color COL_WHEEL_DUST = new Color(0.84f, 0.72f, 0.52f, 1f);

    private static final int MAX_EXPLOSIONS = 24;
    private static final int MAX_FLOATING_TEXTS = 24;
    private static final int MAX_WHEEL_DUST = 10;

    private final GoldMinerGame game;
    private final String levelFileName;
    private final Runnable backToStudioAction;
    private final boolean mapMode;
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

    // Authentic 800x600 Vegas Bottom-Bar Controls (using original Vegas button sprites, never overlap sky)
    private final VegasButton pauseButton;
    private final VegasButton soundButton;
    private final VegasButton menuButton;
    private final VegasButton earlyNextButton;
    private final VegasButton windowButton;
    private final VegasButton helpButton;
    private final Rectangle helpCloseRect = new Rectangle(332f, 200f, 136f, 36f);
    private final Rectangle backToStudioRect = new Rectangle(180f, 566f, 150f, 20f);

    // Debug strip hitboxes (when toggled on via F1, positioned above bottom bezel)
    private final Rectangle dbgNextRect = new Rectangle(16f, 56f, 134f, 22f);
    private final Rectangle dbgCycleRect = new Rectangle(158f, 56f, 120f, 22f);
    private final Rectangle dbgRichRect = new Rectangle(286f, 56f, 154f, 22f);
    private final Rectangle dbgTimeRect = new Rectangle(448f, 56f, 120f, 22f);

    private float cartX;
    private float cartMinX;
    private float cartMaxX;
    private float cartClickTargetX;
    private boolean cartMovingToClick;
    private float cartSfxTimer;
    private int cartSfxStep;
    private float cartRollDistancePx;
    private boolean cartMovingThisFrame;
    private float wheelDustTimer;
    private final float[] dustX = new float[MAX_WHEEL_DUST];
    private final float[] dustY = new float[MAX_WHEEL_DUST];
    private final float[] dustLife = new float[MAX_WHEEL_DUST];

    private String activeMinerAnimKey = AssetKey.MINER_IDLE;
    private float minerAnimTime;
    private float fxTime;
    private float throwAnimTimer;
    private float strengthAnimTimer;
    private float timerPulseTimer;
    private float tntFlashTimer;
    private float collectSparkleTimer;
    private float happyTimer;
    private boolean paused;
    private boolean helpOpen;
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

    public static PlayScreen forMapLevel(GoldMinerGame game, int levelNumber) {
        int clamped = Math.max(1, Math.min(RunSession.MAX_W1_LEVEL, levelNumber));
        return new PlayScreen(game, clamped, "level_" + clamped + ".json", null, true, false);
    }

    public static PlayScreen forContinuedMapLevel(GoldMinerGame game, int levelNumber) {
        int clamped = Math.max(1, Math.min(RunSession.MAX_W1_LEVEL, levelNumber));
        return new PlayScreen(game, clamped, "level_" + clamped + ".json", null, true, true);
    }

    public PlayScreen(GoldMinerGame game, int levelNumber) {
        this(game, levelNumber, "level_" + levelNumber + ".json", null, false, false);
    }

    public PlayScreen(GoldMinerGame game, int levelNumber, String levelFileName, Runnable backToStudioAction) {
        this(game, levelNumber, levelFileName, backToStudioAction, false, false);
    }

    private PlayScreen(
        GoldMinerGame game,
        int levelNumber,
        String levelFileName,
        Runnable backToStudioAction,
        boolean mapMode,
        boolean continueRun
    ) {
        this.game = game;
        this.levelFileName = (levelFileName == null || levelFileName.isEmpty())
            ? ("level_" + levelNumber + ".json")
            : levelFileName;
        this.backToStudioAction = backToStudioAction;
        this.mapMode = mapMode;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VEGAS_W, VEGAS_H, camera);

        this.hookConfig = json.fromJson(HookConfig.class, StudioFileHelper.resolveHookConfigFile());
        this.levelConfig = json.fromJson(
            LevelConfig.class,
            StudioFileHelper.resolveLevelFile(this.levelFileName)
        );

        RunSession run = game.getRunSession();
        if (backToStudioAction != null) {
            int customDur = levelConfig.durationSeconds > 0 ? levelConfig.durationSeconds : hookConfig.defaultDurationSec;
            run.prepareCustomPlayTest(levelNumber, levelConfig.goal, customDur);
        } else if (mapMode) {
            int dur = levelConfig.durationSeconds > 0 ? levelConfig.durationSeconds : hookConfig.defaultDurationSec;
            if (continueRun) {
                run.prepareContinueMapLevel(levelNumber, RunSession.mapTargetForLevel(levelNumber), dur);
            } else {
                run.prepareMapLevel(levelNumber, RunSession.mapTargetForLevel(levelNumber), dur);
            }
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

        this.menuButton = new VegasButton(
            40f, 10f, 156f, 32f,
            AssetKey.BTN_MENU_UP, AssetKey.BTN_MENU_OVER, AssetKey.BTN_MENU_DOWN
        );
        this.earlyNextButton = new VegasButton(
            212f, 10f, 156f, 32f,
            AssetKey.BTN_NEXT_UP, AssetKey.BTN_NEXT_OVER, AssetKey.BTN_NEXT_DOWN
        );
        this.windowButton = new VegasButton(
            458f, 10f, 43f, 32f,
            AssetKey.BTN_WINDOW_UP, AssetKey.BTN_WINDOW_OVER, AssetKey.BTN_WINDOW_DOWN
        );
        this.soundButton = new VegasButton(
            508f, 10f, 43f, 32f,
            AssetKey.BTN_SOUND_ON_UP, AssetKey.BTN_SOUND_ON_OVER, AssetKey.BTN_SOUND_ON_DOWN
        );
        this.pauseButton = new VegasButton(
            561f, 10f, 102f, 32f,
            AssetKey.BTN_PAUSE_UP, AssetKey.BTN_PAUSE_OVER, AssetKey.BTN_PAUSE_DOWN
        );
        this.helpButton = new VegasButton(
            673f, 10f, 86f, 32f,
            AssetKey.BTN_HELP_UP, AssetKey.BTN_HELP_OVER, AssetKey.BTN_HELP_DOWN
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

    private static float toScreenX(float worldX) {
        return worldX * SX;
    }

    private static float toScreenY(float worldY) {
        return MINE_SCREEN_BOTTOM + worldY * SY;
    }

    private static float toWorldX(float screenX) {
        return screenX / SX;
    }

    private static float toWorldY(float screenY) {
        return (screenY - MINE_SCREEN_BOTTOM) / SY;
    }

    @Override
    public void render(float rawDt) {
        float dt = Math.min(rawDt, 0.05f);
        if (handleInput() || transitionTriggered) {
            return;
        }

        if (!paused && !helpOpen) {
            updateGameplay(dt);
        }

        if (!transitionTriggered) {
            renderWorld();
        }
    }

    private boolean handleInput() {
        RunSession run = game.getRunSession();

        if (helpOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.H)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                helpOpen = false;
                return false;
            }
            if (Gdx.input.justTouched()) {
                touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
                viewport.unproject(touchPoint);
                if (helpCloseRect.contains(touchPoint.x, touchPoint.y) || helpButton.contains(touchPoint.x, touchPoint.y)) {
                    helpOpen = false;
                }
            }
            return false;
        }

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
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            helpOpen = !helpOpen;
            if (helpOpen) {
                stopRetractSound();
            }
            return false;
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

            if (helpButton.contains(touchPoint.x, touchPoint.y)) {
                helpOpen = true;
                stopRetractSound();
                return false;
            }
            if (windowButton.contains(touchPoint.x, touchPoint.y)) {
                game.toggleFullscreen();
                return false;
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
                if (mapMode) {
                    game.showWorldMap(run.getCurrentLevel());
                } else {
                    game.showSettlement(false);
                }
                return true;
            }
            if (earlyNextButton.contains(touchPoint.x, touchPoint.y)) {
                finishEarlyAsPass();
                return true;
            }

            if (!paused && !run.isLevelFinished()
                && touchPoint.x >= HOLLOW_LEFT && touchPoint.x <= HOLLOW_RIGHT
                && touchPoint.y >= HOLLOW_BOTTOM && touchPoint.y <= HOLLOW_TOP) {
                handleWorldPointerDown(toWorldX(touchPoint.x), toWorldY(touchPoint.y));
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
        if (tntFlashTimer > 0f) {
            tntFlashTimer = Math.max(0f, tntFlashTimer - dt);
        }
        if (collectSparkleTimer > 0f) {
            collectSparkleTimer = Math.max(0f, collectSparkleTimer - dt);
        }
        if (happyTimer > 0f) {
            happyTimer = Math.max(0f, happyTimer - dt);
        }
        for (int i = 0; i < MAX_WHEEL_DUST; i++) {
            if (dustLife[i] > 0f) {
                dustLife[i] = Math.max(0f, dustLife[i] - dt);
                dustY[i] += 14f * dt;
            }
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
                screenShake.trigger(3.8f, 0.22f);
                tntFlashTimer = 0.22f;
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
            cartMovingThisFrame = false;
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

        float movedUnits = Math.abs(cartX - prevCartX);
        if (movedUnits > 0.001f) {
            cartMovingThisFrame = true;
            cartRollDistancePx += movedUnits * SX;
            wheelDustTimer -= dt;
            if (wheelDustTimer <= 0f) {
                wheelDustTimer = 0.07f;
                float dirSign = cartX >= prevCartX ? 1f : -1f;
                float trailingWheelX = toScreenX(cartX) - dirSign * (MINER_CART_DRAW_W * 0.28f);
                spawnWheelDust(trailingWheelX);
            }
            cartSfxTimer -= dt;
            if (cartSfxTimer <= 0f) {
                cartSfxTimer = 0.28f;
                playCartRollSfx();
            }
        } else {
            cartMovingThisFrame = false;
            wheelDustTimer = 0f;
            cartSfxTimer = 0f;
        }

        hook.setPivotX(cartX);
    }

    private void spawnWheelDust(float screenX) {
        for (int i = 0; i < MAX_WHEEL_DUST; i++) {
            if (dustLife[i] <= 0f) {
                dustX[i] = screenX + MathUtils.random(-3f, 3f);
                dustY[i] = MINER_CART_BOTTOM_Y + 2f;
                dustLife[i] = 0.28f;
                return;
            }
        }
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
        screenShake.trigger(4.2f, 0.22f);
        tntFlashTimer = 0.22f;
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
        happyTimer = 0.45f;
        collectSparkleTimer = 0.45f;
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
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        camera.position.set(
            VEGAS_W * 0.5f + screenShake.offsetX * SX,
            VEGAS_H * 0.5f + screenShake.offsetY * SY,
            0f
        );
        camera.update();
        viewport.apply(true);

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
        batch.setColor(Color.WHITE);

        // Layer 1: Native 800x600 4:3 Vegas Playfield (strictly bottom-to-top, zero stretching or mirroring)
        batch.draw(mine, 0f, 42f, 800f, 370f);
        batch.draw(cave, 0f, 412f, 800f, 93f);
        batch.draw(track, 0f, 408f, 800f, 73f);
        batch.draw(sky, 0f, 497f, 800f, 103f);

        // 2. Mine items & animated creatures (strict 1:1 natural texture aspect ratio, zero distortion)
        for (int i = 0; i < items.size; i++) {
            MineItem item = items.get(i);
            if (!item.active) {
                continue;
            }
            float ix = toScreenX(item.bounds.x);
            float iy = toScreenY(item.bounds.y);
            float iw = item.drawWidth * SX;
            if (item.animated) {
                Animation<TextureRegion> anim = assets.getAnimation(item.assetKey);
                TextureRegion frame = anim.getKeyFrame(item.animTime, true);
                float ih = iw * ((float) frame.getRegionHeight() / frame.getRegionWidth());
                batch.draw(
                    frame,
                    ix - iw * 0.5f,
                    iy - ih * 0.5f,
                    iw * 0.5f,
                    ih * 0.5f,
                    iw,
                    ih,
                    item.facingRight ? -1f : 1f,
                    1f,
                    0f
                );
            } else {
                Texture tex = assets.getTexture(item.assetKey);
                float ih = iw * ((float) tex.getHeight() / tex.getWidth());
                batch.draw(tex, ix - iw * 0.5f, iy - ih * 0.5f, iw, ih);
            }
        }

        // 3. 2D Pixel-Locked Miner + Red Cart (zero cart shake, wheel roll, crank pull, happy celebrate)
        float cartDrawX = toScreenX(cartX) - MINER_CART_DRAW_W * 0.5f;
        float cartDrawY = MINER_CART_BOTTOM_Y;
        float cartScreenX = cartDrawX + MINER_CART_DRAW_W * 0.50f;
        MineItem attached = hook.getAttachedItem();
        boolean pullingItem = hook.getState() == HookState.RETRACTING && attached != null;
        Texture minerCartTex;
        if (pullingItem) {
            boolean heavyItem = attached.getEffectiveWeight() >= 5
                || (attached.type != null && (attached.type.contains("large") || "gold_mega".equals(attached.type)));
            float frameDur = heavyItem ? 0.20f : 0.12f;
            int pullIdx = ((int) (fxTime / frameDur)) % 2;
            minerCartTex = assets.getTexture(pullIdx == 0 ? AssetKey.MINER_CART_PULL_1 : AssetKey.MINER_CART_PULL_2);
        } else if (happyTimer > 0f) {
            minerCartTex = assets.getTexture(AssetKey.MINER_CART_HAPPY);
        } else if (cartMovingThisFrame) {
            int rollIdx = ((int) (cartRollDistancePx / 9f)) % 2;
            minerCartTex = assets.getTexture(rollIdx == 0 ? AssetKey.MINER_CART_IDLE : AssetKey.MINER_CART_ROLL);
        } else {
            minerCartTex = assets.getTexture(AssetKey.MINER_CART_IDLE);
        }
        batch.draw(
            minerCartTex,
            cartDrawX,
            cartDrawY,
            MINER_CART_DRAW_W,
            MINER_CART_DRAW_H
        );

        // Wheel rolling dust specks along the silver track
        Texture whitePx = game.getWhitePixel();
        for (int i = 0; i < MAX_WHEEL_DUST; i++) {
            if (dustLife[i] > 0f) {
                float alpha = MathUtils.clamp(dustLife[i] / 0.28f, 0f, 1f) * 0.78f;
                batch.setColor(COL_WHEEL_DUST.r, COL_WHEEL_DUST.g, COL_WHEEL_DUST.b, alpha);
                float sz = 3.5f * (0.5f + alpha * 0.5f);
                batch.draw(whitePx, dustX[i] - sz * 0.5f, dustY[i], sz, sz);
            }
        }
        batch.setColor(Color.WHITE);

        // Compact Dynamite Count Badge tucked in bottom-right corner of red cart body (clear of center hook)
        int cartDynCount = run.getDynamiteCount();
        if (cartDynCount > 0) {
            float badgeX = cartDrawX + MINER_CART_DRAW_W - 24f;
            float badgeY = cartDrawY + 12f;
            batch.setColor(COL_CART_BADGE_BG);
            batch.draw(whitePx, badgeX, badgeY, 22f, 13f);
            batch.setColor(Color.WHITE);
            batch.draw(assets.getTexture(AssetKey.UI_DYNAMITE_ICON), badgeX + 2f, badgeY + 1f, 6f, 11f);
            BitmapFont tiny = game.getPlaySmallFont();
            tiny.getData().setScale(0.72f);
            tiny.setColor(Color.GOLD);
            tiny.draw(batch, Integer.toString(cartDynCount), badgeX + 9f, badgeY + 10.5f);
            tiny.getData().setScale(1.0f);
        }

        // Golden Sparkle Burst around cart when collecting gold / diamonds / gems
        if (collectSparkleTimer > 0f) {
            float progress = 1f - (collectSparkleTimer / 0.45f);
            float alpha = MathUtils.clamp(collectSparkleTimer / 0.45f, 0f, 1f);
            batch.setColor(COL_SPARKLE_GOLD.r, COL_SPARKLE_GOLD.g, COL_SPARKLE_GOLD.b, alpha);
            for (int sp = 0; sp < 6; sp++) {
                float ang = sp * 60f + progress * 90f;
                float dist = 16f + progress * 28f;
                float sx = cartScreenX + MathUtils.cosDeg(ang) * dist;
                float sy = MINER_CART_BOTTOM_Y + 46f + MathUtils.sinDeg(ang) * dist * 0.7f;
                float sz = 4.5f * (1f - progress * 0.5f);
                batch.draw(whitePx, sx - sz * 0.5f, sy - sz * 0.5f, sz * 0.5f, sz * 0.5f, sz, sz, 1f, 1f, 45f, 0, 0, 1, 1, false, false);
            }
            batch.setColor(Color.WHITE);
        }

        // 4. Cable, Laser sightline, Held item, Claw, and Flying Dynamite
        drawCableAndHook(batch, assets, run);
        if (flyingDynamite.active) {
            Animation<TextureRegion> dynAnim = assets.getAnimation(AssetKey.FX_DYNAMITE_FLY);
            ThemeConfig.AnimationDef dynDef = assets.getAnimationDef(AssetKey.FX_DYNAMITE_FLY);
            TextureRegion dynFrame = dynAnim.getKeyFrame(fxTime, true);
            float dw = dynDef.drawWidth * SX;
            float dh = dynDef.drawHeight * SY;
            batch.draw(
                dynFrame,
                toScreenX(flyingDynamite.x) - dw * 0.5f,
                toScreenY(flyingDynamite.y) - dh * 0.5f,
                dw,
                dh
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
            float fw = fxDef.drawWidth * SX;
            float fh = fxDef.drawHeight * SY;
            batch.draw(
                fxFrame,
                toScreenX(fx.x) - fw * fxDef.anchorX,
                toScreenY(fx.y) - fh * fxDef.anchorY,
                fw,
                fh
            );
        }

        // Brief red-orange TNT flash overlay inside hollow playfield (~0.22s)
        if (tntFlashTimer > 0f) {
            float flashAlpha = (tntFlashTimer / 0.22f) * 0.26f;
            batch.setColor(COL_TNT_FLASH.r, COL_TNT_FLASH.g, COL_TNT_FLASH.b, flashAlpha);
            batch.draw(whitePx, HOLLOW_LEFT, HOLLOW_BOTTOM, HOLLOW_RIGHT - HOLLOW_LEFT, HOLLOW_TOP - HOLLOW_BOTTOM);
            batch.setColor(Color.WHITE);
        }

        // 6. Rounded Golden Vegas Frame & 44px Bottom Control Bar
        drawVegasFrameAndBottomBar(batch, assets);

        // 7. Top HUD overlays (cleanly separated: Logo/LVL left, Dynamite/Gems mid, TARGET/TOTAL/TIMER right)
        drawTopHud(batch, assets, run);

        // 8. Floating Cash & Banners
        drawFloatingTexts(batch);
        if (mysteryBannerTimer > 0f) {
            drawMysteryBanner(batch);
        }

        // 9. Pause, Help & Debug Strip
        if (paused) {
            drawPauseOverlay(batch);
        }
        if (helpOpen) {
            drawHelpOverlay(batch, assets);
        }
        if (debugBarVisible) {
            drawDebugBar(batch);
        }

        batch.end();
    }

    private void drawCableAndHook(SpriteBatch batch, Assets assets, RunSession run) {
        Vector2 pivot = hook.getPivot();
        Vector2 tip = hook.getTip();
        float cartDrawX = toScreenX(pivot.x) - MINER_CART_DRAW_W * 0.5f;
        float cartDrawY = MINER_CART_BOTTOM_Y;
        float px = cartDrawX + MINER_CART_DRAW_W * 0.50f;
        float py = cartDrawY + WINCH_OFFSET_Y;

        float angleRad = hook.getAngleDeg() * MathUtils.degreesToRadians;
        float idleLen = hookConfig.restCableLength * SY;
        float restTx = px + MathUtils.sin(angleRad) * idleLen;
        float restTy = py - MathUtils.cos(angleRad) * idleLen;
        float tx;
        float ty;
        if (hook.getState() == HookState.SWINGING) {
            tx = restTx;
            ty = restTy;
        } else {
            float worldTx = toScreenX(tip.x);
            float worldTy = toScreenY(tip.y);
            float blend = MathUtils.clamp((hook.getCableLength() - hookConfig.restCableLength) / 22f, 0f, 1f);
            tx = MathUtils.lerp(restTx, worldTx, blend);
            ty = MathUtils.lerp(restTy, worldTy, blend);
        }
        cableDelta.set(tx - px, ty - py);
        float len = cableDelta.len();
        float visualAngleDeg = cableDelta.angleDeg() + 90f;
        Texture white = game.getWhitePixel();

        // Laser sightline when Laser Claw is active and swinging
        if (run.getActiveClaw() == ClawType.LASER && hook.getState() == HookState.SWINGING) {
            float beamLen = 520f;
            batch.setColor(1f, 0.15f, 0.12f, 0.52f);
            batch.draw(
                white,
                tx - 1.0f,
                ty - beamLen,
                1.0f,
                beamLen,
                2.0f,
                beamLen,
                1f,
                1f,
                visualAngleDeg,
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
            px - CABLE_THICKNESS * 0.5f,
            py - len,
            CABLE_THICKNESS * 0.5f,
            len,
            CABLE_THICKNESS,
            len,
            1f,
            1f,
            visualAngleDeg,
            0,
            0,
            1,
            1,
            false,
            false
        );
        batch.setColor(Color.WHITE);

        // Held item tucked tightly in claw jaws along rope direction (1:1 aspect ratio)
        MineItem held = hook.getAttachedItem();
        if (held != null) {
            float rad = visualAngleDeg * MathUtils.degreesToRadians;
            float iw = held.drawWidth * SX;
            if (held.animated) {
                Animation<TextureRegion> anim = assets.getAnimation(held.assetKey);
                TextureRegion frame = anim.getKeyFrame(fxTime, true);
                float ih = iw * ((float) frame.getRegionHeight() / frame.getRegionWidth());
                float offset = 8f + ih * 0.32f;
                float itemCx = tx + MathUtils.sin(rad) * offset;
                float itemCy = ty - MathUtils.cos(rad) * offset;
                batch.draw(frame, itemCx - iw * 0.5f, itemCy - ih * 0.5f, iw, ih);
            } else {
                Texture heldTex = assets.getTexture(held.assetKey);
                float ih = iw * ((float) heldTex.getHeight() / heldTex.getWidth());
                float offset = 8f + ih * 0.32f;
                float itemCx = tx + MathUtils.sin(rad) * offset;
                float itemCy = ty - MathUtils.cos(rad) * offset;
                batch.draw(heldTex, itemCx - iw * 0.5f, itemCy - ih * 0.5f, iw, ih);
            }
        }

        // Exact Claw state texture pivoted cleanly at top-center (cable attachment point, 1:1 aspect ratio)
        int clawIdx = Math.max(0, Math.min(3, run.getActiveClaw().id - 1));
        int stateIdx = switch (hook.getState()) {
            case SWINGING -> 0;
            case EXTENDING -> 1;
            case RETRACTING, BRAKING -> 2;
        };
        TextureRegion clawRegion = clawRegions[clawIdx][stateIdx];
        float clawW = clawRegion.getRegionWidth() * CLAW_WORLD_SCALE * SX;
        float clawH = clawW * ((float) clawRegion.getRegionHeight() / clawRegion.getRegionWidth());
        batch.draw(
            clawRegion,
            tx - clawW * 0.5f,
            ty - clawH,
            clawW * 0.5f,
            clawH,
            clawW,
            clawH,
            1f,
            1f,
            visualAngleDeg
        );
    }

    private void drawVegasFrameAndBottomBar(SpriteBatch batch, Assets assets) {
        Texture white = game.getWhitePixel();

        // Layer 3: Authentic 800x600 hollow golden Vegas bezel (center_alpha = 0, NEVER covers playfield)
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_VEGAS_FRAME), 0f, 0f, VEGAS_W, VEGAS_H);

        // Authentic Vegas Bottom-Bar Buttons (y = 10..42 inside the 52px bottom bezel)
        menuButton.draw(batch, assets, viewport, touchPoint);
        earlyNextButton.draw(batch, assets, viewport, touchPoint);
        windowButton.draw(batch, assets, viewport, touchPoint);
        soundButton.draw(batch, assets, viewport, touchPoint);
        pauseButton.draw(batch, assets, viewport, touchPoint);
        helpButton.draw(batch, assets, viewport, touchPoint);

        if (game.isMuted()) {
            batch.setColor(0.95f, 0.15f, 0.15f, 0.95f);
            batch.draw(white, soundButton.bounds.x + 8f, soundButton.bounds.y + 15f, 0f, 1f, 28f, 3f, 1f, 1f, 35f, 0, 0, 1, 1, false, false);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawTopHud(SpriteBatch batch, Assets assets, RunSession run) {
        Texture white = game.getWhitePixel();
        BitmapFont playHud = game.getPlayHudFont();
        BitmapFont playSmall = game.getPlaySmallFont();

        // Layer 4: Marquee Vegas Logo top-left above frame corner (matches original 800x600 frames/342)
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_VEGAS_LOGO), 12f, 506f, 158f, 84f);

        if (backToStudioAction != null) {
            batch.setColor(0.08f, 0.14f, 0.25f, 0.90f);
            batch.draw(white, backToStudioRect.x, backToStudioRect.y, backToStudioRect.width, backToStudioRect.height);
            batch.setColor(Color.WHITE);
            playSmall.setColor(Color.CYAN);
            playSmall.draw(batch, "<- BACK TO STUDIO", backToStudioRect.x, backToStudioRect.y + 16f, backToStudioRect.width, Align.center, false);
        } else {
            // Clean LVL X/6 pill next to Vegas marquee logo (never overlaps dynamite or logo)
            batch.setColor(0.24f, 0.12f, 0.03f, 0.88f);
            batch.draw(white, 180f, 566f, 74f, 20f);
            batch.setColor(0.95f, 0.72f, 0.24f, 1f);
            batch.draw(white, 180f, 566f, 74f, 2f);
            batch.setColor(Color.WHITE);
            playSmall.setColor(Color.GOLD);
            playSmall.draw(batch, levelBadgeText, 180f, 581f, 74f, Align.center, false);
        }

        // Dynamite sticks HUD row cleanly separated right of LVL badge
        Texture dynIcon = assets.getTexture(AssetKey.UI_DYNAMITE_ICON);
        int dynCount = run.getDynamiteCount();
        int visibleSticks = Math.min(6, dynCount);
        if (visibleSticks > 0) {
            for (int s = 0; s < visibleSticks; s++) {
                batch.draw(dynIcon, 262f + s * 14f, 558f, 12f, 24f);
            }
            if (dynCount > 6) {
                playSmall.setColor(Color.ORANGE);
                playSmall.draw(batch, "x" + dynCount, 346f, 575f);
            }
        }

        // 4-Gem Collector HUD strip below dynamite when level has gems (Level 6)
        if (levelHasGems) {
            drawGemTracker(batch, assets, run);
        }

        // TARGET wooden sign on bg_sky_w1
        playHud.setColor(0.25f, 0.10f, 0.02f, 1f);
        playHud.draw(batch, goalText, 480f, 558f, 58f, Align.center, false);

        // TOTAL wooden sign on bg_sky_w1 (green highlight once target reached)
        playHud.setColor(run.isGoalMet() ? COL_TOTAL_GOAL_MET : COL_TOTAL_NORMAL);
        playHud.draw(batch, scoreText, 584f, 554f, 66f, Align.center, false);

        // Clean round black TIMER stone dial centered via GlyphLayout (circle center ~ 695, 549)
        int secs = run.getDisplayedSeconds();
        if (secs <= hookConfig.countdownWarningSec) {
            float pulse = 1.0f + (timerPulseTimer / 0.45f) * 0.25f;
            playHud.getData().setScale(pulse);
            playHud.setColor(1f, 0.20f, 0.18f, 1f);
            glyphLayout.setText(playHud, timerText);
            playHud.draw(batch, glyphLayout, 695f - glyphLayout.width * 0.5f, 549f + glyphLayout.height * 0.5f);
            playHud.getData().setScale(1.0f);
        } else {
            playHud.setColor(0.20f, 1.0f, 0.25f, 1f);
            glyphLayout.setText(playHud, timerText);
            playHud.draw(batch, glyphLayout, 695f - glyphLayout.width * 0.5f, 549f + glyphLayout.height * 0.5f);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawGemTracker(SpriteBatch batch, Assets assets, RunSession run) {
        Texture white = game.getWhitePixel();
        // Brighter warm wooden collector board with golden rim
        batch.setColor(0.84f, 0.64f, 0.34f, 0.95f);
        batch.draw(white, 340f, 522f, 88f, 32f);
        batch.setColor(0.58f, 0.39f, 0.19f, 0.95f);
        batch.draw(white, 342f, 524f, 84f, 28f);

        for (int g = 0; g < 4; g++) {
            int objN = 23 + g;
            String key = switch (g) {
                case 0 -> AssetKey.GEM_1;
                case 1 -> AssetKey.GEM_2;
                case 2 -> AssetKey.GEM_3;
                default -> AssetKey.GEM_4;
            };
            boolean hasGem = run.isGemCollected(objN);
            batch.setColor(1f, 1f, 1f, hasGem ? 1f : 0.42f);
            batch.draw(assets.getTexture(key), 347f + g * 19.5f, 528f, 16f, 20f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawFloatingTexts(SpriteBatch batch) {
        BitmapFont playHud = game.getPlayHudFont();
        for (int i = 0; i < MAX_FLOATING_TEXTS; i++) {
            FloatingText ft = floatingTextPool[i];
            if (!ft.active) {
                continue;
            }
            playHud.getData().setScale(ft.scale);
            playHud.setColor(ft.color);
            glyphLayout.setText(playHud, ft.text);
            playHud.draw(batch, glyphLayout, toScreenX(ft.x) - glyphLayout.width * 0.5f, toScreenY(ft.y));
        }
        playHud.getData().setScale(1.0f);
    }

    private void drawMysteryBanner(SpriteBatch batch) {
        Texture white = game.getWhitePixel();
        BitmapFont playSmall = game.getPlaySmallFont();
        batch.setColor(0.10f, 0.06f, 0.02f, 0.88f);
        batch.draw(white, 200f, 466f, 400f, 28f);
        batch.setColor(Color.WHITE);
        playSmall.setColor(Color.GOLD);
        playSmall.draw(batch, mysteryBannerText, 200f, 486f, 400f, Align.center, false);
    }

    private void drawPauseOverlay(SpriteBatch batch) {
        Texture white = game.getWhitePixel();
        BitmapFont playBanner = game.getPlayBannerFont();
        BitmapFont playHud = game.getPlayHudFont();
        float hollowW = HOLLOW_RIGHT - HOLLOW_LEFT;
        float hollowH = HOLLOW_TOP - HOLLOW_BOTTOM;
        batch.setColor(0f, 0f, 0f, 0.60f);
        batch.draw(white, HOLLOW_LEFT, HOLLOW_BOTTOM, hollowW, hollowH);
        batch.setColor(Color.WHITE);
        playBanner.setColor(Color.GOLD);
        playBanner.draw(batch, "TẠM DỪNG", HOLLOW_LEFT, 340f, hollowW, Align.center, false);
        playHud.setColor(Color.WHITE);
        playHud.draw(batch, "Bấm PAUSE hoặc phím P để tiếp tục", HOLLOW_LEFT, 290f, hollowW, Align.center, false);
    }

    private void drawHelpOverlay(SpriteBatch batch, Assets assets) {
        Texture white = game.getWhitePixel();
        BitmapFont playBanner = game.getPlayBannerFont();
        BitmapFont playSmall = game.getPlaySmallFont();
        float hollowW = HOLLOW_RIGHT - HOLLOW_LEFT;
        float hollowH = HOLLOW_TOP - HOLLOW_BOTTOM;
        batch.setColor(0f, 0f, 0f, 0.65f);
        batch.draw(white, HOLLOW_LEFT, HOLLOW_BOTTOM, hollowW, hollowH);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_MODAL_PANEL), 150f, 175f, 500f, 265f);

        playBanner.setColor(Color.GOLD);
        playBanner.draw(batch, "HƯỚNG DẪN ĐÀO VÀNG", 170f, 412f, 460f, Align.center, false);

        playSmall.setColor(0.98f, 0.94f, 0.80f, 1f);
        playSmall.draw(batch,
            "• A / D hoặc Mũi tên Trái / Phải: Di chuyển xe goòng\n"
                + "• Space / S / Xuống hoặc Click vùng đất: Thả ngoàm gắp\n"
                + "• W / Lên: Ném thuốc nổ phá đá nặng đang kéo\n"
                + "• P: Tạm dừng   |   M: Tắt/bật tiếng   |   F11: Toàn màn hình",
            180f, 366f, 440f, Align.left, true);

        boolean overClose = helpCloseRect.contains(touchPoint.x, touchPoint.y);
        batch.draw(assets.getTexture(overClose ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP),
            helpCloseRect.x, helpCloseRect.y, helpCloseRect.width, helpCloseRect.height);
        playSmall.setColor(Color.WHITE);
        playSmall.draw(batch, "ĐÓNG", helpCloseRect.x, helpCloseRect.y + 24f, helpCloseRect.width, Align.center, false);
    }

    private void drawDebugBar(SpriteBatch batch) {
        Texture white = game.getWhitePixel();
        BitmapFont playSmall = game.getPlaySmallFont();
        float hollowW = HOLLOW_RIGHT - HOLLOW_LEFT;

        batch.setColor(0.05f, 0.05f, 0.08f, 0.88f);
        batch.draw(white, HOLLOW_LEFT, HOLLOW_BOTTOM, hollowW, 30f);

        drawDbgBtn(batch, playSmall, dbgNextRect, "F2: NEXT>>Shop");
        drawDbgBtn(batch, playSmall, dbgCycleRect, "F3: LVL +1");
        drawDbgBtn(batch, playSmall, dbgRichRect, "F4: +$1K/+3TNT");
        drawDbgBtn(batch, playSmall, dbgTimeRect, "F5: TIME=6s");

        playSmall.setColor(Color.LIGHT_GRAY);
        playSmall.draw(batch, "F1: Ẩn Debug", 580f, 72f, 200f, Align.right, false);
        batch.setColor(Color.WHITE);
    }

    private void drawDbgBtn(SpriteBatch batch, BitmapFont playSmall, Rectangle rect, String text) {
        Texture white = game.getWhitePixel();
        batch.setColor(0.22f, 0.25f, 0.34f, 0.95f);
        batch.draw(white, rect.x, rect.y, rect.width, rect.height);
        batch.setColor(Color.WHITE);
        playSmall.setColor(Color.YELLOW);
        playSmall.draw(batch, text, rect.x, rect.y + 16f, rect.width, Align.center, false);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(VEGAS_W * 0.5f, VEGAS_H * 0.5f, 0f);
        camera.update();
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
