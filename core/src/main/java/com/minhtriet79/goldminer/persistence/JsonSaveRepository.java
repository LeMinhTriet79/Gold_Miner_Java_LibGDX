package com.minhtriet79.goldminer.persistence;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.minhtriet79.goldminer.domain.settlement.SettlementSummary;

/**
 * Persists {@link SaveData} to a local JSON file with safe fallback if missing or malformed.
 */
public class JsonSaveRepository {

    private static final String SAVE_FILE_NAME = "gold_miner_save.json";
    private final Json json = new Json();

    public SaveData load() {
        FileHandle handle = Gdx.files.local(SAVE_FILE_NAME);
        if (!handle.exists()) {
            return new SaveData();
        }
        try {
            SaveData loaded = json.fromJson(SaveData.class, handle);
            return loaded != null ? loaded : new SaveData();
        } catch (Exception ignored) {
            return new SaveData();
        }
    }

    public void save(SaveData data) {
        if (data == null) {
            return;
        }
        FileHandle handle = Gdx.files.local(SAVE_FILE_NAME);
        handle.writeString(json.prettyPrint(data), false, "UTF-8");
    }

    public SaveData recordSettlement(SaveData current, SettlementSummary summary) {
        SaveData data = current != null ? current : load();
        if (summary.totalAwardedCoins > 0) {
            data.officialCoins += summary.totalAwardedCoins;
        }
        data.highestClearedLevel = Math.max(data.highestClearedLevel, summary.levelsCleared);
        data.bestMineMoney = Math.max(data.bestMineMoney, summary.finalMineMoney);
        if (summary.completedWorld1) {
            data.totalRunsCompleted++;
        }
        save(data);
        return data;
    }
}
