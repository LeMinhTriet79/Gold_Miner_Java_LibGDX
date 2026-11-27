package com.minhtriet79.goldminer.studio.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.studio.io.ImageCropper;
import com.minhtriet79.goldminer.studio.io.LruTextureCache;
import com.minhtriet79.goldminer.studio.io.StudioFileHelper;
import com.minhtriet79.goldminer.studio.io.VegasSourceScanner;
import com.minhtriet79.goldminer.studio.io.VegasSourceScanner.SourceCategory;
import com.minhtriet79.goldminer.studio.io.VegasSourceScanner.SourceEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 18 FPS Sprite & Animation Previewer backed by a strict 16-frame {@link LruTextureCache}
 * so scrubbing 500+ frame Vegas clips uses negligible VRAM and never freezes or OOMs.
 */
public class SpritePreviewerPanel extends VisTable implements Disposable {

    private static final float VEGAS_FPS_DT = 1f / 18f;

    private final Assets assets;
    private final Consumer<String> statusCallback;
    private final Consumer<String> onExportedCallback;

    private final LruTextureCache frameCache = new LruTextureCache(16);
    private final TextureRegionDrawable reusableDrawable = new TextureRegionDrawable();
    private final TextureRegion reusableRegion = new TextureRegion();

    private final VisTextField searchField = new VisTextField("");
    private final VisList<String> spriteList = new VisList<>();
    private List<SourceEntry> spriteEntries = new ArrayList<>();
    private List<File> currentFrameFiles = new ArrayList<>();

    private final VisImage previewImage = new VisImage();
    private final VisLabel frameInfoLabel = new VisLabel("Chọn 1 DefineSprite_* bên trái để xem ở 18 FPS");
    private final VisSlider frameSlider = new VisSlider(1f, 1f, 1f, false);
    private final VisTextButton playPauseBtn = new VisTextButton("Tạm dừng");

    private final VisTextField startFrameField = new VisTextField("1");
    private final VisTextField endFrameField = new VisTextField("1");
    private final VisTextField exportNameField = new VisTextField("custom_anim");
    private final VisTextField scaleField = new VisTextField("0.27");

    private boolean playing = true;
    private int currentFrameIdx = 0;
    private float frameAccum = 0f;
    private boolean updatingSliderFromCode = false;

    public SpritePreviewerPanel(
        Assets assets,
        Consumer<String> statusCallback,
        Consumer<String> onExportedCallback
    ) {
        this.assets = assets;
        this.statusCallback = statusCallback;
        this.onExportedCallback = onExportedCallback;

        pad(8f);
        buildUi();
        refreshSpriteList();
    }

