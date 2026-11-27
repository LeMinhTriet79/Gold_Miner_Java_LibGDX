package com.minhtriet79.goldminer.gameplay.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.minhtriet79.goldminer.GoldMinerGame;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.domain.account.AccountRecord;
import com.minhtriet79.goldminer.domain.account.AccountService;
import com.minhtriet79.goldminer.domain.account.AuthResult;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Reusable 16-bit wooden-brass top chrome shared by {@code WorldHubScreen}, {@code GoldMineScreen},
 * and {@code WorldMapScreen}: top-left {@code [BACK]}, centered title banner, and top-right Profile Card
 * with hidden dropdown ({@code AUTO LOGIN}, {@code EDIT NAME}, {@code SWITCH ACCOUNT}, {@code LOGOUT}).
 */
public class MenuTopHeader {

    private static final int MAX_RENAME_LEN = 20;

    private static final Color COL_TEXT_SHADOW = new Color(0.06f, 0.03f, 0.01f, 0.96f);
    private static final Color COL_CREAM = new Color(0.99f, 0.95f, 0.82f, 1f);
    private static final Color COL_GOLD = new Color(1.00f, 0.85f, 0.28f, 1f);
    private static final Color COL_ERROR = new Color(1.00f, 0.48f, 0.42f, 1f);
    private static final Color COL_SCRIM = new Color(0.03f, 0.02f, 0.01f, 0.64f);

    private final GoldMinerGame game;
    private final String titleText;
    private final boolean wideBanner;
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // Pre-allocated rectangles anchored on resize
    public final Rectangle backRect = new Rectangle(8f, 238f, 76f, 24f);
    private final Rectangle titleRect = new Rectangle(148f, 236f, 184f, 28f);
    private final Rectangle accountCardRect = new Rectangle(336f, 224f, 136f, 40f);
    private final Rectangle dropdownRect = new Rectangle(336f, 136f, 136f, 88f);
    private final Rectangle dropAutoLoginRect = new Rectangle(342f, 201f, 124f, 17f);
    private final Rectangle dropEditNameRect = new Rectangle(342f, 181f, 124f, 17f);
    private final Rectangle dropSwitchRect = new Rectangle(342f, 161f, 124f, 17f);
    private final Rectangle dropLogoutRect = new Rectangle(342f, 141f, 124f, 17f);

    // Inline Rename Modal
    private final Rectangle renameModalRect = new Rectangle(120f, 78f, 240f, 114f);
    private final Rectangle renameInputRect = new Rectangle(140f, 126f, 200f, 18f);
    private final Rectangle renameSaveRect = new Rectangle(140f, 92f, 92f, 22f);
    private final Rectangle renameCancelRect = new Rectangle(248f, 92f, 92f, 22f);

    private final StringBuilder renameBuf = new StringBuilder();
    private String renameText = "";
    private String renameFeedback = "";
    private boolean renameModalOpen = false;
    private boolean dropdownOpen = false;

    private String cachedDisplayName = "KHÁCH";
    private String cachedCoinsText = "0 XU";
    private String cachedAutoLoginText = "AUTO LOGIN: OFF";

    private float viewLeft = 0f;
    private float viewBottom = 0f;
    private float viewWidth = WorldBounds.WORLD_WIDTH;
    private float viewHeight = WorldBounds.WORLD_HEIGHT;

    public MenuTopHeader(GoldMinerGame game, String titleText, boolean wideBanner) {
        this.game = game;
        this.titleText = titleText;
        this.wideBanner = wideBanner;
        refreshAccountCache();
    }

    public void updateLayout(float viewLeft, float viewBottom, float viewWidth, float viewHeight) {
        this.viewLeft = viewLeft;
        this.viewBottom = viewBottom;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;

        float viewTop = viewBottom + viewHeight;
        float viewRight = viewLeft + viewWidth;

        backRect.set(MathUtils.floor(viewLeft + 8f), MathUtils.floor(viewTop - 30f), 76f, 24f);

        float bannerW = wideBanner ? 220f : 184f;
        titleRect.set(
            MathUtils.floor((WorldBounds.WORLD_WIDTH - bannerW) * 0.5f),
            MathUtils.floor(viewTop - 32f),
            bannerW,
            28f
        );

        float cardX = MathUtils.floor(viewRight - 144f);
        float cardY = MathUtils.floor(viewTop - 44f);
        accountCardRect.set(cardX, cardY, 136f, 40f);
        dropdownRect.set(cardX, cardY - 88f, 136f, 88f);
        dropAutoLoginRect.set(cardX + 6f, cardY - 23f, 124f, 17f);
        dropEditNameRect.set(cardX + 6f, cardY - 43f, 124f, 17f);
        dropSwitchRect.set(cardX + 6f, cardY - 63f, 124f, 17f);
        dropLogoutRect.set(cardX + 6f, cardY - 83f, 124f, 17f);
    }

