package com.minhtriet79.goldminer.studio.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.minhtriet79.goldminer.gameplay.hook.HookConfig;
import com.minhtriet79.goldminer.studio.io.StudioFileHelper;

import java.util.function.Consumer;

/**
 * Live Physics & Rules Editor editing {@code assets/data/hook_config.json}
 * so game designers can balance swing angle, hook speeds, strength curve, and TNT blast radius
 * without touching Java code.
 */
public class PhysicsEditorPanel extends VisTable {

    private final Consumer<String> statusCallback;
    private final Runnable onPlayTestRequested;

    private HookConfig config;

    private final VisTextField swingAngleField = new VisTextField();
    private final VisTextField swingPeriodField = new VisTextField();
    private final VisTextField extendSpeedField = new VisTextField();
    private final VisTextField emptyRetractField = new VisTextField();
    private final VisTextField retractPerStrField = new VisTextField();
    private final VisTextField baseStrengthField = new VisTextField();
    private final VisTextField cartSpeedField = new VisTextField();
    private final VisTextField hookRadiusField = new VisTextField();
    private final VisTextField restLengthField = new VisTextField();
    private final VisTextField maxLengthField = new VisTextField();
    private final VisTextField tntRadiusField = new VisTextField();
    private final VisTextField warnSecField = new VisTextField();
    private final VisTextField defaultDurationField = new VisTextField();
    private final VisTextField startDynamiteField = new VisTextField();

    public PhysicsEditorPanel(Consumer<String> statusCallback, Runnable onPlayTestRequested) {
        this.statusCallback = statusCallback;
        this.onPlayTestRequested = onPlayTestRequested;
        this.config = StudioFileHelper.loadHookConfig();

        pad(16f);
        buildUi();
        populateFields();
    }

    public void reloadFromDisk() {
        this.config = StudioFileHelper.loadHookConfig();
        populateFields();
    }

