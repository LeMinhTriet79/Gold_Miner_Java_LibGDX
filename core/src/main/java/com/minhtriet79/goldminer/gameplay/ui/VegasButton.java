package com.minhtriet79.goldminer.gameplay.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.minhtriet79.goldminer.assets.Assets;

/**
 * Lightweight zero-allocation Vegas button supporting Up / Over / Down sprite states.
 */
public class VegasButton {

    public final Rectangle bounds = new Rectangle();
    private String upKey;
    private String overKey;
    private String downKey;
    public boolean visible = true;

    public VegasButton(float x, float y, float width, float height, String upKey, String overKey, String downKey) {
        this.bounds.set(x, y, width, height);
        this.upKey = upKey;
        this.overKey = overKey;
        this.downKey = downKey;
    }

    public void setKeys(String upKey, String overKey, String downKey) {
        this.upKey = upKey;
        this.overKey = overKey;
        this.downKey = downKey;
    }

    public boolean contains(float worldX, float worldY) {
        return visible && bounds.contains(worldX, worldY);
    }

    public void draw(SpriteBatch batch, Assets assets, Viewport viewport, Vector3 scratchVec) {
        if (!visible || upKey == null) {
            return;
        }
        scratchVec.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(scratchVec);
        boolean hovered = bounds.contains(scratchVec.x, scratchVec.y);
        boolean pressed = hovered && Gdx.input.isTouched();

        String activeKey = pressed ? downKey : (hovered ? overKey : upKey);
        Texture tex = assets.getTexture(activeKey);
        batch.draw(tex, bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
