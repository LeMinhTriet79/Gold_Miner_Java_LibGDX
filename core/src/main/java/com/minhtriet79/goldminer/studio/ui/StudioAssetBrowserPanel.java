package com.minhtriet79.goldminer.studio.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig.UiElementDef;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig.ItemSpawn;
import com.minhtriet79.goldminer.studio.StudioMode;
import com.minhtriet79.goldminer.studio.io.LruTextureCache;
import com.minhtriet79.goldminer.studio.io.VegasSourceScanner;
import com.minhtriet79.goldminer.studio.io.VegasSourceScanner.PageSlice;
import com.minhtriet79.goldminer.studio.io.VegasSourceScanner.SourceCategory;
import com.minhtriet79.goldminer.studio.io.VegasSourceScanner.SourceEntry;
import com.minhtriet79.goldminer.studio.model.EntityPrefabCatalog;
import com.minhtriet79.goldminer.studio.model.EntityPrefabCatalog.Category;
import com.minhtriet79.goldminer.studio.model.EntityPrefabCatalog.PrefabDef;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Left Engine Dock with strict {@code setClip(true)} bounds, leak-free {@link DragAndDrop}
 * sources, reliable in-place selection/double-click placement, 2-row crisp filter chips,
 * Scene Hierarchy tree, and 24-texture LRU Vegas Importer.
 */
public class StudioAssetBrowserPanel extends VisTable implements Disposable {

    public interface HostActions {
        void onArmAsset(String key, boolean isAnimation);
        void onPlaceCenterRequested(String key, boolean isAnimation);
        void onPreviewSpriteRequested(File spriteDir);
        void onSelectHierarchyIndex(int index);
        void onDeleteHierarchyIndex(int index);
        void onAdjustHierarchyZ(int index, int delta);
        void onStatus(String message);
    }

    private enum BrowserTab {
        PREFABS,
        HIERARCHY,
        VEGAS_IMPORTER
    }

    private final Assets assets;
    private final HostActions host;
    private final DragAndDrop dragAndDrop;
    private Stage hostStage;

    private final VisTextButton prefabTabBtn = new VisTextButton("Kho Prefab");
    private final VisTextButton hierarchyTabBtn = new VisTextButton("Đối Tượng");
    private final VisTextButton vegasTabBtn = new VisTextButton("Kho Vegas");
    private final VisTable bodyContainer = new VisTable();

    private final VisTable prefabTabRoot = new VisTable();
    private final VisTable hierarchyTabRoot = new VisTable();
    private final VisTable vegasTabRoot = new VisTable();

    // Tab 1: Prefabs / Project Assets
    private final VisTextField prefabSearchField = new VisTextField("");
    private final VisTable prefabGrid = new VisTable();
    private final Array<Source> activeDragSources = new Array<>();
    private final ObjectMap<String, VisTable> cardMap = new ObjectMap<>();
    private final ObjectMap<String, VisTextButton> filterBtnMap = new ObjectMap<>();
    private String prefabFilterMode = "PREFAB_ALL";
    private String armedKey;

    // Tab 2: Scene Hierarchy
    private final VisTable hierarchyListTable = new VisTable();
    private StudioMode activeMode = StudioMode.LEVEL_MODE;
    private LevelConfig activeLevel;
    private UiLayoutConfig activeUi;
    private int selectedIndex = -1;

    // Tab 3: Vegas Importer (strict 24-entry LRU cache)
    private final VisTextField sourcePathField = new VisTextField(VegasSourceScanner.DEFAULT_VEGAS_SOURCE_PATH);
    private final VisTextField vegasSearchField = new VisTextField("");
    private final VisLabel pageLabel = new VisLabel("1 / 1");
    private final VisTable vegasGrid = new VisTable();
    private final VisTextButton[] categoryBtns = new VisTextButton[SourceCategory.values().length];
    private final LruTextureCache vegasThumbCache = new LruTextureCache(VegasSourceScanner.PAGE_SIZE);

    private SourceCategory activeCategory = SourceCategory.IMAGES;
    private List<SourceEntry> filteredVegasEntries = new ArrayList<>();
    private int currentPage = 0;
    private BrowserTab activeTab = BrowserTab.PREFABS;

