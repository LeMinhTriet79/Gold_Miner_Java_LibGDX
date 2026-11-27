package com.minhtriet79.goldminer.domain.account;

import java.util.ArrayList;
import java.util.List;

/**
 * Root container serialized to {@code data/accounts.json}.
 */
public class AccountDatabase {

    public int schemaVersion = 1;
    public List<AccountRecord> accounts = new ArrayList<>();

    public AccountDatabase() {
    }
}
