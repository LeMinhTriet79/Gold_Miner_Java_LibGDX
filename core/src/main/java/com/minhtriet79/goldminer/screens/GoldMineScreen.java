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
 * Pure 16-bit Pixel Art Gold Mine mode selection screen:
 * {@code 1 PLAYER - STORY CAMPAIGN} (routes to {@link WorldMapScreen}) vs {@code 2 PLAYERS - LOCAL VERSUS} (Coming Soon).
 */
public class GoldMineScreen implements Screen {

    private static final Color COL_GOLD = new Color(1.00f, 0.85f, 0.28f, 1f);
    private static final Color COL_CREAM = new Color(0.99f, 0.95f, 0.82f, 1f);
    private static final Color COL_SCRIM = new Color(0.03f, 0.02f, 0.01f, 0.64f);

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final ExtendViewport viewport;
    private final MenuTopHeader topHeader;
    private final Vector3 touchPoint = new Vector3();

    private final Rectangle card1pRect = new Rectangle(66f, 36f, 164f, 168f);
    private final Rectangle card2pRect = new Rectangle(250f, 36f, 164f, 168f);
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

    private boolean comingSoonOpen = false;
    private float elapsedTime = 0f;

    // Pre-allocated zero-GC Game Juice state
    private final float[] cardHoverAlpha = new float[2];
    private final float[] cardSheenTime = new float[2];
    private int prevHoveredCard = -1;

    private static final int CLOUD_COUNT = 3;
    private final float[] cloudBaseNx = {0.10f, 0.48f, 0.82f};
    private final float[] cloudNy = {0.81f, 0.75f, 0.85f};
    private final float[] cloudW = {46f, 56f, 40f};

    private static final int POLLEN_COUNT = 12;
    private final float[] pollenNx = {0.07f, 0.15f, 0.23f, 0.36f, 0.47f, 0.56f, 0.67f, 0.76f, 0.87f, 0.93f, 0.30f, 0.61f};
    private final float[] pollenNy = {0.24f, 0.66f, 0.40f, 0.77f, 0.31f, 0.55f, 0.21f, 0.67f, 0.43f, 0.72f, 0.16f, 0.82f};
    private final float[] pollenPhase = {0.2f, 1.0f, 1.8f, 2.6f, 0.4f, 1.4f, 2.1f, 2.9f, 0.7f, 1.7f, 2.4f, 1.2f};

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

    public GoldMineScreen(GoldMinerGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new ExtendViewport(WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT, camera);
        this.topHeader = new MenuTopHeader(game, "GOLD MINE", false);
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

        boolean blocked = comingSoonOpen || topHeader.isBlockingModalOpen() || topHeader.isDropdownOpen();
        boolean over1p = !blocked && card1pRect.contains(touchPoint.x, touchPoint.y);
        boolean over2p = !blocked && card2pRect.contains(touchPoint.x, touchPoint.y);
        int hoveredIdx = over1p ? 0 : (over2p ? 1 : -1);
        for (int i = 0; i < 2; i++) {
            boolean h = (hoveredIdx == i);
            cardHoverAlpha[i] = MathUtils.clamp(cardHoverAlpha[i] + (h ? 12f : -10f) * dt, 0f, 1f);
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

        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.BG_HUB_CANYON), bgDrawX, bgDrawY, bgDrawW, bgDrawH);
        drawMineAtmosphere(batch, white);

        drawModeCard(batch, assets, white, small, hud, card1pRect, AssetKey.MODE_CARD_1P,
            "1 PLAYER", "STORY CAMPAIGN", cardHoverAlpha[0], cardSheenTime[0], hoveredIdx == 0);
        drawModeCard(batch, assets, white, small, hud, card2pRect, AssetKey.MODE_CARD_2P,
            "2 PLAYERS", "LOCAL VERSUS", cardHoverAlpha[1], cardSheenTime[1], hoveredIdx == 1);

        topHeader.draw(batch, touchPoint.x, touchPoint.y, elapsedTime);