    public StudioAssetBrowserPanel(Assets assets, DragAndDrop dragAndDrop, HostActions host) {
        this.assets = assets;
        this.dragAndDrop = dragAndDrop;
        this.host = host;

        setClip(true);
        setBackground("window-bg");
        pad(4f);

        VisTable tabRow = new VisTable(true);
        tabRow.add(prefabTabBtn).growX();
        tabRow.add(hierarchyTabBtn).growX();
        tabRow.add(vegasTabBtn).growX();
        add(tabRow).growX().padBottom(4f).row();
        add(bodyContainer).grow().row();

        prefabTabBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchTab(BrowserTab.PREFABS);
            }
        });
        hierarchyTabBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchTab(BrowserTab.HIERARCHY);
            }
        });
        vegasTabBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchTab(BrowserTab.VEGAS_IMPORTER);
            }
        });

        initPrefabTabOnce();
        initHierarchyTabOnce();
        initVegasTabOnce();
        switchTab(BrowserTab.PREFABS);
    }

    public void attachStage(Stage stage) {
        this.hostStage = stage;
    }

    public void bindSceneData(StudioMode mode, LevelConfig level, UiLayoutConfig ui, int selectedIdx) {
        this.activeMode = mode;
        this.activeLevel = level;
        this.activeUi = ui;
        this.selectedIndex = selectedIdx;
        if (activeTab == BrowserTab.HIERARCHY) {
            rebuildHierarchyList();
        }
    }

    public void refreshProjectAssets() {
        if (activeTab == BrowserTab.PREFABS) {
            rebuildPrefabGrid();
        }
    }

    public void setArmedKey(String armedKey) {
        this.armedKey = armedKey;
        updateCardSelectionVisuals();
    }

    private void switchTab(BrowserTab tab) {
        this.activeTab = tab;
        prefabTabBtn.setColor(tab == BrowserTab.PREFABS ? Color.GOLD : Color.WHITE);
        hierarchyTabBtn.setColor(tab == BrowserTab.HIERARCHY ? Color.GOLD : Color.WHITE);
        vegasTabBtn.setColor(tab == BrowserTab.VEGAS_IMPORTER ? Color.GOLD : Color.WHITE);

        bodyContainer.clearChildren();
        switch (tab) {
            case PREFABS -> {
                bodyContainer.add(prefabTabRoot).grow();
                rebuildPrefabGrid();
            }
            case HIERARCHY -> {
                bodyContainer.add(hierarchyTabRoot).grow();
                rebuildHierarchyList();
            }
            case VEGAS_IMPORTER -> {
                bodyContainer.add(vegasTabRoot).grow();
                scanVegasDirectory();
            }
        }
    }

    private void initPrefabTabOnce() {
        prefabTabRoot.top().left();

        VisTable filterRow1 = new VisTable(true);
        addFilterChip(filterRow1, "Prefab", "PREFAB_ALL");
        addFilterChip(filterRow1, "Kho báu", "TREASURE");
        addFilterChip(filterRow1, "Đá & TNT", "OBSTACLE");
        prefabTabRoot.add(filterRow1).growX().padBottom(2f).row();

        VisTable filterRow2 = new VisTable(true);
        addFilterChip(filterRow2, "Sinh vật", "CREATURE");
        addFilterChip(filterRow2, "Nút & UI", "UI_ELEMENTS");
        addFilterChip(filterRow2, "Tất cả", "ALL_ASSETS");
        prefabTabRoot.add(filterRow2).growX().padBottom(4f).row();
        syncFilterChipColors();

        VisTable searchRow = new VisTable(true);
        prefabSearchField.setMessageText("Tìm theo tên / key...");
        prefabSearchField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                rebuildPrefabGrid();
            }
        });
        VisTextButton placeCenterBtn = new VisTextButton("+ Giữa");
        placeCenterBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (armedKey != null) {
                    host.onPlaceCenterRequested(armedKey, assets.hasAnimation(armedKey));
                }
            }
        });
        searchRow.add(prefabSearchField).growX();
        searchRow.add(placeCenterBtn).width(58f);
        prefabTabRoot.add(searchRow).growX().padBottom(3f).row();

        VisLabel hint = new VisLabel("Kéo-thả lên sân khấu hoặc Click đúp để đặt:", "small");
        hint.setColor(Color.LIGHT_GRAY);
        hint.setWrap(true);
        prefabTabRoot.add(hint).width(288f).left().padBottom(3f).row();

        VisScrollPane scroll = new VisScrollPane(prefabGrid);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setCancelTouchFocus(false);
        prefabTabRoot.add(scroll).grow().row();
    }

    private void addFilterChip(VisTable row, String label, String modeKey) {
        VisTextButton b = new VisTextButton(label);
        filterBtnMap.put(modeKey, b);
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                prefabFilterMode = modeKey;
                syncFilterChipColors();
                rebuildPrefabGrid();
            }
        });
        row.add(b).growX();
    }

    private void syncFilterChipColors() {
        for (ObjectMap.Entry<String, VisTextButton> entry : filterBtnMap.entries()) {
            entry.value.setColor(entry.key.equals(prefabFilterMode) ? Color.GOLD : Color.WHITE);
        }
    }

    private void clearGridDragSources() {
        for (int i = 0; i < activeDragSources.size; i++) {
            dragAndDrop.removeSource(activeDragSources.get(i));
        }
        activeDragSources.clear();
        cardMap.clear();
        prefabGrid.clearChildren();
    }

    private void updateCardSelectionVisuals() {
        for (ObjectMap.Entry<String, VisTable> entry : cardMap.entries()) {
            boolean selected = entry.key.equals(armedKey);
            entry.value.setBackground(selected ? "button-down" : "button");
        }
    }

    private void rebuildPrefabGrid() {
        clearGridDragSources();
        prefabGrid.top().left();

        String filter = prefabSearchField.getText().trim().toLowerCase(Locale.ROOT);
        int col = 0;

        if ("UI_ELEMENTS".equals(prefabFilterMode)) {
            Array<String> sprites = assets.getAllSpriteKeys();
            for (int i = 0; i < sprites.size; i++) {
                String k = sprites.get(i);
                if (!k.startsWith("btn_") && !k.startsWith("shop_") && !k.startsWith("ui_") && !k.startsWith("bg_")) {
                    continue;
                }
                if (!filter.isEmpty() && !k.toLowerCase(Locale.ROOT).contains(filter)) {
                    continue;
                }
                addAssetCard(k, k, "UI", false, new TextureRegion(assets.getTexture(k)));
                if (++col % 3 == 0) {
                    prefabGrid.row();
                }
            }
            return;
        }

        if (!"ALL_ASSETS".equals(prefabFilterMode)) {
            List<PrefabDef> defs = switch (prefabFilterMode) {
                case "TREASURE" -> EntityPrefabCatalog.getByCategory(Category.TREASURE);
                case "OBSTACLE" -> EntityPrefabCatalog.getByCategory(Category.OBSTACLE);
                case "CREATURE" -> EntityPrefabCatalog.getByCategory(Category.CREATURE);
                default -> EntityPrefabCatalog.getAll();
            };

            for (PrefabDef def : defs) {
                if (!filter.isEmpty() && !def.key.contains(filter) && !def.displayName.toLowerCase(Locale.ROOT).contains(filter)) {
                    continue;
                }
                TextureRegion region = def.animated && assets.hasAnimation(def.key)
                    ? assets.getAnimation(def.key).getKeyFrame(0f)
                    : new TextureRegion(assets.getTexture(def.key));
                String subLabel = def.value > 0 ? ("$" + def.value + " | w" + def.weight) : ("w" + def.weight);
                addAssetCard(def.key, def.displayName, subLabel, def.animated, region);
                if (++col % 3 == 0) {
                    prefabGrid.row();
                }
            }

            if ("PREFAB_ALL".equals(prefabFilterMode)) {
                Array<String> sprites = assets.getAllSpriteKeys();
                for (int i = 0; i < sprites.size; i++) {
                    String k = sprites.get(i);
                    if (EntityPrefabCatalog.findByKey(k) != null || isInternalChromeKey(k)) {
                        continue;
                    }
                    if (!filter.isEmpty() && !k.toLowerCase(Locale.ROOT).contains(filter)) {
                        continue;
                    }
                    addAssetCard(k, k, "Custom", false, new TextureRegion(assets.getTexture(k)));
                    if (++col % 3 == 0) {
                        prefabGrid.row();
                    }
                }
                Array<String> anims = assets.getAllAnimationKeys();
                for (int i = 0; i < anims.size; i++) {
                    String k = anims.get(i);
                    if (EntityPrefabCatalog.findByKey(k) != null || k.startsWith("miner_") || k.startsWith("fx_") || k.startsWith("lex_")) {
                        continue;
                    }
                    if (!filter.isEmpty() && !k.toLowerCase(Locale.ROOT).contains(filter)) {
                        continue;
                    }
                    addAssetCard(k, k, "Anim", true, assets.getAnimation(k).getKeyFrame(0f));
                    if (++col % 3 == 0) {
                        prefabGrid.row();
                    }
                }
            }
            return;
        }

        // ALL_ASSETS mode: every sprite & animation key
        Array<String> spriteKeys = assets.getAllSpriteKeys();
        for (int i = 0; i < spriteKeys.size; i++) {
            String key = spriteKeys.get(i);
            if (!filter.isEmpty() && !key.toLowerCase(Locale.ROOT).contains(filter)) {
                continue;
            }
            addAssetCard(key, key, "Sprite", false, new TextureRegion(assets.getTexture(key)));
            if (++col % 3 == 0) {
                prefabGrid.row();
            }
        }
        Array<String> animKeys = assets.getAllAnimationKeys();
        for (int i = 0; i < animKeys.size; i++) {
            String key = animKeys.get(i);
            if (!filter.isEmpty() && !key.toLowerCase(Locale.ROOT).contains(filter)) {
                continue;
            }
            addAssetCard(key, key, "Anim", true, assets.getAnimation(key).getKeyFrame(0f));
            if (++col % 3 == 0) {
                prefabGrid.row();
            }
        }
    }

    private boolean isInternalChromeKey(String k) {
        return k.startsWith("claw_")
            || k.startsWith("track_")
            || k.startsWith("bg_")
            || k.startsWith("shop_")
            || k.startsWith("btn_")
            || k.startsWith("ui_")
            || k.equals("cart_standard")
            || k.equals("hook_claw");
    }

    private void addAssetCard(String key, String title, String subtitle, boolean isAnim, TextureRegion region) {
        VisTable card = new VisTable();
        card.setBackground(key.equals(armedKey) ? "button-down" : "button");
        card.pad(3f);

        VisImage img = new VisImage(new TextureRegionDrawable(region));
        img.setScaling(Scaling.fit);
        card.add(img).size(44f, 34f).row();

        VisLabel lbl = new VisLabel(title, "small");
        lbl.setAlignment(Align.center);
        lbl.setEllipsis(true);
        card.add(lbl).width(78f).row();

        VisLabel sub = new VisLabel(subtitle, "small");
        sub.setColor(Color.GOLD);
        sub.setAlignment(Align.center);
        sub.setEllipsis(true);
        card.add(sub).width(78f).row();

        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                armedKey = key;
                updateCardSelectionVisuals();
                host.onArmAsset(key, isAnim);
                if (getTapCount() >= 2) {
                    host.onPlaceCenterRequested(key, isAnim);
                }
            }
        });

        Source source = new Source(card) {
            @Override
            public Payload dragStart(InputEvent event, float x, float y, int pointer) {
                Payload payload = new Payload();
                payload.setObject(key);
                VisImage ghost = new VisImage(new TextureRegionDrawable(region));
                ghost.setSize(38f, 38f);
                payload.setDragActor(ghost);
                return payload;
            }
        };
        dragAndDrop.addSource(source);
        activeDragSources.add(source);
        cardMap.put(key, card);

        prefabGrid.add(card).size(86f, 74f).pad(2f);
    }

    private void initHierarchyTabOnce() {
        hierarchyTabRoot.top().left();
        VisLabel title = new VisLabel("Thứ tự vẽ (Z-Index) - Click để chọn:", "small");
        title.setColor(Color.LIGHT_GRAY);
        hierarchyTabRoot.add(title).left().padBottom(4f).row();

        VisScrollPane scroll = new VisScrollPane(hierarchyListTable);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        hierarchyTabRoot.add(scroll).grow().row();
    }

    private void rebuildHierarchyList() {
        hierarchyListTable.clearChildren();
        hierarchyListTable.top().left();

        if (activeMode == StudioMode.LEVEL_MODE && activeLevel != null) {
            List<Integer> order = new ArrayList<>(activeLevel.items.size);
            for (int i = 0; i < activeLevel.items.size; i++) {
                order.add(i);
            }
            order.sort(Comparator.comparingInt((Integer i) -> activeLevel.items.get(i).zIndex).reversed());

            for (int idx : order) {
                ItemSpawn it = activeLevel.items.get(idx);
                String desc = String.format(Locale.ROOT, "#%d %s (%.0f,%.0f) z%d", idx + 1, it.type, it.x, it.y, it.zIndex);
                addHierarchyRow(idx, desc);
            }
        } else if (activeMode == StudioMode.UI_MODE && activeUi != null) {
            List<Integer> order = new ArrayList<>(activeUi.elements.size);
            for (int i = 0; i < activeUi.elements.size; i++) {
                order.add(i);
            }
            order.sort(Comparator.comparingInt((Integer i) -> activeUi.elements.get(i).zIndex).reversed());

            for (int idx : order) {
                UiElementDef el = activeUi.elements.get(idx);
                String desc = String.format(Locale.ROOT, "#%d %s z%d", idx + 1, el.id, el.zIndex);
                addHierarchyRow(idx, desc);
            }
        }
    }

    private void addHierarchyRow(int idx, String text) {
        VisTable row = new VisTable(true);
        row.setBackground(idx == selectedIndex ? "button-down" : "button");
        row.pad(2f, 4f, 2f, 4f);

        VisLabel lbl = new VisLabel(text, "small");
        lbl.setEllipsis(true);
        lbl.setColor(idx == selectedIndex ? Color.GOLD : Color.WHITE);

        VisTextButton upBtn = new VisTextButton("^");
        VisTextButton downBtn = new VisTextButton("v");
        VisTextButton delBtn = new VisTextButton("X");
        delBtn.setColor(Color.SALMON);

        upBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                host.onAdjustHierarchyZ(idx, 1);
            }
        });
        downBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                host.onAdjustHierarchyZ(idx, -1);
            }
        });
        delBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                host.onDeleteHierarchyIndex(idx);
            }
        });

        row.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                host.onSelectHierarchyIndex(idx);
            }
        });

        row.add(lbl).width(176f).left();
        row.add(upBtn).width(22f);
        row.add(downBtn).width(22f);
        row.add(delBtn).width(22f);
        hierarchyListTable.add(row).width(276f).padBottom(2f).row();
    }

    private void initVegasTabOnce() {
        vegasTabRoot.top().left();

        VisTable pathRow = new VisTable(true);
        VisTextButton goBtn = new VisTextButton("Mở");
        goBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentPage = 0;
                scanVegasDirectory();
            }
        });
        pathRow.add(sourcePathField).growX();
        pathRow.add(goBtn).width(44f);
        vegasTabRoot.add(pathRow).growX().padBottom(4f).row();

        VisTable catRow = new VisTable(true);
        SourceCategory[] cats = SourceCategory.values();
        for (int i = 0; i < cats.length; i++) {
            SourceCategory cat = cats[i];
            VisTextButton b = new VisTextButton(cat.folderName);
            categoryBtns[i] = b;
            b.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    activeCategory = cat;
                    currentPage = 0;
                    syncCategoryButtonColors();
                    scanVegasDirectory();
                }
            });
            catRow.add(b).growX();
        }
        syncCategoryButtonColors();
        vegasTabRoot.add(catRow).growX().padBottom(4f).row();

        vegasSearchField.setMessageText("Lọc tên / ID (vd: 184, 2741)...");
        vegasSearchField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (activeTab == BrowserTab.VEGAS_IMPORTER) {
                    currentPage = 0;
                    scanVegasDirectory();
                }
            }
        });
        vegasTabRoot.add(vegasSearchField).growX().padBottom(4f).row();

        VisTable pageRow = new VisTable(true);
        VisTextButton prevBtn = new VisTextButton("< Trước");
        VisTextButton nextBtn = new VisTextButton("Sau >");
        prevBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (currentPage > 0) {
                    currentPage--;
                    renderVegasPage();
                }
            }
        });
        nextBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentPage++;
                renderVegasPage();
            }
        });
        pageRow.add(prevBtn);
        pageRow.add(pageLabel).growX();
        pageRow.add(nextBtn);
        vegasTabRoot.add(pageRow).growX().padBottom(4f).row();

        VisScrollPane scroll = new VisScrollPane(vegasGrid);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        vegasTabRoot.add(scroll).grow().row();
    }

    private void syncCategoryButtonColors() {
        SourceCategory[] cats = SourceCategory.values();
        for (int i = 0; i < cats.length; i++) {
            if (categoryBtns[i] != null) {
                categoryBtns[i].setColor(cats[i] == activeCategory ? Color.GOLD : Color.WHITE);
            }
        }
    }

    private void scanVegasDirectory() {
        File root = new File(sourcePathField.getText().trim());
        filteredVegasEntries = VegasSourceScanner.scanCategory(root, activeCategory, vegasSearchField.getText());
        renderVegasPage();
    }

    private void renderVegasPage() {
        vegasThumbCache.clear();
        vegasGrid.clearChildren();
        vegasGrid.top().left();

        PageSlice slice = VegasSourceScanner.paginate(filteredVegasEntries, currentPage, VegasSourceScanner.PAGE_SIZE);
        currentPage = slice.pageIndex;
        pageLabel.setText(String.format(Locale.ROOT, "%d / %d (%d)", slice.pageIndex + 1, slice.totalPages, slice.totalItems));
        pageLabel.setAlignment(Align.center);

        if (slice.entries.isEmpty()) {
            vegasGrid.add(new VisLabel("Không tìm thấy file phù hợp.")).pad(12f);
            return;
        }

        int col = 0;
        for (SourceEntry entry : slice.entries) {
            VisTable card = new VisTable();
            card.setBackground("button");
            card.pad(3f);

            if (entry.category == SourceCategory.SOUNDS) {
                VisLabel iconLbl = new VisLabel("[MP3]");
                iconLbl.setColor(Color.CYAN);
                card.add(iconLbl).size(44f, 34f).row();
            } else {
                Texture thumbTex = vegasThumbCache.get(entry.previewFile);
                if (thumbTex != null) {
                    VisImage img = new VisImage(new TextureRegionDrawable(new TextureRegion(thumbTex)));
                    img.setScaling(Scaling.fit);
                    card.add(img).size(44f, 34f).row();
                }
            }

            VisLabel nameLbl = new VisLabel(entry.id, "small");
            nameLbl.setEllipsis(true);
            nameLbl.setAlignment(Align.center);
            card.add(nameLbl).width(78f).row();

            if (entry.category == SourceCategory.SPRITES) {
                VisTextButton prevSpriteBtn = new VisTextButton("Xem 18FPS");
                prevSpriteBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        event.stop();
                        host.onPreviewSpriteRequested(entry.containerDir);
                    }
                });
                card.add(prevSpriteBtn).height(19f).padTop(2f).row();
            }

            card.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (hostStage == null) {
                        return;
                    }
                    String catSuggestion = switch (entry.category) {
                        case BUTTONS -> "ui";
                        case SOUNDS -> "audio";
                        case IMAGES -> "background";
                        case SPRITES -> "entity";
                    };
                    ImportAssetDialog dlg = new ImportAssetDialog(
                        entry.previewFile,
                        catSuggestion,
                        assets,
                        importedKey -> {
                            armedKey = importedKey;
                            updateCardSelectionVisuals();
                            host.onArmAsset(importedKey, false);
                            refreshProjectAssets();
                        },
                        host::onStatus
                    );
                    hostStage.addActor(dlg);
                }
            });

            vegasGrid.add(card).width(86f).pad(2f);
            if (++col % 3 == 0) {
                vegasGrid.row();
            }
        }
    }

    @Override
    public void dispose() {
        clearGridDragSources();
        vegasThumbCache.clear();
    }
}
