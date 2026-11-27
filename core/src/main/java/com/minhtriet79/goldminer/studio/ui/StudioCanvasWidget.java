package com.minhtriet79.goldminer.studio.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.minhtriet79.goldminer.assets.AssetKey;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.assets.ThemeConfig;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig.UiElementDef;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig.ItemSpawn;
import com.minhtriet79.goldminer.studio.StudioMode;
import com.minhtriet79.goldminer.studio.model.CanvasCamera;
import com.minhtriet79.goldminer.studio.model.StudioMath;

import java.util.Comparator;

/**
 * Precision 480x270 (16:9) Interactive Engine Viewport powered by {@link CanvasCamera}.
 * Fixes local-vs-parent coordinate mismatch completely, supports mouse-wheel zoom around cursor,
 * right/middle-drag pan, intuitive corner-handle drag scaling, ghost placement preview,
 * cart min/max rail guides, 5u grid, hitbox & 40.5u TNT blast circles.
 */
public class StudioCanvasWidget extends Widget implements Disposable {

    public interface CanvasHost {
        void onBeforeMutate();
        void onSelectionChanged();
        void onLevelMutated();
        void onUiMutated();
        void onPlaceArmedAsset(float worldX, float worldY);
        void onCancelArmedAsset();
        void onCursorMoved(float worldX, float worldY, float zoom);
    }

    private static final float TNT_BLAST_RADIUS = 40.5f;

    private final Assets assets;
    private final BitmapFont hudFont;
    private final Texture whitePixel;
    private final CanvasHost host;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final CanvasCamera canvasCamera = new CanvasCamera();

    private final Rectangle widgetClipBounds = new Rectangle();
    private final Rectangle scissorRect = new Rectangle();
    private final float[] tmpWorld = new float[2];
    private final float[] tmpOrigin = new float[2];
    private final Array<Integer> renderOrder = new Array<>(64);

    private StudioMode mode = StudioMode.LEVEL_MODE;
    private LevelConfig levelConfig;
    private UiLayoutConfig uiConfig;
    private int selectedIndex = -1;
    private boolean showGrid = true;
    private boolean showHitbox = true;
    private String armedAssetKey;
    private float animPreviewTime;

    private boolean cursorValid;
    private float cursorWorldX;
    private float cursorWorldY;
    private int hoveredIndex = -1;
    private boolean hoveredCorner = false;

    private boolean draggingItem = false;
    private boolean scalingHandle = false;
    private boolean panningCamera = false;
    private boolean dragUndoPushed = false;
    private float lastPanX;
    private float lastPanY;
    private float dragOffsetX;
    private float dragOffsetY;
    private float scaleStartDist;
    private float initialScaleA;
    private float initialScaleB;

    public StudioCanvasWidget(Assets assets, BitmapFont hudFont, Texture whitePixel, CanvasHost host) {
        this.assets = assets;
        this.hudFont = hudFont;
        this.whitePixel = whitePixel;
        this.host = host;
        setupInput();
    }