        if (comingSoonOpen) {
            drawComingSoonModal(batch, assets, white, small, hud);
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawModeCard(
        SpriteBatch batch, Assets assets, Texture white, BitmapFont small, BitmapFont hud,
        Rectangle rect, String artKey, String title, String subtitle,
        float hoverAlpha, float sheenTime, boolean hoveredNow
    ) {
        float scale = 1.0f + 0.035f * hoverAlpha;
        float liftY = 3.0f * hoverAlpha;
        float cx = rect.x + rect.width * 0.5f;
        float cy = rect.y + rect.height * 0.5f + liftY;
        float cardW = rect.width * scale;
        float cardH = rect.height * scale;
        float drawX = MathUtils.floor(cx - cardW * 0.5f);
        float drawY = MathUtils.floor(cy - cardH * 0.5f);

        float artX = MathUtils.floor(drawX + 4f * scale);
        float artY = MathUtils.floor(drawY + 40f * scale);
        float artW = 156f * scale;
        float artH = 124f * scale;
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(artKey), artX, artY, MathUtils.floor(artW), MathUtils.floor(artH));

        if (sheenTime > 0f) {
            drawCardSheen(batch, white, artX, artY, artW, artH, sheenTime / 0.42f);
        }

        boolean showOver = hoveredNow || hoverAlpha > 0.35f;
        batch.setColor(Color.WHITE);
        batch.draw(
            assets.getTexture(showOver ? AssetKey.UI_MODE_CARD_FRAME_OVER : AssetKey.UI_MODE_CARD_FRAME),
            drawX, drawY, MathUtils.floor(cardW), MathUtils.floor(cardH)
        );
        MenuTopHeader.drawShadowText(hud, batch, title, drawX, MathUtils.floor(drawY + 33f * scale),
            MathUtils.floor(cardW), Align.center, false, showOver ? COL_CREAM : COL_GOLD);
        MenuTopHeader.drawShadowText(small, batch, subtitle, drawX, MathUtils.floor(drawY + 17f * scale),
            MathUtils.floor(cardW), Align.center, false, COL_CREAM);
    }

    private void drawMineAtmosphere(SpriteBatch batch, Texture white) {
        batch.setColor(0.16f, 0.10f, 0.05f, 0.16f);
        batch.draw(white, viewLeft - 2f, viewBottom - 2f, viewWidth + 4f, viewHeight + 4f);

        for (int i = 0; i < CLOUD_COUNT; i++) {
            float nx = (cloudBaseNx[i] + elapsedTime * (0.008f + i * 0.003f)) % 1.2f - 0.1f;
            float cx = MathUtils.floor(viewLeft + nx * viewWidth);
            float cy = MathUtils.floor(viewBottom + cloudNy[i] * viewHeight);
            float w = cloudW[i];
            batch.setColor(1.00f, 0.97f, 0.86f, 0.20f);
            batch.draw(white, cx, cy, w, 4f);
            batch.draw(white, cx + 6f, cy + 4f, w - 14f, 3f);
        }

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
        int strips = 8;
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
            "Chế độ 2 PLAYERS (Đấu đối kháng) đang được phát triển.",
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
            if (comingSoonOpen) {
                comingSoonOpen = false;
                return false;
            }
            if (topHeader.handleEscape()) {
                return false;
            }
            game.showWorldHub();
            return true;
        }
        if (!comingSoonOpen && !topHeader.isBlockingModalOpen()
            && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            openStoryMap();
            return true;
        }
        if (!Gdx.input.justTouched()) {
            return false;
        }
        if (comingSoonOpen) {
            if (modalOkRect.contains(touchPoint.x, touchPoint.y) || !modalRect.contains(touchPoint.x, touchPoint.y)) {
                comingSoonOpen = false;
            }
            return false;
        }
        if (topHeader.handleClick(touchPoint.x, touchPoint.y, game::showWorldHub)) {
            return false;
        }
        if (card1pRect.contains(touchPoint.x, touchPoint.y)) {
            openStoryMap();
            return true;
        }
        if (card2pRect.contains(touchPoint.x, touchPoint.y)) {
            comingSoonOpen = true;
            return false;
        }
        return false;
    }

    private void openStoryMap() {
        if (!game.isMuted() && game.getAssets().hasSound(AssetKey.SFX_SHOP_BUY)) {
            game.getAssets().getSound(AssetKey.SFX_SHOP_BUY).play(0.55f);
        }
        game.showWorldMap(game.getMaxUnlockedLevel());
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