    private void buildUi() {
        VisTable card = new VisTable();
        card.setBackground("window-bg");
        card.pad(14f);
        card.defaults().pad(5f).left();

        VisLabel header = new VisLabel("CÂN BẰNG VẬT LÝ & LUẬT CHƠI (hook_config.json)");
        header.setColor(Color.GOLD);
        card.add(header).colspan(3).padBottom(8f).row();

        addRow(card, "Góc lắc tối đa (độ):", swingAngleField, "Chuẩn Vegas: 80.0");
        addRow(card, "Chu kỳ lắc (giây):", swingPeriodField, "Chuẩn Vegas: 5.556");
        addRow(card, "Tốc độ phóng dây (u/s):", extendSpeedField, "Chuẩn Vegas: 106.6");
        addRow(card, "Tốc độ thu dây rỗng (u/s):", emptyRetractField, "Chuẩn Vegas: 242.2");
        addRow(card, "Hệ số kéo theo lực (u/s/đơn vị):", retractPerStrField, "v = K * (S - w), chuẩn 15.14");
        addRow(card, "Sức mạnh mặc định (Strength):", baseStrengthField, "Chuẩn: 10 (w >= 10 bị đứt dây)");
        addRow(card, "Tốc độ xe mỏ (u/s):", cartSpeedField, "Chuẩn Vegas: 24.3");
        addRow(card, "Bán kính hitbox đầu ngoàm (u):", hookRadiusField, "Chuẩn điều chỉnh: 5.0");
        addRow(card, "Chiều dài dây nghỉ (u):", restLengthField, "Chuẩn: 12.0");
        addRow(card, "Chiều dài dây tối đa (u):", maxLengthField, "Chuẩn: 270.0");
        addRow(card, "Bán kính nổ TNT (u):", tntRadiusField, "Chuẩn Vegas: 40.5");
        addRow(card, "Cảnh báo đếm ngược (giây cuối):", warnSecField, "Chuẩn Vegas: 5");
        addRow(card, "Thời lượng màn mặc định (giây):", defaultDurationField, "Chuẩn: 60");
        addRow(card, "Số thuốc nổ khởi đầu:", startDynamiteField, "Easy Vegas: 3");

        VisTable btnRow = new VisTable(true);
        VisTextButton saveBtn = new VisTextButton("Lưu hook_config.json");
        saveBtn.setColor(Color.SKY);
        VisTextButton resetBtn = new VisTextButton("Khôi phục Chuẩn Vegas");
        VisTextButton testBtn = new VisTextButton("> Chơi Thử Ngay");
        testBtn.setColor(Color.LIME);

        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveFieldsToDisk();
            }
        });
        resetBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                config = new HookConfig();
                populateFields();
                StudioFileHelper.saveHookConfig(config);
                statusCallback.accept("Đã khôi phục toàn bộ thông số vật lý về chuẩn Gold Miner Vegas.");
            }
        });
        testBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveFieldsToDisk();
                if (onPlayTestRequested != null) {
                    onPlayTestRequested.run();
                }
            }
        });

        btnRow.add(resetBtn);
        btnRow.add(saveBtn);
        btnRow.add(testBtn);
        card.add(btnRow).colspan(3).padTop(12f).right().row();

        VisScrollPane scroll = new VisScrollPane(card);
        scroll.setFadeScrollBars(false);
        add(scroll).center().grow();
    }

    private void addRow(VisTable table, String label, VisTextField field, String hint) {
        table.add(new VisLabel(label)).width(250f);
        table.add(field).width(120f);
        VisLabel hintLbl = new VisLabel(hint);
        hintLbl.setColor(Color.LIGHT_GRAY);
        table.add(hintLbl).width(240f).row();
    }

    private void populateFields() {
        swingAngleField.setText(Float.toString(config.swingMaxAngleDeg));
        swingPeriodField.setText(Float.toString(config.swingPeriodSec));
        extendSpeedField.setText(Float.toString(config.extendSpeed));
        emptyRetractField.setText(Float.toString(config.emptyRetractSpeed));
        retractPerStrField.setText(Float.toString(config.retractSpeedPerStrengthUnit));
        baseStrengthField.setText(Integer.toString(config.baseMinerStrength));
        cartSpeedField.setText(Float.toString(config.cartSpeed));
        hookRadiusField.setText(Float.toString(config.hookTipRadius));
        restLengthField.setText(Float.toString(config.restCableLength));
        maxLengthField.setText(Float.toString(config.maxCableLength));
        tntRadiusField.setText(Float.toString(config.tntBlastRadius));
        warnSecField.setText(Integer.toString(config.countdownWarningSec));
        defaultDurationField.setText(Integer.toString(config.defaultDurationSec));
        startDynamiteField.setText(Integer.toString(config.startingDynamite));
    }

    private void saveFieldsToDisk() {
        config.swingMaxAngleDeg = parseF(swingAngleField, config.swingMaxAngleDeg, 10f, 89f);
        config.swingPeriodSec = parseF(swingPeriodField, config.swingPeriodSec, 1f, 20f);
        config.extendSpeed = parseF(extendSpeedField, config.extendSpeed, 20f, 400f);
        config.emptyRetractSpeed = parseF(emptyRetractField, config.emptyRetractSpeed, 40f, 600f);
        config.retractSpeedPerStrengthUnit = parseF(retractPerStrField, config.retractSpeedPerStrengthUnit, 2f, 60f);
        config.baseMinerStrength = parseI(baseStrengthField, config.baseMinerStrength, 1, 50);
        config.cartSpeed = parseF(cartSpeedField, config.cartSpeed, 5f, 150f);
        config.hookTipRadius = parseF(hookRadiusField, config.hookTipRadius, 1f, 25f);
        config.restCableLength = parseF(restLengthField, config.restCableLength, 4f, 40f);
        config.maxCableLength = parseF(maxLengthField, config.maxCableLength, 80f, 500f);
        config.tntBlastRadius = parseF(tntRadiusField, config.tntBlastRadius, 10f, 120f);
        config.countdownWarningSec = parseI(warnSecField, config.countdownWarningSec, 1, 20);
        config.defaultDurationSec = parseI(defaultDurationField, config.defaultDurationSec, 15, 300);
        config.startingDynamite = parseI(startDynamiteField, config.startingDynamite, 0, 20);

        StudioFileHelper.saveHookConfig(config);
        populateFields();
        statusCallback.accept("Đã lưu hook_config.json! Có hiệu lực ngay khi Chơi thử.");
    }

    private float parseF(VisTextField tf, float fallback, float min, float max) {
        try {
            float v = Float.parseFloat(tf.getText().trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseI(VisTextField tf, int fallback, int min, int max) {
        try {
            int v = Integer.parseInt(tf.getText().trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
