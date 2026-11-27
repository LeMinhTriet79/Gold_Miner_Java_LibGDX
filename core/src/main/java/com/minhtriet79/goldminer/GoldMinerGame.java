package com.minhtriet79.goldminer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.Hinting;
import com.kotcrab.vis.ui.VisUI;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.domain.account.AccountService;
import com.minhtriet79.goldminer.domain.run.RunSession;
import com.minhtriet79.goldminer.domain.settlement.SettlementCalculator;
import com.minhtriet79.goldminer.domain.settlement.SettlementSummary;
import com.minhtriet79.goldminer.persistence.JsonSaveRepository;
import com.minhtriet79.goldminer.persistence.LocalJsonAccountStore;
import com.minhtriet79.goldminer.persistence.SaveData;
import com.minhtriet79.goldminer.screens.GoldMineScreen;
import com.minhtriet79.goldminer.screens.LevelResultScreen;
import com.minhtriet79.goldminer.screens.MainMenuScreen;
import com.minhtriet79.goldminer.screens.PlayScreen;
import com.minhtriet79.goldminer.screens.SettlementScreen;
import com.minhtriet79.goldminer.screens.ShopScreen;
import com.minhtriet79.goldminer.screens.WorldHubScreen;
import com.minhtriet79.goldminer.screens.WorldMapScreen;
import com.minhtriet79.goldminer.studio.ExternalFileDropListener;
import com.minhtriet79.goldminer.studio.StudioMode;
import com.minhtriet79.goldminer.studio.StudioScreen;
import com.minhtriet79.goldminer.studio.io.StudioFileHelper;
import com.minhtriet79.goldminer.studio.ui.StudioSkinManager;

/**
 * Root libGDX {@link Game} orchestrating the 6-level World 1 campaign,
 * Gold Miner Studio 2D editor, theme assets, FreeType fonts, audio mute state, and saves.
 */
public class GoldMinerGame extends Game {

    private static final String DEFAULT_THEME = "vegas";
    private static final String VIETNAMESE_CHARS =
        FreeTypeFontGenerator.DEFAULT_CHARS
            + "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ"
            + "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ";

    private final boolean startInStudio;

    private SpriteBatch batch;
    private Assets assets;
    private Texture whitePixel;
    private BitmapFont pixelTinyFont;
    private BitmapFont pixelSmallFont;
    private BitmapFont pixelHudFont;
    private BitmapFont menuTinyFont;
    private BitmapFont smallFont;
    private BitmapFont hudFont;
    private BitmapFont floatingFont;
    private BitmapFont bannerFont;

    private final RunSession runSession = new RunSession();
    private JsonSaveRepository saveRepository;
    private SaveData saveData;
    private AccountService accountService;
    private boolean muted;
    private boolean debugAllLevelsUnlocked;
    private boolean pendingUnlockToast;
    private int windowedWidth = 960;
    private int windowedHeight = 540;
    private ExternalFileDropListener externalFileDropListener;
    private StudioSkinManager studioSkinManager;
    private Screen pendingScreen;
    private boolean hasPendingScreen;

    public GoldMinerGame() {
        this(false);
    }

    public GoldMinerGame(boolean startInStudio) {
        this.startInStudio = startInStudio;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets = new Assets();
        assets.loadTheme(DEFAULT_THEME);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        whitePixel.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pixmap.dispose();

        // Crisp 1:1 mono pixel-art fonts with TextureFilter.Nearest across ALL screens (no Linear smoothing)
        FreeTypeFontGenerator pixelGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/tahomabd.ttf"));
        pixelTinyFont = createPixelFont(pixelGenerator, 8);
        pixelSmallFont = createPixelFont(pixelGenerator, 9);
        pixelHudFont = createPixelFont(pixelGenerator, 11);
        floatingFont = createPixelFont(pixelGenerator, 12);
        bannerFont = createPixelFont(pixelGenerator, 14);
        pixelGenerator.dispose();

        menuTinyFont = pixelTinyFont;
        smallFont = pixelSmallFont;
        hudFont = pixelHudFont;

        saveRepository = new JsonSaveRepository();
        saveData = saveRepository.load();
        accountService = new AccountService(new LocalJsonAccountStore());
        accountService.tryAutoLogin();

        if (startInStudio) {
            openStudio(StudioMode.LEVEL_MODE, "level_1.json");
        } else {
            showMainMenu();
        }
    }

