package com.minhtriet79.goldminer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.minhtriet79.goldminer.GoldMinerGame;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.gameplay.ui.MenuTopHeader;

/**
 * Pure 16-bit Pixel Art World Hub screen with 4 wooden-framed destination cards:
 * {@code GOLD MINE} (playable), {@code CASINO}, {@code ARCADE}, and {@code SHOP & DECOR} (Coming Soon).
 */
public class WorldHubScreen implements Screen {

    private static final Color COL_GOLD = new Color(1.00f, 0.85f, 0.28f, 1f);
    private static final Color COL_CREAM = new Color(0.99f, 0.95f, 0.82f, 1f);
    private static final Color COL_SCRIM = new Color(0.03f, 0.02f, 0.01f, 0.64f);

    private static final String[] CARD_ART_KEYS = {
        AssetKey.HUB_CARD_MINE,
        AssetKey.HUB_CARD_CASINO,
        AssetKey.HUB_CARD_ARCADE,
        AssetKey.HUB_CARD_SHOP
    };
    private static final String[] CARD_LABELS = {
        "GOLD MINE",
        "CASINO",
        "ARCADE",
        "SHOP & DECOR"
    };

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final ExtendViewport viewport;
    private final MenuTopHeader topHeader;
    private final Vector3 touchPoint = new Vector3();

    private final Rectangle[] cardRects = {
        new Rectangle(66f, 122f, 164f, 88f),
        new Rectangle(250f, 122f, 164f, 88f),
        new Rectangle(66f, 20f, 164f, 88f),
        new Rectangle(250f, 20f, 164f, 88f)
    };
    private final Rectangle modalRect = new Rectangle(112f, 70f, 256f, 124f);
    private final Rectangle modalOkRect = new Rectangle(194f, 82f, 92f, 22f);

    private float viewLeft = 0f;
    private float viewBottom = 0f;
    private float viewWidth = WorldBounds.WORLD_WIDTH;
    private float viewHeight = WorldBounds.WORLD_HEIGHT;
    private float bgDrawX = 0f;
    private float bgDrawY = 0f;
    private float bgDrawW = WorldBounds.WORLD_WIDTH;
    private float bgDrawH = WorldBounds.WORLD_HEIGHT;

    private String comingSoonFeature = null;
    private float elapsedTime = 0f;

    // Pre-allocated zero-GC Game Juice state
    private final float[] cardHoverAlpha = new float[4];
    private final float[] cardSheenTime = new float[4];
    private int prevHoveredCard = -1;

    private static final int CLOUD_COUNT = 3;
    private final float[] cloudBaseNx = {0.12f, 0.50f, 0.84f};
    private final float[] cloudNy = {0.82f, 0.76f, 0.86f};
    private final float[] cloudW = {46f, 58f, 38f};

    private static final int POLLEN_COUNT = 12;
    private final float[] pollenNx = {0.06f, 0.14f, 0.21f, 0.34f, 0.48f, 0.54f, 0.68f, 0.77f, 0.88f, 0.94f, 0.29f, 0.62f};
    private final float[] pollenNy = {0.22f, 0.64f, 0.41f, 0.78f, 0.30f, 0.56f, 0.19f, 0.68f, 0.44f, 0.74f, 0.15f, 0.83f};
    private final float[] pollenPhase = {0.1f, 0.9f, 1.7f, 2.5f, 0.5f, 1.3f, 2.2f, 3.0f, 0.8f, 1.9f, 2.8f, 1.1f};

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

