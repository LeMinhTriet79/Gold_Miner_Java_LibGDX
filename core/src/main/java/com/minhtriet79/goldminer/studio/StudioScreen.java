package com.minhtriet79.goldminer.studio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import com.minhtriet79.goldminer.GoldMinerGame;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.assets.ThemeConfig;
import com.minhtriet79.goldminer.domain.WorldBounds;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig.UiElementDef;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig.ItemSpawn;
import com.minhtriet79.goldminer.studio.io.StudioFileHelper;
import com.minhtriet79.goldminer.studio.model.EntityPrefabCatalog;
import com.minhtriet79.goldminer.studio.model.StudioMath;
import com.minhtriet79.goldminer.studio.model.UndoRedoHistory;
import com.minhtriet79.goldminer.studio.ui.ImportAssetDialog;
import com.minhtriet79.goldminer.studio.ui.PhysicsEditorPanel;
import com.minhtriet79.goldminer.studio.ui.SpritePreviewerPanel;
import com.minhtriet79.goldminer.studio.ui.StudioAssetBrowserPanel;
import com.minhtriet79.goldminer.studio.ui.StudioCanvasWidget;
import com.minhtriet79.goldminer.studio.ui.StudioInspectorPanel;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Gold Miner Engine & Studio 2.0 - 4-workspace 2D workbench with 2-row crisp Ribbon Toolbar,
 * clean Tool/Brush state indicator, exact DragAndDrop placement, Undo/Redo, Live Validator,
 * LRU 18 FPS Sprite Previewer, Physics Editor, and isolated Play Test transitions.
 */
public class StudioScreen implements Screen, ExternalFileDropListener {

    private static boolean fileChooserInitialized = false;

    private final GoldMinerGame game;
    private final Stage stage;
    private final VisTable rootTable = new VisTable();
    private final DragAndDrop dragAndDrop = new DragAndDrop();

    private final UndoRedoHistory<LevelConfig> levelHistory = new UndoRedoHistory<>(50);
    private final UndoRedoHistory<UiLayoutConfig> uiHistory = new UndoRedoHistory<>(50);
    private final Vector2 tmpDropWorld = new Vector2();

    private StudioMode mode;
    private String activeLevelFile = "level_1.json";
    private String activeUiFile = "main_menu.json";
    private LevelConfig levelConfig;
    private UiLayoutConfig uiConfig;
    private boolean dirty = false;

    // 2-Row Top Ribbon controls
    private final VisTextButton levelModeBtn = new VisTextButton(StudioMode.LEVEL_MODE.title);
    private final VisTextButton uiModeBtn = new VisTextButton(StudioMode.UI_MODE.title);
    private final VisTextButton previewModeBtn = new VisTextButton(StudioMode.SPRITE_PREVIEWER.title);
    private final VisTextButton physicsModeBtn = new VisTextButton(StudioMode.PHYSICS_MODE.title);

    private final VisSelectBox<String> fileSelectBox = new VisSelectBox<>();
    private final VisTextButton newLevelBtn = new VisTextButton("+ Mới");
    private final VisTextButton dupLevelBtn = new VisTextButton("Nhân bản");
    private final VisTextButton saveBtn = new VisTextButton("Lưu (Ctrl+S)");
    private final VisTextButton playTestBtn = new VisTextButton("[>] CHƠI THỬ");

    private final VisTextButton toolModeBtn = new VisTextButton("Con trỏ: Chọn (V)");
    private final VisTextButton importBtn = new VisTextButton("+ Nhập File...");
    private final VisTextButton gridBtn = new VisTextButton("Lưới: BẬT");
    private final VisTextButton hitboxBtn = new VisTextButton("Hitbox: BẬT");
    private final VisTextButton resetCamBtn = new VisTextButton("Căn vừa (F)");

    // Workspaces & Panels
    private final VisTable workspaceContainer = new VisTable();
    private final StudioAssetBrowserPanel assetBrowser;
    private final StudioCanvasWidget canvasWidget;
    private final StudioInspectorPanel inspectorPanel;
    private final SpritePreviewerPanel spritePreviewer;
    private final PhysicsEditorPanel physicsEditor;

    // Bottom Status Bar
    private final VisLabel dirtyLabel = new VisLabel("[OK] Đã lưu", "small");
    private final VisLabel statusLabel = new VisLabel("Sẵn sàng.", "small");
    private final VisLabel cursorLabel = new VisLabel("Zoom: 100% | X: 0.0  Y: 0.0", "small");
    private boolean updatingFileDropdown = false;

