package com.minhtriet79.goldminer.studio.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox.VisCheckBoxStyle;
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle;
import com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle;

/**
 * Configures VisUI's Skin with anti-aliased Segoe UI FreeType Vietnamese fonts
 * so Studio tabs, inspector inputs, lists, and dialogs render razor-sharp UTF-8 text.
 */
public class StudioSkinManager implements Disposable {

    private static final String VIETNAMESE_CHARS =
        FreeTypeFontGenerator.DEFAULT_CHARS
            + "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ"
            + "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ";

    private BitmapFont uiFont;
    private BitmapFont smallFont;
    private boolean initialized;

    public void ensureLoaded() {
        if (!VisUI.isLoaded()) {
            VisUI.load();
        }
        if (initialized) {
            return;
        }
        FileHandle fontFile = Gdx.files.internal("fonts/segoeui.ttf");
        if (!fontFile.exists()) {
            fontFile = Gdx.files.internal("fonts/arialbd.ttf");
        }
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(fontFile);
        uiFont = generateFont(gen, 14);
        smallFont = generateFont(gen, 12);
        gen.dispose();

        applyFontsToVisSkin(VisUI.getSkin());
        initialized = true;
    }

    private BitmapFont generateFont(FreeTypeFontGenerator generator, int pixelSize) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = pixelSize;
        p.minFilter = TextureFilter.Linear;
        p.magFilter = TextureFilter.Linear;
        p.color = Color.WHITE;
        p.characters = VIETNAMESE_CHARS;
        BitmapFont font = generator.generateFont(p);
        font.getData().markupEnabled = false;
        return font;
    }

    private void applyFontsToVisSkin(Skin skin) {
        skin.add("default-font", uiFont, BitmapFont.class);
        skin.add("small-font", smallFont, BitmapFont.class);

        patchFont(skin.getAll(LabelStyle.class), uiFont);
        patchSmallLabels(skin.getAll(LabelStyle.class));
        patchTextButtons(skin.getAll(TextButtonStyle.class), uiFont);
        patchVisTextButtons(skin.getAll(VisTextButtonStyle.class), uiFont);
        patchTextFields(skin.getAll(TextFieldStyle.class), uiFont);
        patchVisTextFields(skin.getAll(VisTextFieldStyle.class), uiFont);
        patchCheckBoxes(skin.getAll(CheckBoxStyle.class), smallFont);
        patchVisCheckBoxes(skin.getAll(VisCheckBoxStyle.class), smallFont);
        patchLists(skin.getAll(ListStyle.class), smallFont);
        patchSelectBoxes(skin.getAll(SelectBoxStyle.class), smallFont);
        patchWindows(skin.getAll(WindowStyle.class), uiFont);
    }

    private void patchSmallLabels(ObjectMap<String, LabelStyle> map) {
        if (map == null) return;
        LabelStyle small = map.get("small");
        if (small != null) {
            small.font = smallFont;
        }
    }

    private void patchFont(ObjectMap<String, LabelStyle> map, BitmapFont font) {
        if (map == null) return;
        for (LabelStyle s : map.values()) {
            s.font = font;
        }
    }

    private void patchTextButtons(ObjectMap<String, TextButtonStyle> map, BitmapFont font) {
        if (map == null) return;
        for (TextButtonStyle s : map.values()) {
            s.font = font;
        }
    }

    private void patchVisTextButtons(ObjectMap<String, VisTextButtonStyle> map, BitmapFont font) {
        if (map == null) return;
        for (VisTextButtonStyle s : map.values()) {
            s.font = font;
        }
    }

    private void patchTextFields(ObjectMap<String, TextFieldStyle> map, BitmapFont font) {
        if (map == null) return;
        for (TextFieldStyle s : map.values()) {
            s.font = font;
        }
    }

    private void patchVisTextFields(ObjectMap<String, VisTextFieldStyle> map, BitmapFont font) {
        if (map == null) return;
        for (VisTextFieldStyle s : map.values()) {
            s.font = font;
        }
    }

    private void patchCheckBoxes(ObjectMap<String, CheckBoxStyle> map, BitmapFont font) {
        if (map == null) return;
        for (CheckBoxStyle s : map.values()) {
            s.font = font;
        }
    }

    private void patchVisCheckBoxes(ObjectMap<String, VisCheckBoxStyle> map, BitmapFont font) {
        if (map == null) return;
        for (VisCheckBoxStyle s : map.values()) {
            s.font = font;
        }
    }

    private void patchLists(ObjectMap<String, ListStyle> map, BitmapFont font) {
        if (map == null) return;
        for (ListStyle s : map.values()) {
            s.font = font;
        }
    }

    private void patchSelectBoxes(ObjectMap<String, SelectBoxStyle> map, BitmapFont font) {
        if (map == null) return;
        for (SelectBoxStyle s : map.values()) {
            s.font = font;
            if (s.listStyle != null) {
                s.listStyle.font = font;
            }
        }
    }

    private void patchWindows(ObjectMap<String, WindowStyle> map, BitmapFont font) {
        if (map == null) return;
        for (WindowStyle s : map.values()) {
            s.titleFont = font;
        }
    }

    public BitmapFont getUiFont() {
        return uiFont;
    }

    public BitmapFont getSmallFont() {
        return smallFont;
    }

    @Override
    public void dispose() {
        if (uiFont != null) {
            uiFont.dispose();
            uiFont = null;
        }
        if (smallFont != null) {
            smallFont.dispose();
            smallFont = null;
        }
        initialized = false;
    }
}
