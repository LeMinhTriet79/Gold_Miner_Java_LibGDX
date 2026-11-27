package com.minhtriet79.goldminer.studio;

/**
 * Callback invoked on the OpenGL render thread when files are dropped onto the Studio window.
 */
@FunctionalInterface
public interface ExternalFileDropListener {
    void onFilesDropped(String[] absolutePaths);
}
