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

    private static final float[] SLOT_CENTERS_X = {70f, 130f, 185f, 238f, 300f};

    private final GoldMinerGame game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Vector3 touchPoint = new Vector3();

    private final int nextLevelNumber;
    private final List<ShopOffer> offers;
    private final VegasButton[] slotButtons = new VegasButton[5];
    private final VegasButton nextLevelButton;
    private final VegasButton menuButton;

    private float stateTime;
    private float buyAnimTimer = -1f;
    private String lexSpeech;
    private String hoveredDesc = "Cham hoac re chuot vao mon do de xem cong dung.";

    public ShopScreen(GoldMinerGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT, camera);
        this.viewport.apply(true);

        RunSession run = game.getRunSession();
        this.nextLevelNumber = Math.min(RunSession.MAX_W1_LEVEL, run.getCurrentLevel() + 1);
        this.offers = VegasW1ShopGenerator.generateOffers(nextLevelNumber, false, bound -> MathUtils.random(bound - 1));

        for (int i = 0; i < 5; i++) {
            ShopOffer offer = offers.get(i);
            if (offer.isAvailable()) {
                float w = 40f;
                float h = 44f;
                if (offer.itemType == ShopItemType.TITANIUM_CABLE) {
                    w = 52f;
                    h = 18f;
                } else if (offer.itemType == ShopItemType.LUCKY_CLOVER || offer.itemType == ShopItemType.POCKET_WATCH) {
                    w = 32f;
                    h = 30f;
                }
                String prefix = offer.itemType.buttonKeyPrefix;
                slotButtons[i] = new VegasButton(
                    SLOT_CENTERS_X[i] - w * 0.5f,
                    82f,
                    w,
                    h,
                    prefix + "_up",
                    prefix + "_over",
                    prefix + "_down"
                );
            }
        }

        this.nextLevelButton = new VegasButton(
            352f, 232f, 108f, 22f,
            AssetKey.BTN_NEXT_UP, AssetKey.BTN_NEXT_OVER, AssetKey.BTN_NEXT_DOWN
        );
        this.menuButton = new VegasButton(
            20f, 232f, 108f, 22f,
            AssetKey.BTN_MENU_UP, AssetKey.BTN_MENU_OVER, AssetKey.BTN_MENU_DOWN
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
        touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(touchPoint);

        for (int i = 0; i < 5; i++) {
            ShopOffer offer = offers.get(i);
            if (slotButtons[i] != null && offer.isAvailable() && slotButtons[i].contains(touchPoint.x, touchPoint.y)) {
                hoveredDesc = offer.itemType.displayName + " ($" + offer.price + "): " + offer.itemType.description;
                break;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.startLevel(nextLevelNumber);
            return;
        }

        if (Gdx.input.justTouched()) {
            if (nextLevelButton.contains(touchPoint.x, touchPoint.y)) {
                game.startLevel(nextLevelNumber);
                return;
            }
            if (menuButton.contains(touchPoint.x, touchPoint.y)) {
                game.showSettlement(false);
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
        ScreenUtils.clear(0.10f, 0.08f, 0.06f, 1f);
        viewport.apply();

        SpriteBatch batch = game.getBatch();
        Assets assets = game.getAssets();
        BitmapFont hud = game.getHudFont();
        BitmapFont small = game.getSmallFont();
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

        // 1. Interior behind counter window
        batch.draw(interior, 38f, 108f, 404f, 128f);

        // 2. Animated Lex behind counter
        batch.draw(lexFrame, 326f, 102f, lexDef.drawWidth * 0.72f, lexDef.drawHeight * 0.72f);

        // 3. Silver trailer frame + wooden counter over bottom/sides
        batch.draw(frame, 0f, 0f, WorldBounds.WORLD_WIDTH, WorldBounds.WORLD_HEIGHT);

        // 4. Lex speech bubble inside top-left of window
        batch.draw(bubble, 44f, 186f, 240f, 44f);
        small.setColor(0.28f, 0.14f, 0.03f, 1f);
        small.draw(batch, lexSpeech, 54f, 217f, 220f, Align.left, true);

        // 5. Lex buy-grab arm animation when purchasing
        if (buyAnimTimer >= 0f) {
            Animation<TextureRegion> armAnim = assets.getAnimation(AssetKey.LEX_BUY_ARM);
            TextureRegion armFrame = armAnim.getKeyFrame(buyAnimTimer, false);
            batch.draw(armFrame, 286f, 52f, 145f, 112f);
        }

        // 6. Description banner pinned under counter
        batch.draw(banner, 94f, 10f, 292f, 56f);
        small.setColor(0.28f, 0.14f, 0.03f, 1f);
        small.draw(batch, hoveredDesc, 110f, 49f, 260f, Align.center, true);

        // 7. 5 Shop item slots + prices
        for (int i = 0; i < 5; i++) {
            ShopOffer offer = offers.get(i);
            if (slotButtons[i] != null && offer.isAvailable()) {
                slotButtons[i].draw(batch, assets, viewport, touchPoint);
                boolean affordable = run.getMineMoney() >= offer.price;
                hud.setColor(affordable ? Color.LIME : Color.SALMON);
                hud.draw(batch, "$" + offer.price, SLOT_CENTERS_X[i] - 28f, 77f, 56f, Align.center, false);
            }
        }

        // 8. Top header bar: Mine Money ($), Next Level target hint, buttons
        hud.setColor(Color.GOLD);
        hud.draw(batch, "TIEN MO ($): $" + run.getMineMoney() + "   |   THUOC NO: " + run.getDynamiteCount(),
            132f, 250f, 214f, Align.center, false);
        small.setColor(0.12f, 0.10f, 0.08f, 1f);
        small.draw(batch, "CUA HANG AUSTRALIA - CHUAN BI MAN " + nextLevelNumber + "/6",
            126f, 265f, 224f, Align.center, false);

        menuButton.draw(batch, assets, viewport, touchPoint);
        nextLevelButton.draw(batch, assets, viewport, touchPoint);

        batch.end();
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
