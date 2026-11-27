package com.minhtriet79.goldminer.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.minhtriet79.goldminer.GoldMinerGame;
import org.lwjgl.glfw.GLFW;

/**
 * Launches the desktop Gold Miner Studio mini-engine (1280x720 resizable editor)
 * with OS drag-and-drop support via {@link Lwjgl3WindowAdapter#filesDropped(String[])}.
 */
public class StudioLauncher {

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }

        if (UIUtils.isLinux) {
            String display = System.getenv("DISPLAY");
            if (display != null && !display.isEmpty()) {
                GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_X11);
            }
        }

        final GoldMinerGame game = new GoldMinerGame(true);

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Gold Miner Studio — 2D Level, UI & Sprite Engine");
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        configuration.setWindowedMode(1280, 720);
        configuration.setResizable(true);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        configuration.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void filesDropped(String[] files) {
                if (files == null || files.length == 0 || Gdx.app == null) {
                    return;
                }
                final String[] copy = files.clone();
                Gdx.app.postRunnable(() -> game.onExternalFilesDropped(copy));
            }
        });

        new Lwjgl3Application(game, configuration);
    }
}
