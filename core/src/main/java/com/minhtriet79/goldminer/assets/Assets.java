package com.minhtriet79.goldminer.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.minhtriet79.goldminer.studio.io.ImportedAssetManifest;
import com.minhtriet79.goldminer.studio.io.StudioFileHelper;

import java.io.File;
import java.util.Locale;

/**
 * Theme-aware wrapper over libGDX {@link AssetManager} plus dynamically imported
 * Studio assets under {@code assets/themes/vegas/imported/} with managed {@link Disposable} cleanup.
 */
public class Assets implements Disposable {

    private final AssetManager manager = new AssetManager();
    private final Json json = new Json();
    private final ObjectMap<String, String> spritePaths = new ObjectMap<>();
    private final ObjectMap<String, String> soundPaths = new ObjectMap<>();
    private final ObjectMap<String, String> musicPaths = new ObjectMap<>();
    private final ObjectMap<String, Texture> dynamicTextures = new ObjectMap<>();
    private final ObjectMap<String, Sound> dynamicSounds = new ObjectMap<>();
    private final ObjectMap<String, Animation<TextureRegion>> animations = new ObjectMap<>();
    private final ObjectMap<String, ThemeConfig.AnimationDef> animationDefs = new ObjectMap<>();
    private final Array<Texture> ownedTextures = new Array<>();
    private final Array<Sound> ownedSounds = new Array<>();

    private ThemeConfig activeConfig;
    private String activeThemeDir;
    private Texture fallbackTexture;

    /**
     * Loads the specified theme synchronously, applies its configured {@link TextureFilter},
     * and scans {@code imported/} for any custom Studio sprites or animations.
     */
    public void loadTheme(String themeName) {
        clearLoadedTheme();

        activeThemeDir = "themes/" + themeName + "/";
        FileHandle configFile = Gdx.files.internal(activeThemeDir + "theme.json");
        activeConfig = json.fromJson(ThemeConfig.class, configFile);

        TextureFilter filter = activeConfig.resolveTextureFilter();
        TextureParameter texParam = new TextureParameter();
        texParam.minFilter = filter;
        texParam.magFilter = filter;

        TextureParameter nearestTexParam = new TextureParameter();
        nearestTexParam.minFilter = TextureFilter.Nearest;
        nearestTexParam.magFilter = TextureFilter.Nearest;

        for (ObjectMap.Entry<String, String> entry : activeConfig.sprites.entries()) {
            String fullPath = activeThemeDir + entry.value;
            spritePaths.put(entry.key, fullPath);
            TextureParameter spriteParam = activeConfig.isNearestSprite(entry.key) ? nearestTexParam : texParam;
            manager.load(fullPath, Texture.class, spriteParam);
        }

        for (ObjectMap.Entry<String, ThemeConfig.AnimationDef> entry : activeConfig.animations.entries()) {
            for (String frameRelPath : entry.value.frames) {
                String fullPath = activeThemeDir + frameRelPath;
                if (!manager.isLoaded(fullPath)) {
                    manager.load(fullPath, Texture.class, texParam);
                }
            }
        }

        for (ObjectMap.Entry<String, String> entry : activeConfig.sounds.entries()) {
            String fullPath = activeThemeDir + entry.value;
            soundPaths.put(entry.key, fullPath);
            manager.load(fullPath, Sound.class);
        }

        for (ObjectMap.Entry<String, String> entry : activeConfig.music.entries()) {
            String fullPath = activeThemeDir + entry.value;
            musicPaths.put(entry.key, fullPath);
            manager.load(fullPath, Music.class);
        }

        manager.finishLoading();
        buildAnimations();
        loadImportedAssets();
    }

    private void buildAnimations() {
        for (ObjectMap.Entry<String, ThemeConfig.AnimationDef> entry : activeConfig.animations.entries()) {
            ThemeConfig.AnimationDef def = entry.value;
            Array<TextureRegion> regions = new Array<>(def.frames.size);
            for (String frameRelPath : def.frames) {
                Texture tex = manager.get(activeThemeDir + frameRelPath, Texture.class);
                regions.add(new TextureRegion(tex));
            }
            Animation.PlayMode mode = def.isLooping() ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL;
            animations.put(entry.key, new Animation<>(def.frameDuration, regions, mode));
            animationDefs.put(entry.key, def);
        }
    }

