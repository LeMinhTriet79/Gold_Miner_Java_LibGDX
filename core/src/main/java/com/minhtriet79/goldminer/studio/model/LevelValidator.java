package com.minhtriet79.goldminer.studio.model;

import com.minhtriet79.goldminer.gameplay.world.LevelConfig;
import com.minhtriet79.goldminer.gameplay.world.LevelConfig.ItemSpawn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Pure-Java live level validator catching impossible economies, out-of-bounds spawns,
 * overlapping items, missing asset keys, and TNT collateral blasts inside Studio.
 */
public final class LevelValidator {

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    public static final class ValidationIssue {
        public final Severity severity;
        public final String message;
        public final int itemIndex;

        public ValidationIssue(Severity severity, String message, int itemIndex) {
            this.severity = severity;
            this.message = message;
            this.itemIndex = itemIndex;
        }
    }

    public static final class ValidationReport {
        public final int totalValue;
        public final float economyRatio;
        public final List<ValidationIssue> issues;

        public ValidationReport(int totalValue, float economyRatio, List<ValidationIssue> issues) {
            this.totalValue = totalValue;
            this.economyRatio = economyRatio;
            this.issues = issues;
        }

        public boolean hasErrors() {
            for (ValidationIssue i : issues) {
                if (i.severity == Severity.ERROR) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final float TNT_BLAST_RADIUS = 40.5f;

    private LevelValidator() {
    }

    public static ValidationReport validate(LevelConfig level, Predicate<String> assetExists) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (level == null || level.items == null) {
            issues.add(new ValidationIssue(Severity.ERROR, "Chưa có dữ liệu màn chơi.", -1));
            return new ValidationReport(0, 0f, issues);
        }

        int totalValue = 0;
        for (int i = 0; i < level.items.size; i++) {
            ItemSpawn it = level.items.get(i);
            totalValue += Math.max(0, it.value);

            float sc = it.scale > 0f ? it.scale : 1f;
            float r = it.radius * sc;

            if (it.x < 8f || it.x > 472f || it.y < 8f || it.y > 186f) {
                issues.add(new ValidationIssue(
                    Severity.WARNING,
                    "#" + (i + 1) + " (" + it.type + ") nằm ngoài vùng lòng đất [8..472, 8..186].",
                    i
                ));
            }

            if (assetExists != null && (it.assetKey == null || !assetExists.test(it.assetKey))) {
                issues.add(new ValidationIssue(
                    Severity.ERROR,
                    "#" + (i + 1) + " thiếu assetKey '" + it.assetKey + "'.",
                    i
                ));
            }

            if (it.objectN == 12 || "tnt_barrel".equals(it.type)) {
                int blastCount = countTntNeighbors(level, i, it);
                if (blastCount > 0) {
                    issues.add(new ValidationIssue(
                        Severity.INFO,
                        "TNT #" + (i + 1) + " sẽ phá " + blastCount + " vật trong bán kính 40.5u.",
                        i
                    ));
                }
            }

            for (int j = i + 1; j < level.items.size; j++) {
                ItemSpawn other = level.items.get(j);
                if (it.moveSpeed > 0f || other.moveSpeed > 0f) {
                    continue;
                }
                float osc = other.scale > 0f ? other.scale : 1f;
                float or = other.radius * osc;
                float dx = other.x - it.x;
                float dy = other.y - it.y;
                float minAllowed = (r + or) * 0.45f;
                if (dx * dx + dy * dy < minAllowed * minAllowed) {
                    issues.add(new ValidationIssue(
                        Severity.WARNING,
                        "Vật #" + (i + 1) + " & #" + (j + 1) + " bị chồng tâm lên nhau.",
                        i
                    ));
                }
            }
        }

        if (level.cartRange == null || level.cartRange.minX >= level.cartRange.maxX) {
            issues.add(new ValidationIssue(Severity.ERROR, "Khoảng di chuyển cartRange không hợp lệ (minX >= maxX).", -1));
        }
        if (level.durationSeconds < 15) {
            issues.add(new ValidationIssue(Severity.WARNING, "Thời gian màn quá ngắn (< 15s).", -1));
        }

        float ratio = level.goal > 0 ? (float) totalValue / level.goal : 0f;
        if (level.goal <= 0) {
            issues.add(new ValidationIssue(Severity.ERROR, "Goal phải lớn hơn 0.", -1));
        } else if (ratio < 1.2f) {
            issues.add(new ValidationIssue(
                Severity.ERROR,
                String.format("Kinh tế quá gắt: Tổng $%d / Goal $%d (%.2fx < 1.2x).", totalValue, level.goal, ratio),
                -1
            ));
        } else if (ratio < 1.6f) {
            issues.add(new ValidationIssue(
                Severity.WARNING,
                String.format("Kinh tế hơi hẹp: Tổng $%d / Goal $%d (%.2fx, nên >= 1.6x).", totalValue, level.goal, ratio),
                -1
            ));
        }

        if (level.items.size == 0) {
            issues.add(new ValidationIssue(Severity.WARNING, "Màn chơi hiện chưa có vật thể nào.", -1));
        }

        return new ValidationReport(totalValue, ratio, issues);
    }

    private static int countTntNeighbors(LevelConfig level, int selfIdx, ItemSpawn tnt) {
        int count = 0;
        for (int j = 0; j < level.items.size; j++) {
            if (j == selfIdx) {
                continue;
            }
            ItemSpawn target = level.items.get(j);
            float tsc = target.scale > 0f ? target.scale : 1f;
            float sum = TNT_BLAST_RADIUS + target.radius * tsc;
            float dx = target.x - tnt.x;
            float dy = target.y - tnt.y;
            if (dx * dx + dy * dy <= sum * sum) {
                count++;
            }
        }
        return count;
    }
}
