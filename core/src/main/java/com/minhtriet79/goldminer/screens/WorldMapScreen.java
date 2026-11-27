package com.minhtriet79.goldminer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.minhtriet79.goldminer.GoldMinerGame;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.domain.run.RunSession;
import com.minhtriet79.goldminer.gameplay.ui.MenuTopHeader;

/**
 * Pure 16-bit Pixel Art World 1: Australia Progression Map (6 levels, strictly NO STARS).
 * Features cleared checkmarks, locked padlocks, pulsing active glow, animated mini-miner
 * walking along the Outback dirt trail & railway, bottom-right level target preview, and {@code F9} unlock-all.
 */
public class WorldMapScreen implements Screen {

    private static final int LEVEL_COUNT = RunSession.MAX_W1_LEVEL;
    private static final float MINER_WALK_SPEED_WP_PER_SEC = 4.6f;

    private static final Color COL_GOLD = new Color(1.00f, 0.85f, 0.28f, 1f);
    private static final Color COL_CREAM = new Color(0.99f, 0.95f, 0.82f, 1f);
    private static final Color COL_SLATE_TEXT = new Color(0.78f, 0.82f, 0.88f, 1f);
    private static final Color COL_TOAST_BG = new Color(0.08f, 0.05f, 0.02f, 0.88f);
    private static final Color COL_TOAST_GOLD = new Color(0.98f, 0.86f, 0.34f, 1f);

    private static final String[] LEVEL_NUM_LABELS = {"1", "2", "3", "4", "5", "6"};
    private static final String[] LEVEL_TITLES = {
        "LEVEL 1", "LEVEL 2", "LEVEL 3", "LEVEL 4", "LEVEL 5", "LEVEL 6"
    };
    private static final String[] TARGET_LABELS = {
        "TARGET: $555",
        "TARGET: $1,150",
        "TARGET: $1,900",
        "TARGET: $2,800",
        "TARGET: $3,800",
        "TARGET: $5,000"
    };

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final ExtendViewport viewport;
    private final MenuTopHeader topHeader;
    private final Vector3 touchPoint = new Vector3();

    // Normalized node coords & winding trail waypoints loaded once in constructor
    private final float[] nodeNx = new float[LEVEL_COUNT];
    private final float[] nodeNy = new float[LEVEL_COUNT];
    private final int[] nodeWaypointIdx = new int[LEVEL_COUNT];
    private final float[] nodeWorldX = new float[LEVEL_COUNT];
    private final float[] nodeWorldY = new float[LEVEL_COUNT];
    private final Rectangle[] nodeHitboxes = new Rectangle[LEVEL_COUNT];

    private final float[] waypointNx;
    private final float[] waypointNy;
    private final float[] waypointWorldX;
    private final float[] waypointWorldY;
    private final int waypointCount;

    // Bottom-right Level Preview Box anchored dynamically inside ExtendViewport
    private final Rectangle previewBoxRect = new Rectangle(356f, 8f, 116f, 76f);
    private final Rectangle startButtonRect = new Rectangle(366f, 16f, 96f, 22f);

    private float viewLeft = 0f;
    private float viewBottom = 0f;
    private float viewWidth = WorldBounds.WORLD_WIDTH;
    private float viewHeight = WorldBounds.WORLD_HEIGHT;
    private float bgDrawX = 0f;
    private float bgDrawY = 0f;
    private float bgDrawW = WorldBounds.WORLD_WIDTH;
    private float bgDrawH = WorldBounds.WORLD_HEIGHT;

    private int selectedLevel;
    private float minerWaypointPos;
    private float targetWaypointPos;
    private float walkAnimTimer = 0f;
    private boolean minerFacingRight = true;
    private float elapsedTime = 0f;

    private String toastText = "";
    private float toastTimer = 0f;

