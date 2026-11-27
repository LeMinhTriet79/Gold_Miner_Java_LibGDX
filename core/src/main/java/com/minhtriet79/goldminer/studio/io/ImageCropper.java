package com.minhtriet79.goldminer.studio.io;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java ImageIO / BufferedImage cropping utilities used by Gold Miner Studio.
 * Handles static auto-crop (removing empty transparent borders) and multi-frame union
 * bounding-box export (preventing jitter across animation sequences).
 */
public final class ImageCropper {

    public static final class AnimationExportResult {
        public final int unionWidth;
        public final int unionHeight;
        public final int frameCount;
        public final List<File> outputFiles;

        public AnimationExportResult(int unionWidth, int unionHeight, int frameCount, List<File> outputFiles) {
            this.unionWidth = unionWidth;
            this.unionHeight = unionHeight;
            this.frameCount = frameCount;
            this.outputFiles = outputFiles;
        }
    }

    private ImageCropper() {
    }

    public static int[] findOpaqueBounds(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return null;
        }
        return new int[] {minX, minY, maxX + 1, maxY + 1};
    }

    public static void autoCropTransparent(File srcFile, File destFile) throws IOException {
        BufferedImage raw = ImageIO.read(srcFile);
        if (raw == null) {
            throw new IOException("Unsupported image file: " + srcFile);
        }
        BufferedImage rgba = toArgb(raw);
        int[] bbox = findOpaqueBounds(rgba);
        BufferedImage result = bbox == null
            ? rgba
            : rgba.getSubimage(bbox[0], bbox[1], bbox[2] - bbox[0], bbox[3] - bbox[1]);

        File parent = destFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        ImageIO.write(result, "png", destFile);
    }

    public static void copyFile(File srcFile, File destFile) throws IOException {
        File parent = destFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Computes union bounding box across {@code frameFiles}, crops every frame identically
     * onto a uniform canvas so the character never jitters, and writes {@code <animName>_01.png..NN.png}.
     */
    public static AnimationExportResult exportAnimationUnionCrop(
        List<File> frameFiles,
        File outDir,
        String animName
    ) throws IOException {
        if (frameFiles == null || frameFiles.isEmpty()) {
            throw new IllegalArgumentException("Frame list cannot be empty");
        }
        outDir.mkdirs();

        List<BufferedImage> loaded = new ArrayList<>(frameFiles.size());
        int ux0 = Integer.MAX_VALUE;
        int uy0 = Integer.MAX_VALUE;
        int ux1 = 0;
        int uy1 = 0;

        for (File file : frameFiles) {
            BufferedImage raw = ImageIO.read(file);
            if (raw == null) {
                throw new IOException("Failed to read animation frame: " + file);
            }
            BufferedImage argb = toArgb(raw);
            loaded.add(argb);
            int[] bb = findOpaqueBounds(argb);
            if (bb != null) {
                ux0 = Math.min(ux0, bb[0]);
                uy0 = Math.min(uy0, bb[1]);
                ux1 = Math.max(ux1, bb[2]);
                uy1 = Math.max(uy1, bb[3]);
            }
        }

        if (ux0 >= ux1 || uy0 >= uy1) {
            ux0 = 0;
            uy0 = 0;
            ux1 = loaded.get(0).getWidth();
            uy1 = loaded.get(0).getHeight();
        }

        int unionW = ux1 - ux0;
        int unionH = uy1 - uy0;
        List<File> outFiles = new ArrayList<>(loaded.size());

        for (int i = 0; i < loaded.size(); i++) {
            BufferedImage src = loaded.get(i);
            BufferedImage canvas = new BufferedImage(unionW, unionH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.drawImage(src, -ux0, -uy0, null);
            g.dispose();

            File outFile = new File(outDir, String.format("%s_%02d.png", animName, i + 1));
            ImageIO.write(canvas, "png", outFile);
            outFiles.add(outFile);
        }

        return new AnimationExportResult(unionW, unionH, outFiles.size(), outFiles);
    }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }
}
