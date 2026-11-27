package com.minhtriet79.goldminer.persistence;

/**
 * Local player progression and persistent Official Currency (Xu) wallet.
 */
public class SaveData {

    public int saveVersion = 1;
    public int officialCoins = 0;
    public int highestClearedLevel = 0;
    public int bestMineMoney = 0;
    public int totalRunsCompleted = 0;

    public SaveData() {
    }
}
