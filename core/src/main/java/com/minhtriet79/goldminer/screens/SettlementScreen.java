package com.minhtriet79.goldminer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.minhtriet79.goldminer.domain.settlement.SettlementSummary;
import com.minhtriet79.goldminer.gameplay.ui.VegasButton;
import com.minhtriet79.goldminer.persistence.SaveData;

/**
 * End-of-run settlement screen converting temporary Mine Money ($) and cleared levels
 * into persistent Official Currency (Xu) saved in {@code gold_miner_save.json}.
 */
public class SettlementScreen implements Screen {

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Vector3 touchPoint = new Vector3();

    private final SettlementSummary summary;
    private final VegasButton playAgainButton;

    public SettlementScreen(GoldMinerGame game, SettlementSummary summary) {
        this.game = game;
        this.summary = summary;

        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT, camera);
        this.viewport.apply(true);

        this.playAgainButton = new VegasButton(
            174f, 18f, 132f, 26f,
            AssetKey.BTN_NEXT_UP, AssetKey.BTN_NEXT_OVER, AssetKey.BTN_NEXT_DOWN
        );
    }

    @Override
    public void show() {
        game.playMusic(AssetKey.BGM_MENU, 0.35f);
        if (!game.isMuted() && summary.totalAwardedCoins > 0) {
            game.getAssets().getSound(AssetKey.SFX_COIN).play(0.8f);
        }
    }

    @Override
    public void render(float rawDt) {
        Math.min(rawDt, 0.05f);
        handleInput();

        ScreenUtils.clear(0.08f, 0.06f, 0.04f, 1f);
        viewport.apply();

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        Texture bg = assets.getTexture(AssetKey.BG_RESULT_W1);
        BitmapFont banner = game.getBannerFont();
        BitmapFont hud = game.getHudFont();
        BitmapFont small = game.getSmallFont();
        SaveData save = game.getSaveData();
        AccountService accountService = game.getAccountService();
        AccountRecord activeAccount = accountService != null ? accountService.getActiveAccount() : null;
        int displayCoins = activeAccount != null ? activeAccount.coins : save.officialCoins;
        int displayBestMine = activeAccount != null ? activeAccount.bestMineMoney : save.bestMineMoney;
        int displayHighestLevel = activeAccount != null ? activeAccount.highestClearedLevel : save.highestClearedLevel;
        String accountTag = activeAccount != null ? activeAccount.displayName + " | " : "";

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(bg, 0f, 0f, WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT);

        banner.setColor(summary.completedWorld1 ? new Color(0.08f, 0.45f, 0.08f, 1f) : new Color(0.35f, 0.14f, 0.03f, 1f));
        String title = summary.completedWorld1 ? "CHINH PHUC WORLD 1 AUSTRALIA!" : "KET TOAN CHUYEN DAO VANG";
        banner.draw(batch, title, 86f, 206f, 308f, Align.center, false);

        hud.setColor(0.22f, 0.10f, 0.02f, 1f);
        hud.draw(batch, "So man da vuot qua: " + summary.levelsCleared + " / 6", 86f, 172f, 308f, Align.center, false);
        hud.draw(batch, "Tien Mo cuoi luot ($): $" + summary.finalMineMoney, 86f, 152f, 308f, Align.center, false);
        hud.draw(batch, "Quy doi Tien Mo: +" + summary.convertedCoins + " Xu   |   Thuong vuot man: +" + summary.clearBonusCoins + " Xu", 86f, 132f, 308f, Align.center, false);

        hud.setColor(0.04f, 0.42f, 0.08f, 1f);
        hud.draw(batch, "TONG NHAN DUOC: +" + summary.totalAwardedCoins + " XU", 86f, 110f, 308f, Align.center, false);

        small.setColor(0.18f, 0.10f, 0.03f, 1f);
        small.draw(
            batch,
            accountTag + "Vi Xu da luu: " + displayCoins + " Xu   |   Ky luc ($): $" + displayBestMine
                + "   |   Man cao nhat: " + displayHighestLevel + "/6",
            86f, 86f, 308f, Align.center, false
        );

        playAgainButton.draw(batch, assets, viewport, touchPoint);
        small.setColor(Color.WHITE);
        small.draw(batch, "CHƠI LẠI TỪ MÀN 1 (ENTER)   |   ESC: MENU CHÍNH", 86f, 14f, 308f, Align.center, false);
        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showMainMenu();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            game.startNewCampaign();
            return;
        }
        if (Gdx.input.justTouched()) {
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            viewport.unproject(touchPoint);
            if (playAgainButton.contains(touchPoint.x, touchPoint.y)) {
                game.startNewCampaign();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }
}
