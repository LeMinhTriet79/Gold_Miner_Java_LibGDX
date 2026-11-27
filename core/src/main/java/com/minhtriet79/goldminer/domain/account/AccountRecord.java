package com.minhtriet79.goldminer.domain.account;

/**
 * Persistent offline player account record stored in {@code data/accounts.json}.
 */
public class AccountRecord {

    public String username = "";
    public String displayName = "";
    public String passwordHash = "";
    public int coins = 0;
    public int bestMineMoney = 0;
    public int highestClearedLevel = 0;
    public int maxUnlockedLevel = 1;
    public int totalRunsCompleted = 0;
    public long createdAtEpochMs = 0L;

    public AccountRecord() {
    }

    public AccountRecord copy() {
        AccountRecord copy = new AccountRecord();
        copy.username = this.username;
        copy.displayName = this.displayName;
        copy.passwordHash = this.passwordHash;
        copy.coins = this.coins;
        copy.bestMineMoney = this.bestMineMoney;
        copy.highestClearedLevel = this.highestClearedLevel;
        copy.maxUnlockedLevel = this.maxUnlockedLevel;
        copy.totalRunsCompleted = this.totalRunsCompleted;
        copy.createdAtEpochMs = this.createdAtEpochMs;
        return copy;
    }
}
