package com.minhtriet79.goldminer.studio.io;

import com.badlogic.gdx.utils.ObjectMap;
import com.minhtriet79.goldminer.assets.ThemeConfig;

/**
 * Manifest stored at {@code assets/themes/vegas/imported/manifest.json}
 * tracking user-imported sprites, animations, and audio inside Gold Miner Studio.
 */
public class ImportedAssetManifest {
    public ObjectMap<String, String> sprites = new ObjectMap<>();
    public ObjectMap<String, ThemeConfig.AnimationDef> animations = new ObjectMap<>();
    public ObjectMap<String, String> audio = new ObjectMap<>();

    public ImportedAssetManifest() {
    }
}
