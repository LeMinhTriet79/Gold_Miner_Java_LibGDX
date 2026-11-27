package com.minhtriet79.goldminer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.minhtriet79.goldminer.GoldMinerGame;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.domain.account.AccountRecord;
import com.minhtriet79.goldminer.domain.account.AccountService;
import com.minhtriet79.goldminer.domain.account.AuthResult;
import com.minhtriet79.goldminer.persistence.SaveData;
import com.minhtriet79.goldminer.studio.StudioMode;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Pure 16-bit Pixel Art Main Menu screen using {@link ExtendViewport} (base {@code 480x270},
 * zero black bars on maximize/fullscreen, {@code F11} fullscreen toggle), strict {@code Nearest}
 * texture & mono pixel-font filtering, integer pixel snapping, chasing marquee bulbs,
 * hover-glowing curved PLAY button, and offline account/auto-login UI.
 */
public class MainMenuScreen implements Screen {

    private static final int TAB_LOGIN = 0;
    private static final int TAB_REGISTER = 1;

    private static final int FIELD_NONE = -1;
    private static final int FIELD_USERNAME = 0;
    private static final int FIELD_DISPLAY_NAME = 1;
    private static final int FIELD_PASSWORD = 2;
    private static final int FIELD_CONFIRM_PASSWORD = 3;
    private static final int FIELD_RENAME = 4;

    private static final int MODAL_NONE = 0;
    private static final int MODAL_HIGH_SCORES = 1;
    private static final int MODAL_EXTRAS = 2;
    private static final int MODAL_HELP = 3;
    private static final int MODAL_RENAME = 4;

    private static final int MAX_INPUT_LEN = 24;
    private static final int MAX_LEADERBOARD_ROWS = 5;

    // 16-bit palette colors (pre-allocated, zero GC in render)
    private static final Color COL_OUTLINE = new Color(0.13f, 0.06f, 0.02f, 1f);
    private static final Color COL_TEXT_SHADOW = new Color(0.06f, 0.03f, 0.01f, 0.96f);
    private static final Color COL_POST = new Color(0.38f, 0.40f, 0.45f, 1f);
    private static final Color COL_POST_HI = new Color(0.56f, 0.59f, 0.64f, 1f);
    private static final Color COL_BAR_BORDER = new Color(68f / 255f, 36f / 255f, 14f / 255f, 1f);
    private static final Color COL_BAR_HI = new Color(246f / 255f, 200f / 255f, 108f / 255f, 1f);
    private static final Color COL_BAR_MID = new Color(230f / 255f, 168f / 255f, 48f / 255f, 1f);
    private static final Color COL_BAR_BODY = new Color(210f / 255f, 140f / 255f, 14f / 255f, 1f);
    private static final Color COL_CREAM = new Color(0.99f, 0.95f, 0.82f, 1f);
    private static final Color COL_GOLD_TEXT = new Color(1.00f, 0.85f, 0.28f, 1f);
    private static final Color COL_ERROR_TEXT = new Color(1.00f, 0.48f, 0.42f, 1f);
    private static final Color COL_OK_TEXT = new Color(0.56f, 0.96f, 0.52f, 1f);
    private static final Color COL_SCRIM = new Color(0.03f, 0.02f, 0.01f, 0.64f);

    // PLAY button geometry on 480x270 virtual grid (matches 148x124 sprite 1:1)
    private static final float PLAY_X = 278f;
    private static final float PLAY_Y = 44f;
    private static final float PLAY_W = 148f;
    private static final float PLAY_H = 124f;
    private static final float PLAY_GLOW_SCALE_W = 180f / 148f;
    private static final float PLAY_GLOW_SCALE_H = 156f / 124f;

    private static final String[] MENU_TEXTURE_KEYS = {
        AssetKey.BG_MENU_16BIT,
        AssetKey.MENU_AVATAR,
        AssetKey.MENU_COIN_ICON,
        AssetKey.MENU_BULB_LIT,
        AssetKey.MENU_BULB_GLOW,
        AssetKey.BTN_PLAY_UP,
        AssetKey.BTN_PLAY_OVER,
        AssetKey.BTN_PLAY_GLOW,
        AssetKey.UI_PANEL_AUTH,
        AssetKey.UI_ACCOUNT_CARD,
        AssetKey.UI_ACCOUNT_DROPDOWN,
        AssetKey.UI_MODAL_PANEL,
        AssetKey.UI_RENAME_PANEL,
        AssetKey.UI_BTN_TICKET_BLUE_UP,
        AssetKey.UI_BTN_TICKET_BLUE_OVER,
        AssetKey.UI_BTN_TICKET_RED_UP,
        AssetKey.UI_BTN_TICKET_RED_OVER,
        AssetKey.UI_TAB_ACTIVE,
        AssetKey.UI_TAB_INACTIVE,
        AssetKey.UI_INPUT_NORMAL,
        AssetKey.UI_INPUT_FOCUS,
        AssetKey.UI_CHECKBOX_OFF,
        AssetKey.UI_CHECKBOX_ON,
        AssetKey.UI_BTN_GREEN_UP,
        AssetKey.UI_BTN_GREEN_OVER,
        AssetKey.UI_BTN_BLUE_UP,
        AssetKey.UI_BTN_BLUE_OVER,
        AssetKey.UI_BTN_RED_UP,
        AssetKey.UI_BTN_RED_OVER
    };

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final ExtendViewport viewport;
    private final String uiFileName;
    private final Runnable backToStudioAction;

