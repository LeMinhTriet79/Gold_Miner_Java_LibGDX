package com.minhtriet79.goldminer.studio.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.utils.Disposable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded LRU cache for disk-backed preview textures so inspecting large folders
 * (e.g. 560-frame DefineSprite_3075 or 18k sprites/) stays within a fixed VRAM ceiling.
 */
public class LruTextureCache implements Disposable {

    private final int maxEntries;
    private final LinkedHashMap<String, Texture> cache;

    public LruTextureCache(int maxEntries) {
        this.maxEntries = Math.max(4, maxEntries);
        this.cache = new LinkedHashMap<>(this.maxEntries + 2, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Texture> eldest) {
                if (size() > LruTextureCache.this.maxEntries) {
                    if (eldest.getValue() != null) {
                        eldest.getValue().dispose();
                    }
                    return true;
                }
                return false;
            }
        };
    }

    public Texture get(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        String key = file.getAbsolutePath();
        Texture existing = cache.get(key);
        if (existing != null) {
            return existing;
        }
        Texture loaded = new Texture(Gdx.files.absolute(key));
        loaded.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        cache.put(key, loaded);
        return loaded;
    }

    public void clear() {
        for (Texture tex : cache.values()) {
            if (tex != null) {
                tex.dispose();
            }
        }
        cache.clear();
    }

    @Override
    public void dispose() {
        clear();
    }
}
