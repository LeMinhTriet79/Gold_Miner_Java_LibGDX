package com.minhtriet79.goldminer.studio.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig;
import com.minhtriet79.goldminer.gameplay.ui.UiLayoutConfig.UiElementDef;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig.ItemSpawn;
import com.minhtriet79.goldminer.studio.StudioMode;
import com.minhtriet79.goldminer.studio.model.EntityPrefabCatalog;
import com.minhtriet79.goldminer.studio.model.LevelValidator;
import com.minhtriet79.goldminer.studio.model.LevelValidator.ValidationIssue;
import com.minhtriet79.goldminer.studio.model.LevelValidator.ValidationReport;
import com.minhtriet79.goldminer.studio.model.StudioMath;

import java.util.Locale;

/**
 * Right Engine Inspector with strict {@code setClip(true)} and constrained flush-left
 * layout (never exceeds scroll width, zero left-clipping), numeric steppers, Enter-key commit,
 * rail range sync, and live {@link LevelValidator} diagnostics.
 */
public class StudioInspectorPanel extends VisTable {

    public interface InspectorHost {
        void onBeforeMutate();
        void onDeleteSelected();
        void onDuplicateSelected();
        void onAdjustZIndex(int delta);
        void onSelectItemFromValidator(int itemIndex);
        void onLevelChanged();
        void onUiChanged();
    }

    private static final float CONTENT_WIDTH = 268f;
    private static final float LABEL_COL_W = 88f;
    private static final float FIELD_COL_W = 166f;

    private final Assets assets;
    private final InspectorHost host;
    private final VisTable contentTable = new VisTable();

    private StudioMode mode = StudioMode.LEVEL_MODE;
    private LevelConfig levelConfig;
    private UiLayoutConfig uiConfig;
    private int selectedIndex = -1;

    // Level Item fields
    private final VisTextField typeField = new VisTextField("");
    private final VisTextField objectNField = new VisTextField("0");
    private final VisTextField assetKeyField = new VisTextField("");
    private final VisTextField xField = new VisTextField("0");
    private final VisTextField yField = new VisTextField("0");
    private final VisTextField valueField = new VisTextField("0");
    private final VisTextField weightField = new VisTextField("0");
    private final VisTextField radiusField = new VisTextField("10");
    private final VisTextField scaleField = new VisTextField("1.0");
    private final VisTextField speedField = new VisTextField("0");
    private final VisTextField zIndexField = new VisTextField("0");
    private final VisCheckBox animatedCheck = new VisCheckBox("Animation chạy ngang", false);

    // Level Global fields & Validator
    private final VisLabel totalValueLabel = new VisLabel("$0");
    private final VisLabel economyRatioLabel = new VisLabel("0.0x");
    private final VisTextField goalField = new VisTextField("555");
    private final VisTextField durationField = new VisTextField("60");
    private final VisTextField trackNField = new VisTextField("1");
    private final VisTextField cartMinField = new VisTextField("12");
    private final VisTextField cartMaxField = new VisTextField("468");
    private final VisTable validatorListTable = new VisTable();

    // UI Element fields
    private final VisTextField uiBgField = new VisTextField("bg_pass_w1");
    private final VisTextField uiIdField = new VisTextField("");
    private final VisSelectBox<String> uiTypeBox = new VisSelectBox<>();
    private final VisTextField uiTexField = new VisTextField("");
    private final VisTextField uiHoverField = new VisTextField("");
    private final VisTextField uiXField = new VisTextField("240");
    private final VisTextField uiYField = new VisTextField("135");
    private final VisTextField uiWField = new VisTextField("100");
    private final VisTextField uiHField = new VisTextField("24");
    private final VisTextField uiScaleXField = new VisTextField("1.0");
    private final VisTextField uiScaleYField = new VisTextField("1.0");
    private final VisTextField uiZField = new VisTextField("0");
    private final VisSelectBox<String> uiActionBox = new VisSelectBox<>();
    private final VisTextField uiLabelField = new VisTextField("");

