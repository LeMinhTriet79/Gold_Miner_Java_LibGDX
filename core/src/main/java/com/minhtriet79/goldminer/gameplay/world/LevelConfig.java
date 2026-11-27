package com.minhtriet79.goldminer.gameplay.world;

import com.badlogic.gdx.utils.Array;

/**
 * Data-driven level definition deserialized from {@code assets/data/levels/*.json}.
 */
public class LevelConfig {

    public int level = 1;
    public int world = 1;
    public String themeHint = "australia";
    public int trackN = 1;
    public int durationSeconds = 60;
    public int goal = 555;
    public int levelValue = 1455;
    public CartRange cartRange = new CartRange();
    public Array<ItemSpawn> items = new Array<>();

    public LevelConfig() {
    }

    public static class CartRange {
        public float minX = 12f;
        public float maxX = 468f;

        public CartRange() {
        }
    }

    public static class ItemSpawn {
        public int objectN;
        public String type;
        public String assetKey;
        public float x;
        public float y;
        public int value;
        public int weight;
        public float radius;
        public float drawWidth;
        public float drawHeight;
        public float scale = 1.0f;
        public float moveSpeed;
        public int zIndex = 0;
        public boolean animated;

        public ItemSpawn() {
        }

        public ItemSpawn copy() {
            ItemSpawn c = new ItemSpawn();
            c.objectN = objectN;
            c.type = type;
            c.assetKey = assetKey;
            c.x = x;
            c.y = y;
            c.value = value;
            c.weight = weight;
            c.radius = radius;
            c.drawWidth = drawWidth;
            c.drawHeight = drawHeight;
            c.scale = scale > 0f ? scale : 1.0f;
            c.moveSpeed = moveSpeed;
            c.zIndex = zIndex;
            c.animated = animated;
            return c;
        }
    }
}
