package com.minhtriet79.goldminer.studio.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Lazy-paginated scanner for the Vegas Source directory ({@code images/}, {@code buttons/},
 * {@code sprites/}, {@code sounds/}). Strict page cap (24 entries) avoids loading 18k+ PNGs into RAM.
 */
public final class VegasSourceScanner {

    public static final String DEFAULT_VEGAS_SOURCE_PATH =
        "C:\\Users\\Minh Triet\\Desktop\\MAVIS-Platform\\Gold_Miner_Vegas_Source";
    public static final int PAGE_SIZE = 24;

    public enum SourceCategory {
        IMAGES("images"),
        BUTTONS("buttons"),
        SPRITES("sprites"),
        SOUNDS("sounds");

        public final String folderName;

        SourceCategory(String folderName) {
            this.folderName = folderName;
        }
    }

    public static final class SourceEntry {
        public final String id;
        public final File previewFile;
        public final File containerDir;
        public final SourceCategory category;

        public SourceEntry(String id, File previewFile, File containerDir, SourceCategory category) {
            this.id = id;
            this.previewFile = previewFile;
            this.containerDir = containerDir;
            this.category = category;
        }
    }

    public static final class PageSlice {
        public final List<SourceEntry> entries;
        public final int pageIndex;
        public final int totalPages;
        public final int totalItems;

        public PageSlice(List<SourceEntry> entries, int pageIndex, int totalPages, int totalItems) {
            this.entries = entries;
            this.pageIndex = pageIndex;
            this.totalPages = totalPages;
            this.totalItems = totalItems;
        }
    }

    private VegasSourceScanner() {
    }

    public static List<SourceEntry> scanCategory(File sourceRoot, SourceCategory category, String query) {
        List<SourceEntry> results = new ArrayList<>();
        if (sourceRoot == null || !sourceRoot.isDirectory()) {
            return results;
        }
        File subDir = new File(sourceRoot, category.folderName);
        if (!subDir.isDirectory()) {
            return results;
        }

        String filter = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        File[] children = subDir.listFiles();
        if (children == null) {
            return results;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));

        for (File child : children) {
            String nameLower = child.getName().toLowerCase(Locale.ROOT);
            if (!filter.isEmpty() && !nameLower.contains(filter)) {
                continue;
            }
            if (category == SourceCategory.IMAGES && child.isFile() && isImageFile(nameLower)) {
                results.add(new SourceEntry(child.getName(), child, subDir, category));
            } else if (category == SourceCategory.SOUNDS && child.isFile() && isAudioFile(nameLower)) {
                results.add(new SourceEntry(child.getName(), child, subDir, category));
            } else if (category == SourceCategory.BUTTONS && child.isDirectory()) {
                File preview = resolveButtonPreview(child);
                if (preview != null) {
                    results.add(new SourceEntry(child.getName(), preview, child, category));
                }
            } else if (category == SourceCategory.SPRITES && child.isDirectory()) {
                File preview = resolveSpriteFirstFrame(child);
                if (preview != null) {
                    results.add(new SourceEntry(child.getName(), preview, child, category));
                }
            }
        }
        return results;
    }

    public static PageSlice paginate(List<SourceEntry> allEntries, int requestedPage, int pageSize) {
        int totalItems = allEntries.size();
        int cap = Math.max(1, pageSize);
        int totalPages = Math.max(1, (totalItems + cap - 1) / cap);
        int page = Math.max(0, Math.min(totalPages - 1, requestedPage));
        int from = page * cap;
        int to = Math.min(totalItems, from + cap);
        List<SourceEntry> slice = from < to ? allEntries.subList(from, to) : List.of();
        return new PageSlice(slice, page, totalPages, totalItems);
    }

    public static List<File> listOrderedSpriteFrames(File spriteDir) {
        List<File> frames = new ArrayList<>();
        if (spriteDir == null || !spriteDir.isDirectory()) {
            return frames;
        }
        File[] pngs = spriteDir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (pngs == null || pngs.length == 0) {
            return frames;
        }
        Arrays.sort(pngs, Comparator.comparingInt(f -> parseLeadingInt(f.getName())));
        frames.addAll(Arrays.asList(pngs));
        return frames;
    }

    private static int parseLeadingInt(String name) {
        String digits = name.replaceAll("\\..*$", "").replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static File resolveButtonPreview(File dir) {
        File f1 = new File(dir, "1_up.png");
        if (f1.isFile()) {
            return f1;
        }
        File[] pngs = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (pngs == null || pngs.length == 0) {
            return null;
        }
        Arrays.sort(pngs, Comparator.comparing(File::getName));
        return pngs[0];
    }

    private static File resolveSpriteFirstFrame(File dir) {
        File f1 = new File(dir, "1.png");
        if (f1.isFile()) {
            return f1;
        }
        List<File> ordered = listOrderedSpriteFrames(dir);
        return ordered.isEmpty() ? null : ordered.get(0);
    }

    private static boolean isImageFile(String lower) {
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static boolean isAudioFile(String lower) {
        return lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".wav");
    }
}