    private final Vector3 touchPoint = new Vector3();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);

    // Normalized & pre-computed world marquee bulb coordinates
    private final float[] bulbNx;
    private final float[] bulbNy;
    private final float[] bulbWorldX;
    private final float[] bulbWorldY;
    private final int bulbCount;

    // Viewport-derived cover-fill & anchor geometry (updated only in resize, zero GC in render)
    private float viewLeft = 0f;
    private float viewBottom = 0f;
    private float viewWidth = WorldBounds.WORLD_WIDTH;
    private float viewHeight = WorldBounds.WORLD_HEIGHT;
    private float bgDrawX = 0f;
    private float bgDrawY = 0f;
    private float bgDrawW = WorldBounds.WORLD_WIDTH;
    private float bgDrawH = WorldBounds.WORLD_HEIGHT;

    private int windowedWidth = 960;
    private int windowedHeight = 540;

    // Interactive bounds (pre-allocated)
    private final Rectangle backButtonRect = new Rectangle(6f, 252f, 108f, 14f);
    private final Rectangle playBounds = new Rectangle(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

    // Bottom bar ticket-border buttons (86x18 centered inside 480u base width)
    private final Rectangle btnHighScoresRect = new Rectangle(25f, 3f, 86f, 18f);
    private final Rectangle btnExtrasRect = new Rectangle(140f, 3f, 86f, 18f);
    private final Rectangle btnHelpRect = new Rectangle(255f, 3f, 86f, 18f);
    private final Rectangle btnExitRect = new Rectangle(369f, 3f, 86f, 18f);

    // Logged-out Auth Panel bounds (matches 200x190 sprite 1:1)
    private final Rectangle authPanelRect = new Rectangle(264f, 30f, 200f, 190f);
    private final Rectangle loginTabRect = new Rectangle(274f, 219f, 86f, 18f);
    private final Rectangle registerTabRect = new Rectangle(368f, 219f, 86f, 18f);

    // Login tab inputs
    private final Rectangle loginUserRect = new Rectangle(280f, 174f, 168f, 18f);
    private final Rectangle loginPassRect = new Rectangle(280f, 132f, 168f, 18f);
    private final Rectangle loginAutoCheckRect = new Rectangle(280f, 107f, 13f, 13f);
    private final Rectangle loginAutoRowRect = new Rectangle(280f, 104f, 144f, 18f);
    private final Rectangle loginSubmitRect = new Rectangle(292f, 44f, 144f, 26f);

    // Register tab inputs
    private final Rectangle regUserRect = new Rectangle(280f, 183f, 168f, 15f);
    private final Rectangle regDisplayRect = new Rectangle(280f, 152f, 168f, 15f);
    private final Rectangle regPassRect = new Rectangle(280f, 121f, 168f, 15f);
    private final Rectangle regConfirmRect = new Rectangle(280f, 90f, 168f, 15f);
    private final Rectangle regAutoCheckRect = new Rectangle(280f, 70f, 11f, 11f);
    private final Rectangle regAutoRowRect = new Rectangle(280f, 68f, 144f, 15f);
    private final Rectangle regSubmitRect = new Rectangle(292f, 36f, 144f, 22f);

    // Top-right Account Card & Dropdown (anchored dynamically to top-right of ExtendViewport in resize)
    private final Rectangle accountCardRect = new Rectangle(336f, 222f, 136f, 40f);
    private final Rectangle dropdownRect = new Rectangle(336f, 134f, 136f, 88f);
    private final Rectangle dropAutoLoginRect = new Rectangle(342f, 199f, 124f, 17f);
    private final Rectangle dropEditNameRect = new Rectangle(342f, 179f, 124f, 17f);
    private final Rectangle dropSwitchAccountRect = new Rectangle(342f, 159f, 124f, 17f);
    private final Rectangle dropLogoutRect = new Rectangle(342f, 139f, 124f, 17f);

    // Modals (matches 312x182 & 240x114 sprites 1:1)
    private final Rectangle modalRect = new Rectangle(84f, 44f, 312f, 182f);
    private final Rectangle modalCloseRect = new Rectangle(196f, 54f, 88f, 20f);
    private final Rectangle modalStudioRect = new Rectangle(120f, 134f, 240f, 24f);
    private final Rectangle modalMuteRect = new Rectangle(120f, 102f, 240f, 24f);

    private final Rectangle renameModalRect = new Rectangle(120f, 78f, 240f, 114f);
    private final Rectangle renameInputRect = new Rectangle(140f, 126f, 200f, 18f);
    private final Rectangle renameSaveRect = new Rectangle(140f, 92f, 92f, 22f);
    private final Rectangle renameCancelRect = new Rectangle(248f, 92f, 92f, 22f);

    // Input buffers & cached display strings (mutated only on user events)
    private final StringBuilder usernameBuf = new StringBuilder();
    private final StringBuilder displayNameBuf = new StringBuilder();
    private final StringBuilder passwordBuf = new StringBuilder();
    private final StringBuilder confirmBuf = new StringBuilder();
    private final StringBuilder renameBuf = new StringBuilder();
    private final StringBuilder maskScratch = new StringBuilder();

    private String usernameText = "";
    private String displayNameText = "";
    private String passwordMasked = "";
    private String confirmMasked = "";
    private String renameText = "";

    private String cachedDisplayName = "KHÁCH";
    private String cachedCoinsText = "0 XU";
    private String cachedAutoLoginText = "AUTO LOGIN: OFF";
    private String cachedSoundButtonText = "ÂM THANH: BẬT";
    private final String[] cachedLeaderboardRows = new String[MAX_LEADERBOARD_ROWS];
    private int cachedLeaderboardCount = 0;

    private int activeTab = TAB_LOGIN;
    private int focusedField = FIELD_USERNAME;
    private boolean formAutoLogin = true;
    private String authFeedback = "";
    private boolean authFeedbackError = true;
    private String renameFeedback = "";

    private boolean dropdownOpen = false;
    private int activeModal = MODAL_NONE;

    private float elapsedTime = 0f;
    private float playHoverAlpha = 0f;

    private final InputAdapter inputProcessor = new InputAdapter() {
        @Override
        public boolean keyTyped(char character) {
            return handleKeyTyped(character);
        }

        @Override
        public boolean keyDown(int keycode) {
            return handleKeyDown(keycode);
        }
    };

    public MainMenuScreen(GoldMinerGame game, String uiFileName, Runnable backToStudioAction) {
        this.game = game;
        this.uiFileName = (uiFileName == null || uiFileName.isEmpty()) ? "main_menu.json" : uiFileName;
        this.backToStudioAction = backToStudioAction;

        this.camera = new OrthographicCamera();
        this.viewport = new ExtendViewport(WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT, camera);

        float[][] loadedBulbs = loadNormalizedMarqueeBulbs();
        this.bulbCount = loadedBulbs[0].length;
        this.bulbNx = loadedBulbs[0];
        this.bulbNy = loadedBulbs[1];
        this.bulbWorldX = new float[bulbCount];
        this.bulbWorldY = new float[bulbCount];

        if (!Gdx.graphics.isFullscreen()) {
            this.windowedWidth = Math.max(480, Gdx.graphics.getWidth());
            this.windowedHeight = Math.max(270, Gdx.graphics.getHeight());
        }
        updateViewportLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        syncCachedAccountLabels();
    }

    private float[][] loadNormalizedMarqueeBulbs() {
        FileHandle handle = Gdx.files.internal("data/ui/marquee_bulbs.json");
        if (handle.exists()) {
            try {
                JsonValue root = new JsonReader().parse(handle);
                JsonValue bulbs = root.get("bulbs");
                if (bulbs != null && bulbs.size > 0) {
                    float[] nx = new float[bulbs.size];
                    float[] ny = new float[bulbs.size];
                    int idx = 0;
                    for (JsonValue entry = bulbs.child; entry != null; entry = entry.next) {
                        nx[idx] = entry.getFloat("x", 90f) / WorldBounds.WORLD_WIDTH;
                        ny[idx] = entry.getFloat("y", 220f) / WorldBounds.WORLD_HEIGHT;
                        idx++;
                    }
                    return new float[][] { nx, ny };
                }
            } catch (Exception ignored) {
                // Fall back to analytical perimeter bulbs if JSON fails
            }
        }
        int fallbackCount = 24;
        float[] nx = new float[fallbackCount];
        float[] ny = new float[fallbackCount];
        for (int i = 0; i < fallbackCount; i++) {
            float angle = (float) (i * (Math.PI * 2.0 / fallbackCount));
            nx[i] = (95f + 80f * (float) Math.sin(angle)) / WorldBounds.WORLD_WIDTH;
            ny[i] = (222f + 40f * (float) Math.cos(angle)) / WorldBounds.WORLD_HEIGHT;
        }
        return new float[][] { nx, ny };
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputProcessor);
        ensureNearestFilters(game.getAssets());
        game.playMusic(AssetKey.BGM_MENU, 0.38f);
        syncCachedAccountLabels();
    }

    private void ensureNearestFilters(Assets assets) {
        for (String key : MENU_TEXTURE_KEYS) {
            if (assets.hasTexture(key)) {
                assets.getTexture(key).setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            }
        }
        if (game.getWhitePixel() != null) {
            game.getWhitePixel().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        }
        enforceFontNearest(game.getPixelTinyFont());
        enforceFontNearest(game.getPixelSmallFont());
        enforceFontNearest(game.getPixelHudFont());
    }

    private void enforceFontNearest(BitmapFont font) {
        if (font == null) {
            return;
        }
        Array<TextureRegion> regions = font.getRegions();
        for (int i = 0; i < regions.size; i++) {
            regions.get(i).getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        }
        font.setUseIntegerPositions(true);
    }

    @Override
    public void render(float rawDt) {
        float dt = Math.min(rawDt, 0.05f);
        elapsedTime += dt;

        touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touchPoint);

        if (handleTouchAndShortcuts()) {
            return;
        }

        AccountService accountService = game.getAccountService();
        boolean loggedIn = accountService != null && accountService.isLoggedIn();
        boolean playInteractive = loggedIn && !dropdownOpen && activeModal == MODAL_NONE;
        boolean playHovered = playInteractive && playBounds.contains(touchPoint.x, touchPoint.y);

        if (playHovered) {
            playHoverAlpha = Math.min(1f, playHoverAlpha + dt * 8f);
        } else {
            playHoverAlpha = Math.max(0f, playHoverAlpha - dt * 6f);
        }

        ScreenUtils.clear(0.06f, 0.04f, 0.03f, 1f);
        viewport.apply();

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        Texture white = game.getWhitePixel();
        BitmapFont tiny = game.getPixelTinyFont();
        BitmapFont small = game.getPixelSmallFont();
        BitmapFont hud = game.getPixelHudFont();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        // 1. Cover-fill 16-bit western gold mine backdrop (zero black bars at any aspect ratio)
        batch.draw(assets.getTexture(AssetKey.BG_MENU_16BIT), bgDrawX, bgDrawY, bgDrawW, bgDrawH);

        // 2. Extended edge-to-edge 16-bit golden bottom bar across full viewport width
        drawFullWidthBottomBar(batch, white);

        // 3. Chasing marquee lights around the GOLD MINER sign rim
        drawChasingSignLights(batch, assets);

        // 4. Right side: Login/Register panel OR Top-Right Account Card + PLAY button
        if (loggedIn) {
            drawPlayButton(batch, assets, white, playHovered);
            drawAccountCard(batch, assets, white, tiny, small);
            if (dropdownOpen) {
                drawAccountDropdown(batch, assets, tiny);
            }
        } else {
            drawAuthPanel(batch, assets, white, tiny, small);
        }

        // 5. Bottom perforated ticket-border buttons centered in 480u base area
        drawBottomBarButtons(batch, assets, small);

        // 6. Optional Studio Play-Test back pill
        if (backToStudioAction != null) {
            batch.setColor(0.08f, 0.12f, 0.22f, 0.90f);
            batch.draw(white, backButtonRect.x, backButtonRect.y, backButtonRect.width, backButtonRect.height);
            batch.setColor(Color.WHITE);
            drawCenteredText(tiny, batch, "<- BACK TO STUDIO (ESC)", backButtonRect, Color.CYAN);
        }

        // 7. Modals (High Scores / Extras / Help / Rename)
        if (activeModal != MODAL_NONE) {
            drawActiveModal(batch, assets, white, tiny, small, hud);
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawFullWidthBottomBar(SpriteBatch batch, Texture white) {
        int barX = MathUtils.floor(viewLeft) - 1;
        int barW = MathUtils.ceil(viewWidth) + 2;
        int barBottom = MathUtils.floor(viewBottom) - 1;
        int bodyH = Math.max(21, 21 - barBottom);

        batch.setColor(COL_BAR_BODY);
        batch.draw(white, barX, barBottom, barW, bodyH);
        batch.setColor(COL_BAR_MID);
        batch.draw(white, barX, 21f, barW, 1f);
        batch.setColor(COL_BAR_HI);
        batch.draw(white, barX, 22f, barW, 1f);
        batch.setColor(COL_BAR_BORDER);
        batch.draw(white, barX, 23f, barW, 1f);
        batch.setColor(Color.WHITE);
    }

    private void drawChasingSignLights(SpriteBatch batch, Assets assets) {
        float chasePhase = (elapsedTime * 5.0f) % 4.0f;
        Texture bulbGlow = assets.getTexture(AssetKey.MENU_BULB_GLOW);
        Texture bulbLit = assets.getTexture(AssetKey.MENU_BULB_LIT);

        for (int i = 0; i < bulbCount; i++) {
            float mod = (i - chasePhase) % 4.0f;
            if (mod < 0f) {
                mod += 4.0f;
            }
            float intensity;
            if (mod < 1.0f) {
                intensity = 0.55f + 0.45f * (float) Math.sin(mod * Math.PI);
            } else if (mod < 2.0f) {
                intensity = 0.55f * (1.0f - (mod - 1.0f));
            } else {
                intensity = 0f;
            }
            if (intensity <= 0.02f) {
                continue;
            }

            float bx = bulbWorldX[i];
            float by = bulbWorldY[i];

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(0.76f, 0.94f, 1.0f, 0.50f * intensity);
            batch.draw(bulbGlow, MathUtils.floor(bx - 5f), MathUtils.floor(by - 5f), 10f, 10f);

            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(1f, 1f, 1f, 0.96f * intensity);
            batch.draw(bulbLit, MathUtils.floor(bx - 3f), MathUtils.floor(by - 3f), 6f, 6f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawPlayButton(SpriteBatch batch, Assets assets, Texture white, boolean playHovered) {
        // 16-bit post below PLAY button down to bottom bar
        int postX = MathUtils.floor(PLAY_X + PLAY_W * 0.5f - 3f);
        batch.setColor(COL_OUTLINE);
        batch.draw(white, postX - 1f, 24f, 7f, 26f);
        batch.setColor(COL_POST);
        batch.draw(white, postX, 24f, 5f, 26f);
        batch.setColor(COL_POST_HI);
        batch.draw(white, postX + 1f, 24f, 1f, 26f);

        float breathe = 0.5f + 0.5f * (float) Math.sin(elapsedTime * 4.2f);
        float scale = 1.0f + 0.05f * playHoverAlpha + 0.01f * playHoverAlpha * breathe;

        float centerX = PLAY_X + PLAY_W * 0.5f;
        float centerY = PLAY_Y + PLAY_H * 0.5f;
        float drawW = PLAY_W * scale;
        float drawH = PLAY_H * scale;
        float drawX = centerX - drawW * 0.5f;
        float drawY = centerY - drawH * 0.5f;
        if (playHoverAlpha < 0.005f) {
            drawX = MathUtils.floor(PLAY_X);
            drawY = MathUtils.floor(PLAY_Y);
            drawW = PLAY_W;
            drawH = PLAY_H;
        }

        // Stepped 16-bit golden aura behind PLAY button on hover
        if (playHoverAlpha > 0.01f) {
            Texture glowTex = assets.getTexture(AssetKey.BTN_PLAY_GLOW);
            float glowW = drawW * PLAY_GLOW_SCALE_W;
            float glowH = drawH * PLAY_GLOW_SCALE_H;
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 0.90f, 0.42f, (0.58f + 0.24f * breathe) * playHoverAlpha);
            batch.draw(glowTex, MathUtils.floor(centerX - glowW * 0.5f), MathUtils.floor(centerY - glowH * 0.5f), glowW, glowH);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        boolean showOver = playHovered || playHoverAlpha > 0.35f;
        Texture btnTex = assets.getTexture(showOver ? AssetKey.BTN_PLAY_OVER : AssetKey.BTN_PLAY_UP);
        batch.setColor(Color.WHITE);
        batch.draw(btnTex, MathUtils.floor(drawX), MathUtils.floor(drawY), MathUtils.floor(drawW), MathUtils.floor(drawH));

        if (playHoverAlpha > 0.01f) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 0.95f, 0.65f, 0.18f * playHoverAlpha * (0.7f + 0.3f * breathe));
            batch.draw(btnTex, MathUtils.floor(drawX), MathUtils.floor(drawY), MathUtils.floor(drawW), MathUtils.floor(drawH));
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawAuthPanel(SpriteBatch batch, Assets assets, Texture white, BitmapFont tiny, BitmapFont small) {
        drawSpriteTab(batch, assets, small, loginTabRect, "LOGIN", activeTab == TAB_LOGIN);
        drawSpriteTab(batch, assets, small, registerTabRect, "REGISTER", activeTab == TAB_REGISTER);

        batch.setColor(Color.WHITE);
        batch.draw(
            assets.getTexture(AssetKey.UI_PANEL_AUTH),
            MathUtils.floor(authPanelRect.x), MathUtils.floor(authPanelRect.y),
            authPanelRect.width, authPanelRect.height
        );

        if (activeTab == TAB_LOGIN) {
            drawSpriteField(batch, assets, white, tiny, small, "USERNAME", usernameText, loginUserRect, focusedField == FIELD_USERNAME, true);
            drawSpriteField(batch, assets, white, tiny, small, "PASSWORD", passwordMasked, loginPassRect, focusedField == FIELD_PASSWORD, true);
            drawSpriteCheckbox(batch, assets, small, loginAutoCheckRect, formAutoLogin, "AUTO LOGIN");

            if (!authFeedback.isEmpty()) {
                drawShadowText(tiny, batch, authFeedback, authPanelRect.x + 10f, 92f, authPanelRect.width - 20f, Align.center, true,
                    authFeedbackError ? COL_ERROR_TEXT : COL_OK_TEXT);
            }

            boolean overLogin = loginSubmitRect.contains(touchPoint.x, touchPoint.y) && activeModal == MODAL_NONE;
            drawSpriteButton(batch, assets, small, loginSubmitRect, "LOGIN",
                overLogin ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP, Color.WHITE);
        } else {
            drawSpriteField(batch, assets, white, tiny, small, "USERNAME", usernameText, regUserRect, focusedField == FIELD_USERNAME, false);
            drawSpriteField(batch, assets, white, tiny, small, "DISPLAY NAME", displayNameText, regDisplayRect, focusedField == FIELD_DISPLAY_NAME, false);
            drawSpriteField(batch, assets, white, tiny, small, "PASSWORD", passwordMasked, regPassRect, focusedField == FIELD_PASSWORD, false);
            drawSpriteField(batch, assets, white, tiny, small, "CONFIRM PASSWORD", confirmMasked, regConfirmRect, focusedField == FIELD_CONFIRM_PASSWORD, false);
            drawSpriteCheckbox(batch, assets, tiny, regAutoCheckRect, formAutoLogin, "AUTO LOGIN");

            if (!authFeedback.isEmpty()) {
                drawShadowText(tiny, batch, authFeedback, authPanelRect.x + 8f, 67f, authPanelRect.width - 16f, Align.center, false,
                    authFeedbackError ? COL_ERROR_TEXT : COL_OK_TEXT);
            }

            boolean overReg = regSubmitRect.contains(touchPoint.x, touchPoint.y) && activeModal == MODAL_NONE;
            drawSpriteButton(batch, assets, small, regSubmitRect, "REGISTER",
                overReg ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP, Color.WHITE);
        }
    }

    private void drawAccountCard(SpriteBatch batch, Assets assets, Texture white, BitmapFont tiny, BitmapFont small) {
        int cardX = MathUtils.floor(accountCardRect.x);
        int cardY = MathUtils.floor(accountCardRect.y);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_ACCOUNT_CARD), cardX, cardY, accountCardRect.width, accountCardRect.height);

        // Exact matching quantized 16-bit miner portrait inside recessed frame (x+6, y+5, 30x30)
        batch.draw(assets.getTexture(AssetKey.MENU_AVATAR), cardX + 6f, cardY + 5f, 30f, 30f);

        // Display name + pixel expand/collapse triangle arrow
        drawShadowText(small, batch, cachedDisplayName, cardX + 41f, cardY + 32f, 78f, Align.left, false, COL_CREAM);
        drawDropdownArrow(batch, white, cardX + 123, cardY + 27, dropdownOpen);

        // Coin icon + balance
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.MENU_COIN_ICON), cardX + 41f, cardY + 8f, 10f, 10f);
        drawShadowText(tiny, batch, cachedCoinsText, cardX + 54f, cardY + 17f, 76f, Align.left, false, COL_GOLD_TEXT);
    }

    private void drawAccountDropdown(SpriteBatch batch, Assets assets, BitmapFont tiny) {
        batch.setColor(Color.WHITE);
        batch.draw(
            assets.getTexture(AssetKey.UI_ACCOUNT_DROPDOWN),
            MathUtils.floor(dropdownRect.x), MathUtils.floor(dropdownRect.y),
            dropdownRect.width, dropdownRect.height
        );

        // 1. Auto Login toggle row
        boolean overAuto = dropAutoLoginRect.contains(touchPoint.x, touchPoint.y) && activeModal == MODAL_NONE;
        batch.draw(
            assets.getTexture(overAuto ? AssetKey.UI_INPUT_FOCUS : AssetKey.UI_INPUT_NORMAL),
            MathUtils.floor(dropAutoLoginRect.x), MathUtils.floor(dropAutoLoginRect.y),
            dropAutoLoginRect.width, dropAutoLoginRect.height
        );

        boolean autoOn = game.getAccountService().isAutoLoginEnabled();
        batch.draw(
            assets.getTexture(autoOn ? AssetKey.UI_CHECKBOX_ON : AssetKey.UI_CHECKBOX_OFF),
            MathUtils.floor(dropAutoLoginRect.x + 3f), MathUtils.floor(dropAutoLoginRect.y + 3f), 11f, 11f
        );
        float autoTextY = MathUtils.floor(dropAutoLoginRect.y + (dropAutoLoginRect.height + tiny.getCapHeight()) * 0.5f);
        drawShadowText(tiny, batch, cachedAutoLoginText, MathUtils.floor(dropAutoLoginRect.x + 17f), autoTextY,
            dropAutoLoginRect.width - 20f, Align.left, false, COL_CREAM);

        // 2. Edit Display Name
        boolean overEdit = dropEditNameRect.contains(touchPoint.x, touchPoint.y) && activeModal == MODAL_NONE;
        drawSpriteButton(batch, assets, tiny, dropEditNameRect, "EDIT NAME",
            overEdit ? AssetKey.UI_BTN_BLUE_OVER : AssetKey.UI_BTN_BLUE_UP, Color.WHITE);

        // 3. Switch Account
        boolean overSwitch = dropSwitchAccountRect.contains(touchPoint.x, touchPoint.y) && activeModal == MODAL_NONE;
        drawSpriteButton(batch, assets, tiny, dropSwitchAccountRect, "SWITCH ACCOUNT",
            overSwitch ? AssetKey.UI_BTN_BLUE_OVER : AssetKey.UI_BTN_BLUE_UP, Color.WHITE);

        // 4. Logout (strictly hidden inside dropdown!)
        boolean overLogout = dropLogoutRect.contains(touchPoint.x, touchPoint.y) && activeModal == MODAL_NONE;
        drawSpriteButton(batch, assets, tiny, dropLogoutRect, "LOGOUT",
            overLogout ? AssetKey.UI_BTN_RED_OVER : AssetKey.UI_BTN_RED_UP, Color.WHITE);
    }

    private void drawBottomBarButtons(SpriteBatch batch, Assets assets, BitmapFont small) {
        boolean canHover = activeModal == MODAL_NONE;
        drawSpriteButton(batch, assets, small, btnHighScoresRect, "HIGH SCORES",
            canHover && btnHighScoresRect.contains(touchPoint.x, touchPoint.y)
                ? AssetKey.UI_BTN_TICKET_BLUE_OVER : AssetKey.UI_BTN_TICKET_BLUE_UP,
            Color.WHITE);
        drawSpriteButton(batch, assets, small, btnExtrasRect, "EXTRAS",
            canHover && btnExtrasRect.contains(touchPoint.x, touchPoint.y)
                ? AssetKey.UI_BTN_TICKET_BLUE_OVER : AssetKey.UI_BTN_TICKET_BLUE_UP,
            Color.WHITE);
        drawSpriteButton(batch, assets, small, btnHelpRect, "HELP",
            canHover && btnHelpRect.contains(touchPoint.x, touchPoint.y)
                ? AssetKey.UI_BTN_TICKET_BLUE_OVER : AssetKey.UI_BTN_TICKET_BLUE_UP,
            Color.WHITE);
        drawSpriteButton(batch, assets, small, btnExitRect, "EXIT",
            canHover && btnExitRect.contains(touchPoint.x, touchPoint.y)
                ? AssetKey.UI_BTN_TICKET_RED_OVER : AssetKey.UI_BTN_TICKET_RED_UP,
            Color.WHITE);
    }

    private void drawActiveModal(SpriteBatch batch, Assets assets, Texture white, BitmapFont tiny, BitmapFont small, BitmapFont hud) {
        batch.setColor(COL_SCRIM);
        batch.draw(white, viewLeft, viewBottom, viewWidth, viewHeight);
        batch.setColor(Color.WHITE);

        if (activeModal == MODAL_RENAME) {
            int rx = MathUtils.floor(renameModalRect.x);
            int ry = MathUtils.floor(renameModalRect.y);
            batch.draw(assets.getTexture(AssetKey.UI_RENAME_PANEL), rx, ry, renameModalRect.width, renameModalRect.height);
            drawShadowText(hud, batch, "ĐỔI TÊN HIỂN THỊ", rx, ry + 105f, renameModalRect.width, Align.center, false, COL_GOLD_TEXT);

            drawSpriteField(batch, assets, white, tiny, small, "DISPLAY NAME", renameText, renameInputRect, focusedField == FIELD_RENAME, true);
            if (!renameFeedback.isEmpty()) {
                drawShadowText(tiny, batch, renameFeedback, rx + 12f, ry + 43f, renameModalRect.width - 24f, Align.center, false, COL_ERROR_TEXT);
            }
            boolean overSave = renameSaveRect.contains(touchPoint.x, touchPoint.y);
            boolean overCancel = renameCancelRect.contains(touchPoint.x, touchPoint.y);
            drawSpriteButton(batch, assets, small, renameSaveRect, "LƯU",
                overSave ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP, Color.WHITE);
            drawSpriteButton(batch, assets, small, renameCancelRect, "HỦY",
                overCancel ? AssetKey.UI_BTN_RED_OVER : AssetKey.UI_BTN_RED_UP, Color.WHITE);
            return;
        }

        int mx = MathUtils.floor(modalRect.x);
        int my = MathUtils.floor(modalRect.y);
        batch.draw(assets.getTexture(AssetKey.UI_MODAL_PANEL), mx, my, modalRect.width, modalRect.height);

        if (activeModal == MODAL_HIGH_SCORES) {
            drawShadowText(hud, batch, "BẢNG XẾP HẠNG & KỶ LỤC", mx, my + 170f, modalRect.width, Align.center, false, COL_GOLD_TEXT);

            AccountService svc = game.getAccountService();
            AccountRecord active = svc != null ? svc.getActiveAccount() : null;
            SaveData save = game.getSaveData();
            if (active != null) {
                drawShadowText(small, batch,
                    "Tài khoản: " + active.displayName + "   |   Ví: " + cachedCoinsText,
                    mx + 14f, my + 148f, modalRect.width - 28f, Align.center, false, COL_CREAM);
            } else {
                drawShadowText(small, batch,
                    "Kỷ lục chung: $" + save.bestMineMoney + "   |   Màn cao nhất: " + save.highestClearedLevel + "/6",
                    mx + 14f, my + 148f, modalRect.width - 28f, Align.center, false, COL_CREAM);
            }

            drawShadowText(tiny, batch, "TOP THỢ MỎ OFFLINE (XU):", mx + 18f, my + 130f, modalRect.width - 36f, Align.left, false, COL_GOLD_TEXT);
            if (cachedLeaderboardCount == 0) {
                drawShadowText(tiny, batch, "(Chưa có tài khoản nào được tạo)", mx + 18f, my + 114f, modalRect.width - 36f, Align.left, false, Color.WHITE);
            } else {
                for (int i = 0; i < cachedLeaderboardCount; i++) {
                    drawShadowText(tiny, batch, cachedLeaderboardRows[i], mx + 18f, my + 114f - i * 13f, modalRect.width - 36f, Align.left, false, Color.WHITE);
                }
            }
        } else if (activeModal == MODAL_EXTRAS) {
            drawShadowText(hud, batch, "EXTRAS & CÔNG CỤ", mx, my + 170f, modalRect.width, Align.center, false, COL_GOLD_TEXT);

            boolean overStudio = modalStudioRect.contains(touchPoint.x, touchPoint.y);
            boolean overMute = modalMuteRect.contains(touchPoint.x, touchPoint.y);
            drawSpriteButton(batch, assets, small, modalStudioRect, "MỞ GOLD MINER STUDIO (F10)",
                overStudio ? AssetKey.UI_BTN_BLUE_OVER : AssetKey.UI_BTN_BLUE_UP, Color.WHITE);
            drawSpriteButton(batch, assets, small, modalMuteRect, cachedSoundButtonText,
                overMute ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP, Color.WHITE);
        } else if (activeModal == MODAL_HELP) {
            drawShadowText(hud, batch, "HƯỚNG DẪN CHƠI", mx, my + 170f, modalRect.width, Align.center, false, COL_GOLD_TEXT);
            drawShadowText(small, batch,
                "A / D hoặc Mũi tên Trái/Phải: Di chuyển xe mỏ\n"
                    + "Space / S / Xuống / Click đất: Thả ngoàm cào vàng\n"
                    + "W / Lên: Ném thuốc nổ khi đang kéo vật nặng\n"
                    + "F10: Mở Gold Miner Studio   |   F11: Toàn màn hình\n"
                    + "Đăng nhập tài khoản để tích lũy Xu sau mỗi lượt chơi.",
                mx + 18f, my + 144f, modalRect.width - 36f, Align.left, true, COL_CREAM);
        }

        boolean overClose = modalCloseRect.contains(touchPoint.x, touchPoint.y);
        drawSpriteButton(batch, assets, small, modalCloseRect, "ĐÓNG",
            overClose ? AssetKey.UI_BTN_RED_OVER : AssetKey.UI_BTN_RED_UP, Color.WHITE);
    }

    private void drawSpriteTab(SpriteBatch batch, Assets assets, BitmapFont small, Rectangle rect, String label, boolean active) {
        int x = MathUtils.floor(rect.x);
        int y = MathUtils.floor(rect.y);
        int w = MathUtils.floor(rect.width);
        int h = MathUtils.floor(rect.height);

        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(active ? AssetKey.UI_TAB_ACTIVE : AssetKey.UI_TAB_INACTIVE), x, y, w, h);

        float textY = MathUtils.floor(y + (h + small.getCapHeight()) * 0.5f);
        if (active) {
            small.setColor(COL_OUTLINE);
            small.draw(batch, label, x, textY, w, Align.center, false);
        } else {
            drawShadowText(small, batch, label, x, textY, w, Align.center, false, COL_CREAM);
        }
    }

    private void drawSpriteField(
        SpriteBatch batch,
        Assets assets,
        Texture white,
        BitmapFont tiny,
        BitmapFont small,
        String label,
        String valueText,
        Rectangle rect,
        boolean focused,
        boolean useSmallLabel
    ) {
        int x = MathUtils.floor(rect.x);
        int y = MathUtils.floor(rect.y);
        int w = MathUtils.floor(rect.width);
        int h = MathUtils.floor(rect.height);

        BitmapFont labelFont = useSmallLabel ? small : tiny;
        drawShadowText(labelFont, batch, label, x, y + h + (useSmallLabel ? 10f : 8f), w, Align.left, false, COL_CREAM);

        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(focused ? AssetKey.UI_INPUT_FOCUS : AssetKey.UI_INPUT_NORMAL), x, y, w, h);

        BitmapFont valueFont = useSmallLabel ? small : tiny;
        valueFont.setColor(Color.WHITE);
        float textY = MathUtils.floor(y + (h + valueFont.getCapHeight()) * 0.5f);
        valueFont.draw(batch, valueText, x + 5f, textY, w - 10f, Align.left, false);

        if (focused && ((int) (elapsedTime * 2.2f) % 2 == 0)) {
            glyphLayout.setText(valueFont, valueText);
            float caretX = Math.min(x + w - 5f, x + 5f + glyphLayout.width + 1f);
            batch.setColor(COL_GOLD_TEXT);
            batch.draw(white, MathUtils.floor(caretX), y + 3f, 1f, h - 6f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawSpriteCheckbox(SpriteBatch batch, Assets assets, BitmapFont font, Rectangle rect, boolean checked, String label) {
        int x = MathUtils.floor(rect.x);
        int y = MathUtils.floor(rect.y);
        int s = MathUtils.floor(rect.width);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(checked ? AssetKey.UI_CHECKBOX_ON : AssetKey.UI_CHECKBOX_OFF), x, y, s, s);
        float textY = MathUtils.floor(y + (s + font.getCapHeight()) * 0.5f);
        drawShadowText(font, batch, label, x + s + 5f, textY, 120f, Align.left, false, COL_CREAM);
    }

    private void drawSpriteButton(
        SpriteBatch batch,
        Assets assets,
        BitmapFont font,
        Rectangle rect,
        String text,
        String textureKey,
        Color textColor
    ) {
        int x = MathUtils.floor(rect.x);
        int y = MathUtils.floor(rect.y);
        int w = MathUtils.floor(rect.width);
        int h = MathUtils.floor(rect.height);

        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(textureKey), x, y, w, h);
        float textY = MathUtils.floor(y + (h + font.getCapHeight()) * 0.5f);
        drawShadowText(font, batch, text, x, textY, w, Align.center, false, textColor);
    }

    private void drawCenteredText(BitmapFont font, SpriteBatch batch, String text, Rectangle rect, Color color) {
        int x = MathUtils.floor(rect.x);
        int y = MathUtils.floor(rect.y + (rect.height + font.getCapHeight()) * 0.5f);
        font.setColor(color);
        font.draw(batch, text, x, y, MathUtils.floor(rect.width), Align.center, false);
    }

    private void drawShadowText(
        BitmapFont font,
        SpriteBatch batch,
        String text,
        float x,
        float y,
        float targetWidth,
        int halign,
        boolean wrap,
        Color color
    ) {
        int ix = MathUtils.floor(x);
        int iy = MathUtils.floor(y);
        font.setColor(COL_TEXT_SHADOW);
        font.draw(batch, text, ix + 1f, iy - 1f, targetWidth, halign, wrap);
        font.setColor(color);
        font.draw(batch, text, ix, iy, targetWidth, halign, wrap);
    }

    private void drawDropdownArrow(SpriteBatch batch, Texture white, int x, int y, boolean pointUp) {
        batch.setColor(COL_GOLD_TEXT);
        if (pointUp) {
            batch.draw(white, x + 2, y + 2, 1, 1);
            batch.draw(white, x + 1, y + 1, 3, 1);
            batch.draw(white, x, y, 5, 1);
        } else {
            batch.draw(white, x, y + 2, 5, 1);
            batch.draw(white, x + 1, y + 1, 3, 1);
            batch.draw(white, x + 2, y, 1, 1);
        }
        batch.setColor(Color.WHITE);
    }

    private boolean handleTouchAndShortcuts() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
            return false;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F10)) {
            game.openStudio(StudioMode.UI_MODE, uiFileName);
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (activeModal != MODAL_NONE) {
                activeModal = MODAL_NONE;
                focusedField = FIELD_NONE;
                return false;
            }
            if (dropdownOpen) {
                dropdownOpen = false;
                return false;
            }
            if (backToStudioAction != null) {
                backToStudioAction.run();
                return true;
            }
        }

        if (!Gdx.input.justTouched()) {
            return false;
        }

        if (backToStudioAction != null && backButtonRect.contains(touchPoint.x, touchPoint.y)) {
            backToStudioAction.run();
            return true;
        }

        if (activeModal != MODAL_NONE) {
            return handleModalClick();
        }

        if (btnHighScoresRect.contains(touchPoint.x, touchPoint.y)) {
            openHighScoresModal();
            return false;
        }
        if (btnExtrasRect.contains(touchPoint.x, touchPoint.y)) {
            dropdownOpen = false;
            syncSoundButtonLabel();
            activeModal = MODAL_EXTRAS;
            return false;
        }
        if (btnHelpRect.contains(touchPoint.x, touchPoint.y)) {
            dropdownOpen = false;
            activeModal = MODAL_HELP;
            return false;
        }
        if (btnExitRect.contains(touchPoint.x, touchPoint.y)) {
            if (backToStudioAction != null) {
                backToStudioAction.run();
            } else {
                Gdx.app.exit();
            }
            return true;
        }

        AccountService accountService = game.getAccountService();
        boolean loggedIn = accountService != null && accountService.isLoggedIn();
        if (loggedIn) {
            return handleLoggedInClick();
        }
        return handleAuthFormClick();
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(windowedWidth, windowedHeight);
        } else {
            windowedWidth = Math.max(480, Gdx.graphics.getWidth());
            windowedHeight = Math.max(270, Gdx.graphics.getHeight());
            Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(mode);
        }
    }

    private boolean handleModalClick() {
        if (activeModal == MODAL_RENAME) {
            if (renameInputRect.contains(touchPoint.x, touchPoint.y)) {
                focusedField = FIELD_RENAME;
                return false;
            }
            if (renameSaveRect.contains(touchPoint.x, touchPoint.y)) {
                submitRename();
                return false;
            }
            if (renameCancelRect.contains(touchPoint.x, touchPoint.y) || !renameModalRect.contains(touchPoint.x, touchPoint.y)) {
                activeModal = MODAL_NONE;
                focusedField = FIELD_NONE;
                renameFeedback = "";
            }
            return false;
        }

        if (activeModal == MODAL_EXTRAS) {
            if (modalStudioRect.contains(touchPoint.x, touchPoint.y)) {
                game.openStudio(StudioMode.UI_MODE, uiFileName);
                return true;
            }
            if (modalMuteRect.contains(touchPoint.x, touchPoint.y)) {
                game.toggleMuted();
                syncSoundButtonLabel();
                return false;
            }
        }

        if (modalCloseRect.contains(touchPoint.x, touchPoint.y) || !modalRect.contains(touchPoint.x, touchPoint.y)) {
            activeModal = MODAL_NONE;
        }
        return false;
    }

    private boolean handleLoggedInClick() {
        if (accountCardRect.contains(touchPoint.x, touchPoint.y)) {
            dropdownOpen = !dropdownOpen;
            return false;
        }

        if (dropdownOpen) {
            if (dropAutoLoginRect.contains(touchPoint.x, touchPoint.y)) {
                AccountService svc = game.getAccountService();
                svc.setAutoLogin(!svc.isAutoLoginEnabled());
                syncCachedAccountLabels();
                return false;
            }
            if (dropEditNameRect.contains(touchPoint.x, touchPoint.y)) {
                AccountRecord active = game.getAccountService().getActiveAccount();
                renameBuf.setLength(0);
                if (active != null && active.displayName != null) {
                    renameBuf.append(active.displayName);
                }
                renameText = renameBuf.toString();
                renameFeedback = "";
                dropdownOpen = false;
                activeModal = MODAL_RENAME;
                focusedField = FIELD_RENAME;
                return false;
            }
            if (dropSwitchAccountRect.contains(touchPoint.x, touchPoint.y)) {
                game.getAccountService().switchAccount();
                resetAuthFormForSwitch(TAB_LOGIN);
                return false;
            }
            if (dropLogoutRect.contains(touchPoint.x, touchPoint.y)) {
                game.getAccountService().logout();
                resetAuthFormForSwitch(TAB_LOGIN);
                return false;
            }
            if (!dropdownRect.contains(touchPoint.x, touchPoint.y)) {
                dropdownOpen = false;
            }
            return false;
        }

        if (playBounds.contains(touchPoint.x, touchPoint.y)) {
            if (!game.isMuted() && game.getAssets().hasSound(AssetKey.SFX_SHOP_BUY)) {
                game.getAssets().getSound(AssetKey.SFX_SHOP_BUY).play(0.65f);
            }
            game.startNewCampaign();
            return true;
        }
        return false;
    }

    private boolean handleAuthFormClick() {
        if (loginTabRect.contains(touchPoint.x, touchPoint.y)) {
            switchAuthTab(TAB_LOGIN);
            return false;
        }
        if (registerTabRect.contains(touchPoint.x, touchPoint.y)) {
            switchAuthTab(TAB_REGISTER);
            return false;
        }

        if (activeTab == TAB_LOGIN) {
            if (loginUserRect.contains(touchPoint.x, touchPoint.y)) {
                focusedField = FIELD_USERNAME;
                return false;
            }
            if (loginPassRect.contains(touchPoint.x, touchPoint.y)) {
                focusedField = FIELD_PASSWORD;
                return false;
            }
            if (loginAutoRowRect.contains(touchPoint.x, touchPoint.y)) {
                formAutoLogin = !formAutoLogin;
                return false;
            }
            if (loginSubmitRect.contains(touchPoint.x, touchPoint.y)) {
                submitAuthForm();
                return false;
            }
        } else {
            if (regUserRect.contains(touchPoint.x, touchPoint.y)) {
                focusedField = FIELD_USERNAME;
                return false;
            }
            if (regDisplayRect.contains(touchPoint.x, touchPoint.y)) {
                focusedField = FIELD_DISPLAY_NAME;
                return false;
            }
            if (regPassRect.contains(touchPoint.x, touchPoint.y)) {
                focusedField = FIELD_PASSWORD;
                return false;
            }
            if (regConfirmRect.contains(touchPoint.x, touchPoint.y)) {
                focusedField = FIELD_CONFIRM_PASSWORD;
                return false;
            }
            if (regAutoRowRect.contains(touchPoint.x, touchPoint.y)) {
                formAutoLogin = !formAutoLogin;
                return false;
            }
            if (regSubmitRect.contains(touchPoint.x, touchPoint.y)) {
                submitAuthForm();
                return false;
            }
        }
        return false;
    }

    private boolean handleKeyDown(int keycode) {
        if (keycode == Input.Keys.TAB) {
            boolean reverse = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            cycleFocusField(reverse);
            return true;
        }
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
            if (activeModal == MODAL_RENAME) {
                submitRename();
                return true;
            }
            AccountService svc = game.getAccountService();
            if (svc != null && svc.isLoggedIn()) {
                if (!dropdownOpen && activeModal == MODAL_NONE) {
                    game.startNewCampaign();
                    return true;
                }
            } else if (activeModal == MODAL_NONE) {
                submitAuthForm();
                return true;
            }
        }
        return false;
    }

    private boolean handleKeyTyped(char character) {
        if (activeModal != MODAL_NONE && activeModal != MODAL_RENAME) {
            return false;
        }
        AccountService svc = game.getAccountService();
        if (svc != null && svc.isLoggedIn() && activeModal != MODAL_RENAME) {
            return false;
        }
        StringBuilder target = getFocusedBuffer();
        if (target == null) {
            return false;
        }
        if (character == '\b') {
            if (target.length() > 0) {
                target.setLength(target.length() - 1);
                syncInputDisplayStrings();
            }
            return true;
        }
        if (character == '\r' || character == '\n' || character == '\t') {
            return false;
        }
        if (character >= 32 && character != 127 && target.length() < MAX_INPUT_LEN) {
            target.append(character);
            syncInputDisplayStrings();
            return true;
        }
        return false;
    }

    private StringBuilder getFocusedBuffer() {
        switch (focusedField) {
            case FIELD_USERNAME:
                return usernameBuf;
            case FIELD_DISPLAY_NAME:
                return displayNameBuf;
            case FIELD_PASSWORD:
                return passwordBuf;
            case FIELD_CONFIRM_PASSWORD:
                return confirmBuf;
            case FIELD_RENAME:
                return renameBuf;
            default:
                return null;
        }
    }

    private void cycleFocusField(boolean reverse) {
        if (activeModal == MODAL_RENAME) {
            focusedField = FIELD_RENAME;
            return;
        }
        if (activeTab == TAB_LOGIN) {
            focusedField = (focusedField == FIELD_USERNAME) ? FIELD_PASSWORD : FIELD_USERNAME;
            return;
        }
        int[] order = { FIELD_USERNAME, FIELD_DISPLAY_NAME, FIELD_PASSWORD, FIELD_CONFIRM_PASSWORD };
        int cur = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == focusedField) {
                cur = i;
                break;
            }
        }
        int next = reverse ? (cur + order.length - 1) % order.length : (cur + 1) % order.length;
        focusedField = order[next];
    }

    private void switchAuthTab(int nextTab) {
        if (activeTab == nextTab) {
            return;
        }
        activeTab = nextTab;
        authFeedback = "";
        focusedField = FIELD_USERNAME;
    }

    private void resetAuthFormForSwitch(int targetTab) {
        dropdownOpen = false;
        activeModal = MODAL_NONE;
        activeTab = targetTab;
        focusedField = FIELD_USERNAME;
        formAutoLogin = true;
        authFeedback = "";
        passwordBuf.setLength(0);
        confirmBuf.setLength(0);
        syncInputDisplayStrings();
        syncCachedAccountLabels();
    }

    private void submitAuthForm() {
        AccountService svc = game.getAccountService();
        if (svc == null) {
            return;
        }
        AuthResult result;
        if (activeTab == TAB_LOGIN) {
            result = svc.login(usernameText, passwordBuf.toString(), formAutoLogin);
        } else {
            result = svc.register(usernameText, displayNameText, passwordBuf.toString(), confirmBuf.toString(), formAutoLogin);
        }

        if (result.success) {
            authFeedback = "";
            authFeedbackError = false;
            passwordBuf.setLength(0);
            confirmBuf.setLength(0);
            displayNameBuf.setLength(0);
            syncInputDisplayStrings();
            dropdownOpen = false;
            focusedField = FIELD_NONE;
            syncCachedAccountLabels();
            if (!game.isMuted() && game.getAssets().hasSound(AssetKey.SFX_COIN)) {
                game.getAssets().getSound(AssetKey.SFX_COIN).play(0.6f);
            }
        } else {
            authFeedback = result.message;
            authFeedbackError = true;
        }
    }

    private void submitRename() {
        AccountService svc = game.getAccountService();
        if (svc == null) {
            return;
        }
        AuthResult res = svc.updateDisplayName(renameText);
        if (res.success) {
            renameFeedback = "";
            activeModal = MODAL_NONE;
            focusedField = FIELD_NONE;
            syncCachedAccountLabels();
        } else {
            renameFeedback = res.message;
        }
    }

    private void openHighScoresModal() {
        dropdownOpen = false;
        AccountService svc = game.getAccountService();
        cachedLeaderboardCount = 0;
        if (svc != null) {
            List<AccountRecord> board = svc.getLeaderboard();
            int limit = Math.min(MAX_LEADERBOARD_ROWS, board.size());
            for (int i = 0; i < limit; i++) {
                AccountRecord r = board.get(i);
                cachedLeaderboardRows[i] = "#" + (i + 1) + "  " + r.displayName
                    + "  -  " + numberFormat.format(r.coins) + " XU"
                    + "  (Mỏ max: $" + r.bestMineMoney + ", Màn " + r.highestClearedLevel + "/6)";
                cachedLeaderboardCount++;
            }
        }
        activeModal = MODAL_HIGH_SCORES;
    }

    private void syncInputDisplayStrings() {
        usernameText = usernameBuf.toString();
        displayNameText = displayNameBuf.toString();
        passwordMasked = maskOf(passwordBuf.length());
        confirmMasked = maskOf(confirmBuf.length());
        renameText = renameBuf.toString();
    }

    private String maskOf(int length) {
        maskScratch.setLength(0);
        for (int i = 0; i < length; i++) {
            maskScratch.append('*');
        }
        return maskScratch.toString();
    }

    private void syncCachedAccountLabels() {
        AccountService svc = game.getAccountService();
        if (svc != null && svc.isLoggedIn()) {
            AccountRecord active = svc.getActiveAccount();
            cachedDisplayName = active.displayName != null && !active.displayName.isEmpty()
                ? active.displayName
                : active.username;
            cachedCoinsText = numberFormat.format(active.coins) + " XU";
            cachedAutoLoginText = svc.isAutoLoginEnabled() ? "AUTO LOGIN: ON" : "AUTO LOGIN: OFF";
        } else {
            cachedDisplayName = "KHÁCH";
            cachedCoinsText = "0 XU";
            cachedAutoLoginText = "AUTO LOGIN: OFF";
        }
        syncSoundButtonLabel();
    }

    private void syncSoundButtonLabel() {
        cachedSoundButtonText = game.isMuted() ? "ÂM THANH: TẮT" : "ÂM THANH: BẬT";
    }

    private void updateViewportLayout(int screenWidth, int screenHeight) {
        int safeW = Math.max(1, screenWidth);
        int safeH = Math.max(1, screenHeight);
        viewport.update(safeW, safeH, true);

        viewWidth = viewport.getWorldWidth();
        viewHeight = viewport.getWorldHeight();
        viewLeft = camera.position.x - viewWidth * 0.5f;
        viewBottom = camera.position.y - viewHeight * 0.5f;

        // Cover-fill background keeping exact 16:9 (480:270) aspect ratio without distortion
        float bgScale = Math.max(viewWidth / WorldBounds.WORLD_WIDTH, viewHeight / WorldBounds.WORLD_HEIGHT);
        if (Math.abs(bgScale - 1f) < 0.002f) {
            bgDrawX = 0f;
            bgDrawY = 0f;
            bgDrawW = WorldBounds.WORLD_WIDTH;
            bgDrawH = WorldBounds.WORLD_HEIGHT;
        } else {
            bgDrawW = WorldBounds.WORLD_WIDTH * bgScale;
            bgDrawH = WorldBounds.WORLD_HEIGHT * bgScale;
            bgDrawX = MathUtils.floor(camera.position.x - bgDrawW * 0.5f);
            bgDrawY = MathUtils.floor(camera.position.y - bgDrawH * 0.5f);
        }

        for (int i = 0; i < bulbCount; i++) {
            bulbWorldX[i] = bgDrawX + bulbNx[i] * bgDrawW;
            bulbWorldY[i] = bgDrawY + bulbNy[i] * bgDrawH;
        }

        // Anchor Profile Card & Dropdown to top-right of the extended viewport
        float cardX = MathUtils.floor(viewLeft + viewWidth - 8f - 136f);
        float cardY = MathUtils.floor(viewBottom + viewHeight - 8f - 40f);
        accountCardRect.set(cardX, cardY, 136f, 40f);
        dropdownRect.set(cardX, cardY - 88f, 136f, 88f);
        dropAutoLoginRect.set(cardX + 6f, cardY - 23f, 124f, 17f);
        dropEditNameRect.set(cardX + 6f, cardY - 43f, 124f, 17f);
        dropSwitchAccountRect.set(cardX + 6f, cardY - 63f, 124f, 17f);
        dropLogoutRect.set(cardX + 6f, cardY - 83f, 124f, 17f);

        // Anchor Studio back pill to top-left of the extended viewport
        backButtonRect.set(MathUtils.floor(viewLeft + 6f), MathUtils.floor(viewBottom + viewHeight - 18f), 108f, 14f);
    }

    @Override
    public void resize(int width, int height) {
        if (!Gdx.graphics.isFullscreen() && width >= 480 && height >= 270) {
            windowedWidth = width;
            windowedHeight = height;
        }
        updateViewportLayout(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == inputProcessor) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        if (Gdx.input.getInputProcessor() == inputProcessor) {
            Gdx.input.setInputProcessor(null);
        }
    }
}
