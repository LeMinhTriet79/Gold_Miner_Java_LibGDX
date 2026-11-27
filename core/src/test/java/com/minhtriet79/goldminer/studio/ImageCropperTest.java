package com.minhtriet79.goldminer.studio;

import com.minhtriet79.goldminer.studio.io.ImageCropper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageCropperTest {

    @Test
    void autoCropRemovesTransparentMarginsCleanly(@TempDir Path tempDir) throws Exception {
        File src = tempDir.resolve("raw.png").toFile();
        File dst = tempDir.resolve("cropped.png").toFile();

        BufferedImage img = new BufferedImage(100, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.YELLOW);
        g.fillRect(20, 10, 30, 25);
        g.dispose();
        ImageIO.write(img, "png", src);

        ImageCropper.autoCropTransparent(src, dst);
        BufferedImage out = ImageIO.read(dst);
        assertEquals(30, out.getWidth());
        assertEquals(25, out.getHeight());
    }

    @Test
    void exportAnimationComputesUnionBoundingBoxAcrossAllFrames(@TempDir Path tempDir) throws Exception {
        File f1 = tempDir.resolve("1.png").toFile();
        File f2 = tempDir.resolve("2.png").toFile();
        File outDir = tempDir.resolve("exported").toFile();

        BufferedImage im1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = im1.createGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(10, 20, 20, 30); // [10..30, 20..50]
        g1.dispose();
        ImageIO.write(im1, "png", f1);

        BufferedImage im2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = im2.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(25, 10, 25, 45); // [25..50, 10..55] -> union = [10..50, 10..55] = 40x45
        g2.dispose();
        ImageIO.write(im2, "png", f2);

        ImageCropper.AnimationExportResult res = ImageCropper.exportAnimationUnionCrop(
            List.of(f1, f2),
            outDir,
            "miner_test"
        );
        assertEquals(40, res.unionWidth);
        assertEquals(45, res.unionHeight);
        assertEquals(2, res.frameCount);

        BufferedImage out1 = ImageIO.read(res.outputFiles.get(0));
        BufferedImage out2 = ImageIO.read(res.outputFiles.get(1));
        assertEquals(40, out1.getWidth());
        assertEquals(45, out1.getHeight());
        assertEquals(40, out2.getWidth());
        assertEquals(45, out2.getHeight());
    }
}