    public StudioScreen(GoldMinerGame game, StudioMode initialMode, String initialFile) {
        this.game = game;
        this.mode = initialMode != null ? initialMode : StudioMode.LEVEL_MODE;
        if (initialFile != null && !initialFile.isEmpty()) {
            if (this.mode == StudioMode.UI_MODE) {
                this.activeUiFile = initialFile;
            } else {
                this.activeLevelFile = initialFile;
            }
        }

        if (!fileChooserInitialized) {
            FileChooser.setDefaultPrefsName("com.minhtriet79.goldminer.studio");
            fileChooserInitialized = true;
        }

        this.stage = new Stage(new ScreenViewport(), game.getBatch());
        Gdx.input.setInputProcessor(stage);
        game.setExternalFileDropListener(this);

        this.dragAndDrop.setDragTime(110);
        this.dragAndDrop.setKeepWithinStage(false);

        this.levelConfig = StudioFileHelper.loadLevel(activeLevelFile);
        this.uiConfig = StudioFileHelper.loadUiLayout(activeUiFile);

        this.assetBrowser = new StudioAssetBrowserPanel(game.getAssets(), dragAndDrop, new StudioAssetBrowserPanel.HostActions() {
            @Override
            public void onArmAsset(String key, boolean isAnimation) {
                armAssetForPlacement(key);
            }

            @Override
            public void onPlaceCenterRequested(String key, boolean isAnimation) {
                placeAssetOnCanvas(key, WorldBounds.WORLD_WIDTH * 0.5f, 110f);
                disarmAsset();
            }

            @Override
            public void onPreviewSpriteRequested(File spriteDir) {
                switchMode(StudioMode.SPRITE_PREVIEWER);
                spritePreviewer.loadSpriteDirectory(spriteDir);
                setStatus("Đang xem trước " + spriteDir.getName() + " ở 18 FPS.");
            }

            @Override
            public void onSelectHierarchyIndex(int index) {
                disarmAsset();
                canvasWidget.setSelectedIndex(index);
                inspectorPanel.bindState(mode, levelConfig, uiConfig, index);
                assetBrowser.bindSceneData(mode, levelConfig, uiConfig, index);
            }

            @Override
            public void onDeleteHierarchyIndex(int index) {
                canvasWidget.setSelectedIndex(index);
                deleteSelected();
            }

            @Override
            public void onAdjustHierarchyZ(int index, int delta) {
                canvasWidget.setSelectedIndex(index);
                adjustSelectedZIndex(delta);
            }

            @Override
            public void onStatus(String message) {
                setStatus(message);
            }
        });
        this.assetBrowser.attachStage(stage);

        this.canvasWidget = new StudioCanvasWidget(
            game.getAssets(),
            game.getHudFont(),
            game.getWhitePixel(),
            new StudioCanvasWidget.CanvasHost() {
                @Override
                public void onBeforeMutate() {
                    pushUndoSnapshot();
                }

                @Override
                public void onSelectionChanged() {
                    int sel = canvasWidget.getSelectedIndex();
                    inspectorPanel.bindState(mode, levelConfig, uiConfig, sel);
                    assetBrowser.bindSceneData(mode, levelConfig, uiConfig, sel);
                }

                @Override
                public void onLevelMutated() {
                    markDirty(true);
                    inspectorPanel.refreshFieldValuesOnly();
                    assetBrowser.bindSceneData(mode, levelConfig, uiConfig, canvasWidget.getSelectedIndex());
                }

                @Override
                public void onUiMutated() {
                    markDirty(true);
                    inspectorPanel.refreshFieldValuesOnly();
                    assetBrowser.bindSceneData(mode, levelConfig, uiConfig, canvasWidget.getSelectedIndex());
                }

                @Override
                public void onPlaceArmedAsset(float worldX, float worldY) {
                    String armed = canvasWidget.getArmedAssetKey();
                    if (armed != null) {
                        placeAssetOnCanvas(armed, worldX, worldY);
                        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                        if (!shift) {
                            disarmAsset();
                        }
                    }
                }

                @Override
                public void onCancelArmedAsset() {
                    disarmAsset();
                    setStatus("Đã quay về chế độ Con trỏ Chọn (click vật thể để chỉnh).");
                }

                @Override
                public void onCursorMoved(float worldX, float worldY, float zoom) {
                    cursorLabel.setText(String.format(Locale.ROOT, "Zoom: %.0f%% | X: %.1f  Y: %.1f", zoom * 100f, worldX, worldY));
                }
            }
        );

        this.dragAndDrop.addTarget(new Target(canvasWidget) {
            @Override
            public boolean drag(Source source, Payload payload, float x, float y, int pointer) {
                return payload.getObject() instanceof String;
            }

            @Override
            public void drop(Source source, Payload payload, float x, float y, int pointer) {
                if (!(payload.getObject() instanceof String key)) {
                    return;
                }
                // x and y passed by Scene2D Target.drop() are already local to canvasWidget!
                if (canvasWidget.localToWorld(x, y, tmpDropWorld)) {
                    float wx = StudioMath.snapToGrid(tmpDropWorld.x, StudioMath.GRID_STEP, canvasWidget.isShowGrid());
                    float wy = StudioMath.snapToGrid(tmpDropWorld.y, StudioMath.GRID_STEP, canvasWidget.isShowGrid());
                    placeAssetOnCanvas(key, wx, wy);
                    disarmAsset();
                }
            }
        });

        this.inspectorPanel = new StudioInspectorPanel(game.getAssets(), new StudioInspectorPanel.InspectorHost() {
            @Override
            public void onBeforeMutate() {
                pushUndoSnapshot();
            }

            @Override
            public void onDeleteSelected() {
                deleteSelected();
            }

            @Override
            public void onDuplicateSelected() {
                duplicateSelected();
            }

            @Override
            public void onAdjustZIndex(int delta) {
                adjustSelectedZIndex(delta);
            }

            @Override
            public void onSelectItemFromValidator(int itemIndex) {
                disarmAsset();
                canvasWidget.setSelectedIndex(itemIndex);
                inspectorPanel.bindState(mode, levelConfig, uiConfig, itemIndex);
                assetBrowser.bindSceneData(mode, levelConfig, uiConfig, itemIndex);
            }

            @Override
            public void onLevelChanged() {
                markDirty(true);
                assetBrowser.bindSceneData(mode, levelConfig, uiConfig, canvasWidget.getSelectedIndex());
                setStatus("Đã cập nhật thông số Màn.");
            }

            @Override
            public void onUiChanged() {
                markDirty(true);
                assetBrowser.bindSceneData(mode, levelConfig, uiConfig, canvasWidget.getSelectedIndex());
                setStatus("Đã cập nhật thuộc tính UI.");
            }
        });

        this.spritePreviewer = new SpritePreviewerPanel(
            game.getAssets(),
            this::setStatus,
            exportedKey -> {
                assetBrowser.refreshProjectAssets();
                setStatus("Xuất animation '" + exportedKey + "' thành công! Đã có trong Kho Prefab.");
            }
        );

        this.physicsEditor = new PhysicsEditorPanel(this::setStatus, this::launchPlayTest);

        buildRootLayout();
        switchMode(this.mode);
    }

