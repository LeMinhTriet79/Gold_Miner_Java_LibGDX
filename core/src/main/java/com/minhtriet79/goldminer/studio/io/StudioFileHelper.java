package com.minhtriet79.goldminer.studio.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.assets.ThemeConfig;
import com.minhtriet79.goldminer.gameplay.hook.HookConfig;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Workspace-safe file resolver & persistence helper for Gold Miner Studio.
 * Works identically whether launched from Gradle (workingDir = assets/) or IDE (project root).
 */
public final class StudioFileHelper {

    public static final String IMPORTED_REL_ROOT = "themes/vegas/imported";
    public static final String MANIFEST_REL_PATH = IMPORTED_REL_ROOT + "/manifest.json";

    private static final Json JSON = createPrettyJson();

    private StudioFileHelper() {
    }

    private static Json createPrettyJson() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        return json;
    }

    public static File resolveAssetsRoot() {
        File cwd = new File(".").getAbsoluteFile();
        File direct = new File(cwd, "themes/vegas");
        if (direct.isDirectory()) {
            return cwd;
        }
        File nested = new File(cwd, "assets");
        if (new File(nested, "themes/vegas").isDirectory()) {
            return nested;
        }
        File parentAssets = new File(cwd.getParentFile(), "assets");
        if (new File(parentAssets, "themes/vegas").isDirectory()) {
            return parentAssets;
        }
        return cwd;
    }

    public static FileHandle resolveLevelFile(String fileName) {
        File localFile = new File(resolveAssetsRoot(), "data/levels/" + fileName);
        if (localFile.exists()) {
            return Gdx.files.absolute(localFile.getAbsolutePath());
        }
        return Gdx.files.internal("data/levels/" + fileName);
    }

    public static FileHandle resolveUiFile(String fileName) {
        File localFile = new File(resolveAssetsRoot(), "data/ui/" + fileName);
        if (localFile.exists()) {
            return Gdx.files.absolute(localFile.getAbsolutePath());
        }
        return Gdx.files.internal("data/ui/" + fileName);
    }

    public static FileHandle resolveHookConfigFile() {
        File localFile = new File(resolveAssetsRoot(), "data/hook_config.json");
        if (localFile.exists()) {
            return Gdx.files.absolute(localFile.getAbsolutePath());
        }
        return Gdx.files.internal("data/hook_config.json");
    }

    public static HookConfig loadHookConfig() {
        FileHandle handle = resolveHookConfigFile();
        if (!handle.exists()) {
            return new HookConfig();
        }
        return JSON.fromJson(HookConfig.class, handle);
    }

    public static void saveHookConfig(HookConfig config) {
        File target = new File(resolveAssetsRoot(), "data/hook_config.json");
        target.getParentFile().mkdirs();
        Gdx.files.absolute(target.getAbsolutePath()).writeString(JSON.prettyPrint(config), false, "UTF-8");
    }

    public static String duplicateLevel(String sourceFileName) {
        LevelConfig src = loadLevel(sourceFileName);
        String nextName = nextNewLevelFileName();
        src.level = parseLevelNumber(nextName);
        saveLevel(nextName, src);
        return nextName;
    }

    public static List<String> listLevelFiles() {
        File dir = new File(resolveAssetsRoot(), "data/levels");
        dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        List<String> names = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File f : files) {
                names.add(f.getName());
            }
        }
        if (names.isEmpty()) {
            names.add("level_1.json");
        }
        return names;
    }

    public static List<String> listUiFiles() {
        File dir = new File(resolveAssetsRoot(), "data/ui");
        dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        List<String> names = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File f : files) {
                names.add(f.getName());
            }
        }
        if (names.isEmpty()) {
            names.add("main_menu.json");
        }
        return names;
    }

    public static LevelConfig loadLevel(String fileName) {
        FileHandle handle = resolveLevelFile(fileName);
        if (!handle.exists()) {
            LevelConfig fresh = new LevelConfig();
            fresh.level = parseLevelNumber(fileName);
            return fresh;
        }
        LevelConfig cfg = JSON.fromJson(LevelConfig.class, handle);
        if (cfg.items == null) {
            cfg.items = new Array<>();
        }
        if (cfg.cartRange == null) {
            cfg.cartRange = new LevelConfig.CartRange();
        }
        for (int i = 0; i < cfg.items.size; i++) {
            LevelConfig.ItemSpawn it = cfg.items.get(i);
            if (it.scale <= 0f) {
                it.scale = 1.0f;
            }
        }
        return cfg;
    }

    public static void saveLevel(String fileName, LevelConfig config) {
        File target = new File(resolveAssetsRoot(), "data/levels/" + fileName);
        target.getParentFile().mkdirs();
        Gdx.files.absolute(target.getAbsolutePath()).writeString(JSON.prettyPrint(config), false, "UTF-8");
    }

    public static String nextNewLevelFileName() {
        List<String> existing = listLevelFiles();
        int idx = 1;
        while (existing.contains("level_" + idx + ".json")) {
            idx++;
        }
        return "level_" + idx + ".json";
    }

    public static int parseLevelNumber(String fileName) {
        String digits = fileName.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    public static UiLayoutConfig loadUiLayout(String fileName) {
        FileHandle handle = resolveUiFile(fileName);
        if (!handle.exists()) {
            return new UiLayoutConfig();
        }
        UiLayoutConfig cfg = JSON.fromJson(UiLayoutConfig.class, handle);
        if (cfg.elements == null) {
            cfg.elements = new Array<>();
        }
        return cfg;
    }

    public static void saveUiLayout(String fileName, UiLayoutConfig config) {
        File target = new File(resolveAssetsRoot(), "data/ui/" + fileName);
        target.getParentFile().mkdirs();
        Gdx.files.absolute(target.getAbsolutePath()).writeString(JSON.prettyPrint(config), false, "UTF-8");
    }

    public static ImportedAssetManifest loadImportedManifest() {
        File manifestFile = new File(resolveAssetsRoot(), MANIFEST_REL_PATH);
        if (!manifestFile.exists()) {
            return new ImportedAssetManifest();
        }
        try {
            ImportedAssetManifest manifest = JSON.fromJson(
                ImportedAssetManifest.class,
                Gdx.files.absolute(manifestFile.getAbsolutePath())
            );
            return manifest != null ? manifest : new ImportedAssetManifest();
        } catch (Exception ignored) {
            return new ImportedAssetManifest();
        }
    }

    public static void saveImportedManifest(ImportedAssetManifest manifest) {
        File manifestFile = new File(resolveAssetsRoot(), MANIFEST_REL_PATH);
        manifestFile.getParentFile().mkdirs();
        Gdx.files.absolute(manifestFile.getAbsolutePath()).writeString(JSON.prettyPrint(manifest), false, "UTF-8");
    }

    public static String sanitizeKey(String raw) {
        if (raw == null) {
            return "custom_asset";
        }
        String base = raw.trim().toLowerCase(Locale.ROOT);
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        String cleaned = base.replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "custom_asset" : cleaned;
    }

    /**
     * Imports an external PNG/JPG/MP3/OGG file into {@code assets/themes/vegas/imported/<category>/},
     * updates {@code manifest.json}, and registers the texture immediately in {@link Assets}.
     */
    public static String importAssetFile(
        File sourceFile,
        String semanticKey,
        String category,
        boolean autoCrop,
        Assets assets
    ) throws IOException {
        String key = sanitizeKey(semanticKey);
        String cat = category.toLowerCase(Locale.ROOT);
        File importedRoot = new File(resolveAssetsRoot(), IMPORTED_REL_ROOT);
        File catDir = new File(importedRoot, cat);
        catDir.mkdirs();

        String lowerName = sourceFile.getName().toLowerCase(Locale.ROOT);
        boolean isAudio = "audio".equals(cat) || lowerName.endsWith(".mp3") || lowerName.endsWith(".ogg") || lowerName.endsWith(".wav");

        ImportedAssetManifest manifest = loadImportedManifest();
        if (isAudio) {
            String ext = lowerName.contains(".") ? lowerName.substring(lowerName.lastIndexOf('.')) : ".mp3";
            File dest = new File(catDir, key + ext);
            ImageCropper.copyFile(sourceFile, dest);
            manifest.audio.put(key, "imported/" + cat + "/" + dest.getName());
            saveImportedManifest(manifest);
            if (assets != null) {
                assets.registerImportedSound(key, Gdx.files.absolute(dest.getAbsolutePath()));
            }
            return key;
        }

        File dest = new File(catDir, key + ".png");
        if (autoCrop) {
            ImageCropper.autoCropTransparent(sourceFile, dest);
        } else {
            ImageCropper.copyFile(sourceFile, dest);
        }
        String relPath = "imported/" + cat + "/" + dest.getName();
        manifest.sprites.put(key, relPath);
        saveImportedManifest(manifest);

        if (assets != null) {
            assets.registerImportedTexture(key, Gdx.files.absolute(dest.getAbsolutePath()));
        }
        return key;
    }

    public static void registerExportedAnimation(
        String animKey,
        float worldScale,
        ImageCropper.AnimationExportResult result,
        Assets assets
    ) {
        String key = sanitizeKey(animKey);
        ThemeConfig.AnimationDef def = new ThemeConfig.AnimationDef();
        def.frameDuration = 1f / 18f;
        def.playMode = "LOOP";
        def.drawWidth = Math.round(result.unionWidth * worldScale * 100f) / 100f;
        def.drawHeight = Math.round(result.unionHeight * worldScale * 100f) / 100f;
        def.anchorX = 0.5f;
        def.anchorY = 0.5f;

        for (File outFile : result.outputFiles) {
            def.frames.add("imported/animations/" + key + "/" + outFile.getName());
        }

        ImportedAssetManifest manifest = loadImportedManifest();
        manifest.animations.put(key, def);
        saveImportedManifest(manifest);

        if (assets != null) {
            File themeRoot = new File(resolveAssetsRoot(), "themes/vegas");
            assets.registerImportedAnimation(key, def, Gdx.files.absolute(themeRoot.getAbsolutePath()));
        }
    }
}
