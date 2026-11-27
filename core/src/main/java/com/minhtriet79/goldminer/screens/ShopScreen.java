package com.minhtriet79.goldminer.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.minhtriet79.goldminer.GoldMinerGame;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.assets.ThemeConfig;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.domain.run.RunSession;
import com.minhtriet79.goldminer.domain.shop.ShopItemType;
import com.minhtriet79.goldminer.domain.shop.ShopOffer;
import com.minhtriet79.goldminer.domain.shop.ShopService;
import com.minhtriet79.goldminer.domain.shop.VegasW1ShopGenerator;
import com.minhtriet79.goldminer.gameplay.ui.VegasButton;

import java.util.List;

/**
 * Faithful World 1 (Australia) Between-Level Shop featuring Lex (DefineSprite_1568).
 * Generates 5 dynamic Vegas shop slots, plays talk/buy animations & SFX,
 * deducts Mine Money ($), and carries purchased buffs/dynamite into the next level.
 */
public class ShopScreen implements Screen {

    private static final String[] LEX_QUIPS = {
        "Crikey! You have a lot of gold, mate!",
        "No gold shinier than Australian gold!",
        "Stock up for the next mine, mate!",
        "Prices too steep? Dig deeper next round!"
    };

    private static final float VEGAS_W = 800f;
    private static final float VEGAS_H = 600f;
    private static final float[] SLOT_CENTERS_X = {116f, 214f, 308f, 398f, 502f};

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Vector3 touchPoint = new Vector3();

    private final int completedLevel;
    private final int nextLevelNumber;
    private final List<ShopOffer> offers;
    private final VegasButton[] slotButtons = new VegasButton[5];
    private final VegasButton nextLevelButton;
    private final VegasButton menuButton;

    private float stateTime;
    private float buyAnimTimer = -1f;
    private String lexSpeech;
    private String hoveredDesc = "Chạm hoặc rê chuột vào món đồ để xem công dụng.";

    public ShopScreen(GoldMinerGame game) {
        this(game, game.getRunSession().getCurrentLevel());
    }

    public ShopScreen(GoldMinerGame game, int completedLevel) {
        this.game = game;
        this.completedLevel = Math.max(1, Math.min(RunSession.MAX_W1_LEVEL, completedLevel));
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VEGAS_W, VEGAS_H, camera);

        RunSession run = game.getRunSession();
        this.nextLevelNumber = Math.min(RunSession.MAX_W1_LEVEL, this.completedLevel + 1);
        this.offers = VegasW1ShopGenerator.generateOffers(nextLevelNumber, false, bound -> MathUtils.random(bound - 1));

        for (int i = 0; i < 5; i++) {
            ShopOffer offer = offers.get(i);
            if (offer.isAvailable()) {
                float w = 68f;
                float h = 88f;
                if (offer.itemType == ShopItemType.TITANIUM_CABLE) {
                    w = 96f;
                    h = 38f;
                } else if (offer.itemType == ShopItemType.LUCKY_CLOVER || offer.itemType == ShopItemType.POCKET_WATCH) {
                    w = 56f;
                    h = 58f;
                }
                String prefix = offer.itemType.buttonKeyPrefix;
                slotButtons[i] = new VegasButton(
                    SLOT_CENTERS_X[i] - w * 0.5f,
                    186f,
                    w,
                    h,
                    prefix + "_up",
                    prefix + "_over",
                    prefix + "_down"
                );
            }
        }

        this.menuButton = new VegasButton(
            40f, 10f, 156f, 32f,
            AssetKey.BTN_MENU_UP, AssetKey.BTN_MENU_OVER, AssetKey.BTN_MENU_DOWN
        );
        this.nextLevelButton = new VegasButton(
            604f, 10f, 156f, 32f,
            AssetKey.BTN_NEXT_UP, AssetKey.BTN_NEXT_OVER, AssetKey.BTN_NEXT_DOWN
        );