    private BitmapFont createPixelFont(FreeTypeFontGenerator generator, int pixelSize) {
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = pixelSize;
        param.mono = true;
        param.hinting = Hinting.Full;
        param.minFilter = TextureFilter.Nearest;
        param.magFilter = TextureFilter.Nearest;
        param.characters = VIETNAMESE_CHARS;
        BitmapFont font = generator.generateFont(param);
        font.getData().setScale(1.0f);
        font.setUseIntegerPositions(true);
        return font;
    }

    private void switchScreen(Screen nextScreen) {
        if (getScreen() == null) {
            setScreenNow(nextScreen);
            return;
        }
        if (pendingScreen != null && pendingScreen != nextScreen) {
            pendingScreen.dispose();
        }
        pendingScreen = nextScreen;
        hasPendingScreen = true;
    }

    private void setScreenNow(Screen nextScreen) {
        Gdx.input.setInputProcessor(null);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Screen prev = getScreen();
        setScreen(nextScreen);
        if (nextScreen != null) {
            nextScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        if (prev != null && prev != nextScreen) {
            prev.dispose();
        }
    }

    private void applyPendingScreen() {
        if (!hasPendingScreen) {
            return;
        }
        Screen next = pendingScreen;
        pendingScreen = null;
        hasPendingScreen = false;
        setScreenNow(next);
    }

    @Override
    public void render() {
        handleGlobalHotkeys();
        applyPendingScreen();
        super.render();
        applyPendingScreen();
    }

    private void handleGlobalHotkeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            unlockAllLevelsDebug();
        }
    }

    public void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(windowedWidth, windowedHeight);
        } else {
            windowedWidth = Math.max(480, Gdx.graphics.getWidth());
            windowedHeight = Math.max(270, Gdx.graphics.getHeight());
            Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(mode);
        }
    }

    public void unlockAllLevelsDebug() {
        debugAllLevelsUnlocked = true;
        pendingUnlockToast = true;
        if (accountService != null) {
            accountService.unlockAllLevels();
        }
        if (!muted && assets != null && assets.hasSound(AssetKey.SFX_COIN)) {
            assets.getSound(AssetKey.SFX_COIN).play(0.65f);
        }
    }

    public boolean consumeUnlockToast() {
        boolean val = pendingUnlockToast;
        pendingUnlockToast = false;
        return val;
    }

    public boolean isLevelUnlocked(int levelNumber) {
        if (levelNumber < 1 || levelNumber > RunSession.MAX_W1_LEVEL) {
            return false;
        }
        if (debugAllLevelsUnlocked) {
            return true;
        }
        return accountService != null ? accountService.isLevelUnlocked(levelNumber) : (levelNumber == 1);
    }

    public int getMaxUnlockedLevel() {
        if (debugAllLevelsUnlocked) {
            return RunSession.MAX_W1_LEVEL;
        }
        return accountService != null ? accountService.getMaxUnlockedLevel() : 1;
    }

    public boolean isLevelCleared(int levelNumber) {
        return accountService != null && accountService.isLevelCleared(levelNumber);
    }

    public void openStudio() {
        openStudio(StudioMode.LEVEL_MODE, "level_1.json");
    }

    public void openStudio(StudioMode initialMode, String initialFile) {
        stopAllMusic();
        if (studioSkinManager == null) {
            studioSkinManager = new StudioSkinManager();
        }
        studioSkinManager.ensureLoaded();
        switchScreen(new StudioScreen(this, initialMode, initialFile));
    }

    public StudioSkinManager getStudioSkinManager() {
        return studioSkinManager;
    }

    public void showMainMenu() {
        switchScreen(new MainMenuScreen(this, "main_menu.json", null));
    }

    public void showWorldHub() {
        switchScreen(new WorldHubScreen(this));
    }

    public void showGoldMineSelector() {
        switchScreen(new GoldMineScreen(this));
    }

    public void showWorldMap(int initialSelectedLevel) {
        int clamped = Math.max(1, Math.min(RunSession.MAX_W1_LEVEL, initialSelectedLevel));
        switchScreen(new WorldMapScreen(this, clamped));
    }

    public void startNewCampaign() {
        runSession.startNewRun();
        switchScreen(new PlayScreen(this, 1));
    }

    public void startLevel(int levelNumber) {
        int clamped = Math.max(1, Math.min(RunSession.MAX_W1_LEVEL, levelNumber));
        switchScreen(new PlayScreen(this, clamped));
    }

    public void startMapLevel(int levelNumber) {
        int clamped = Math.max(1, Math.min(RunSession.MAX_W1_LEVEL, levelNumber));
        switchScreen(PlayScreen.forMapLevel(this, clamped));
    }

    public void startLevelPlayTest(String levelFileName, Runnable backToStudioAction) {
        int levelNum = StudioFileHelper.parseLevelNumber(levelFileName);
        runSession.startNewRun();
        switchScreen(new PlayScreen(this, levelNum, levelFileName, backToStudioAction));
    }

    public void startUiPlayTest(String uiFileName, Runnable backToStudioAction) {
        switchScreen(new MainMenuScreen(this, uiFileName, backToStudioAction));
    }

    public void showLevelResult(boolean passed, int scoreEarnedThisLevel) {
        runSession.finishLevel(passed);
        switchScreen(new LevelResultScreen(this, passed, scoreEarnedThisLevel));
    }

    public void showShop() {
        switchScreen(new ShopScreen(this));
    }

    public void showShopFromLevelWin(int completedLevel) {
        switchScreen(new ShopScreen(this, completedLevel));
    }

    public void showSettlement(boolean completedWorld1) {
        SettlementSummary summary = SettlementCalculator.settle(runSession, completedWorld1);
        saveData = saveRepository.recordSettlement(saveData, summary);
        if (accountService != null) {
            accountService.recordSettlement(summary);
        }
        switchScreen(new SettlementScreen(this, summary));
    }

    public void setExternalFileDropListener(ExternalFileDropListener listener) {
        this.externalFileDropListener = listener;
    }

    public void onExternalFilesDropped(String[] files) {
        if (externalFileDropListener != null && files != null && files.length > 0) {
            externalFileDropListener.onFilesDropped(files);
        }
    }

    public void toggleMuted() {
        muted = !muted;
        syncMusicVolumes();
    }

    public void playMusic(String musicKey, float baseVolume) {
        stopAllMusicExcept(musicKey);
        Music bgm = assets.getMusic(musicKey);
        bgm.setLooping(true);
        bgm.setVolume(muted ? 0f : baseVolume);
        if (!bgm.isPlaying()) {
            bgm.play();
        }
    }

    public void stopAllMusic() {
        stopMusicIfPlaying(AssetKey.BGM_MENU);
        stopMusicIfPlaying(AssetKey.BGM_W1);
    }

    private void stopAllMusicExcept(String keepKey) {
        if (!AssetKey.BGM_MENU.equals(keepKey)) {
            stopMusicIfPlaying(AssetKey.BGM_MENU);
        }
        if (!AssetKey.BGM_W1.equals(keepKey)) {
            stopMusicIfPlaying(AssetKey.BGM_W1);
        }
    }

    private void stopMusicIfPlaying(String key) {
        Music music = assets.getMusic(key);
        if (music.isPlaying()) {
            music.stop();
        }
    }

    private void syncMusicVolumes() {
        assets.getMusic(AssetKey.BGM_W1).setVolume(muted ? 0f : 0.42f);
        assets.getMusic(AssetKey.BGM_MENU).setVolume(muted ? 0f : 0.42f);
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public Assets getAssets() {
        return assets;
    }

    public Texture getWhitePixel() {
        return whitePixel;
    }

    public BitmapFont getPixelTinyFont() {
        return pixelTinyFont;
    }

    public BitmapFont getPixelSmallFont() {
        return pixelSmallFont;
    }

    public BitmapFont getPixelHudFont() {
        return pixelHudFont;
    }

    public BitmapFont getMenuTinyFont() {
        return menuTinyFont;
    }

    public BitmapFont getSmallFont() {
        return smallFont;
    }

    public BitmapFont getHudFont() {
        return hudFont;
    }

    public BitmapFont getFloatingFont() {
        return floatingFont;
    }

    public BitmapFont getBannerFont() {
        return bannerFont;
    }

    public RunSession getRunSession() {
        return runSession;
    }

    public SaveData getSaveData() {
        return saveData;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public boolean isMuted() {
        return muted;
    }

    @Override
    public void dispose() {
        if (pendingScreen != null) {
            pendingScreen.dispose();
            pendingScreen = null;
            hasPendingScreen = false;
        }
        super.dispose();
        if (screen != null) {
            screen.dispose();
        }
        if (studioSkinManager != null) {
            studioSkinManager.dispose();
        }
        if (VisUI.isLoaded()) {
            VisUI.dispose();
        }
        if (pixelTinyFont != null) {
            pixelTinyFont.dispose();
        }
        if (pixelSmallFont != null) {
            pixelSmallFont.dispose();
        }
        if (pixelHudFont != null) {
            pixelHudFont.dispose();
        }
        if (floatingFont != null) {
            floatingFont.dispose();
        }
        if (bannerFont != null) {
            bannerFont.dispose();
        }
        if (whitePixel != null) {
            whitePixel.dispose();
        }
        if (assets != null) {
            assets.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }
}