    private void loadImportedAssets() {
        File themeRoot = new File(StudioFileHelper.resolveAssetsRoot(), activeThemeDir);
        ImportedAssetManifest manifest = StudioFileHelper.loadImportedManifest();

        for (ObjectMap.Entry<String, String> entry : manifest.sprites.entries()) {
            File file = new File(themeRoot, entry.value);
            if (file.isFile()) {
                registerImportedTexture(entry.key, Gdx.files.absolute(file.getAbsolutePath()));
            }
        }

        FileHandle themeHandle = Gdx.files.absolute(themeRoot.getAbsolutePath());
        for (ObjectMap.Entry<String, ThemeConfig.AnimationDef> entry : manifest.animations.entries()) {
            registerImportedAnimation(entry.key, entry.value, themeHandle);
        }

        for (ObjectMap.Entry<String, String> entry : manifest.audio.entries()) {
            File file = new File(themeRoot, entry.value);
            if (file.isFile()) {
                registerImportedSound(entry.key, Gdx.files.absolute(file.getAbsolutePath()));
            }
        }

        // Also scan imported/{entity,ui,background} folders for manually dropped textures
        for (String cat : new String[] {"entity", "ui", "background"}) {
            File dir = new File(themeRoot, "imported/" + cat);
            File[] files = dir.listFiles();
            if (files == null) {
                continue;
            }
            for (File f : files) {
                String lower = f.getName().toLowerCase(Locale.ROOT);
                if (f.isFile() && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
                    String key = StudioFileHelper.sanitizeKey(f.getName());
                    if (!dynamicTextures.containsKey(key) && !spritePaths.containsKey(key)) {
                        registerImportedTexture(key, Gdx.files.absolute(f.getAbsolutePath()));
                    }
                }
            }
        }
    }

    public void registerImportedSound(String key, FileHandle file) {
        if (!file.exists()) {
            return;
        }
        Sound sound = Gdx.audio.newSound(file);
        dynamicSounds.put(key, sound);
        ownedSounds.add(sound);
    }

    public boolean hasSound(String semanticKey) {
        return semanticKey != null && (soundPaths.containsKey(semanticKey) || dynamicSounds.containsKey(semanticKey));
    }

    public void registerImportedTexture(String key, FileHandle file) {
        if (!file.exists()) {
            return;
        }
        Texture tex = new Texture(file);
        TextureFilter filter = activeConfig != null ? activeConfig.resolveTextureFilter() : TextureFilter.Linear;
        tex.setFilter(filter, filter);
        dynamicTextures.put(key, tex);
        ownedTextures.add(tex);
    }

    public void registerImportedAnimation(String key, ThemeConfig.AnimationDef def, FileHandle themeDir) {
        if (def == null || def.frames == null || def.frames.size == 0) {
            return;
        }
        TextureFilter filter = activeConfig != null ? activeConfig.resolveTextureFilter() : TextureFilter.Linear;
        Array<TextureRegion> regions = new Array<>(def.frames.size);
        for (String rel : def.frames) {
            FileHandle fh = themeDir.child(rel);
            if (!fh.exists()) {
                continue;
            }
            Texture tex = new Texture(fh);
            tex.setFilter(filter, filter);
            ownedTextures.add(tex);
            regions.add(new TextureRegion(tex));
        }
        if (regions.size == 0) {
            return;
        }
        Animation.PlayMode mode = def.isLooping() ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL;
        animations.put(key, new Animation<>(def.frameDuration, regions, mode));
        animationDefs.put(key, def);
    }

    public boolean hasTexture(String semanticKey) {
        if (semanticKey == null || semanticKey.isEmpty()) {
            return false;
        }
        return spritePaths.containsKey(semanticKey)
            || dynamicTextures.containsKey(semanticKey)
            || tryLoadImportedOnDemand(semanticKey) != null;
    }

    public boolean hasAnimation(String semanticKey) {
        return semanticKey != null && animations.containsKey(semanticKey);
    }

