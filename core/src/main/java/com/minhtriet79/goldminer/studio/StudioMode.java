package com.minhtriet79.goldminer.studio;

/**
 * Primary workspaces inside Gold Miner Engine & Studio.
 */
public enum StudioMode {
    LEVEL_MODE("Thiết kế Màn"),
    UI_MODE("Thiết kế UI"),
    SPRITE_PREVIEWER("Xem & Cắt Anim"),
    PHYSICS_MODE("Cân Bằng Vật Lý");

    public final String title;

    StudioMode(String title) {
        this.title = title;
    }
}
