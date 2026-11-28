package com.minhtriet79.goldminer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.minhtriet79.goldminer.GoldMinerGame;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.domain.account.AccountRecord;
import com.minhtriet79.goldminer.domain.account.AccountService;
import com.minhtriet79.goldminer.domain.run.RunSession;
import com.minhtriet79.goldminer.gameplay.ui.MenuTopHeader;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Pure 16-bit Pixel Art Between-Level PASS / FAIL screen with {@link ExtendViewport} (zero black bars):
 * <ul>
 *   <li>PASS: converts Mine Money ($) into Coins ({@code Xu = $ / 10}), unlocks next level in {@code accounts.json},
 *   and offers {@code [CONTINUE]} (to {@link ShopScreen}) or {@code [WORLD MAP]}.</li>
 *   <li>FAIL: offers {@code [RETRY]} (replay current level) and {@code [QUIT TO MAP]} (return to {@link WorldMapScreen}).</li>
 * </ul>
 */
public class LevelResultScreen implements Screen {

    private static final float VEGAS_W = 800f;
    private static final float VEGAS_H = 600f;

    private static final Color COL_GOLD = new Color(1.00f, 0.85f, 0.28f, 1f);
    private static final Color COL_CREAM = new Color(0.99f, 0.95f, 0.82f, 1f);
    private static final Color COL_LIME = new Color(0.56f, 0.96f, 0.52f, 1f);
    private static final Color COL_FAIL = new Color(1.00f, 0.45f, 0.40f, 1f);
    private static final Color COL_SCRIM = new Color(0.04f, 0.02f, 0.01f, 0.52f);

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Vector3 touchPoint = new Vector3();

    private final boolean passed;
    private final int finishedLevel;
    private final int totalMoney;
    private final int goal;
    private final int earnedThisLevel;
    private final int coinsAwarded;
    private final int unlockedNextLevel;

    private final String titleLine;
    private final String summaryLine;
    private final String highlightLine;
    private final String subLine;
    private final String leftBtnLabel;
    private final String rightBtnLabel;

    private final Rectangle panelRect = new Rectangle(140f, 120f, 520f, 360f);
    private final Rectangle leftBtnRect = new Rectangle(180f, 152f, 200f, 44f);
    private final Rectangle rightBtnRect = new Rectangle(420f, 152f, 200f, 44f);