    public ThemeConfig.AnimationDef getAnimationDef(String semanticKey) {
        ThemeConfig.AnimationDef def = animationDefs.get(semanticKey);
        if (def == null) {
            throw new IllegalArgumentException("Unknown animation def key: " + semanticKey);
        }
        return def;
    }

    public Texture getTexture(String semanticKey) {
        if (semanticKey != null) {
            String path = spritePaths.get(semanticKey);
            if (path != null) {
                return manager.get(path, Texture.class);
            }
            Texture dyn = dynamicTextures.get(semanticKey);
            if (dyn != null) {
                return dyn;
            }
            Texture onDemand = tryLoadImportedOnDemand(semanticKey);
            if (onDemand != null) {
                return onDemand;
            }
        }
        return getOrCreateFallbackTexture();
    }

    private Texture tryLoadImportedOnDemand(String key) {
        File themeRoot = new File(StudioFileHelper.resolveAssetsRoot(), activeThemeDir != null ? activeThemeDir : "themes/vegas/");
        for (String cat : new String[] {"entity", "ui", "background"}) {
            for (String ext : new String[] {".png", ".jpg", ".jpeg"}) {
                File candidate = new File(themeRoot, "imported/" + cat + "/" + key + ext);
                if (candidate.isFile()) {
                    registerImportedTexture(key, Gdx.files.absolute(candidate.getAbsolutePath()));
                    return dynamicTextures.get(key);
                }
            }
        }
        return null;
    }

    private Texture getOrCreateFallbackTexture() {
        if (fallbackTexture == null) {
            Pixmap pm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
            pm.setColor(0.85f, 0.2f, 0.85f, 0.9f);
            pm.fill();
            fallbackTexture = new Texture(pm);
            pm.dispose();
            ownedTextures.add(fallbackTexture);
        }
        return fallbackTexture;
    }

    public Animation<TextureRegion> getAnimation(String semanticKey) {
        Animation<TextureRegion> anim = animations.get(semanticKey);
        if (anim == null) {
            throw new IllegalArgumentException("Unknown animation key: " + semanticKey);
        }
        return anim;
    }

    public Array<String> getAllSpriteKeys() {
        Array<String> keys = new Array<>(spritePaths.size + dynamicTextures.size);
        for (String k : spritePaths.keys()) {
            keys.add(k);
        }
        for (String k : dynamicTextures.keys()) {
            if (!spritePaths.containsKey(k)) {
                keys.add(k);
            }
        }
        keys.sort();
        return keys;
    }

    public Array<String> getAllAnimationKeys() {
        Array<String> keys = new Array<>(animations.size);
        for (String k : animations.keys()) {
            keys.add(k);
        }
        keys.sort();
        return keys;
    }

    public Sound getSound(String semanticKey) {
        Sound dyn = dynamicSounds.get(semanticKey);
        if (dyn != null) {
            return dyn;
        }
        String path = soundPaths.get(semanticKey);
        if (path == null) {
            throw new IllegalArgumentException("Unknown sound key: " + semanticKey);
        }
        return manager.get(path, Sound.class);
    }

    public Music getMusic(String semanticKey) {
        String path = musicPaths.get(semanticKey);
        if (path == null) {
            throw new IllegalArgumentException("Unknown music key: " + semanticKey);
        }
        return manager.get(path, Music.class);
    }

    public ThemeConfig getActiveConfig() {
        return activeConfig;
    }

    private void clearLoadedTheme() {
        for (int i = 0; i < ownedTextures.size; i++) {
            ownedTextures.get(i).dispose();
        }
        ownedTextures.clear();
        for (int i = 0; i < ownedSounds.size; i++) {
            ownedSounds.get(i).dispose();
        }
        ownedSounds.clear();
        dynamicSounds.clear();
        dynamicTextures.clear();
        fallbackTexture = null;
        animations.clear();
        animationDefs.clear();
        spritePaths.clear();
        soundPaths.clear();
        musicPaths.clear();
        manager.clear();
    }

    @Override
    public void dispose() {
        clearLoadedTheme();
        manager.dispose();
    }
}
