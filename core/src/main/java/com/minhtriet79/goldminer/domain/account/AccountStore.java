package com.minhtriet79.goldminer.domain.account;

/**
 * Persistence port for offline accounts and session auto-login state.
 */
public interface AccountStore {

    AccountDatabase loadAccounts();

    void saveAccounts(AccountDatabase database);

    SessionState loadSession();

    void saveSession(SessionState session);
}
