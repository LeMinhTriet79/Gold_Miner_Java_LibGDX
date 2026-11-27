package com.minhtriet79.goldminer.studio.model;

import com.minhtriet79.goldminer.gameplay.world.LevelConfig.ItemSpawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Master catalog of Vegas Australia prefabs & canonical track rail ranges (`track_1..9`).
 * Ensures placing prefabs from the Studio Browser produces balanced gameplay items.
 */
public final class EntityPrefabCatalog {

    public enum Category {
        TREASURE("Kho báu"),
        OBSTACLE("Đá & TNT"),
        CREATURE("Sinh vật"),
        UI_CONTROL("Nút & UI");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public static final class PrefabDef {
        public final String key;
        public final String displayName;
        public final Category category;
        public final int objectN;
        public final int value;
        public final int weight;
        public final float radius;
        public final float drawWidth;
        public final float drawHeight;
        public final float moveSpeed;
        public final boolean animated;

        public PrefabDef(
            String key,
            String displayName,
            Category category,
            int objectN,
            int value,
            int weight,
            float radius,
            float drawWidth,
            float drawHeight,
            float moveSpeed,
            boolean animated
        ) {
            this.key = key;
            this.displayName = displayName;
            this.category = category;
            this.objectN = objectN;
            this.value = value;
            this.weight = weight;
            this.radius = radius;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.moveSpeed = moveSpeed;
            this.animated = animated;
        }
    }

    private static final float[][] TRACK_RANGES = {
        {12f, 468f},   // 1
        {155f, 371f},  // 2
        {229f, 468f},  // 3
        {12f, 292f},   // 4
        {123f, 468f},  // 5
        {189f, 304f},  // 6
        {171f, 468f},  // 7
        {12f, 468f},   // 8
        {243f, 468f}   // 9
    };

    private static final List<PrefabDef> ALL_PREFABS = new ArrayList<>();
    private static final Map<String, PrefabDef> BY_KEY = new HashMap<>();

    static {
        // Treasures
        reg("gold_small", "Vàng Nhỏ", Category.TREASURE, 2, 50, 3, 4.2f, 8.2f, 7.6f, 0f, false);
        reg("gold_small_med", "Vàng Nhỡ", Category.TREASURE, 3, 100, 7, 6.8f, 14.5f, 13.5f, 0f, false);
        reg("gold_medium", "Vàng Vừa", Category.TREASURE, 4, 250, 8, 10.8f, 25.0f, 20.0f, 0f, false);
        reg("gold_large", "Vàng Lớn", Category.TREASURE, 5, 500, 9, 16.0f, 40.0f, 27.0f, 0f, false);
        reg("gold_mega", "Vàng Khổng Lồ", Category.TREASURE, 6, 1000, 11, 19.4f, 42.0f, 36.0f, 0f, false);
        reg("gold_pan", "Khay Đãi Vàng", Category.TREASURE, 7, 25, 0, 5.8f, 15.0f, 8.2f, 0f, false);
        reg("diamond", "Kim Cương", Category.TREASURE, 13, 600, 2, 4.8f, 9.5f, 9.0f, 0f, false);
        reg("mystery_bag", "Túi Bí Ẩn", Category.TREASURE, 11, 0, 7, 9.5f, 20.0f, 19.0f, 0f, false);
        reg("gem_1", "Đá Quý 1", Category.TREASURE, 23, 100, 3, 5.0f, 8.8f, 10.5f, 0f, false);
        reg("gem_2", "Đá Quý 2", Category.TREASURE, 24, 100, 3, 5.0f, 9.2f, 10.0f, 0f, false);
        reg("gem_3", "Đá Quý 3", Category.TREASURE, 25, 100, 3, 5.0f, 9.2f, 11.0f, 0f, false);
        reg("gem_4", "Đá Quý 4", Category.TREASURE, 26, 100, 3, 5.0f, 8.8f, 10.5f, 0f, false);

        // Obstacles & Explosives
        reg("tnt_barrel", "Thùng Thuốc Nổ", Category.OBSTACLE, 12, 1, 2, 12.2f, 24.5f, 25.5f, 0f, false);
        reg("rock_small", "Đá Nhỏ", Category.OBSTACLE, 10, 5, 4, 4.8f, 10.5f, 9.5f, 0f, false);
        reg("rock_medium", "Đá Vừa", Category.OBSTACLE, 9, 11, 8, 9.0f, 18.5f, 19.0f, 0f, false);
        reg("rock_large", "Đá Lớn", Category.OBSTACLE, 8, 20, 9, 14.5f, 39.0f, 21.0f, 0f, false);
        reg("skull", "Đầu Lâu", Category.OBSTACLE, 14, 20, 2, 5.0f, 11.0f, 9.2f, 0f, false);
        reg("bone_small", "Xương Nhỏ", Category.OBSTACLE, 15, 5, 2, 5.8f, 14.0f, 11.2f, 0f, false);
        reg("bone_long_v", "Xương Dọc", Category.OBSTACLE, 16, 10, 5, 8.2f, 8.8f, 25.5f, 0f, false);
        reg("bone_long_h", "Xương Ngang", Category.OBSTACLE, 17, 10, 5, 8.2f, 25.5f, 8.8f, 0f, false);
        reg("skeleton", "Bộ Xương", Category.OBSTACLE, 18, 20, 7, 7.6f, 17.5f, 14.2f, 0f, false);

        // Animated Creatures
        reg("bilby_walk", "Chuột Bilby", Category.CREATURE, 21, 10, 3, 10.5f, 23.0f, 24.2f, 42.1f, true);
        reg("bilby_diamond_walk", "Bilby Kim Cương", Category.CREATURE, 22, 610, 5, 11.5f, 24.0f, 24.8f, 40.0f, true);
        reg("tasmanian_walk", "Quỷ Tasmania", Category.CREATURE, 27, 30, 7, 12.0f, 30.2f, 19.0f, 16.2f, true);
    }