        this.lexSpeech = nextLevelNumber == 2
            ? "My name's Alexander, call me Lex! Click an item... please."
            : LEX_QUIPS[MathUtils.random(LEX_QUIPS.length - 1)];
    }

    @Override
    public void show() {
        game.playMusic(AssetKey.BGM_MENU, 0.35f);
        if (!game.isMuted()) {
            game.getAssets().getSound(AssetKey.SFX_SHOP_OPEN).play(0.75f);
            game.getAssets().getSound(AssetKey.SFX_SHOP_TALK).play(0.65f);
        }
    }

    @Override
    public void render(float rawDt) {
        float dt = Math.min(rawDt, 0.05f);
        stateTime += dt;
        if (buyAnimTimer >= 0f) {
            buyAnimTimer += dt;
            ThemeConfig.AnimationDef armDef = game.getAssets().getAnimationDef(AssetKey.LEX_BUY_ARM);
            if (buyAnimTimer >= armDef.frameDuration * armDef.frames.size) {
                buyAnimTimer = -1f;
            }
        }

        handleHoverAndInput();
        drawShop();
    }

    private void handleHoverAndInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            game.toggleFullscreen();
        }
        touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touchPoint);

        for (int i = 0; i < 5; i++) {
            ShopOffer offer = offers.get(i);
            if (slotButtons[i] != null && offer.isAvailable() && slotButtons[i].contains(touchPoint.x, touchPoint.y)) {
                hoveredDesc = offer.itemType.displayName + " ($" + offer.price + "): " + offer.itemType.description;
                break;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showWorldMap(nextLevelNumber);
            return;
        }

        if (Gdx.input.justTouched()) {
            if (nextLevelButton.contains(touchPoint.x, touchPoint.y)) {
                game.showWorldMap(nextLevelNumber);
                return;
            }
            if (menuButton.contains(touchPoint.x, touchPoint.y)) {
                game.showWorldMap(nextLevelNumber);
                return;
            }
            for (int i = 0; i < 5; i++) {
                ShopOffer offer = offers.get(i);
                if (slotButtons[i] != null && offer.isAvailable() && slotButtons[i].contains(touchPoint.x, touchPoint.y)) {
                    tryPurchase(i, offer);
                    return;
                }
            }
        }
    }

    private void tryPurchase(int idx, ShopOffer offer) {
        ShopService.PurchaseResult result = ShopService.purchase(game.getRunSession(), offer);
        if (result == ShopService.PurchaseResult.SUCCESS) {
            slotButtons[idx].visible = false;
            buyAnimTimer = 0f;
            lexSpeech = "Thanks, mate! Good luck on Level " + nextLevelNumber + "!";
            hoveredDesc = "Da mua " + offer.itemType.displayName + "!";
            if (!game.isMuted()) {
                game.getAssets().getSound(AssetKey.SFX_SHOP_BUY).play(0.85f);
                game.getAssets().getSound(AssetKey.SFX_COIN).play(0.65f);
            }
        } else if (result == ShopService.PurchaseResult.INSUFFICIENT_FUNDS) {
            lexSpeech = "Not enough cash for that one, mate!";
        }
    }

    private void drawShop() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        viewport.apply(true);

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        Texture white = game.getWhitePixel();
        BitmapFont hud = game.getPlayHudFont();
        BitmapFont small = game.getPlaySmallFont();
        RunSession run = game.getRunSession();

        Texture interior = assets.getTexture(AssetKey.BG_SHOP_INTERIOR);
        Texture frame = assets.getTexture(AssetKey.BG_SHOP_FRAME);
        Texture banner = assets.getTexture(AssetKey.SHOP_DESC_BANNER);
        Texture bubble = assets.getTexture(AssetKey.SHOP_SPEECH_BUBBLE);

        Animation<TextureRegion> lexAnim = assets.getAnimation(AssetKey.LEX_TALK);
        ThemeConfig.AnimationDef lexDef = assets.getAnimationDef(AssetKey.LEX_TALK);
        TextureRegion lexFrame = lexAnim.getKeyFrame(stateTime, true);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);

        // 1. Interior behind counter window
        batch.draw(interior, 64f, 230f, 673f, 280f);

        // 2. Animated Lex behind counter
        batch.draw(lexFrame, 536f, 224f, lexDef.drawWidth * 1.40f, lexDef.drawHeight * 1.40f);

        // 3. Silver trailer frame + wooden counter (native 800x600)
        batch.draw(frame, 0f, 0f, VEGAS_W, VEGAS_H);

        // 4. Lex speech bubble inside top-left of window
        batch.draw(bubble, 82f, 412f, 396f, 96f);
        small.setColor(0.28f, 0.14f, 0.03f, 1f);
        small.draw(batch, lexSpeech, 100f, 480f, 360f, Align.left, true);

        // 5. Lex buy-grab arm animation when purchasing
        if (buyAnimTimer >= 0f) {
            Animation<TextureRegion> armAnim = assets.getAnimation(AssetKey.LEX_BUY_ARM);
            TextureRegion armFrame = armAnim.getKeyFrame(buyAnimTimer, false);
            batch.draw(armFrame, 476f, 120f, 240f, 190f);
        }

        // 6. Description banner pinned under counter
        batch.draw(banner, 156f, 56f, 486f, 115f);
        small.setColor(0.28f, 0.14f, 0.03f, 1f);
        small.draw(batch, hoveredDesc, 186f, 138f, 426f, Align.center, true);

        // 7. 5 Shop item slots + prices
        for (int i = 0; i < 5; i++) {
            ShopOffer offer = offers.get(i);
            if (slotButtons[i] != null && offer.isAvailable()) {
                slotButtons[i].draw(batch, assets, viewport, touchPoint);
                boolean affordable = run.getMineMoney() >= offer.price;
                hud.setColor(affordable ? Color.LIME : Color.SALMON);
                hud.draw(batch, "$" + offer.price, SLOT_CENTERS_X[i] - 45f, 180f, 90f, Align.center, false);
            }
        }

        // 8. Top header title inside silver trailer top panel
        small.setColor(0.14f, 0.10f, 0.06f, 1f);
        small.draw(batch, "CỬA HÀNG AUSTRALIA - MỞ KHÓA MÀN " + nextLevelNumber + "/6   |   THUỐC NỔ: " + run.getDynamiteCount(),
            178f, 578f, 520f, Align.center, false);

        // 9. Authentic hollow 800x600 Vegas Bezel & Bottom Bar + Logo
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_VEGAS_FRAME), 0f, 0f, VEGAS_W, VEGAS_H);
        batch.draw(assets.getTexture(AssetKey.UI_VEGAS_LOGO), 12f, 506f, 158f, 84f);

        // Black cash readout box in center of bottom bar (like Vegas Shop)
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(white, 316f, 12f, 168f, 28f);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.MENU_COIN_ICON), 286f, 14f, 24f, 24f);
        hud.setColor(Color.LIME);
        hud.draw(batch, "$" + run.getMineMoney(), 320f, 33f, 160f, Align.center, false);

        menuButton.draw(batch, assets, viewport, touchPoint);
        nextLevelButton.draw(batch, assets, viewport, touchPoint);

        batch.end();
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
    }

    @Override
    public void dispose() {
    }
}
