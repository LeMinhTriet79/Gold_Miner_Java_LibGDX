package com.minhtriet79.goldminer.gameplay.ui;

import com.badlogic.gdx.utils.Array;

/**
 * Data-driven UI screen definition loaded from {@code assets/data/ui/*.json}
 * and editable inside Gold Miner Studio's UI / Menu Mode.
 */
public class UiLayoutConfig {

    public String screenId = "main_menu";
    public String backgroundKey = "bg_pass_w1";
    public Array<UiElementDef> elements = new Array<>();

    public UiLayoutConfig() {
    }

    public static class UiElementDef {
        public String id = "element";
        public String type = "BUTTON";
        public String texture = "btn_next_up";
        public String hoverTexture = "btn_next_over";
        public float x = 240f;
        public float y = 135f;
        public float width = 104f;
        public float height = 22f;
        public float scaleX = 1f;
        public float scaleY = 1f;
        public int zIndex = 0;
        public String action = "NONE";
        public String label = "";

        public UiElementDef() {
        }

        public UiElementDef copy() {
            UiElementDef c = new UiElementDef();
            c.id = id;
            c.type = type;
            c.texture = texture;
            c.hoverTexture = hoverTexture;
            c.x = x;
            c.y = y;
            c.width = width;
            c.height = height;
            c.scaleX = scaleX;
            c.scaleY = scaleY;
            c.zIndex = zIndex;
            c.action = action;
            c.label = label;
            return c;
        }
    }
}