    private void setupInput() {
        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1 && getStage() != null) {
                    getStage().setScrollFocus(StudioCanvasWidget.this);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1) {
                    cursorValid = false;
                    hoveredIndex = -1;
                    hoveredCorner = false;
                }
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                canvasCamera.setViewportSize(getWidth(), getHeight());
                canvasCamera.localToWorld(x, y, tmpWorld);
                cursorWorldX = tmpWorld[0];
                cursorWorldY = tmpWorld[1];
                cursorValid = cursorWorldX >= 0f && cursorWorldX <= WorldBounds.WORLD_WIDTH
                    && cursorWorldY >= 0f && cursorWorldY <= WorldBounds.WORLD_HEIGHT;
                if (armedAssetKey == null && cursorValid) {
                    hoveredCorner = selectedIndex >= 0 && isOnSelectionCorner(cursorWorldX, cursorWorldY);
                    hoveredIndex = hoveredCorner ? selectedIndex : pickTopmostAt(cursorWorldX, cursorWorldY);
                } else {
                    hoveredIndex = -1;
                    hoveredCorner = false;
                }
                host.onCursorMoved(cursorWorldX, cursorWorldY, canvasCamera.getZoom());
                return false;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                canvasCamera.setViewportSize(getWidth(), getHeight());
                boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);

                if (ctrl && selectedIndex >= 0) {
                    scaleSelectedByDelta(-amountY * 0.08f);
                    return true;
                }

                float zoomFactor = amountY < 0f ? 1.12f : (1f / 1.12f);
                canvasCamera.zoomAtLocalPoint(x, y, zoomFactor);
                canvasCamera.localToWorld(x, y, tmpWorld);
                host.onCursorMoved(tmpWorld[0], tmpWorld[1], canvasCamera.getZoom());
                return true;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                canvasCamera.setViewportSize(getWidth(), getHeight());
                if (getStage() != null) {
                    getStage().unfocusAll();
                    getStage().setScrollFocus(StudioCanvasWidget.this);
                }

                if (button == Input.Buttons.RIGHT && armedAssetKey != null) {
                    host.onCancelArmedAsset();
                    return true;
                }
                if (button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) {
                    panningCamera = true;
                    lastPanX = x;
                    lastPanY = y;
                    return true;
                }

                if (button != Input.Buttons.LEFT) {
                    return false;
                }

                canvasCamera.localToWorld(x, y, tmpWorld);
                float wx = tmpWorld[0];
                float wy = tmpWorld[1];

                if (armedAssetKey != null) {
                    if (wx >= 0f && wx <= WorldBounds.WORLD_WIDTH && wy >= 0f && wy <= WorldBounds.WORLD_HEIGHT) {
                        float sx = StudioMath.snapToGrid(wx, StudioMath.GRID_STEP, showGrid);
                        float sy = StudioMath.snapToGrid(wy, StudioMath.GRID_STEP, showGrid);
                        host.onPlaceArmedAsset(sx, sy);
                    }
                    return true;
                }

                if (selectedIndex >= 0 && isOnSelectionCorner(wx, wy)) {
                    scalingHandle = true;
                    draggingItem = false;
                    dragUndoPushed = false;
                    float[] box = getSelectedCenter();
                    scaleStartDist = Math.max(4f, Vector2.dst(box[0], box[1], wx, wy));
                    captureInitialScale();
                    return true;
                }

                int hit = pickTopmostAt(wx, wy);
                selectedIndex = hit;
                host.onSelectionChanged();

                if (hit >= 0) {
                    float[] box = getSelectedCenter();
                    dragOffsetX = box[0] - wx;
                    dragOffsetY = box[1] - wy;
                    draggingItem = true;
                    scalingHandle = false;
                    dragUndoPushed = false;
                }
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                canvasCamera.setViewportSize(getWidth(), getHeight());
                if (panningCamera) {
                    canvasCamera.panByLocalDelta(x - lastPanX, y - lastPanY);
                    lastPanX = x;
                    lastPanY = y;
                    return;
                }

                if ((!draggingItem && !scalingHandle) || selectedIndex < 0) {
                    return;
                }

                canvasCamera.localToWorld(x, y, tmpWorld);
                float wx = tmpWorld[0];
                float wy = tmpWorld[1];
                cursorWorldX = wx;
                cursorWorldY = wy;
                host.onCursorMoved(wx, wy, canvasCamera.getZoom());

                if (!dragUndoPushed) {
                    host.onBeforeMutate();
                    dragUndoPushed = true;
                }

                if (scalingHandle) {
                    float[] box = getSelectedCenter();
                    float dist = Math.max(4f, Vector2.dst(box[0], box[1], wx, wy));
                    float ratio = dist / scaleStartDist;
                    if (mode == StudioMode.LEVEL_MODE && levelConfig != null && selectedIndex < levelConfig.items.size) {
                        ItemSpawn item = levelConfig.items.get(selectedIndex);
                        item.scale = Math.max(0.25f, Math.min(4.0f, Math.round(initialScaleA * ratio * 100f) / 100f));
                        host.onLevelMutated();
                    } else if (mode == StudioMode.UI_MODE && uiConfig != null && selectedIndex < uiConfig.elements.size) {
                        UiElementDef el = uiConfig.elements.get(selectedIndex);
                        el.scaleX = Math.max(0.2f, Math.min(4.0f, Math.round(initialScaleA * ratio * 100f) / 100f));
                        el.scaleY = Math.max(0.2f, Math.min(4.0f, Math.round(initialScaleB * ratio * 100f) / 100f));
                        host.onUiMutated();
                    }
                    host.onSelectionChanged();
                    return;
                }

                float targetX = StudioMath.snapToGrid(wx + dragOffsetX, StudioMath.GRID_STEP, showGrid);
                float targetY = StudioMath.snapToGrid(wy + dragOffsetY, StudioMath.GRID_STEP, showGrid);
                if (mode == StudioMode.LEVEL_MODE && levelConfig != null && selectedIndex < levelConfig.items.size) {
                    ItemSpawn item = levelConfig.items.get(selectedIndex);
                    item.x = Math.round(targetX * 10f) / 10f;
                    item.y = Math.round(targetY * 10f) / 10f;
                    host.onLevelMutated();
                } else if (mode == StudioMode.UI_MODE && uiConfig != null && selectedIndex < uiConfig.elements.size) {
                    UiElementDef el = uiConfig.elements.get(selectedIndex);
                    el.x = Math.round(targetX * 10f) / 10f;
                    el.y = Math.round(targetY * 10f) / 10f;
                    host.onUiMutated();
                }
                host.onSelectionChanged();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                draggingItem = false;
                scalingHandle = false;
                panningCamera = false;
                dragUndoPushed = false;
            }
        });
    }

    private void scaleSelectedByDelta(float delta) {
        host.onBeforeMutate();
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null && selectedIndex < levelConfig.items.size) {
            ItemSpawn item = levelConfig.items.get(selectedIndex);
            float next = Math.max(0.25f, Math.min(4.0f, (item.scale > 0f ? item.scale : 1f) + delta));
            item.scale = Math.round(next * 100f) / 100f;
            host.onLevelMutated();
            host.onSelectionChanged();
        } else if (mode == StudioMode.UI_MODE && uiConfig != null && selectedIndex < uiConfig.elements.size) {
            UiElementDef el = uiConfig.elements.get(selectedIndex);
            el.scaleX = Math.max(0.2f, Math.min(4.0f, Math.round((el.scaleX + delta) * 100f) / 100f));
            el.scaleY = Math.max(0.2f, Math.min(4.0f, Math.round((el.scaleY + delta) * 100f) / 100f));
            host.onUiMutated();
            host.onSelectionChanged();
        }
    }

    @Override
    public void layout() {
        super.layout();
        canvasCamera.setViewportSize(getWidth(), getHeight());
    }

    public boolean localToWorld(float localX, float localY, Vector2 outWorld) {
        canvasCamera.setViewportSize(getWidth(), getHeight());
        canvasCamera.localToWorld(localX, localY, tmpWorld);
        outWorld.set(tmpWorld[0], tmpWorld[1]);
        return outWorld.x >= 0f && outWorld.x <= WorldBounds.WORLD_WIDTH
            && outWorld.y >= 0f && outWorld.y <= WorldBounds.WORLD_HEIGHT;
    }

    public void resetView() {
        canvasCamera.setViewportSize(getWidth(), getHeight());
        canvasCamera.resetFit();
    }

    public float getZoom() {
        return canvasCamera.getZoom();
    }

    public void setData(StudioMode mode, LevelConfig levelConfig, UiLayoutConfig uiConfig, int selectedIndex) {
        this.mode = mode;
        this.levelConfig = levelConfig;
        this.uiConfig = uiConfig;
        this.selectedIndex = selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowHitbox(boolean showHitbox) {
        this.showHitbox = showHitbox;
    }

    public boolean isShowHitbox() {
        return showHitbox;
    }

    public void setArmedAssetKey(String armedAssetKey) {
        this.armedAssetKey = armedAssetKey;
    }

    public String getArmedAssetKey() {
        return armedAssetKey;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        animPreviewTime += delta;
    }

    private int pickTopmostAt(float wx, float wy) {
        rebuildRenderOrder();
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null) {
            for (int r = renderOrder.size - 1; r >= 0; r--) {
                int idx = renderOrder.get(r);
                ItemSpawn it = levelConfig.items.get(idx);
                float sc = it.scale > 0f ? it.scale : 1f;
                float hw = Math.max(it.radius * sc, it.drawWidth * sc * 0.5f);
                float hh = Math.max(it.radius * sc, it.drawHeight * sc * 0.5f);
                if (wx >= it.x - hw && wx <= it.x + hw && wy >= it.y - hh && wy <= it.y + hh) {
                    return idx;
                }
            }
        } else if (mode == StudioMode.UI_MODE && uiConfig != null) {
            for (int r = renderOrder.size - 1; r >= 0; r--) {
                int idx = renderOrder.get(r);
                UiElementDef el = uiConfig.elements.get(idx);
                float hw = el.width * el.scaleX * 0.5f;
                float hh = el.height * el.scaleY * 0.5f;
                if (wx >= el.x - hw && wx <= el.x + hw && wy >= el.y - hh && wy <= el.y + hh) {
                    return idx;
                }
            }
        }
        return -1;
    }

    private float[] getSelectedCenter() {
        return getCenterForIndex(selectedIndex);
    }

    private float[] getCenterForIndex(int idx) {
        if (idx < 0) {
            return new float[] {0f, 0f, 10f, 10f};
        }
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null && idx < levelConfig.items.size) {
            ItemSpawn it = levelConfig.items.get(idx);
            float sc = it.scale > 0f ? it.scale : 1f;
            return new float[] {it.x, it.y, it.drawWidth * sc, it.drawHeight * sc};
        }
        if (mode == StudioMode.UI_MODE && uiConfig != null && idx < uiConfig.elements.size) {
            UiElementDef el = uiConfig.elements.get(idx);
            return new float[] {el.x, el.y, el.width * el.scaleX, el.height * el.scaleY};
        }
        return new float[] {0f, 0f, 10f, 10f};
    }

    private void captureInitialScale() {
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null && selectedIndex < levelConfig.items.size) {
            ItemSpawn it = levelConfig.items.get(selectedIndex);
            initialScaleA = it.scale > 0f ? it.scale : 1f;
            initialScaleB = initialScaleA;
        } else if (mode == StudioMode.UI_MODE && uiConfig != null && selectedIndex < uiConfig.elements.size) {
            UiElementDef el = uiConfig.elements.get(selectedIndex);
            initialScaleA = el.scaleX;
            initialScaleB = el.scaleY;
        }
    }

    private boolean isOnSelectionCorner(float wx, float wy) {
        float[] box = getSelectedCenter();
        float hw = box[2] * 0.5f;
        float hh = box[3] * 0.5f;
        float tolWorld = Math.max(4f, 6f / canvasCamera.getPixelsPerWorldUnit());
        float[][] corners = {
            {box[0] - hw, box[1] - hh},
            {box[0] + hw, box[1] - hh},
            {box[0] - hw, box[1] + hh},
            {box[0] + hw, box[1] + hh}
        };
        for (float[] c : corners) {
            if (Math.abs(wx - c[0]) <= tolWorld && Math.abs(wy - c[1]) <= tolWorld) {
                return true;
            }
        }
        return false;
    }

    private void rebuildRenderOrder() {
        renderOrder.clear();
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null) {
            for (int i = 0; i < levelConfig.items.size; i++) {
                renderOrder.add(i);
            }
            renderOrder.sort(Comparator.comparingInt(idx -> levelConfig.items.get(idx).zIndex));
        } else if (mode == StudioMode.UI_MODE && uiConfig != null) {
            for (int i = 0; i < uiConfig.elements.size; i++) {
                renderOrder.add(i);
            }
            renderOrder.sort(Comparator.comparingInt(idx -> uiConfig.elements.get(idx).zIndex));
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (getWidth() <= 4f || getHeight() <= 4f || getStage() == null) {
            return;
        }
        canvasCamera.setViewportSize(getWidth(), getHeight());
        canvasCamera.getWorldOriginInLocal(tmpOrigin);

        float stageOriginX = getX() + tmpOrigin[0];
        float stageOriginY = getY() + tmpOrigin[1];
        float pxPerUnit = canvasCamera.getPixelsPerWorldUnit();
        float stageW = WorldBounds.WORLD_WIDTH * pxPerUnit;
        float stageH = WorldBounds.WORLD_HEIGHT * pxPerUnit;

        batch.flush();
        widgetClipBounds.set(getX(), getY(), getWidth(), getHeight());
        ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), widgetClipBounds, scissorRect);
        boolean scissorActive = ScissorStack.pushScissors(scissorRect);

        // Editor surround background
        batch.setColor(0.08f, 0.09f, 0.11f, 1f);
        batch.draw(whitePixel, getX(), getY(), getWidth(), getHeight());
        batch.setColor(Color.WHITE);

        if (mode == StudioMode.LEVEL_MODE && levelConfig != null) {
            drawLevelStage(batch, stageOriginX, stageOriginY, pxPerUnit);
        } else if (mode == StudioMode.UI_MODE && uiConfig != null) {
            drawUiStage(batch, stageOriginX, stageOriginY, pxPerUnit);
        }

        // Ghost placement preview under cursor
        if (armedAssetKey != null && cursorValid) {
            float sx = StudioMath.snapToGrid(cursorWorldX, StudioMath.GRID_STEP, showGrid);
            float sy = StudioMath.snapToGrid(cursorWorldY, StudioMath.GRID_STEP, showGrid);
            batch.setColor(0.6f, 1f, 1f, 0.55f);
            Texture tex = assets.hasAnimation(armedAssetKey)
                ? assets.getAnimation(armedAssetKey).getKeyFrame(0f).getTexture()
                : assets.getTexture(armedAssetKey);
            float gw = 22f * pxPerUnit;
            float gh = 22f * pxPerUnit;
            batch.draw(tex, stageOriginX + sx * pxPerUnit - gw * 0.5f, stageOriginY + sy * pxPerUnit - gh * 0.5f, gw, gh);
            batch.setColor(Color.WHITE);
        }

        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
        drawShapeOverlays(stageOriginX, stageOriginY, stageW, stageH, pxPerUnit);

        if (scissorActive) {
            ScissorStack.popScissors();
        }
        batch.begin();
    }

    private void drawLevelStage(Batch batch, float sx, float sy, float k) {
        Texture mine = assets.getTexture(AssetKey.BG_MINE_W1);
        Texture cave = assets.getTexture(AssetKey.BG_CAVE_W1);
        Texture track = assets.getTexture(AssetKey.trackKey(levelConfig.trackN));
        Texture sky = assets.getTexture(AssetKey.BG_SKY_W1);
        Texture cart = assets.getTexture(AssetKey.CART_STANDARD);

        batch.draw(mine, sx, sy, WorldBounds.WORLD_WIDTH * k, WorldBounds.MINE_TOP_Y * k);
        batch.draw(cave, sx, sy + WorldBounds.MINE_TOP_Y * k, WorldBounds.WORLD_WIDTH * k,
            (WorldBounds.HUD_BOTTOM_Y - WorldBounds.MINE_TOP_Y) * k);
        batch.draw(track, sx, sy + (WorldBounds.TRACK_Y - 1.5f) * k, WorldBounds.WORLD_WIDTH * k, 32f * k);
        batch.draw(sky, sx, sy + WorldBounds.HUD_BOTTOM_Y * k, WorldBounds.WORLD_WIDTH * k,
            WorldBounds.HUD_TOP_HEIGHT * k);

        Animation<TextureRegion> minerAnim = assets.getAnimation(AssetKey.MINER_IDLE);
        ThemeConfig.AnimationDef minerDef = assets.getAnimationDef(AssetKey.MINER_IDLE);
        TextureRegion minerFrame = minerAnim.getKeyFrame(0f);
        float cartX = WorldBounds.WORLD_WIDTH * 0.5f;
        batch.draw(minerFrame,
            sx + (cartX - minerDef.drawWidth * minerDef.anchorX) * k,
            sy + (WorldBounds.TRACK_Y + 11f - minerDef.drawHeight * minerDef.anchorY) * k,
            minerDef.drawWidth * k, minerDef.drawHeight * k);
        batch.draw(cart, sx + (cartX - 21f) * k, sy + (WorldBounds.TRACK_Y + 2f) * k, 42f * k, 25.5f * k);

        rebuildRenderOrder();
        for (int r = 0; r < renderOrder.size; r++) {
            ItemSpawn item = levelConfig.items.get(renderOrder.get(r));
            float sc = item.scale > 0f ? item.scale : 1f;
            float dw = item.drawWidth * sc * k;
            float dh = item.drawHeight * sc * k;
            float left = sx + item.x * k - dw * 0.5f;
            float bottom = sy + item.y * k - dh * 0.5f;
            if (item.animated && assets.hasAnimation(item.assetKey)) {
                TextureRegion frame = assets.getAnimation(item.assetKey).getKeyFrame(animPreviewTime, true);
                batch.draw(frame, left, bottom, dw, dh);
            } else {
                Texture tex = assets.getTexture(item.assetKey);
                batch.draw(tex, left, bottom, dw, dh);
            }
        }
    }

    private void drawUiStage(Batch batch, float sx, float sy, float k) {
        Texture bg = assets.getTexture(uiConfig.backgroundKey);
        batch.draw(bg, sx, sy, WorldBounds.WORLD_WIDTH * k, WorldBounds.WORLD_HEIGHT * k);

        float prevScaleX = hudFont.getData().scaleX;
        float prevScaleY = hudFont.getData().scaleY;
        float labelScale = Math.max(0.35f, k * 0.45f);

        rebuildRenderOrder();
        for (int r = 0; r < renderOrder.size; r++) {
            UiElementDef el = uiConfig.elements.get(renderOrder.get(r));
            float dw = el.width * el.scaleX * k;
            float dh = el.height * el.scaleY * k;
            float left = sx + el.x * k - dw * 0.5f;
            float bottom = sy + el.y * k - dh * 0.5f;

            if (assets.hasAnimation(el.texture)) {
                TextureRegion frame = assets.getAnimation(el.texture).getKeyFrame(animPreviewTime, true);
                batch.draw(frame, left, bottom, dw, dh);
            } else {
                Texture tex = assets.getTexture(el.texture);
                batch.draw(tex, left, bottom, dw, dh);
            }

            if (el.label != null && !el.label.isEmpty()) {
                hudFont.getData().setScale(labelScale);
                hudFont.setColor(Color.WHITE);
                hudFont.draw(batch, el.label, left, sy + el.y * k + 5f * k * 0.5f, dw, Align.center, false);
            }
        }
        hudFont.getData().setScale(prevScaleX, prevScaleY);
    }

    private void drawShapeOverlays(float sx, float sy, float sw, float sh, float k) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        if (showGrid) {
            for (int gx = 0; gx <= 480; gx += 5) {
                boolean major = gx % 25 == 0;
                shapeRenderer.setColor(1f, 1f, 1f, major ? 0.22f : 0.07f);
                float px = sx + gx * k;
                shapeRenderer.line(px, sy, px, sy + sh);
            }
            for (int gy = 0; gy <= 270; gy += 5) {
                boolean major = gy % 25 == 0;
                shapeRenderer.setColor(1f, 1f, 1f, major ? 0.22f : 0.07f);
                float py = sy + gy * k;
                shapeRenderer.line(sx, py, sx + sw, py);
            }
        }

        if (mode == StudioMode.LEVEL_MODE && levelConfig != null) {
            // Underground ceiling guide (y = 192) & Cart Rail bounds
            shapeRenderer.setColor(0.25f, 0.85f, 1f, 0.45f);
            shapeRenderer.line(sx, sy + WorldBounds.MINE_TOP_Y * k, sx + sw, sy + WorldBounds.MINE_TOP_Y * k);
            if (levelConfig.cartRange != null) {
                float minX = sx + levelConfig.cartRange.minX * k;
                float maxX = sx + levelConfig.cartRange.maxX * k;
                shapeRenderer.line(minX, sy + WorldBounds.MINE_TOP_Y * k, minX, sy + WorldBounds.HUD_BOTTOM_Y * k);
                shapeRenderer.line(maxX, sy + WorldBounds.MINE_TOP_Y * k, maxX, sy + WorldBounds.HUD_BOTTOM_Y * k);
            }

            if (showHitbox) {
                for (int i = 0; i < levelConfig.items.size; i++) {
                    ItemSpawn item = levelConfig.items.get(i);
                    float sc = item.scale > 0f ? item.scale : 1f;
                    float cx = sx + item.x * k;
                    float cy = sy + item.y * k;
                    shapeRenderer.setColor(0.2f, 1f, 0.35f, 0.78f);
                    shapeRenderer.circle(cx, cy, item.radius * sc * k, 24);

                    if (item.objectN == 12 || "tnt_barrel".equals(item.type)) {
                        shapeRenderer.setColor(1f, 0.48f, 0.1f, 0.88f);
                        shapeRenderer.circle(cx, cy, TNT_BLAST_RADIUS * k, 36);
                    }
                }
            }
        }

        if (hoveredIndex >= 0 && hoveredIndex != selectedIndex) {
            float[] box = getCenterForIndex(hoveredIndex);
            float left = sx + (box[0] - box[2] * 0.5f) * k;
            float bottom = sy + (box[1] - box[3] * 0.5f) * k;
            shapeRenderer.setColor(0.3f, 0.95f, 1f, 0.72f);
            shapeRenderer.rect(left, bottom, box[2] * k, box[3] * k);
        }

        if (selectedIndex >= 0) {
            float[] box = getSelectedCenter();
            float left = sx + (box[0] - box[2] * 0.5f) * k;
            float bottom = sy + (box[1] - box[3] * 0.5f) * k;
            float bw = box[2] * k;
            float bh = box[3] * k;
            shapeRenderer.setColor(Color.GOLD);
            shapeRenderer.rect(left, bottom, bw, bh);

            float hs = hoveredCorner ? 6.5f : 5f;
            shapeRenderer.setColor(hoveredCorner ? Color.CYAN : Color.GOLD);
            shapeRenderer.rect(left - hs, bottom - hs, hs * 2f, hs * 2f);
            shapeRenderer.rect(left + bw - hs, bottom - hs, hs * 2f, hs * 2f);
            shapeRenderer.rect(left - hs, bottom + bh - hs, hs * 2f, hs * 2f);
            shapeRenderer.rect(left + bw - hs, bottom + bh - hs, hs * 2f, hs * 2f);
        }

        shapeRenderer.setColor(0.35f, 0.72f, 1f, 0.95f);
        shapeRenderer.rect(sx, sy, sw, sh);
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