    private final InputAdapter inputAdapter = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            return topHeader.handleKeyDown(keycode);
        }

        @Override
        public boolean keyTyped(char character) {
            return topHeader.handleKeyTyped(character);
        }
    };

    public WorldMapScreen(GoldMinerGame game, int initialSelectedLevel) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new ExtendViewport(WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT, camera);
        this.topHeader = new MenuTopHeader(game, "WORLD 1: AUSTRALIA", true);

        for (int i = 0; i < LEVEL_COUNT; i++) {
            nodeHitboxes[i] = new Rectangle(0f, 0f, 40f, 28f);
        }

        JsonValue root = loadMapJson();
        JsonValue nodesArr = root != null ? root.get("nodes") : null;
        if (nodesArr != null && nodesArr.size >= LEVEL_COUNT) {
            for (int i = 0; i < LEVEL_COUNT; i++) {
                JsonValue n = nodesArr.get(i);
                nodeNx[i] = n.getFloat("nx", 0.1f + i * 0.15f);
                nodeNy[i] = n.getFloat("ny", 0.45f);
                nodeWaypointIdx[i] = n.getInt("waypointIndex", i);
            }
        } else {
            for (int i = 0; i < LEVEL_COUNT; i++) {
                nodeNx[i] = 0.10f + i * 0.15f;
                nodeNy[i] = 0.45f;
                nodeWaypointIdx[i] = i;
            }
        }

        JsonValue wpArr = root != null ? root.get("trailWaypoints") : null;
        if (wpArr != null && wpArr.size >= 2) {
            waypointCount = wpArr.size;
            waypointNx = new float[waypointCount];
            waypointNy = new float[waypointCount];
            for (int i = 0; i < waypointCount; i++) {
                JsonValue w = wpArr.get(i);
                waypointNx[i] = w.getFloat("nx", 0.5f);
                waypointNy[i] = w.getFloat("ny", 0.5f);
            }
        } else {
            waypointCount = LEVEL_COUNT;
            waypointNx = new float[LEVEL_COUNT];
            waypointNy = new float[LEVEL_COUNT];
            for (int i = 0; i < LEVEL_COUNT; i++) {
                waypointNx[i] = nodeNx[i];
                waypointNy[i] = nodeNy[i];
            }
        }
        waypointWorldX = new float[waypointCount];
        waypointWorldY = new float[waypointCount];

        int maxUnlocked = game.getMaxUnlockedLevel();
        int desired = Math.max(1, Math.min(LEVEL_COUNT, initialSelectedLevel));
        if (!game.isLevelUnlocked(desired)) {
            desired = maxUnlocked;
        }
        this.selectedLevel = desired;
        this.minerWaypointPos = nodeWaypointIdx[selectedLevel - 1];
        this.targetWaypointPos = this.minerWaypointPos;
    }

    private JsonValue loadMapJson() {
        FileHandle fh = Gdx.files.internal("data/ui/australia_map_nodes.json");
        if (!fh.exists()) {
            return null;
        }
        return new JsonReader().parse(fh);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputAdapter);
        topHeader.refreshAccountCache();
        game.playMusic(AssetKey.BGM_MENU, 0.42f);
        if (game.consumeUnlockToast()) {
            showToast("F9: Đã mở khóa tất cả 6 màn Australia!", 2.5f);
        }
    }

    @Override
    public void render(float rawDt) {
        float dt = Math.min(rawDt, 0.05f);
        elapsedTime += dt;

        if (game.consumeUnlockToast()) {
            showToast("F9: Đã mở khóa tất cả 6 màn Australia!", 2.5f);
        }
        if (toastTimer > 0f) {
            toastTimer = Math.max(0f, toastTimer - dt);
        }

        updateMinerWalk(dt);

        touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touchPoint);

        if (handleInput()) {
            return;
        }

        ScreenUtils.clear(0.14f, 0.07f, 0.04f, 1f);
        viewport.apply();

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        Texture white = game.getWhitePixel();
        BitmapFont tiny = game.getPixelTinyFont();
        BitmapFont small = game.getPixelSmallFont();
        BitmapFont hud = game.getPixelHudFont();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 1. Clean 16-bit Australia Map (ZERO stars, ZERO sparkles, NEAREST pixel grid)
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.BG_MAP_AUSTRALIA), bgDrawX, bgDrawY, bgDrawW, bgDrawH);

        // 2. Render 6 Circular Level Nodes (Cleared checkmarks / Selected glow / Locked padlocks — NO STARS)
        drawMapNodes(batch, assets, hud);

        // 3. Render Mini-Miner walking or idling at active trail coordinate
        drawMiniMiner(batch, assets);

        // 4. Bottom-Right Preview Box ("LEVEL N", "TARGET: $...", "[START]")
        drawPreviewBox(batch, assets, small, hud);

        // 5. Optional feedback toast (locked click or F9 unlock-all)
        if (toastTimer > 0f && !toastText.isEmpty()) {
            float tw = Math.min(viewWidth - 16f, 224f);
            float tx = MathUtils.floor(viewLeft + 8f);
            float ty = MathUtils.floor(viewBottom + 8f);
            batch.setColor(COL_TOAST_BG);
            batch.draw(white, tx, ty, tw, 18f);
            batch.setColor(Color.WHITE);
            MenuTopHeader.drawShadowText(tiny, batch, toastText, tx + 6f, ty + 12f, tw - 12f, Align.left, false,
                COL_TOAST_GOLD);
        }

        // 6. Shared Top Chrome ([BACK] / WORLD 1: AUSTRALIA / Profile Card + Dropdown)
        topHeader.draw(batch, touchPoint.x, touchPoint.y, elapsedTime);

        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void updateMinerWalk(float dt) {
        if (Math.abs(targetWaypointPos - minerWaypointPos) <= 0.01f) {
            minerWaypointPos = targetWaypointPos;
            walkAnimTimer = 0f;
            return;
        }
        float dir = targetWaypointPos > minerWaypointPos ? 1f : -1f;
        float prevX = sampleWaypointWorldX(minerWaypointPos);
        minerWaypointPos += dir * MINER_WALK_SPEED_WP_PER_SEC * dt;
        if ((dir > 0f && minerWaypointPos >= targetWaypointPos)
            || (dir < 0f && minerWaypointPos <= targetWaypointPos)) {
            minerWaypointPos = targetWaypointPos;
        }
        float nextX = sampleWaypointWorldX(minerWaypointPos);
        if (Math.abs(nextX - prevX) > 0.02f) {
            minerFacingRight = nextX >= prevX;
        }
        walkAnimTimer += dt;
    }

    private void drawMapNodes(SpriteBatch batch, Assets assets, BitmapFont hud) {
        Texture glowTex = assets.getTexture(AssetKey.MAP_NODE_GLOW);
        Texture discCleared = assets.getTexture(AssetKey.MAP_NODE_CLEARED);
        Texture discUnlocked = assets.getTexture(AssetKey.MAP_NODE_UNLOCKED);
        Texture discSelected = assets.getTexture(AssetKey.MAP_NODE_SELECTED);
        Texture discLocked = assets.getTexture(AssetKey.MAP_NODE_LOCKED);
        Texture checkBadge = assets.getTexture(AssetKey.MAP_CHECK_BADGE);
        Texture lockSmall = assets.getTexture(AssetKey.MAP_LOCK_SMALL);
        Texture lockBig = assets.getTexture(AssetKey.MAP_LOCK_BIG);

        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedTime * 4.8f);

        for (int i = 0; i < LEVEL_COUNT; i++) {
            int level = i + 1;
            boolean unlocked = game.isLevelUnlocked(level);
            boolean cleared = game.isLevelCleared(level);
            boolean selected = (level == selectedLevel);
            boolean hovered = !topHeader.isBlockingModalOpen() && !topHeader.isDropdownOpen()
                && nodeHitboxes[i].contains(touchPoint.x, touchPoint.y);

            float cx = nodeWorldX[i];
            float cy = nodeWorldY[i];

            if (selected) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                batch.setColor(0.90f, 1.00f, 0.52f, 0.55f + 0.35f * pulse);
                batch.draw(glowTex, MathUtils.floor(cx - 24f), MathUtils.floor(cy - 17f), 48f, 34f);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }

            Texture disc = selected ? discSelected : (cleared ? discCleared : (unlocked ? discUnlocked : discLocked));
            int dw = selected ? 36 : 34;
            int dh = selected ? 24 : 22;
            int dy = MathUtils.floor(cy - dh * 0.5f) + (hovered && unlocked ? 1 : 0);
            batch.setColor(Color.WHITE);
            batch.draw(disc, MathUtils.floor(cx - dw * 0.5f), dy, dw, dh);

            // Level number inside disc
            float numY = MathUtils.floor(dy + (dh + hud.getCapHeight()) * 0.5f + 1f);
            Color numCol = unlocked ? Color.WHITE : COL_SLATE_TEXT;
            MenuTopHeader.drawShadowText(hud, batch, LEVEL_NUM_LABELS[i],
                MathUtils.floor(cx - 14f), numY, 28f, Align.center, false, numCol);

            // Strictly NO STARS: only green/gold checkmark above cleared nodes, padlock above locked nodes
            if (cleared) {
                batch.setColor(Color.WHITE);
                batch.draw(checkBadge, MathUtils.floor(cx + 8f), MathUtils.floor(cy + 6f), 14f, 14f);
            } else if (!unlocked) {
                Texture lk = (level == LEVEL_COUNT) ? lockBig : lockSmall;
                float lw = lk.getWidth();
                float lh = lk.getHeight();
                batch.setColor(Color.WHITE);
                batch.draw(lk, MathUtils.floor(cx - lw * 0.5f), MathUtils.floor(cy + 6f), lw, lh);
            }
        }
    }

    private void drawMiniMiner(SpriteBatch batch, Assets assets) {
        boolean walking = Math.abs(targetWaypointPos - minerWaypointPos) > 0.01f;
        float mx = sampleWaypointWorldX(minerWaypointPos);
        float my = sampleWaypointWorldY(minerWaypointPos);

        boolean useWalkFrame = walking && ((int) (walkAnimTimer / 0.13f) % 2 == 1);
        Texture minerTex = assets.getTexture(useWalkFrame ? AssetKey.MAP_MINI_MINER_WALK : AssetKey.MAP_MINI_MINER_IDLE);

        float idleBob = walking ? 0f : (Math.sin(elapsedTime * 3.6f) > 0.4 ? 1f : 0f);
        int drawX = MathUtils.floor(mx - 12f);
        int drawY = MathUtils.floor(my + 4f + idleBob);
        batch.setColor(Color.WHITE);
        batch.draw(
            minerTex,
            drawX, drawY,
            24f, 32f,
            0, 0, minerTex.getWidth(), minerTex.getHeight(),
            !minerFacingRight, false
        );
    }

    private void drawPreviewBox(SpriteBatch batch, Assets assets, BitmapFont small, BitmapFont hud) {
        int px = MathUtils.floor(previewBoxRect.x);
        int py = MathUtils.floor(previewBoxRect.y);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_MAP_PREVIEW), px, py, previewBoxRect.width, previewBoxRect.height);

        int idx = Math.max(0, Math.min(LEVEL_COUNT - 1, selectedLevel - 1));
        MenuTopHeader.drawShadowText(hud, batch, LEVEL_TITLES[idx],
            px, py + 66f, previewBoxRect.width, Align.center, false, COL_GOLD);
        MenuTopHeader.drawShadowText(small, batch, TARGET_LABELS[idx],
            px, py + 51f, previewBoxRect.width, Align.center, false, COL_CREAM);

        boolean overStart = !topHeader.isBlockingModalOpen() && !topHeader.isDropdownOpen()
            && startButtonRect.contains(touchPoint.x, touchPoint.y);
        batch.setColor(Color.WHITE);
        batch.draw(
            assets.getTexture(overStart ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP),
            startButtonRect.x, startButtonRect.y, startButtonRect.width, startButtonRect.height
        );
        float startY = MathUtils.floor(startButtonRect.y + (startButtonRect.height + hud.getCapHeight()) * 0.5f);
        MenuTopHeader.drawShadowText(hud, batch, "START",
            startButtonRect.x, startY, startButtonRect.width, Align.center, false, Color.WHITE);
    }

    private boolean handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (topHeader.handleEscape()) {
                return false;
            }
            game.showGoldMineSelector();
            return true;
        }

        if (!topHeader.isBlockingModalOpen() && !topHeader.isDropdownOpen()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                stepSelection(-1);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                stepSelection(1);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                launchSelectedLevel();
                return true;
            }
        }

        if (!Gdx.input.justTouched()) {
            return false;
        }

        if (topHeader.handleClick(touchPoint.x, touchPoint.y, game::showGoldMineSelector)) {
            return false;
        }

        if (startButtonRect.contains(touchPoint.x, touchPoint.y)) {
            launchSelectedLevel();
            return true;
        }

        for (int i = 0; i < LEVEL_COUNT; i++) {
            if (nodeHitboxes[i].contains(touchPoint.x, touchPoint.y)) {
                int clickedLevel = i + 1;
                if (!game.isLevelUnlocked(clickedLevel)) {
                    if (!game.isMuted() && game.getAssets().hasSound(AssetKey.SFX_NOPASS)) {
                        game.getAssets().getSound(AssetKey.SFX_NOPASS).play(0.35f);
                    }
                    showToast("Chưa mở khóa Màn " + clickedLevel + "! (F9 để mở khóa tất cả)", 2.2f);
                    return false;
                }
                if (clickedLevel == selectedLevel && Math.abs(targetWaypointPos - minerWaypointPos) <= 0.1f) {
                    launchSelectedLevel();
                    return true;
                }
                selectLevel(clickedLevel);
                return false;
            }
        }
        return false;
    }

    private void stepSelection(int delta) {
        int next = Math.max(1, Math.min(LEVEL_COUNT, selectedLevel + delta));
        if (next != selectedLevel && game.isLevelUnlocked(next)) {
            selectLevel(next);
        }
    }

    private void selectLevel(int level) {
        this.selectedLevel = level;
        this.targetWaypointPos = nodeWaypointIdx[level - 1];
        if (!game.isMuted() && game.getAssets().hasSound(AssetKey.SFX_TALLY)) {
            game.getAssets().getSound(AssetKey.SFX_TALLY).play(0.45f);
        }
    }

    private void launchSelectedLevel() {
        if (!game.isLevelUnlocked(selectedLevel)) {
            return;
        }
        if (!game.isMuted() && game.getAssets().hasSound(AssetKey.SFX_SHOP_BUY)) {
            game.getAssets().getSound(AssetKey.SFX_SHOP_BUY).play(0.6f);
        }
        game.startMapLevel(selectedLevel);
    }

    private void showToast(String msg, float durationSec) {
        this.toastText = msg;
        this.toastTimer = durationSec;
    }

    private float sampleWaypointWorldX(float pos) {
        int i0 = MathUtils.clamp((int) Math.floor(pos), 0, waypointCount - 1);
        int i1 = MathUtils.clamp(i0 + 1, 0, waypointCount - 1);
        float alpha = pos - i0;
        return MathUtils.lerp(waypointWorldX[i0], waypointWorldX[i1], alpha);
    }

    private float sampleWaypointWorldY(float pos) {
        int i0 = MathUtils.clamp((int) Math.floor(pos), 0, waypointCount - 1);
        int i1 = MathUtils.clamp(i0 + 1, 0, waypointCount - 1);
        float alpha = pos - i0;
        return MathUtils.lerp(waypointWorldY[i0], waypointWorldY[i1], alpha);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        camera.position.set(WorldBounds.WORLD_WIDTH * 0.5f, WorldBounds.WORLD_HEIGHT * 0.5f, 0f);
        camera.update();

        viewWidth = viewport.getWorldWidth();
        viewHeight = viewport.getWorldHeight();
        viewLeft = camera.position.x - viewWidth * 0.5f;
        viewBottom = camera.position.y - viewHeight * 0.5f;

        float bgScale = Math.max(viewWidth / WorldBounds.WORLD_WIDTH, viewHeight / WorldBounds.WORLD_HEIGHT);
        bgDrawW = WorldBounds.WORLD_WIDTH * bgScale;
        bgDrawH = WorldBounds.WORLD_HEIGHT * bgScale;
        bgDrawX = MenuTopHeader.computeCoverFillX(viewLeft, viewWidth, bgDrawW);
        bgDrawY = MenuTopHeader.computeCoverFillY(viewBottom, viewHeight, bgDrawH);

        for (int i = 0; i < LEVEL_COUNT; i++) {
            float wx = bgDrawX + nodeNx[i] * bgDrawW;
            float wy = bgDrawY + nodeNy[i] * bgDrawH;
            nodeWorldX[i] = wx;
            nodeWorldY[i] = wy;
            nodeHitboxes[i].set(wx - 20f, wy - 14f, 40f, 28f);
        }
        for (int i = 0; i < waypointCount; i++) {
            waypointWorldX[i] = bgDrawX + waypointNx[i] * bgDrawW;
            waypointWorldY[i] = bgDrawY + waypointNy[i] * bgDrawH;
        }

        float viewRight = viewLeft + viewWidth;
        int px = MathUtils.floor(viewRight - 124f);
        int py = MathUtils.floor(viewBottom + 8f);
        previewBoxRect.set(px, py, 116f, 76f);
        startButtonRect.set(px + 10f, py + 8f, 96f, 22f);

        topHeader.updateLayout(viewLeft, viewBottom, viewWidth, viewHeight);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        Gdx.input.setInputProcessor(null);
    }
}
