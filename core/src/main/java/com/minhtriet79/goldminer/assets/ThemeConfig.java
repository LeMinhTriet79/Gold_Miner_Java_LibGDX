package com.minhtriet79.goldminer.assets;

import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * POJO loaded from {@code themes/<themeName>/theme.json} mapping semantic keys to concrete asset files.
 */
public class ThemeConfig {
    public String name = "vegas";
    public String filter = "LINEAR";
    public Array<String> nearestSprites = new Array<>();
    public ObjectMap<String, String> sprites = new ObjectMap<>();
    public ObjectMap<String, AnimationDef> animations = new ObjectMap<>();
    public ObjectMap<String, String> sounds = new ObjectMap<>();
    public ObjectMap<String, String> music = new ObjectMap<>();

    public ThemeConfig() {
    }

    public TextureFilter resolveTextureFilter() {
        if ("NEAREST".equalsIgnoreCase(filter)) {
            return TextureFilter.Nearest;
        }
        return TextureFilter.Linear;
    }

    public boolean isNearestSprite(String key) {
        return key != null && nearestSprites != null && nearestSprites.contains(key, false);
    }

    /**
     * Animation sequence metadata in a theme pack.
     */
    public static class AnimationDef {
        public float frameDuration = 1f / 18f;
        public String playMode = "LOOP";
        public float drawWidth;
        public float drawHeight;
        public float anchorX = 0.5f;
        public float anchorY = 0f;
        public Array<String> frames = new Array<>();

        public AnimationDef() {
        }

        public boolean isLooping() {
            return !"NORMAL".equalsIgnoreCase(playMode);
        }
    }
}
