package com.minhtriet79.goldminer.studio.model;

/**
 * Pure-Java 2D editor camera mapping widget-local pixel coordinates {@code [0..widgetW, 0..widgetH]}
 * to virtual game world coordinates {@code [0..480, 0..270]} with centered aspect fitting,
 * zoom-around-cursor, and panning. Zero libGDX dependencies.
 */
public class CanvasCamera {

    public static final float DEFAULT_WORLD_WIDTH = 480f;
    public static final float DEFAULT_WORLD_HEIGHT = 270f;
    public static final float MIN_ZOOM = 0.50f;
    public static final float MAX_ZOOM = 4.00f;
    private static final float FIT_PADDING = 16f;

    private final float worldWidth;
    private final float worldHeight;

    private float widgetWidth = 640f;
    private float widgetHeight = 400f;
    private float zoom = 1.0f;
    private float panX = 0f;
    private float panY = 0f;

    public CanvasCamera() {
        this(DEFAULT_WORLD_WIDTH, DEFAULT_WORLD_HEIGHT);
    }

    public CanvasCamera(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public void setViewportSize(float widgetWidth, float widgetHeight) {
        this.widgetWidth = Math.max(1f, widgetWidth);
        this.widgetHeight = Math.max(1f, widgetHeight);
    }

    public void resetFit() {
        this.zoom = 1.0f;
        this.panX = 0f;
        this.panY = 0f;
    }

    public void panByLocalDelta(float deltaLocalX, float deltaLocalY) {
        this.panX += deltaLocalX;
        this.panY += deltaLocalY;
    }

    /**
     * Adjusts zoom around the given widget-local cursor position so the world point
     * under the cursor remains visually stationary.
     */
    public void zoomAtLocalPoint(float localX, float localY, float zoomFactor) {
        float[] before = new float[2];
        localToWorld(localX, localY, before);

        float nextZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * zoomFactor));
        if (Math.abs(nextZoom - zoom) < 0.0001f) {
            return;
        }
        this.zoom = nextZoom;

        float[] after = new float[2];
        worldToLocal(before[0], before[1], after);
        this.panX += (localX - after[0]);
        this.panY += (localY - after[1]);
    }

    public float getBaseFitScale() {
        float availW = Math.max(1f, widgetWidth - FIT_PADDING);
        float availH = Math.max(1f, widgetHeight - FIT_PADDING);
        return Math.min(availW / worldWidth, availH / worldHeight);
    }

    public float getPixelsPerWorldUnit() {
        return getBaseFitScale() * zoom;
    }

    public void getWorldOriginInLocal(float[] outOrigin) {
        float scale = getPixelsPerWorldUnit();
        float drawnW = worldWidth * scale;
        float drawnH = worldHeight * scale;
        outOrigin[0] = (widgetWidth - drawnW) * 0.5f + panX;
        outOrigin[1] = (widgetHeight - drawnH) * 0.5f + panY;
    }

    public void localToWorld(float localX, float localY, float[] outWorld) {
        float[] origin = new float[2];
        getWorldOriginInLocal(origin);
        float scale = getPixelsPerWorldUnit();
        outWorld[0] = (localX - origin[0]) / scale;
        outWorld[1] = (localY - origin[1]) / scale;
    }

    public void worldToLocal(float worldX, float worldY, float[] outLocal) {
        float[] origin = new float[2];
        getWorldOriginInLocal(origin);
        float scale = getPixelsPerWorldUnit();
        outLocal[0] = origin[0] + worldX * scale;
        outLocal[1] = origin[1] + worldY * scale;
    }

    public boolean isLocalInsideWorld(float localX, float localY) {
        float[] w = new float[2];
        localToWorld(localX, localY, w);
        return w[0] >= 0f && w[0] <= worldWidth && w[1] >= 0f && w[1] <= worldHeight;
    }

    public float getZoom() {
        return zoom;
    }

    public float getWorldWidth() {
        return worldWidth;
    }

    public float getWorldHeight() {
        return worldHeight;
    }
}
