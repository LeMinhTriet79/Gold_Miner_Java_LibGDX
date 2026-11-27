package com.minhtriet79.goldminer.studio.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.minhtriet79.goldminer.assets.Assets;
import com.minhtriet79.goldminer.studio.io.StudioFileHelper;

import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Clean Vietnamese 1-click import modal for PNG/JPG/MP3/OGG files
 * (from Vegas Importer, FileChooser, or OS drag-and-drop).
 */
public class ImportAssetDialog extends VisWindow {

    public ImportAssetDialog(
        File sourceFile,
        String suggestedCategory,
        Assets assets,
        Consumer<String> onImportedCallback,
        Consumer<String> onStatusMessage
    ) {
        super("Nhập Tài Nguyên Vào Dự Án");
        setModal(true);
        addCloseButton();

        String lower = sourceFile.getName().toLowerCase(Locale.ROOT);
        boolean isAudio = lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".wav");

        String initialKey = StudioFileHelper.sanitizeKey(
            sourceFile.getParentFile() != null && "1.png".equals(sourceFile.getName())
                ? sourceFile.getParentFile().getName()
                : sourceFile.getName()
        );

        VisTextField keyField = new VisTextField(initialKey);
        VisSelectBox<String> categoryBox = new VisSelectBox<>();
        categoryBox.setItems("entity", "ui", "background", "audio");
        if (isAudio) {
            categoryBox.setSelected("audio");
        } else if (suggestedCategory != null) {
            categoryBox.setSelected(suggestedCategory);
        }

        VisCheckBox autoCropCheck = new VisCheckBox("Tự cắt viền trong suốt (bỏ tích nếu là frame anim)", !isAudio);
        if (isAudio) {
            autoCropCheck.setDisabled(true);
        }

        defaults().pad(5f).left();
        add(new VisLabel("File nguồn: " + sourceFile.getName())).colspan(2).row();
        add(new VisLabel("Semantic Key:"));
        add(keyField).width(235f).row();
        add(new VisLabel("Phân loại:"));
        add(categoryBox).width(235f).row();
        add(autoCropCheck).colspan(2).row();

        VisTextButton importBtn = new VisTextButton("Nhập & Dùng Liền");
        importBtn.setColor(Color.LIME);
        VisTextButton cancelBtn = new VisTextButton("Hủy");

        importBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    String finalKey = StudioFileHelper.importAssetFile(
                        sourceFile,
                        keyField.getText(),
                        categoryBox.getSelected(),
                        autoCropCheck.isChecked() && !autoCropCheck.isDisabled(),
                        assets
                    );
                    if (onStatusMessage != null) {
                        onStatusMessage.accept("Đã nhập '" + finalKey + "' vào imported/" + categoryBox.getSelected());
                    }
                    if (onImportedCallback != null) {
                        onImportedCallback.accept(finalKey);
                    }
                    close();
                } catch (Exception ex) {
                    if (onStatusMessage != null) {
                        onStatusMessage.accept("Lỗi nhập file: " + ex.getMessage());
                    }
                }
            }
        });

        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                close();
            }
        });

        add(cancelBtn).right();
        add(importBtn).left().row();
        pack();
        centerWindow();
    }
}