    public void refreshAccountCache() {
        AccountService svc = game.getAccountService();
        AccountRecord active = svc != null ? svc.getActiveAccount() : null;
        if (active != null) {
            cachedDisplayName = (active.displayName != null && !active.displayName.trim().isEmpty())
                ? active.displayName.trim().toUpperCase(Locale.ROOT)
                : active.username.toUpperCase(Locale.ROOT);
            cachedCoinsText = numberFormat.format(active.coins) + " XU";
        } else {
            cachedDisplayName = "KHÁCH";
            cachedCoinsText = numberFormat.format(game.getSaveData().officialCoins) + " XU";
        }
        boolean autoOn = svc != null && svc.isAutoLoginEnabled();
        cachedAutoLoginText = autoOn ? "AUTO LOGIN: ON" : "AUTO LOGIN: OFF";
    }

    public boolean isBlockingModalOpen() {
        return renameModalOpen;
    }

    public boolean isDropdownOpen() {
        return dropdownOpen;
    }

    public boolean handleEscape() {
        if (renameModalOpen) {
            renameModalOpen = false;
            renameFeedback = "";
            return true;
        }
        if (dropdownOpen) {
            dropdownOpen = false;
            return true;
        }
        return false;
    }

    public boolean handleKeyDown(int keycode) {
        if (renameModalOpen && (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER)) {
            submitRename();
            return true;
        }
        return false;
    }

    public boolean handleKeyTyped(char character) {
        if (!renameModalOpen) {
            return false;
        }
        if (character == '\b') {
            if (renameBuf.length() > 0) {
                renameBuf.setLength(renameBuf.length() - 1);
                renameText = renameBuf.toString();
            }
            return true;
        }
        if (character == '\r' || character == '\n' || character == '\t') {
            return false;
        }
        if (character >= 32 && character != 127 && renameBuf.length() < MAX_RENAME_LEN) {
            renameBuf.append(character);
            renameText = renameBuf.toString();
            return true;
        }
        return false;
    }

    public boolean handleClick(float wx, float wy, Runnable onBack) {
        if (renameModalOpen) {
            if (renameSaveRect.contains(wx, wy)) {
                submitRename();
                return true;
            }
            if (renameCancelRect.contains(wx, wy) || !renameModalRect.contains(wx, wy)) {
                renameModalOpen = false;
                renameFeedback = "";
                return true;
            }
            return renameModalRect.contains(wx, wy);
        }

        if (accountCardRect.contains(wx, wy)) {
            dropdownOpen = !dropdownOpen;
            return true;
        }

        if (dropdownOpen) {
            AccountService svc = game.getAccountService();
            if (dropAutoLoginRect.contains(wx, wy)) {
                if (svc != null) {
                    svc.setAutoLogin(!svc.isAutoLoginEnabled());
                    refreshAccountCache();
                }
                return true;
            }
            if (dropEditNameRect.contains(wx, wy)) {
                AccountRecord active = svc != null ? svc.getActiveAccount() : null;
                renameBuf.setLength(0);
                if (active != null && active.displayName != null) {
                    renameBuf.append(active.displayName);
                }
                renameText = renameBuf.toString();
                renameFeedback = "";
                dropdownOpen = false;
                renameModalOpen = true;
                return true;
            }
            if (dropSwitchRect.contains(wx, wy)) {
                if (svc != null) {
                    svc.switchAccount();
                }
                dropdownOpen = false;
                game.showMainMenu();
                return true;
            }
            if (dropLogoutRect.contains(wx, wy)) {
                if (svc != null) {
                    svc.logout();
                }
                dropdownOpen = false;
                game.showMainMenu();
                return true;
            }
            if (!dropdownRect.contains(wx, wy)) {
                dropdownOpen = false;
                return true;
            }
            return true;
        }

        if (backRect.contains(wx, wy)) {
            if (onBack != null) {
                onBack.run();
            }
            return true;
        }
        return false;
    }

    private void submitRename() {
        AccountService svc = game.getAccountService();
        if (svc == null) {
            renameModalOpen = false;
            return;
        }
        AuthResult res = svc.updateDisplayName(renameText);
        if (res.success) {
            renameModalOpen = false;
            renameFeedback = "";
            refreshAccountCache();
        } else {
            renameFeedback = res.message;
        }
    }