    private void buildUi() {
        VisTable leftCol = new VisTable();
        leftCol.pad(4f);
        searchField.setMessageText("Tìm DefineSprite_... (vd: 2707, 2741)");
        searchField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                refreshSpriteList();
            }
        });
        leftCol.add(new VisLabel("Kho Sprites Vegas")).left().padBottom(4f).row();
        leftCol.add(searchField).growX().padBottom(4f).row();

        VisScrollPane listScroll = new VisScrollPane(spriteList);
        listScroll.setFadeScrollBars(false);
        leftCol.add(listScroll).grow().row();

        spriteList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int idx = spriteList.getSelectedIndex();
                if (idx >= 0 && idx < spriteEntries.size()) {
                    loadSpriteDirectory(spriteEntries.get(idx).containerDir);
                }
            }
        });

        VisTable centerCol = new VisTable();
        centerCol.pad(6f);
        previewImage.setScaling(Scaling.fit);
        VisTable stageBox = new VisTable();
        stageBox.setBackground("window-bg");
        stageBox.add(previewImage).grow().pad(14f);

        centerCol.add(frameInfoLabel).padBottom(6f).row();
        centerCol.add(stageBox).grow().padBottom(6f).row();

        VisTable playbackRow = new VisTable(true);
        VisTextButton prevBtn = new VisTextButton("< Frame");
        VisTextButton nextBtn = new VisTextButton("Frame >");

        playPauseBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playing = !playing;
                playPauseBtn.setText(playing ? "Tạm dừng" : "Chạy");
            }
        });
        prevBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playing = false;
                playPauseBtn.setText("Chạy");
                stepFrame(-1);
            }
        });
        nextBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playing = false;
                playPauseBtn.setText("Chạy");
                stepFrame(1);
            }
        });
        frameSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (updatingSliderFromCode || currentFrameFiles.isEmpty()) {
                    return;
                }
                playing = false;
                playPauseBtn.setText("Chạy");
                currentFrameIdx = Math.max(0, Math.min(currentFrameFiles.size() - 1, (int) frameSlider.getValue() - 1));
                applyCurrentFrame();
            }
        });

        playbackRow.add(playPauseBtn).width(80f);
        playbackRow.add(prevBtn);
        playbackRow.add(frameSlider).growX();
        playbackRow.add(nextBtn);
        centerCol.add(playbackRow).growX().row();

        VisTable rightCol = new VisTable();
        rightCol.top().left().pad(8f);
        rightCol.setBackground("window-bg");
        rightCol.defaults().pad(4f).left();

        VisLabel expTitle = new VisLabel("XUẤT ANIMATION (UNION BBOX)");
        expTitle.setColor(Color.GOLD);
        rightCol.add(expTitle).colspan(2).row();
        VisLabel hintLbl = new VisLabel("Tính chung khung bao toàn chuỗi chống cà giật.");
        hintLbl.setColor(Color.LIGHT_GRAY);
        rightCol.add(hintLbl).colspan(2).row();

        VisTextButton markStartBtn = new VisTextButton("Đặt đầu = hiện tại");
        VisTextButton markEndBtn = new VisTextButton("Đặt cuối = hiện tại");
        markStartBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startFrameField.setText(Integer.toString(currentFrameIdx + 1));
            }
        });
        markEndBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                endFrameField.setText(Integer.toString(currentFrameIdx + 1));
            }
        });

        rightCol.add(new VisLabel("Từ Frame:"));
        rightCol.add(startFrameField).width(100f).row();
        rightCol.add(new VisLabel("Đến Frame:"));
        rightCol.add(endFrameField).width(100f).row();

        VisTable markRow = new VisTable(true);
        markRow.add(markStartBtn).growX();
        markRow.add(markEndBtn).growX();
        rightCol.add(markRow).colspan(2).growX().row();

        rightCol.add(new VisLabel("Tên Key:"));
        rightCol.add(exportNameField).width(150f).row();
        rightCol.add(new VisLabel("World Scale:"));
        rightCol.add(scaleField).width(100f).row();

        VisTextButton exportBtn = new VisTextButton("XUẤT ANIMATION");
        exportBtn.setColor(Color.LIME);
        exportBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                exportSelectedRange();
            }
        });
        rightCol.add(exportBtn).colspan(2).growX().padTop(10f).row();

        add(leftCol).width(245f).growY();
        add(centerCol).grow().padLeft(8f).padRight(8f);
        add(rightCol).width(275f).growY();
    }

    public void refreshSpriteList() {
        File root = new File(VegasSourceScanner.DEFAULT_VEGAS_SOURCE_PATH);
        spriteEntries = VegasSourceScanner.scanCategory(root, SourceCategory.SPRITES, searchField.getText());
        String[] labels = new String[spriteEntries.size()];
        for (int i = 0; i < spriteEntries.size(); i++) {
            labels[i] = spriteEntries.get(i).id;
        }
        spriteList.setItems(labels);
    }

    public void loadSpriteDirectory(File spriteDir) {
        clearCachedFrames();
        if (spriteDir == null || !spriteDir.isDirectory()) {
            return;
        }

        currentFrameFiles = VegasSourceScanner.listOrderedSpriteFrames(spriteDir);
        if (currentFrameFiles.isEmpty()) {
            frameInfoLabel.setText(spriteDir.getName() + " (không có frame PNG)");
            return;
        }

        currentFrameIdx = 0;
        frameAccum = 0f;
        playing = true;
        playPauseBtn.setText("Tạm dừng");

        updatingSliderFromCode = true;
        frameSlider.setRange(1f, currentFrameFiles.size());
        frameSlider.setValue(1f);
        updatingSliderFromCode = false;

        startFrameField.setText("1");
        endFrameField.setText(Integer.toString(currentFrameFiles.size()));
        exportNameField.setText(StudioFileHelper.sanitizeKey(spriteDir.getName()));
        applyCurrentFrame();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!isVisible() || !playing || currentFrameFiles.size() <= 1) {
            return;
        }
        frameAccum += delta;
        if (frameAccum >= VEGAS_FPS_DT) {
            frameAccum -= VEGAS_FPS_DT;
            currentFrameIdx = (currentFrameIdx + 1) % currentFrameFiles.size();
            applyCurrentFrame();
        }
    }

    private void stepFrame(int delta) {
        if (currentFrameFiles.isEmpty()) {
            return;
        }
        int total = currentFrameFiles.size();
        currentFrameIdx = (currentFrameIdx + delta + total) % total;
        applyCurrentFrame();
    }

    private void applyCurrentFrame() {
        if (currentFrameFiles.isEmpty()) {
            return;
        }
        Texture tex = frameCache.get(currentFrameFiles.get(currentFrameIdx));
        if (tex == null) {
            return;
        }
        reusableRegion.setRegion(tex);
        reusableDrawable.setRegion(reusableRegion);
        previewImage.setDrawable(reusableDrawable);
        previewImage.invalidateHierarchy();

        frameInfoLabel.setText(
            "Frame " + (currentFrameIdx + 1) + " / " + currentFrameFiles.size()
                + "  (" + tex.getWidth() + "x" + tex.getHeight() + " px @ 18 FPS)"
        );
        frameInfoLabel.setAlignment(Align.center);

        updatingSliderFromCode = true;
        frameSlider.setValue(currentFrameIdx + 1);
        updatingSliderFromCode = false;
    }

    private void exportSelectedRange() {
        if (currentFrameFiles.isEmpty()) {
            statusCallback.accept("Chưa chọn sprite để xuất.");
            return;
        }
        try {
            int start = Math.max(1, Integer.parseInt(startFrameField.getText().trim()));
            int end = Math.min(currentFrameFiles.size(), Integer.parseInt(endFrameField.getText().trim()));
            if (end < start) {
                end = start;
            }
            String key = StudioFileHelper.sanitizeKey(exportNameField.getText());
            float scale = Math.max(0.05f, Float.parseFloat(scaleField.getText().trim()));

            List<File> slice = currentFrameFiles.subList(start - 1, end);
            File outDir = new File(
                StudioFileHelper.resolveAssetsRoot(),
                StudioFileHelper.IMPORTED_REL_ROOT + "/animations/" + key
            );

            ImageCropper.AnimationExportResult result = ImageCropper.exportAnimationUnionCrop(slice, outDir, key);
            StudioFileHelper.registerExportedAnimation(key, scale, result, assets);

            statusCallback.accept(
                "Đã xuất animation '" + key + "' (" + result.frameCount + " frame, union "
                    + result.unionWidth + "x" + result.unionHeight + "px)"
            );
            if (onExportedCallback != null) {
                onExportedCallback.accept(key);
            }
        } catch (Exception ex) {
            statusCallback.accept("Lỗi xuất animation: " + ex.getMessage());
        }
    }

    private void clearCachedFrames() {
        previewImage.setDrawable((Drawable) null);
        frameCache.clear();
        currentFrameFiles = new ArrayList<>();
    }

    @Override
    public void dispose() {
        clearCachedFrames();
    }
}
