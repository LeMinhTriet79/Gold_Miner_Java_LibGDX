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

        boolean blocked = comingSoonOpen || topHeader.isBlockingModalOpen() || topHeader.isDropdownOpen();
        drawModeCard(batch, assets, small, hud, card1pRect, AssetKey.MODE_CARD_1P,
            "1 PLAYER", "STORY CAMPAIGN", !blocked && card1pRect.contains(touchPoint.x, touchPoint.y));
        drawModeCard(batch, assets, small, hud, card2pRect, AssetKey.MODE_CARD_2P,
            "2 PLAYERS", "LOCAL VERSUS", !blocked && card2pRect.contains(touchPoint.x, touchPoint.y));

        topHeader.draw(batch, touchPoint.x, touchPoint.y, elapsedTime);

        if (comingSoonOpen) {
            drawComingSoonModal(batch, assets, white, small, hud);
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawModeCard(
        SpriteBatch batch, Assets assets, BitmapFont small, BitmapFont hud,
        Rectangle rect, String artKey, String title, String subtitle, boolean hovered
    ) {
        int drawX = MathUtils.floor(rect.x);
        int drawY = MathUtils.floor(rect.y) + (hovered ? 1 : 0);

        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(artKey), drawX + 4, drawY + 40, 156f, 124f);
        batch.draw(
            assets.getTexture(hovered ? AssetKey.UI_MODE_CARD_FRAME_OVER : AssetKey.UI_MODE_CARD_FRAME),
            drawX, drawY, rect.width, rect.height
        );
        MenuTopHeader.drawShadowText(hud, batch, title, drawX, drawY + 33f, rect.width, Align.center, false,
            hovered ? Color.WHITE : COL_GOLD);
        MenuTopHeader.drawShadowText(small, batch, subtitle, drawX, drawY + 17f, rect.width, Align.center, false, COL_CREAM);
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