    public void draw(SpriteBatch batch, float mouseX, float mouseY, float elapsedTime) {
        Assets assets = game.getAssets();
        Texture white = game.getWhitePixel();
        BitmapFont tiny = game.getPixelTinyFont();
        BitmapFont small = game.getPixelSmallFont();
        BitmapFont hud = game.getPixelHudFont();

        // 1. Top-left [BACK]
        boolean overBack = !renameModalOpen && backRect.contains(mouseX, mouseY);
        batch.setColor(Color.WHITE);
        batch.draw(
            assets.getTexture(overBack ? AssetKey.UI_BTN_BACK_OVER : AssetKey.UI_BTN_BACK_UP),
            backRect.x, backRect.y, backRect.width, backRect.height
        );
        float backTextY = MathUtils.floor(backRect.y + (backRect.height + small.getCapHeight()) * 0.5f);
        drawShadowText(small, batch, "BACK", backRect.x, backTextY, backRect.width, Align.center, false,
            overBack ? Color.WHITE : COL_CREAM);

        // 2. Top-center Title Banner
        batch.setColor(Color.WHITE);
        batch.draw(
            assets.getTexture(wideBanner ? AssetKey.UI_TITLE_BANNER_WIDE : AssetKey.UI_TITLE_BANNER),
            titleRect.x, titleRect.y, titleRect.width, titleRect.height
        );
        float titleTextY = MathUtils.floor(titleRect.y + (titleRect.height + hud.getCapHeight()) * 0.5f);
        drawShadowText(hud, batch, titleText, titleRect.x, titleTextY, titleRect.width, Align.center, false, COL_GOLD);

        // 3. Top-right Profile Card
        int cx = MathUtils.floor(accountCardRect.x);
        int cy = MathUtils.floor(accountCardRect.y);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.UI_ACCOUNT_CARD), cx, cy, accountCardRect.width, accountCardRect.height);
        batch.draw(assets.getTexture(AssetKey.MENU_AVATAR), cx + 6f, cy + 5f, 30f, 30f);

        drawShadowText(small, batch, cachedDisplayName, cx + 41f, cy + 32f, 78f, Align.left, false, COL_CREAM);
        drawDropdownArrow(batch, white, cx + 123, cy + 27, dropdownOpen);

        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(AssetKey.MENU_COIN_ICON), cx + 41f, cy + 8f, 10f, 10f);
        drawShadowText(tiny, batch, cachedCoinsText, cx + 54f, cy + 17f, 76f, Align.left, false, COL_GOLD);

        // 4. Hidden Dropdown Menu (Auto Login / Edit Name / Switch Account / Logout)
        if (dropdownOpen) {
            batch.setColor(Color.WHITE);
            batch.draw(
                assets.getTexture(AssetKey.UI_ACCOUNT_DROPDOWN),
                dropdownRect.x, dropdownRect.y, dropdownRect.width, dropdownRect.height
            );

            boolean overAuto = !renameModalOpen && dropAutoLoginRect.contains(mouseX, mouseY);
            batch.draw(
                assets.getTexture(overAuto ? AssetKey.UI_INPUT_FOCUS : AssetKey.UI_INPUT_NORMAL),
                dropAutoLoginRect.x, dropAutoLoginRect.y, dropAutoLoginRect.width, dropAutoLoginRect.height
            );
            AccountService svc = game.getAccountService();
            boolean autoOn = svc != null && svc.isAutoLoginEnabled();
            batch.draw(
                assets.getTexture(autoOn ? AssetKey.UI_CHECKBOX_ON : AssetKey.UI_CHECKBOX_OFF),
                dropAutoLoginRect.x + 3f, dropAutoLoginRect.y + 3f, 11f, 11f
            );
            float autoTy = MathUtils.floor(dropAutoLoginRect.y + (dropAutoLoginRect.height + tiny.getCapHeight()) * 0.5f);
            drawShadowText(tiny, batch, cachedAutoLoginText, dropAutoLoginRect.x + 17f, autoTy,
                dropAutoLoginRect.width - 20f, Align.left, false, COL_CREAM);

            boolean overEdit = !renameModalOpen && dropEditNameRect.contains(mouseX, mouseY);
            drawSpriteButton(batch, assets, tiny, dropEditNameRect, "EDIT NAME",
                overEdit ? AssetKey.UI_BTN_BLUE_OVER : AssetKey.UI_BTN_BLUE_UP, Color.WHITE);

            boolean overSwitch = !renameModalOpen && dropSwitchRect.contains(mouseX, mouseY);
            drawSpriteButton(batch, assets, tiny, dropSwitchRect, "SWITCH ACCOUNT",
                overSwitch ? AssetKey.UI_BTN_BLUE_OVER : AssetKey.UI_BTN_BLUE_UP, Color.WHITE);

            boolean overLogout = !renameModalOpen && dropLogoutRect.contains(mouseX, mouseY);
            drawSpriteButton(batch, assets, tiny, dropLogoutRect, "LOGOUT",
                overLogout ? AssetKey.UI_BTN_RED_OVER : AssetKey.UI_BTN_RED_UP, Color.WHITE);
        }

        // 5. Optional Inline Rename Modal
        if (renameModalOpen) {
            batch.setColor(COL_SCRIM);
            batch.draw(white, viewLeft - 2f, viewBottom - 2f, viewWidth + 4f, viewHeight + 4f);
            batch.setColor(Color.WHITE);
            batch.draw(
                assets.getTexture(AssetKey.UI_RENAME_PANEL),
                renameModalRect.x, renameModalRect.y, renameModalRect.width, renameModalRect.height
            );
            drawShadowText(small, batch, "ĐỔI TÊN HIỂN THỊ",
                renameModalRect.x, renameModalRect.y + 98f, renameModalRect.width, Align.center, false, COL_GOLD);

            batch.draw(assets.getTexture(AssetKey.UI_INPUT_FOCUS),
                renameInputRect.x, renameInputRect.y, renameInputRect.width, renameInputRect.height);
            float inTy = MathUtils.floor(renameInputRect.y + (renameInputRect.height + small.getCapHeight()) * 0.5f);
            drawShadowText(small, batch, renameText, renameInputRect.x + 6f, inTy, renameInputRect.width - 12f,
                Align.left, false, Color.WHITE);
            if ((int) (elapsedTime * 2.2f) % 2 == 0) {
                glyphLayout.setText(small, renameText);
                float caretX = Math.min(renameInputRect.x + renameInputRect.width - 5f,
                    renameInputRect.x + 6f + glyphLayout.width + 1f);
                batch.setColor(COL_GOLD);
                batch.draw(white, MathUtils.floor(caretX), renameInputRect.y + 3f, 1f, renameInputRect.height - 6f);
                batch.setColor(Color.WHITE);
            }

            if (!renameFeedback.isEmpty()) {
                drawShadowText(tiny, batch, renameFeedback,
                    renameModalRect.x + 12f, renameModalRect.y + 121f, renameModalRect.width - 24f,
                    Align.center, false, COL_ERROR);
            }

            boolean overSave = renameSaveRect.contains(mouseX, mouseY);
            boolean overCancel = renameCancelRect.contains(mouseX, mouseY);
            drawSpriteButton(batch, assets, small, renameSaveRect, "LƯU TÊN",
                overSave ? AssetKey.UI_BTN_GREEN_OVER : AssetKey.UI_BTN_GREEN_UP, Color.WHITE);
            drawSpriteButton(batch, assets, small, renameCancelRect, "HỦY",
                overCancel ? AssetKey.UI_BTN_TICKET_RED_OVER : AssetKey.UI_BTN_TICKET_RED_UP, Color.WHITE);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawSpriteButton(
        SpriteBatch batch, Assets assets, BitmapFont font, Rectangle rect, String text, String texKey, Color textCol
    ) {
        int x = MathUtils.floor(rect.x);
        int y = MathUtils.floor(rect.y);
        int w = MathUtils.floor(rect.width);
        int h = MathUtils.floor(rect.height);
        batch.setColor(Color.WHITE);
        batch.draw(assets.getTexture(texKey), x, y, w, h);
        float textY = MathUtils.floor(y + (h + font.getCapHeight()) * 0.5f);
        drawShadowText(font, batch, text, x, textY, w, Align.center, false, textCol);
    }

    private void drawDropdownArrow(SpriteBatch batch, Texture white, int x, int y, boolean pointUp) {
        batch.setColor(COL_GOLD);
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

    public static void drawShadowText(
        BitmapFont font, SpriteBatch batch, String text, float x, float y,
        float width, int align, boolean wrap, Color color
    ) {
        int ix = MathUtils.floor(x);
        int iy = MathUtils.floor(y);
        font.setColor(COL_TEXT_SHADOW);
        font.draw(batch, text, ix + 1f, iy - 1f, width, align, wrap);
        font.setColor(color);
        font.draw(batch, text, ix, iy, width, align, wrap);
    }

    public static float computeCoverFillX(float viewLeft, float viewWidth, float bgDrawW) {
        return MathUtils.floor(viewLeft + (viewWidth - bgDrawW) * 0.5f);
    }

    public static float computeCoverFillY(float viewBottom, float viewHeight, float bgDrawH) {
        return MathUtils.floor(viewBottom + (viewHeight - bgDrawH) * 0.5f);
    }
}
