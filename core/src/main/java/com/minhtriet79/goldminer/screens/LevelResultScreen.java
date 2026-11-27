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
import com.minhtriet79.goldminer.domain.run.RunSession;
import com.minhtriet79.goldminer.gameplay.ui.VegasButton;

/**
 * Between-level PASS / FAIL screen with authentic Vegas backgrounds and SFX.
 * Routes PASS Lv 1..5 to Australia Shop (Lex), PASS Lv 6 or FAIL to Settlement.
 */
public class LevelResultScreen implements Screen {

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Vector3 touchPoint = new Vector3();

    private final boolean passed;
    private final int finishedLevel;
    private final int totalMoney;
    private final int goal;
    private final int earnedThisLevel;

    private final VegasButton continueButton;
    private final VegasButton menuButton;

    public LevelResultScreen(GoldMinerGame game, boolean passed, int earnedThisLevel) {
        this.game = game;
        this.passed = passed;
        this.earnedThisLevel = earnedThisLevel;

        RunSession run = game.getRunSession();
        this.finishedLevel = run.getCurrentLevel();
        this.totalMoney = run.getMineMoney();
        this.goal = run.getGoal();

        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT, camera);
        this.viewport.apply(true);

        this.continueButton = new VegasButton(
            258f, 22f, 118f, 24f,
            AssetKey.BTN_NEXT_UP, AssetKey.BTN_NEXT_OVER, AssetKey.BTN_NEXT_DOWN
        );
        this.menuButton = new VegasButton(
            104f, 22f, 118f, 24f,
            AssetKey.BTN_MENU_UP, AssetKey.BTN_MENU_OVER, AssetKey.BTN_MENU_DOWN
        );
    }

    @Override
    public void show() {
        game.stopAllMusic();
        if (!game.isMuted()) {
            String sfx = passed ? AssetKey.SFX_PASS : AssetKey.SFX_NOPASS;
            game.getAssets().getSound(sfx).play(0.8f);
        }
    }

    @Override
    public void render(float rawDt) {
        float dt = Math.min(rawDt, 0.05f);
        handleInput();

        ScreenUtils.clear(0.06f, 0.05f, 0.03f, 1f);
        viewport.apply();

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        Texture bg = assets.getTexture(passed ? AssetKey.BG_PASS_W1 : AssetKey.BG_RESULT_W1);
        Texture white = game.getWhitePixel();
        BitmapFont banner = game.getBannerFont();
        BitmapFont hud = game.getHudFont();
        BitmapFont small = game.getSmallFont();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(bg, 0f, 0f, WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT);

        if (passed) {
            batch.setColor(0.08f, 0.05f, 0.02f, 0.72f);
            batch.draw(white, 92f, 64f, 296f, 152f);
            batch.setColor(Color.WHITE);

            banner.setColor(Color.GOLD);
            banner.draw(batch, "HOAN THANH MAN " + finishedLevel + " / 6!", 92f, 200f, 296f, Align.center, false);

            hud.setColor(Color.WHITE);
            hud.draw(batch, "Tien kiem duoc man nay: +$" + earnedThisLevel, 92f, 164f, 296f, Align.center, false);
            hud.draw(batch, "Tong Tien Mo ($): $" + totalMoney + "   /   Muc tieu: $" + goal, 92f, 140f, 296f, Align.center, false);

            int surplus = Math.max(0, totalMoney - goal);
            hud.setColor(Color.LIME);
            hud.draw(batch, "Vuot chi tieu: +$" + surplus, 92f, 116f, 296f, Align.center, false);

            small.setColor(Color.LIGHT_GRAY);
            String hint = finishedLevel < RunSession.MAX_W1_LEVEL
                ? "Bam NEXT LEVEL de ghe Cua hang Australia (Lex)"
                : "Ban da chinh phuc tron ven World 1! Bam NEXT LEVEL de ket toan Xu";
            small.draw(batch, hint, 92f, 88f, 296f, Align.center, false);
        } else {
            banner.setColor(0.55f, 0.08f, 0.05f, 1f);
            banner.draw(batch, "THAT BAI O MAN " + finishedLevel + " / 6", 90f, 202f, 300f, Align.center, false);

            hud.setColor(0.22f, 0.10f, 0.02f, 1f);
            hud.draw(batch, "Tong Tien Mo ($): $" + totalMoney, 90f, 164f, 300f, Align.center, false);
            hud.draw(batch, "Muc tieu can dat: $" + goal, 90f, 140f, 300f, Align.center, false);
            hud.setColor(0.65f, 0.12f, 0.08f, 1f);
            hud.draw(batch, "Con thieu: $" + Math.max(0, goal - totalMoney), 90f, 116f, 300f, Align.center, false);

            small.setColor(0.28f, 0.16f, 0.05f, 1f);
            small.draw(batch, "Bam NEXT LEVEL hoac ENTER de ket toan Xu", 90f, 88f, 300f, Align.center, false);
        }

        menuButton.draw(batch, assets, viewport, touchPoint);
        continueButton.draw(batch, assets, viewport, touchPoint);
        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            advanceNext();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showSettlement(passed && finishedLevel >= RunSession.MAX_W1_LEVEL);
            return;
        }
        if (Gdx.input.justTouched()) {
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            viewport.unproject(touchPoint);
            if (continueButton.contains(touchPoint.x, touchPoint.y)) {
                advanceNext();
            } else if (menuButton.contains(touchPoint.x, touchPoint.y)) {
                game.showSettlement(passed && finishedLevel >= RunSession.MAX_W1_LEVEL);
            }
        }
    }

    private void advanceNext() {
        if (!passed) {
            game.showSettlement(false);
        } else if (finishedLevel >= RunSession.MAX_W1_LEVEL) {
            game.showSettlement(true);
        } else {
            game.showShop();
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