    public LevelResultScreen(GoldMinerGame game, boolean passed, int earnedThisLevel) {
        this.game = game;
        this.passed = passed;
        this.earnedThisLevel = earnedThisLevel;

        RunSession run = game.getRunSession();
        this.finishedLevel = run.getCurrentLevel();
        this.totalMoney = run.getMineMoney();
        this.goal = run.getGoal();

        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
        AccountService svc = game.getAccountService();
        if (passed) {
            this.coinsAwarded = svc != null
                ? svc.recordLevelWin(finishedLevel, earnedThisLevel)
                : (Math.max(0, earnedThisLevel) / 10);
            this.unlockedNextLevel = Math.min(RunSession.MAX_W1_LEVEL, finishedLevel + 1);

            AccountRecord active = svc != null ? svc.getActiveAccount() : null;
            int wallet = active != null ? active.coins : (game.getSaveData().officialCoins + coinsAwarded);

            this.titleLine = "HOÀN THÀNH MÀN " + finishedLevel + " / " + RunSession.MAX_W1_LEVEL + "!";
            this.summaryLine = "Tiền Mỏ ($): $" + nf.format(totalMoney) + "   /   Mục tiêu: $" + nf.format(goal);
            this.highlightLine = "Quy đổi Xu (Xu = $ / 10): +" + nf.format(coinsAwarded) + " XU";
            this.subLine = finishedLevel < RunSession.MAX_W1_LEVEL
                ? "Ví: " + nf.format(wallet) + " XU   |   Đã mở khóa Màn " + unlockedNextLevel + "!"
                : "Ví: " + nf.format(wallet) + " XU   |   Đã chinh phục World 1 Australia!";
            this.leftBtnLabel = "WORLD MAP";
            this.rightBtnLabel = finishedLevel >= RunSession.MAX_W1_LEVEL ? "HOÀN TẤT" : "CONTINUE";
        } else {
            this.coinsAwarded = 0;
            this.unlockedNextLevel = finishedLevel;
            int missing = Math.max(0, goal - totalMoney);

            this.titleLine = "THẤT BẠI Ở MÀN " + finishedLevel + " / " + RunSession.MAX_W1_LEVEL;
            this.summaryLine = "Tiền Mỏ ($): $" + nf.format(totalMoney) + "   /   Mục tiêu: $" + nf.format(goal);
            this.highlightLine = "Còn thiếu: $" + nf.format(missing);
            this.subLine = "Thử lại màn này hoặc quay về Bản đồ Australia.";
            this.leftBtnLabel = "QUIT TO MAP";
            this.rightBtnLabel = "RETRY";
        }

        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VEGAS_W, VEGAS_H, camera);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
        game.stopAllMusic();
        if (!game.isMuted()) {
            String sfx = passed ? AssetKey.SFX_PASS : AssetKey.SFX_NOPASS;
            game.getAssets().getSound(sfx).play(0.8f);
        }
    }

    @Override
    public void render(float rawDt) {
        Math.min(rawDt, 0.05f);

        touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touchPoint);

        if (handleInput()) {
            return;
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        viewport.apply(true);

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        Texture bg = assets.getTexture(passed ? AssetKey.BG_PASS_W1 : AssetKey.BG_RESULT_W1);
        Texture white = game.getWhitePixel();
        BitmapFont banner = game.getPlayBannerFont();
        BitmapFont hud = game.getPlayHudFont();
        BitmapFont small = game.getPlaySmallFont();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        batch.setColor(Color.WHITE);
        batch.draw(bg, 0f, 0f, VEGAS_W, VEGAS_H);
        batch.setColor(COL_SCRIM);
        batch.draw(white, 0f, 0f, VEGAS_W, VEGAS_H);

        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_MODAL_PANEL),
            panelRect.x, panelRect.y, panelRect.width, panelRect.height);

        MenuTopHeader.drawShadowText(banner, batch, titleLine,
            panelRect.x + 20f, panelRect.y + 318f, panelRect.width - 40f, Align.center, false,
            passed ? COL_GOLD : COL_FAIL);

        MenuTopHeader.drawShadowText(small, batch, summaryLine,
            panelRect.x + 24f, panelRect.y + 268f, panelRect.width - 48f, Align.center, false, COL_CREAM);

        MenuTopHeader.drawShadowText(hud, batch, highlightLine,
            panelRect.x + 24f, panelRect.y + 228f, panelRect.width - 48f, Align.center, false,
            passed ? COL_LIME : COL_FAIL);

        MenuTopHeader.drawShadowText(small, batch, subLine,
            panelRect.x + 24f, panelRect.y + 190f, panelRect.width - 48f, Align.center, true, COL_GOLD);

        boolean overLeft = leftBtnRect.contains(touchPoint.x, touchPoint.y);
        String leftKey = passed
            ? (overLeft ? AssetKey.UI_BTN_BLUE_OVER : AssetKey.UI_BTN_BLUE_UP)
            : (overLeft ? AssetKey.UI_BTN_RED_OVER : AssetKey.UI_BTN_RED_UP);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(leftKey), leftBtnRect.x, leftBtnRect.y, leftBtnRect.width, leftBtnRect.height);
        float leftY = MathUtils.floor(leftBtnRect.y + (leftBtnRect.height + small.getCapHeight()) * 0.5f);
        MenuTopHeader.drawShadowText(small, batch, leftBtnLabel,
            leftBtnRect.x, leftY, leftBtnRect.width, Align.center, false, Color.WHITE);

        boolean overRight = rightBtnRect.contains(touchPoint.x, touchPoint.y);
        String rightKey = overRight ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP;
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(rightKey), rightBtnRect.x, rightBtnRect.y, rightBtnRect.width, rightBtnRect.height);
        float rightY = MathUtils.floor(rightBtnRect.y + (rightBtnRect.height + small.getCapHeight()) * 0.5f);
        MenuTopHeader.drawShadowText(small, batch, rightBtnLabel,
            rightBtnRect.x, rightY, rightBtnRect.width, Align.center, false, Color.WHITE);

        // Authentic hollow 800x600 Vegas bezel frame (transparent interior)
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_VEGAS_FRAME), 0f, 0f, VEGAS_W, VEGAS_H);

        batch.end();
    }

    private boolean handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            triggerPrimaryAction();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            triggerMapReturn();
            return true;
        }
        if (Gdx.input.justTouched()) {
            if (rightBtnRect.contains(touchPoint.x, touchPoint.y)) {
                triggerPrimaryAction();
                return true;
            }
            if (leftBtnRect.contains(touchPoint.x, touchPoint.y)) {
                triggerMapReturn();
                return true;
            }
        }
        return false;
    }

    private void triggerPrimaryAction() {
        if (passed) {
            if (finishedLevel >= RunSession.MAX_W1_LEVEL) {
                game.showWorldMap(RunSession.MAX_W1_LEVEL);
            } else {
                game.showShopFromLevelWin(finishedLevel);
            }
        } else {
            game.startMapLevel(finishedLevel);
        }
    }

    private void triggerMapReturn() {
        game.showWorldMap(passed ? unlockedNextLevel : finishedLevel);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(VEGAS_W * 0.5f, VEGAS_H * 0.5f, 0f);
        camera.update();
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