    public WorldHubScreen(GoldMinerGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new ExtendViewport(WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT, camera);
        this.topHeader = new MenuTopHeader(game, "WORLD HUB", false);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputAdapter);
        topHeader.refreshAccountCache();
        game.playMusic(AssetKey.BGM_MENU, 0.42f);
    }

    @Override
    public void render(float rawDt) {
        float dt = Math.min(rawDt, 0.05f);
        elapsedTime += dt;

        touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touchPoint);

        if (handleInput()) {
            return;
        }

        boolean blocked = comingSoonFeature != null || topHeader.isBlockingModalOpen() || topHeader.isDropdownOpen();
        int hoveredIdx = -1;
        for (int i = 0; i < 4; i++) {
            boolean hover = !blocked && cardRects[i].contains(touchPoint.x, touchPoint.y);
            if (hover) {
                hoveredIdx = i;
            }
            cardHoverAlpha[i] = MathUtils.clamp(cardHoverAlpha[i] + (hover ? 12f : -10f) * dt, 0f, 1f);
            if (cardSheenTime[i] > 0f) {
                cardSheenTime[i] += dt;
                if (cardSheenTime[i] > 0.42f) {
                    cardSheenTime[i] = 0f;
                }
            }
        }
        if (hoveredIdx != prevHoveredCard && hoveredIdx >= 0) {
            game.playHoverSfx();
            cardSheenTime[hoveredIdx] = 0.0001f;
        }
        prevHoveredCard = hoveredIdx;

        ScreenUtils.clear(0.08f, 0.04f, 0.02f, 1f);
        viewport.apply();

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        Texture white = game.getWhitePixel();
        BitmapFont small = game.getPixelSmallFont();
        BitmapFont hud = game.getPixelHudFont();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Cover-fill 16-bit valley backdrop + warm translucent scrim + ambient clouds/pollen
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.BG_HUB_CANYON), bgDrawX, bgDrawY, bgDrawW, bgDrawH);
        drawHubAtmosphere(batch, white);

        // 2x2 Wooden-brass Hub Cards with smooth lift, scale, sheen, & unified gold static text
        for (int i = 0; i < 4; i++) {
            Rectangle r = cardRects[i];
            float ha = cardHoverAlpha[i];
            float scale = 1.0f + 0.035f * ha;
            float liftY = 3.0f * ha;
            float cx = r.x + r.width * 0.5f;
            float cy = r.y + r.height * 0.5f + liftY;
            float cardW = r.width * scale;
            float cardH = r.height * scale;
            float drawX = MathUtils.floor(cx - cardW * 0.5f);
            float drawY = MathUtils.floor(cy - cardH * 0.5f);

            float artW = 156f * scale;
            float artH = 66f * scale;
            batch.setColor(Color.WHITE);
            batch.draw(assets.getTexture(CARD_ART_KEYS[i]),
                MathUtils.floor(drawX + 4f * scale), MathUtils.floor(drawY + 18f * scale),
                MathUtils.floor(artW), MathUtils.floor(artH));

            if (cardSheenTime[i] > 0f) {
                drawCardSheen(batch, white, drawX + 4f * scale, drawY + 18f * scale, artW, artH, cardSheenTime[i] / 0.42f);
            }

            boolean showOver = (hoveredIdx == i) || ha > 0.35f;
            batch.setColor(Color.WHITE);
            batch.draw(
                assets.getTexture(showOver ? AssetKey.UI_HUB_CARD_FRAME_OVER : AssetKey.UI_HUB_CARD_FRAME),
                drawX, drawY, MathUtils.floor(cardW), MathUtils.floor(cardH)
            );
            float labelY = MathUtils.floor(drawY + 4f * scale + (14f * scale + hud.getCapHeight()) * 0.5f);
            MenuTopHeader.drawShadowText(hud, batch, CARD_LABELS[i], drawX, labelY, MathUtils.floor(cardW), Align.center, false,
                showOver ? COL_CREAM : COL_GOLD);
        }

        // Shared Top Chrome + Profile Card
        topHeader.draw(batch, touchPoint.x, touchPoint.y, elapsedTime);

        // Coming Soon Wooden Modal
        if (comingSoonFeature != null) {
            drawComingSoonModal(batch, assets, white, small, hud);
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawHubAtmosphere(SpriteBatch batch, Texture white) {
        // Soft warm sepia-gold scrim so wooden cards pop cleanly over scenic horizon
        batch.setColor(0.16f, 0.10f, 0.05f, 0.16f);
        batch.draw(white, viewLeft - 2f, viewBottom - 2f, viewWidth + 4f, viewHeight + 4f);

        // Gently drifting layered pixel clouds
        for (int i = 0; i < CLOUD_COUNT; i++) {
            float nx = (cloudBaseNx[i] + elapsedTime * (0.008f + i * 0.003f)) % 1.2f - 0.1f;
            float cx = MathUtils.floor(viewLeft + nx * viewWidth);
            float cy = MathUtils.floor(viewBottom + cloudNy[i] * viewHeight);
            float w = cloudW[i];
            batch.setColor(1.00f, 0.97f, 0.86f, 0.20f);
            batch.draw(white, cx, cy, w, 4f);
            batch.draw(white, cx + 6f, cy + 4f, w - 14f, 3f);
        }

        // Drifting warm pollen sparkles behind cards
        for (int i = 0; i < POLLEN_COUNT; i++) {
            float nx = (pollenNx[i] + elapsedTime * 0.012f + 0.014f * MathUtils.sin(elapsedTime + pollenPhase[i])) % 1.0f;
            float ny = (pollenNy[i] + elapsedTime * 0.015f) % 0.92f + 0.04f;
            float alpha = 0.18f + 0.18f * (0.5f + 0.5f * MathUtils.sin(elapsedTime * 2.4f + pollenPhase[i]));
            batch.setColor(1.00f, 0.94f, 0.68f, alpha);
            batch.draw(white, MathUtils.floor(viewLeft + nx * viewWidth), MathUtils.floor(viewBottom + ny * viewHeight),
                (i % 3 == 0) ? 2f : 1f, (i % 3 == 0) ? 2f : 1f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawCardSheen(SpriteBatch batch, Texture white, float x, float y, float w, float h, float t) {
        float bandCenterX = x + MathUtils.lerp(-18f, w + 18f, t);
        int strips = 6;
        float stripH = h / strips;
        batch.setColor(1.00f, 0.98f, 0.86f, 0.18f * (1f - Math.abs(t - 0.5f) * 1.4f));
        for (int s = 0; s < strips; s++) {
            float sx = bandCenterX + (s - strips * 0.5f) * 5f;
            float left = Math.max(x, sx - 6f);
            float right = Math.min(x + w, sx + 6f);
            if (right > left) {
                batch.draw(white, MathUtils.floor(left), MathUtils.floor(y + s * stripH),
                    MathUtils.floor(right - left), MathUtils.ceil(stripH));
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawComingSoonModal(
        SpriteBatch batch, Assets assets, Texture white, BitmapFont small, BitmapFont hud
    ) {
        batch.setColor(COL_SCRIM);
        batch.draw(white, viewLeft - 2f, viewBottom - 2f, viewWidth + 4f, viewHeight + 4f);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_RENAME_PANEL),
            modalRect.x, modalRect.y, modalRect.width, modalRect.height);

        MenuTopHeader.drawShadowText(hud, batch, "COMING SOON!",
            modalRect.x, modalRect.y + 105f, modalRect.width, Align.center, false, COL_GOLD);
        MenuTopHeader.drawShadowText(small, batch,
            "Khu vực " + comingSoonFeature + " đang được hoàn thiện.",
            modalRect.x + 14f, modalRect.y + 78f, modalRect.width - 28f, Align.center, true, COL_CREAM);

        boolean overOk = modalOkRect.contains(touchPoint.x, touchPoint.y);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(overOk ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP),
            modalOkRect.x, modalOkRect.y, modalOkRect.width, modalOkRect.height);
        float okY = MathUtils.floor(modalOkRect.y + (modalOkRect.height + small.getCapHeight()) * 0.5f);
        MenuTopHeader.drawShadowText(small, batch, "ĐÓNG",
            modalOkRect.x, okY, modalOkRect.width, Align.center, false, Color.WHITE);
    }

    private boolean handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (comingSoonFeature != null) {
                comingSoonFeature = null;
                return false;
            }
            if (topHeader.handleEscape()) {
                return false;
            }
            game.showMainMenu();
            return true;
        }

        if (!Gdx.input.justTouched()) {
            return false;
        }

        if (comingSoonFeature != null) {
            if (modalOkRect.contains(touchPoint.x, touchPoint.y) || !modalRect.contains(touchPoint.x, touchPoint.y)) {
                comingSoonFeature = null;
            }
            return false;
        }

        if (topHeader.handleClick(touchPoint.x, touchPoint.y, game::showMainMenu)) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            if (cardRects[i].contains(touchPoint.x, touchPoint.y)) {
                if (i == 0) {
                    playClickSound();
                    game.showGoldMineSelector();
                    return true;
                }
                comingSoonFeature = CARD_LABELS[i];
                return false;
            }
        }
        return false;
    }

    private void playClickSound() {
        if (!game.isMuted() && game.getAssets().hasSound(AssetKey.SFX_SHOP_BUY)) {
            game.getAssets().getSound(AssetKey.SFX_SHOP_BUY).play(0.55f);
        }
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