    public StudioInspectorPanel(Assets assets, InspectorHost host) {
        this.assets = assets;
        this.host = host;

        setClip(true);
        setBackground("window-bg");
        pad(5f);

        uiTypeBox.setItems("IMAGE", "BUTTON");
        uiActionBox.setItems("NONE", "PLAY_GAME", "OPEN_STUDIO", "HIGH_SCORES", "EXTRAS", "HELP", "EXIT");

        attachEnterCommit(true, typeField, objectNField, assetKeyField, xField, yField, valueField, weightField, radiusField, scaleField, speedField, zIndexField);
        attachEnterCommit(false, uiIdField, uiTexField, uiHoverField, uiXField, uiYField, uiWField, uiHField, uiScaleXField, uiScaleYField, uiZField, uiLabelField);

        VisScrollPane scroll = new VisScrollPane(contentTable);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        add(scroll).top().left().grow();
    }

    private void attachEnterCommit(boolean isLevelItem, VisTextField... fields) {
        InputListener enterListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                    if (isLevelItem) {
                        applyLevelItemEdits();
                    } else {
                        applyUiElementEdits();
                    }
                    return true;
                }
                return false;
            }
        };
        for (VisTextField f : fields) {
            f.addListener(enterListener);
        }
    }

    public void bindState(StudioMode mode, LevelConfig levelConfig, UiLayoutConfig uiConfig, int selectedIndex) {
        this.mode = mode;
        this.levelConfig = levelConfig;
        this.uiConfig = uiConfig;
        this.selectedIndex = selectedIndex;
        rebuildForm();
    }

    public void refreshFieldValuesOnly() {
        if (mode == StudioMode.LEVEL_MODE && levelConfig != null) {
            refreshValidatorAndEconomy();
            if (selectedIndex >= 0 && selectedIndex < levelConfig.items.size) {
                populateLevelItemFields(levelConfig.items.get(selectedIndex));
            }
        } else if (mode == StudioMode.UI_MODE && uiConfig != null) {
            if (selectedIndex >= 0 && selectedIndex < uiConfig.elements.size) {
                populateUiElementFields(uiConfig.elements.get(selectedIndex));
            }
        }
    }

    private void rebuildForm() {
        contentTable.clearChildren();
        contentTable.top().left().pad(2f).defaults().pad(2f).left();

        if (mode == StudioMode.LEVEL_MODE && levelConfig != null) {
            buildLevelInspector();
        } else if (mode == StudioMode.UI_MODE && uiConfig != null) {
            buildUiInspector();
        } else {
            VisLabel info = new VisLabel("Chọn Thiết kế Màn hoặc UI để chỉnh thông số.", "small");
            info.setWrap(true);
            contentTable.add(info).width(CONTENT_WIDTH).left().row();
        }
    }

    private void buildLevelInspector() {
        VisLabel title = new VisLabel("THUỘC TÍNH VẬT THỂ");
        title.setColor(Color.GOLD);
        title.setWrap(true);
        contentTable.add(title).colspan(2).width(CONTENT_WIDTH).left().row();

        if (selectedIndex >= 0 && selectedIndex < levelConfig.items.size) {
            ItemSpawn item = levelConfig.items.get(selectedIndex);
            populateLevelItemFields(item);

            addFieldRow("type:", typeField);
            addFieldRow("objectN:", objectNField);
            addFieldRow("assetKey:", assetKeyField);
            addStepperRow("X (0..480):", xField, -5f, 5f, false);
            addStepperRow("Y (0..270):", yField, -5f, 5f, false);
            addStepperRow("value ($):", valueField, -50f, 50f, true);
            addStepperRow("weight:", weightField, -1f, 1f, true);
            addStepperRow("radius:", radiusField, -1f, 1f, false);
            addStepperRow("scale:", scaleField, -0.1f, 0.1f, false);
            addFieldRow("speed:", speedField);
            addStepperRow("zIndex:", zIndexField, -1f, 1f, true);
            contentTable.add(animatedCheck).colspan(2).width(CONTENT_WIDTH).left().row();

            VisTable btnRow1 = new VisTable(true);
            VisTextButton applyBtn = new VisTextButton("Áp dụng");
            applyBtn.setColor(Color.LIME);
            VisTextButton dupBtn = new VisTextButton("Nhân đôi");
            VisTextButton delBtn = new VisTextButton("Xóa");
            delBtn.setColor(Color.SALMON);
            applyBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    applyLevelItemEdits();
                }
            });
            dupBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    host.onDuplicateSelected();
                }
            });
            delBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    host.onDeleteSelected();
                }
            });
            btnRow1.add(applyBtn).growX();
            btnRow1.add(dupBtn).growX();
            btnRow1.add(delBtn);
            contentTable.add(btnRow1).colspan(2).width(CONTENT_WIDTH).padBottom(6f).row();
        } else {
            VisLabel noneLbl = new VisLabel("Chưa chọn vật thể. Kéo từ Kho Prefab hoặc click trực tiếp lên sân khấu.", "small");
            noneLbl.setColor(Color.LIGHT_GRAY);
            noneLbl.setWrap(true);
            contentTable.add(noneLbl).colspan(2).width(CONTENT_WIDTH).left().padBottom(6f).row();
        }

        // Level Globals
        VisLabel globalTitle = new VisLabel("CẤU HÌNH TOÀN MÀN");
        globalTitle.setColor(Color.CYAN);
        globalTitle.setWrap(true);
        contentTable.add(globalTitle).colspan(2).width(CONTENT_WIDTH).left().padTop(4f).row();

        goalField.setText(Integer.toString(levelConfig.goal));
        durationField.setText(Integer.toString(levelConfig.durationSeconds));
        trackNField.setText(Integer.toString(levelConfig.trackN));
        cartMinField.setText(Float.toString(levelConfig.cartRange != null ? levelConfig.cartRange.minX : 12f));
        cartMaxField.setText(Float.toString(levelConfig.cartRange != null ? levelConfig.cartRange.maxX : 468f));

        contentTable.add(new VisLabel("Tổng giá trị:", "small")).width(LABEL_COL_W).left();
        contentTable.add(totalValueLabel).width(FIELD_COL_W).left().row();
        contentTable.add(new VisLabel("Tỉ lệ Kinh tế:", "small")).width(LABEL_COL_W).left();
        contentTable.add(economyRatioLabel).width(FIELD_COL_W).left().row();

        addFieldRow("Goal ($):", goalField);
        VisTextButton autoGoalBtn = new VisTextButton("Tự tính 40%");
        autoGoalBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                host.onBeforeMutate();
                ValidationReport rep = LevelValidator.validate(levelConfig, null);
                int auto = StudioMath.computeAutoGoal(rep.totalValue);
                levelConfig.levelValue = rep.totalValue;
                levelConfig.goal = auto;
                goalField.setText(Integer.toString(auto));
                refreshValidatorAndEconomy();
                host.onLevelChanged();
            }
        });
        contentTable.add(new VisLabel("")).width(LABEL_COL_W);
        contentTable.add(autoGoalBtn).left().row();

        addFieldRow("Thời gian (s):", durationField);
        addFieldRow("Đường ray:", trackNField);
        VisTextButton syncTrackBtn = new VisTextButton("Khớp biên Ray");
        syncTrackBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int tn = Math.max(1, Math.min(9, parseI(trackNField.getText(), levelConfig.trackN)));
                float[] range = EntityPrefabCatalog.getTrackRange(tn);
                trackNField.setText(Integer.toString(tn));
                cartMinField.setText(Float.toString(range[0]));
                cartMaxField.setText(Float.toString(range[1]));
                applyLevelGlobalEdits();
            }
        });
        contentTable.add(new VisLabel("")).width(LABEL_COL_W);
        contentTable.add(syncTrackBtn).left().row();

        addFieldRow("cartMinX:", cartMinField);
        addFieldRow("cartMaxX:", cartMaxField);

        VisTextButton applyGlobalBtn = new VisTextButton("Lưu thông số Màn");
        applyGlobalBtn.setColor(Color.SKY);
        applyGlobalBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyLevelGlobalEdits();
            }
        });
        contentTable.add(applyGlobalBtn).colspan(2).width(CONTENT_WIDTH).padTop(4f).padBottom(6f).row();

        // Live Validator
        VisLabel valHeader = new VisLabel("KIỂM TRA HỢP LỆ");
        valHeader.setColor(Color.ORANGE);
        valHeader.setWrap(true);
        contentTable.add(valHeader).colspan(2).width(CONTENT_WIDTH).left().row();
        contentTable.add(validatorListTable).colspan(2).width(CONTENT_WIDTH).left().row();
        refreshValidatorAndEconomy();
    }

    private void buildUiInspector() {
        VisLabel title = new VisLabel("THUỘC TÍNH UI / MENU");
        title.setColor(Color.GOLD);
        title.setWrap(true);
        contentTable.add(title).colspan(2).width(CONTENT_WIDTH).left().row();

        uiBgField.setText(uiConfig.backgroundKey != null ? uiConfig.backgroundKey : "bg_pass_w1");
        addFieldRow("Background:", uiBgField);
        VisTextButton applyBgBtn = new VisTextButton("Đổi Background");
        applyBgBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                host.onBeforeMutate();
                uiConfig.backgroundKey = uiBgField.getText().trim();
                host.onUiChanged();
            }
        });
        contentTable.add(applyBgBtn).colspan(2).width(CONTENT_WIDTH).padBottom(8f).row();

        if (selectedIndex >= 0 && selectedIndex < uiConfig.elements.size) {
            populateUiElementFields(uiConfig.elements.get(selectedIndex));
            addFieldRow("id:", uiIdField);
            addFieldRow("type:", uiTypeBox);
            addFieldRow("texture:", uiTexField);
            addFieldRow("hoverTex:", uiHoverField);
            addFieldRow("X:", uiXField);
            addFieldRow("Y:", uiYField);
            addFieldRow("width:", uiWField);
            addFieldRow("height:", uiHField);
            addFieldRow("scaleX:", uiScaleXField);
            addFieldRow("scaleY:", uiScaleYField);
            addFieldRow("zIndex:", uiZField);
            addFieldRow("action:", uiActionBox);
            addFieldRow("label:", uiLabelField);

            VisTable row = new VisTable(true);
            VisTextButton applyBtn = new VisTextButton("Áp dụng");
            applyBtn.setColor(Color.LIME);
            VisTextButton dupBtn = new VisTextButton("Nhân đôi");
            VisTextButton delBtn = new VisTextButton("Xóa");
            delBtn.setColor(Color.SALMON);
            applyBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    applyUiElementEdits();
                }
            });
            dupBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    host.onDuplicateSelected();
                }
            });
            delBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    host.onDeleteSelected();
                }
            });
            row.add(applyBtn).growX();
            row.add(dupBtn).growX();
            row.add(delBtn);
            contentTable.add(row).colspan(2).width(CONTENT_WIDTH).padTop(4f).row();
        } else {
            VisLabel none = new VisLabel("Click 1 nút/ảnh UI hoặc kéo từ Kho Prefab.", "small");
            none.setColor(Color.LIGHT_GRAY);
            none.setWrap(true);
            contentTable.add(none).colspan(2).width(CONTENT_WIDTH).left().row();
        }
    }

    private void addFieldRow(String label, Actor field) {
        VisLabel l = new VisLabel(label, "small");
        l.setEllipsis(true);
        contentTable.add(l).width(LABEL_COL_W).left();
        contentTable.add(field).width(FIELD_COL_W).left().row();
    }

    private void addStepperRow(String label, VisTextField field, float stepMinus, float stepPlus, boolean integer) {
        VisLabel l = new VisLabel(label, "small");
        l.setEllipsis(true);
        contentTable.add(l).width(LABEL_COL_W).left();

        VisTable group = new VisTable(true);
        VisTextButton minusBtn = new VisTextButton("-");
        VisTextButton plusBtn = new VisTextButton("+");
        minusBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                nudgeField(field, stepMinus, integer);
                applyLevelItemEdits();
            }
        });
        plusBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                nudgeField(field, stepPlus, integer);
                applyLevelItemEdits();
            }
        });
        group.add(minusBtn).width(22f);
        group.add(field).width(114f);
        group.add(plusBtn).width(22f);
        contentTable.add(group).width(FIELD_COL_W).left().row();
    }

    private void nudgeField(VisTextField field, float delta, boolean integer) {
        float cur = parseF(field.getText(), 0f);
        float next = cur + delta;
        if (integer) {
            field.setText(Integer.toString(Math.round(next)));
        } else {
            field.setText(Float.toString(Math.round(next * 100f) / 100f));
        }
    }

    private void refreshValidatorAndEconomy() {
        if (levelConfig == null) {
            return;
        }
        ValidationReport report = LevelValidator.validate(
            levelConfig,
            key -> assets.hasTexture(key) || assets.hasAnimation(key)
        );
        levelConfig.levelValue = report.totalValue;
        totalValueLabel.setText("$" + report.totalValue);
        totalValueLabel.setColor(Color.GOLD);

        economyRatioLabel.setText(String.format(Locale.ROOT, "%.2fx", report.economyRatio));
        economyRatioLabel.setColor(report.economyRatio >= 1.6f ? Color.LIME : (report.economyRatio >= 1.2f ? Color.ORANGE : Color.SALMON));

        validatorListTable.clearChildren();
        validatorListTable.left();
        if (report.issues.isEmpty()) {
            VisLabel ok = new VisLabel("[OK] Màn chơi cân bằng & hợp lệ.", "small");
            ok.setColor(Color.LIME);
            ok.setWrap(true);
            validatorListTable.add(ok).width(CONTENT_WIDTH - 4f).left().row();
            return;
        }
        for (ValidationIssue issue : report.issues) {
            VisLabel lbl = new VisLabel("- " + issue.message, "small");
            lbl.setWrap(true);
            Color c = switch (issue.severity) {
                case ERROR -> Color.SALMON;
                case WARNING -> Color.ORANGE;
                case INFO -> Color.SKY;
            };
            lbl.setColor(c);
            if (issue.itemIndex >= 0) {
                lbl.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        host.onSelectItemFromValidator(issue.itemIndex);
                    }
                });
            }
            validatorListTable.add(lbl).width(CONTENT_WIDTH - 6f).left().padBottom(3f).row();
        }
    }

    private void populateLevelItemFields(ItemSpawn item) {
        typeField.setText(item.type != null ? item.type : "");
        objectNField.setText(Integer.toString(item.objectN));
        assetKeyField.setText(item.assetKey != null ? item.assetKey : "");
        xField.setText(Float.toString(item.x));
        yField.setText(Float.toString(item.y));
        valueField.setText(Integer.toString(item.value));
        weightField.setText(Integer.toString(item.weight));
        radiusField.setText(Float.toString(item.radius));
        scaleField.setText(Float.toString(item.scale > 0f ? item.scale : 1f));
        speedField.setText(Float.toString(item.moveSpeed));
        zIndexField.setText(Integer.toString(item.zIndex));
        animatedCheck.setChecked(item.animated);
    }

    private void populateUiElementFields(UiElementDef el) {
        uiIdField.setText(el.id != null ? el.id : "");
        uiTypeBox.setSelected("IMAGE".equalsIgnoreCase(el.type) ? "IMAGE" : "BUTTON");
        uiTexField.setText(el.texture != null ? el.texture : "");
        uiHoverField.setText(el.hoverTexture != null ? el.hoverTexture : "");
        uiXField.setText(Float.toString(el.x));
        uiYField.setText(Float.toString(el.y));
        uiWField.setText(Float.toString(el.width));
        uiHField.setText(Float.toString(el.height));
        uiScaleXField.setText(Float.toString(el.scaleX));
        uiScaleYField.setText(Float.toString(el.scaleY));
        uiZField.setText(Integer.toString(el.zIndex));
        uiActionBox.setSelected(el.action != null ? el.action : "NONE");
        uiLabelField.setText(el.label != null ? el.label : "");
    }

    private void applyLevelItemEdits() {
        if (levelConfig == null || selectedIndex < 0 || selectedIndex >= levelConfig.items.size) {
            return;
        }
        host.onBeforeMutate();
        ItemSpawn item = levelConfig.items.get(selectedIndex);
        item.type = typeField.getText().trim();
        item.objectN = parseI(objectNField.getText(), item.objectN);
        item.assetKey = assetKeyField.getText().trim();
        item.x = parseF(xField.getText(), item.x);
        item.y = parseF(yField.getText(), item.y);
        item.value = parseI(valueField.getText(), item.value);
        item.weight = parseI(weightField.getText(), item.weight);
        item.radius = Math.max(2f, parseF(radiusField.getText(), item.radius));
        item.scale = Math.max(0.25f, Math.min(4f, parseF(scaleField.getText(), 1f)));
        item.moveSpeed = Math.max(0f, parseF(speedField.getText(), item.moveSpeed));
        item.zIndex = parseI(zIndexField.getText(), item.zIndex);
        item.animated = animatedCheck.isChecked();
        refreshValidatorAndEconomy();
        host.onLevelChanged();
    }

    private void applyLevelGlobalEdits() {
        if (levelConfig == null) {
            return;
        }
        host.onBeforeMutate();
        levelConfig.goal = Math.max(50, parseI(goalField.getText(), levelConfig.goal));
        levelConfig.durationSeconds = Math.max(10, parseI(durationField.getText(), levelConfig.durationSeconds));
        levelConfig.trackN = Math.max(1, Math.min(9, parseI(trackNField.getText(), levelConfig.trackN)));
        if (levelConfig.cartRange == null) {
            levelConfig.cartRange = new LevelConfig.CartRange();
        }
        levelConfig.cartRange.minX = parseF(cartMinField.getText(), levelConfig.cartRange.minX);
        levelConfig.cartRange.maxX = parseF(cartMaxField.getText(), levelConfig.cartRange.maxX);
        refreshValidatorAndEconomy();
        host.onLevelChanged();
    }

    private void applyUiElementEdits() {
        if (uiConfig == null || selectedIndex < 0 || selectedIndex >= uiConfig.elements.size) {
            return;
        }
        host.onBeforeMutate();
        UiElementDef el = uiConfig.elements.get(selectedIndex);
        el.id = uiIdField.getText().trim();
        el.type = uiTypeBox.getSelected();
        el.texture = uiTexField.getText().trim();
        el.hoverTexture = uiHoverField.getText().trim();
        el.x = parseF(uiXField.getText(), el.x);
        el.y = parseF(uiYField.getText(), el.y);
        el.width = Math.max(8f, parseF(uiWField.getText(), el.width));
        el.height = Math.max(8f, parseF(uiHField.getText(), el.height));
        el.scaleX = Math.max(0.2f, Math.min(4f, parseF(uiScaleXField.getText(), el.scaleX)));
        el.scaleY = Math.max(0.2f, Math.min(4f, parseF(uiScaleYField.getText(), el.scaleY)));
        el.zIndex = parseI(uiZField.getText(), el.zIndex);
        el.action = uiActionBox.getSelected();
        el.label = uiLabelField.getText();
        host.onUiChanged();
    }

    private int parseI(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private float parseF(String s, float fallback) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