    private void buildRootLayout() {
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // 2-ROW RIBBON TOOLBAR (never overflows or chops text on 1280x720)
        VisTable ribbon = new VisTable();
        ribbon.setBackground("window-bg");
        ribbon.pad(4f, 6f, 4f, 6f);

        VisTable row1 = new VisTable(true);
        levelModeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchMode(StudioMode.LEVEL_MODE);
            }
        });
        uiModeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchMode(StudioMode.UI_MODE);
            }
        });
        previewModeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchMode(StudioMode.SPRITE_PREVIEWER);
            }
        });
        physicsModeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchMode(StudioMode.PHYSICS_MODE);
            }
        });

        fileSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (updatingFileDropdown) {
                    return;
                }
                String chosen = fileSelectBox.getSelected();
                if (chosen == null) {
                    return;
                }
                disarmAsset();
                if (mode == StudioMode.LEVEL_MODE) {
                    activeLevelFile = chosen;
                    levelConfig = StudioFileHelper.loadLevel(activeLevelFile);
                    levelHistory.clear();
                    markDirty(false);
                    syncAllPanels(-1);
                    setStatus("Đã mở " + activeLevelFile);
                } else if (mode == StudioMode.UI_MODE) {
                    activeUiFile = chosen;
                    uiConfig = StudioFileHelper.loadUiLayout(activeUiFile);
                    uiHistory.clear();
                    markDirty(false);
                    syncAllPanels(-1);
                    setStatus("Đã mở " + activeUiFile);
                }
            }
        });

        newLevelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                createNewLevel();
            }
        });
        dupLevelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                duplicateCurrentLevel();
            }
        });
        saveBtn.setColor(Color.SKY);
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveActiveFile();
            }
        });
        playTestBtn.setColor(Color.LIME);
        playTestBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                launchPlayTest();
            }
        });

        row1.add(levelModeBtn);
        row1.add(uiModeBtn);
        row1.add(previewModeBtn);
        row1.add(physicsModeBtn);
        row1.add(fileSelectBox).width(142f).padLeft(8f);
        row1.add(newLevelBtn);
        row1.add(dupLevelBtn);
        row1.add().growX();
        row1.add(saveBtn);
        row1.add(playTestBtn).width(126f);

        VisTable row2 = new VisTable(true);
        toolModeBtn.setColor(Color.CYAN);
        toolModeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                disarmAsset();
                setStatus("Đã chuyển về chế độ Con trỏ Chọn.");
            }
        });
        importBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openImportFileChooser();
            }
        });
        gridBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleGrid();
            }
        });
        hitboxBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleHitbox();
            }
        });
        resetCamBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                canvasWidget.resetView();
                cursorLabel.setText("Zoom: 100% | X: 240.0  Y: 135.0");
            }
        });
        VisTextButton undoBtn = new VisTextButton("Undo (Ctrl+Z)");
        undoBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                performUndo();
            }
        });
        VisTextButton redoBtn = new VisTextButton("Redo (Ctrl+Y)");
        redoBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                performRedo();
            }
        });

        row2.add(toolModeBtn);
        row2.add(importBtn).padLeft(6f);
        row2.add(gridBtn);
        row2.add(hitboxBtn);
        row2.add(resetCamBtn);
        row2.add(undoBtn);
        row2.add(redoBtn);
        row2.add().growX();

        ribbon.add(row1).growX().padBottom(3f).row();
        ribbon.add(row2).growX().row();

        rootTable.add(ribbon).growX().row();
        rootTable.add(workspaceContainer).grow().row();

        // BOTTOM STATUS BAR
        VisTable statusBar = new VisTable(true);
        statusBar.setBackground("window-bg");
        statusBar.pad(3f, 8f, 3f, 8f);
        dirtyLabel.setColor(Color.LIME);
        statusLabel.setColor(Color.LIGHT_GRAY);
        cursorLabel.setColor(Color.GOLD);
        statusBar.add(dirtyLabel).width(96f).left();
        statusBar.add(statusLabel).growX().left();
        statusBar.add(cursorLabel).right();
        rootTable.add(statusBar).growX().row();
    }

    private void armAssetForPlacement(String key) {
        canvasWidget.setArmedAssetKey(key);
        toolModeBtn.setText("Đang đặt: " + key + " (Click phải/ESC để Hủy)");
        toolModeBtn.setColor(Color.GOLD);
        setStatus("Đang đặt '" + key + "' - Click vào sân khấu để đặt, hoặc Chuột phải / ESC để quay lại Chọn.");
    }

    private void disarmAsset() {
        canvasWidget.setArmedAssetKey(null);
        assetBrowser.setArmedKey(null);
        toolModeBtn.setText("Con trỏ: Chọn (V)");
        toolModeBtn.setColor(Color.CYAN);
    }

    private void switchMode(StudioMode targetMode) {
        this.mode = targetMode;
        disarmAsset();

        levelModeBtn.setColor(mode == StudioMode.LEVEL_MODE ? Color.GOLD : Color.WHITE);
        uiModeBtn.setColor(mode == StudioMode.UI_MODE ? Color.GOLD : Color.WHITE);
        previewModeBtn.setColor(mode == StudioMode.SPRITE_PREVIEWER ? Color.GOLD : Color.WHITE);
        physicsModeBtn.setColor(mode == StudioMode.PHYSICS_MODE ? Color.GOLD : Color.WHITE);

        boolean canvasActive = mode == StudioMode.LEVEL_MODE || mode == StudioMode.UI_MODE;
        fileSelectBox.setVisible(canvasActive);
        newLevelBtn.setVisible(mode == StudioMode.LEVEL_MODE);
        dupLevelBtn.setVisible(mode == StudioMode.LEVEL_MODE);
        toolModeBtn.setVisible(canvasActive);
        gridBtn.setVisible(canvasActive);
        hitboxBtn.setVisible(mode == StudioMode.LEVEL_MODE);
        resetCamBtn.setVisible(canvasActive);

        if (canvasActive) {
            refreshFileDropdown();
        }

        workspaceContainer.clearChildren();
        switch (mode) {
            case SPRITE_PREVIEWER -> workspaceContainer.add(spritePreviewer).grow();
            case PHYSICS_MODE -> {
                physicsEditor.reloadFromDisk();
                workspaceContainer.add(physicsEditor).grow();
            }
            case LEVEL_MODE, UI_MODE -> {
                syncAllPanels(-1);
                workspaceContainer.add(assetBrowser).width(314f).growY();
                workspaceContainer.add(canvasWidget).grow();
                workspaceContainer.add(inspectorPanel).width(304f).growY();
            }
        }
    }

    private void syncAllPanels(int selectedIdx) {
        canvasWidget.setData(mode, levelConfig, uiConfig, selectedIdx);
        inspectorPanel.bindState(mode, levelConfig, uiConfig, selectedIdx);
        assetBrowser.bindSceneData(mode, levelConfig, uiConfig, selectedIdx);
    }

    private void refreshFileDropdown() {
        updatingFileDropdown = true;
        if (mode == StudioMode.UI_MODE) {
            List<String> uiFiles = StudioFileHelper.listUiFiles();
            fileSelectBox.setItems(uiFiles.toArray(new String[0]));
            if (uiFiles.contains(activeUiFile)) {
                fileSelectBox.setSelected(activeUiFile);
            }
        } else {
            List<String> levelFiles = StudioFileHelper.listLevelFiles();
            fileSelectBox.setItems(levelFiles.toArray(new String[0]));
            if (levelFiles.contains(activeLevelFile)) {
                fileSelectBox.setSelected(activeLevelFile);
            }
        }
        updatingFileDropdown = false;
    }

    private void createNewLevel() {
        disarmAsset();
        String newName = StudioFileHelper.nextNewLevelFileName();
        LevelConfig fresh = new LevelConfig();
        fresh.level = StudioFileHelper.parseLevelNumber(newName);
        fresh.goal = 555;
        fresh.durationSeconds = 60;
        StudioFileHelper.saveLevel(newName, fresh);

        activeLevelFile = newName;
        levelConfig = fresh;
        levelHistory.clear();
        markDirty(false);
        refreshFileDropdown();
        syncAllPanels(-1);
        setStatus("Đã tạo màn chơi mới: " + newName);
    }

    private void duplicateCurrentLevel() {
        if (mode != StudioMode.LEVEL_MODE) {
            return;
        }
        disarmAsset();
        saveActiveFile();
        String newName = StudioFileHelper.duplicateLevel(activeLevelFile);
        activeLevelFile = newName;
        levelConfig = StudioFileHelper.loadLevel(newName);
        levelHistory.clear();
        markDirty(false);
        refreshFileDropdown();
        syncAllPanels(-1);
        setStatus("Đã nhân bản sang màn: " + newName);
    }

    private void openImportFileChooser() {
        FileChooser chooser = new FileChooser(FileChooser.Mode.OPEN);
        chooser.setSelectionMode(FileChooser.SelectionMode.FILES);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDirectory(StudioFileHelper.resolveAssetsRoot());
        chooser.setListener(new FileChooserAdapter() {
            @Override
            public void selected(Array<FileHandle> files) {
                if (files != null && files.size > 0) {
                    openImportDialog(files.first().file());
                }
            }
        });
        stage.addActor(chooser);
    }

    private void openImportDialog(File file) {
        String suggested = mode == StudioMode.UI_MODE ? "ui" : "entity";
        ImportAssetDialog dialog = new ImportAssetDialog(
            file,
            suggested,
            game.getAssets(),
            importedKey -> {
                armAssetForPlacement(importedKey);
                assetBrowser.setArmedKey(importedKey);
                assetBrowser.refreshProjectAssets();
            },
            this::setStatus
        );
        stage.addActor(dialog);
    }

    private void placeAssetOnCanvas(String key, float wx, float wy) {
        pushUndoSnapshot();
        Assets assets = game.getAssets();
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null) {
            boolean isAnim = assets.hasAnimation(key);
            float dw = 20f;
            float dh = 20f;
            if (isAnim) {
                ThemeConfig.AnimationDef def = assets.getAnimationDef(key);
                dw = def.drawWidth;
                dh = def.drawHeight;
            } else {
                Texture tex = assets.getTexture(key);
                dw = Math.max(10f, Math.min(48f, tex.getWidth() * 0.45f));
                dh = Math.max(10f, Math.min(48f, tex.getHeight() * 0.45f));
            }
            ItemSpawn spawn = EntityPrefabCatalog.createCustomLevelSpawn(key, dw, dh, isAnim, wx, wy, levelConfig.items.size);
            levelConfig.items.add(spawn);
            markDirty(true);
            syncAllPanels(levelConfig.items.size - 1);
            setStatus(String.format(Locale.ROOT, "Đã đặt '%s' tại (%.1f, %.1f).", key, spawn.x, spawn.y));
        } else if (mode == StudioMode.UI_MODE && uiConfig != null) {
            UiElementDef el = createUiElementPreset(key, wx, wy);
            uiConfig.elements.add(el);
            markDirty(true);
            syncAllPanels(uiConfig.elements.size - 1);
            setStatus(String.format(Locale.ROOT, "Đã đặt UI '%s' tại (%.1f, %.1f).", key, el.x, el.y));
        }
    }

    private UiElementDef createUiElementPreset(String key, float wx, float wy) {
        Assets assets = game.getAssets();
        UiElementDef el = new UiElementDef();
        el.id = key + "_" + (uiConfig.elements.size + 1);
        boolean looksButton = key.contains("btn") || key.contains("shop_");
        el.type = looksButton ? "BUTTON" : "IMAGE";
        el.texture = key;
        String hoverCandidate = key.replace("_up", "_over");
        el.hoverTexture = (!hoverCandidate.equals(key) && assets.hasTexture(hoverCandidate)) ? hoverCandidate : "";
        el.x = wx;
        el.y = wy;
        Texture tex = assets.getTexture(key);
        el.width = Math.max(16f, Math.min(180f, tex.getWidth() * 0.55f));
        el.height = Math.max(14f, Math.min(140f, tex.getHeight() * 0.55f));
        el.scaleX = 1f;
        el.scaleY = 1f;
        el.zIndex = uiConfig.elements.size;
        el.action = looksButton ? "PLAY_GAME" : "NONE";
        return el;
    }

    private LevelConfig cloneLevel(LevelConfig src) {
        LevelConfig snap = new LevelConfig();
        snap.level = src.level;
        snap.world = src.world;
        snap.themeHint = src.themeHint;
        snap.trackN = src.trackN;
        snap.durationSeconds = src.durationSeconds;
        snap.goal = src.goal;
        snap.levelValue = src.levelValue;
        if (src.cartRange != null) {
            snap.cartRange.minX = src.cartRange.minX;
            snap.cartRange.maxX = src.cartRange.maxX;
        }
        for (int i = 0; i < src.items.size; i++) {
            snap.items.add(src.items.get(i).copy());
        }
        return snap;
    }

    private UiLayoutConfig cloneUi(UiLayoutConfig src) {
        UiLayoutConfig snap = new UiLayoutConfig();
        snap.screenId = src.screenId;
        snap.backgroundKey = src.backgroundKey;
        for (int i = 0; i < src.elements.size; i++) {
            snap.elements.add(src.elements.get(i).copy());
        }
        return snap;
    }

    private void pushUndoSnapshot() {
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null) {
            levelHistory.push(cloneLevel(levelConfig));
        } else if (mode == StudioMode.UI_MODE && uiConfig != null) {
            uiHistory.push(cloneUi(uiConfig));
        }
    }

    private void performUndo() {
        if (mode == StudioMode.LEVEL_MODE && levelHistory.canUndo()) {
            levelConfig = levelHistory.undo(cloneLevel(levelConfig));
            markDirty(true);
            syncAllPanels(-1);
            setStatus("Hoàn tác (Undo: " + levelHistory.undoSize() + " | Redo: " + levelHistory.redoSize() + ").");
        } else if (mode == StudioMode.UI_MODE && uiHistory.canUndo()) {
            uiConfig = uiHistory.undo(cloneUi(uiConfig));
            markDirty(true);
            syncAllPanels(-1);
            setStatus("Hoàn tác UI (Undo: " + uiHistory.undoSize() + " | Redo: " + uiHistory.redoSize() + ").");
        } else {
            setStatus("Không còn bước nào để hoàn tác.");
        }
    }

    private void performRedo() {
        if (mode == StudioMode.LEVEL_MODE && levelHistory.canRedo()) {
            levelConfig = levelHistory.redo(cloneLevel(levelConfig));
            markDirty(true);
            syncAllPanels(-1);
            setStatus("Làm lại (Undo: " + levelHistory.undoSize() + " | Redo: " + levelHistory.redoSize() + ").");
        } else if (mode == StudioMode.UI_MODE && uiHistory.canRedo()) {
            uiConfig = uiHistory.redo(cloneUi(uiConfig));
            markDirty(true);
            syncAllPanels(-1);
            setStatus("Làm lại UI (Undo: " + uiHistory.undoSize() + " | Redo: " + uiHistory.redoSize() + ").");
        } else {
            setStatus("Không có bước nào để làm lại.");
        }
    }

    private void deleteSelected() {
        int idx = canvasWidget.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null && idx < levelConfig.items.size) {
            pushUndoSnapshot();
            levelConfig.items.removeIndex(idx);
            markDirty(true);
            syncAllPanels(-1);
            setStatus("Đã xóa vật thể.");
        } else if (mode == StudioMode.UI_MODE && uiConfig != null && idx < uiConfig.elements.size) {
            pushUndoSnapshot();
            uiConfig.elements.removeIndex(idx);
            markDirty(true);
            syncAllPanels(-1);
            setStatus("Đã xóa phần tử UI.");
        }
    }

    private void duplicateSelected() {
        int idx = canvasWidget.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null && idx < levelConfig.items.size) {
            pushUndoSnapshot();
            ItemSpawn dup = levelConfig.items.get(idx).copy();
            dup.x = Math.min(WorldBounds.WORLD_WIDTH - 10f, dup.x + 12f);
            dup.y = Math.max(10f, dup.y - 10f);
            dup.zIndex = levelConfig.items.size;
            levelConfig.items.add(dup);
            markDirty(true);
            syncAllPanels(levelConfig.items.size - 1);
            setStatus("Đã nhân đôi vật thể.");
        } else if (mode == StudioMode.UI_MODE && uiConfig != null && idx < uiConfig.elements.size) {
            pushUndoSnapshot();
            UiElementDef dup = uiConfig.elements.get(idx).copy();
            dup.id = dup.id + "_copy";
            dup.x = Math.min(WorldBounds.WORLD_WIDTH - 10f, dup.x + 12f);
            dup.y = Math.max(10f, dup.y - 10f);
            dup.zIndex = uiConfig.elements.size;
            uiConfig.elements.add(dup);
            markDirty(true);
            syncAllPanels(uiConfig.elements.size - 1);
            setStatus("Đã nhân đôi phần tử UI.");
        }
    }

    private void adjustSelectedZIndex(int delta) {
        int idx = canvasWidget.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null && idx < levelConfig.items.size) {
            pushUndoSnapshot();
            levelConfig.items.get(idx).zIndex += delta;
            markDirty(true);
            inspectorPanel.refreshFieldValuesOnly();
            assetBrowser.bindSceneData(mode, levelConfig, uiConfig, idx);
            setStatus("zIndex = " + levelConfig.items.get(idx).zIndex);
        } else if (mode == StudioMode.UI_MODE && uiConfig != null && idx < uiConfig.elements.size) {
            pushUndoSnapshot();
            uiConfig.elements.get(idx).zIndex += delta;
            markDirty(true);
            inspectorPanel.refreshFieldValuesOnly();
            assetBrowser.bindSceneData(mode, levelConfig, uiConfig, idx);
            setStatus("zIndex = " + uiConfig.elements.get(idx).zIndex);
        }
    }

    private void nudgeSelectedByKeys(float dx, float dy) {
        int idx = canvasWidget.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null && idx < levelConfig.items.size) {
            pushUndoSnapshot();
            ItemSpawn it = levelConfig.items.get(idx);
            it.x = Math.round((it.x + dx) * 10f) / 10f;
            it.y = Math.round((it.y + dy) * 10f) / 10f;
            markDirty(true);
            inspectorPanel.refreshFieldValuesOnly();
        } else if (mode == StudioMode.UI_MODE && uiConfig != null && idx < uiConfig.elements.size) {
            pushUndoSnapshot();
            UiElementDef el = uiConfig.elements.get(idx);
            el.x = Math.round((el.x + dx) * 10f) / 10f;
            el.y = Math.round((el.y + dy) * 10f) / 10f;
            markDirty(true);
            inspectorPanel.refreshFieldValuesOnly();
        }
    }

    private void toggleGrid() {
        boolean next = !canvasWidget.isShowGrid();
        canvasWidget.setShowGrid(next);
        gridBtn.setText(next ? "Lưới: BẬT" : "Lưới: TẮT");
    }

    private void toggleHitbox() {
        boolean next = !canvasWidget.isShowHitbox();
        canvasWidget.setShowHitbox(next);
        hitboxBtn.setText(next ? "Hitbox: BẬT" : "Hitbox: TẮT");
    }

    private void saveActiveFile() {
        if (mode == StudioMode.UI_MODE && uiConfig != null) {
            StudioFileHelper.saveUiLayout(activeUiFile, uiConfig);
            markDirty(false);
            setStatus("Đã lưu " + activeUiFile + " thành công!");
        } else if (levelConfig != null) {
            StudioFileHelper.saveLevel(activeLevelFile, levelConfig);
            markDirty(false);
            setStatus("Đã lưu " + activeLevelFile + " (Goal $" + levelConfig.goal + ", " + levelConfig.items.size + " vật thể).");
        }
    }

    private void launchPlayTest() {
        saveActiveFile();
        if (mode == StudioMode.UI_MODE) {
            final String fileCopy = activeUiFile;
            game.startUiPlayTest(fileCopy, () -> game.openStudio(StudioMode.UI_MODE, fileCopy));
        } else {
            final String fileCopy = activeLevelFile;
            final StudioMode returnMode = mode;
            game.startLevelPlayTest(fileCopy, () -> game.openStudio(returnMode, fileCopy));
        }
    }

    private void markDirty(boolean isDirty) {
        this.dirty = isDirty;
        dirtyLabel.setText(dirty ? "[*] Chưa lưu" : "[OK] Đã lưu");
        dirtyLabel.setColor(dirty ? Color.ORANGE : Color.LIME);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    @Override
    public void onFilesDropped(String[] absolutePaths) {
        if (absolutePaths == null || absolutePaths.length == 0) {
            return;
        }
        File dropped = new File(absolutePaths[0]);
        if (dropped.isFile()) {
            openImportDialog(dropped);
            setStatus("Nhận file kéo-thả: " + dropped.getName());
        } else if (dropped.isDirectory()) {
            switchMode(StudioMode.SPRITE_PREVIEWER);
            spritePreviewer.loadSpriteDirectory(dropped);
            setStatus("Mở thư mục sprite kéo-thả: " + dropped.getName());
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        game.setExternalFileDropListener(this);
    }

    @Override
    public void render(float rawDt) {
        float dt = Math.min(rawDt, 0.05f);
        handleShortcuts();

        ScreenUtils.clear(0.11f, 0.12f, 0.14f, 1f);
        stage.getViewport().apply();
        stage.act(dt);
        stage.draw();
    }

    private void handleShortcuts() {
        boolean typing = stage.getKeyboardFocus() instanceof TextField;
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            saveActiveFile();
            return;
        }
        if (ctrl && ((shift && Gdx.input.isKeyJustPressed(Input.Keys.Z)) || Gdx.input.isKeyJustPressed(Input.Keys.Y))) {
            performRedo();
            return;
        }
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            performUndo();
            return;
        }
        if (typing) {
            return;
        }

        if ((Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.V))
            && canvasWidget.getArmedAssetKey() != null) {
            disarmAsset();
            setStatus("Đã quay về chế độ Con trỏ Chọn.");
            return;
        }
        if (ctrl && Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            duplicateSelected();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            canvasWidget.resetView();
            cursorLabel.setText("Zoom: 100% | X: 240.0  Y: 135.0");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            toggleGrid();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H) && mode == StudioMode.LEVEL_MODE) {
            toggleHitbox();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL) || Gdx.input.isKeyJustPressed(Input.Keys.DEL)) {
            deleteSelected();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            adjustSelectedZIndex(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            adjustSelectedZIndex(1);
        }

        float step = shift ? 5f : 1f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            nudgeSelectedByKeys(-step, 0f);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            nudgeSelectedByKeys(step, 0f);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            nudgeSelectedByKeys(0f, step);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            nudgeSelectedByKeys(0f, -step);
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        game.setExternalFileDropListener(null);
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        game.setExternalFileDropListener(null);
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
        assetBrowser.dispose();
        canvasWidget.dispose();
        spritePreviewer.dispose();
        stage.dispose();
    }
}