    private EntityPrefabCatalog() {
    }

    private static void reg(
        String key, String displayName, Category category, int objectN, int value, int weight,
        float radius, float drawWidth, float drawHeight, float moveSpeed, boolean animated
    ) {
        PrefabDef def = new PrefabDef(key, displayName, category, objectN, value, weight, radius, drawWidth, drawHeight, moveSpeed, animated);
        ALL_PREFABS.add(def);
        BY_KEY.put(key, def);
    }

    public static List<PrefabDef> getAll() {
        return Collections.unmodifiableList(ALL_PREFABS);
    }

    public static List<PrefabDef> getByCategory(Category category) {
        if (category == null) {
            return getAll();
        }
        List<PrefabDef> filtered = new ArrayList<>();
        for (PrefabDef def : ALL_PREFABS) {
            if (def.category == category) {
                filtered.add(def);
            }
        }
        return filtered;
    }

    public static PrefabDef findByKey(String key) {
        return key == null ? null : BY_KEY.get(key);
    }

    public static float[] getTrackRange(int trackN) {
        int idx = Math.max(0, Math.min(TRACK_RANGES.length - 1, trackN - 1));
        return new float[] {TRACK_RANGES[idx][0], TRACK_RANGES[idx][1]};
    }

    public static ItemSpawn createLevelSpawn(PrefabDef def, float x, float y, int zIndex) {
        ItemSpawn spawn = new ItemSpawn();
        spawn.objectN = def.objectN;
        spawn.type = def.key;
        spawn.assetKey = def.key;
        spawn.x = x;
        spawn.y = y;
        spawn.value = def.value;
        spawn.weight = def.weight;
        spawn.radius = def.radius;
        spawn.drawWidth = def.drawWidth;
        spawn.drawHeight = def.drawHeight;
        spawn.scale = 1.0f;
        spawn.moveSpeed = def.moveSpeed;
        spawn.zIndex = zIndex;
        spawn.animated = def.animated;
        return spawn;
    }

    public static ItemSpawn createCustomLevelSpawn(
        String key,
        float drawW,
        float drawH,
        boolean animated,
        float x,
        float y,
        int zIndex
    ) {
        PrefabDef preset = findByKey(key);
        if (preset != null) {
            return createLevelSpawn(preset, x, y, zIndex);
        }
        ItemSpawn spawn = new ItemSpawn();
        spawn.objectN = 0;
        spawn.type = key;
        spawn.assetKey = key;
        spawn.x = x;
        spawn.y = y;
        spawn.value = 100;
        spawn.weight = 4;
        spawn.drawWidth = Math.max(10f, drawW);
        spawn.drawHeight = Math.max(10f, drawH);
        spawn.radius = Math.max(4.5f, (spawn.drawWidth + spawn.drawHeight) * 0.22f);
        spawn.scale = 1.0f;
        spawn.moveSpeed = 0f;
        spawn.zIndex = zIndex;
        spawn.animated = animated;
        return spawn;
    }
}
