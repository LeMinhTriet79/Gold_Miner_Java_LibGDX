package com.minhtriet79.goldminer.persistence;

import com.minhtriet79.goldminer.domain.account.AccountDatabase;
import com.minhtriet79.goldminer.domain.account.AccountRecord;
import com.minhtriet79.goldminer.domain.account.AccountService;
import com.minhtriet79.goldminer.domain.account.SessionState;
import com.minhtriet79.goldminer.domain.settlement.SettlementSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalJsonAccountStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsEmptyDefaultsWhenFilesMissingAndRecoversFromMalformedJson() throws IOException {
        LocalJsonAccountStore store = new LocalJsonAccountStore(tempDir);

        AccountDatabase emptyDb = store.loadAccounts();
        assertNotNull(emptyDb.accounts);
        assertTrue(emptyDb.accounts.isEmpty());

        SessionState emptySession = store.loadSession();
        assertFalse(emptySession.autoLogin);
        assertEquals("", emptySession.lastUsername);

        Files.writeString(store.getAccountsPath(), "{not-valid-json", StandardCharsets.UTF_8);
        Files.writeString(store.getSessionPath(), "{{broken", StandardCharsets.UTF_8);

        assertTrue(store.loadAccounts().accounts.isEmpty());
        assertFalse(store.loadSession().autoLogin);
    }

    @Test
    void persistsUtf8AccountsAndSessionAcrossRestartLifecycle() {
        LocalJsonAccountStore store = new LocalJsonAccountStore(tempDir);
        AccountService firstRun = new AccountService(store);

        assertNull(firstRun.tryAutoLogin());
        assertTrue(firstRun.register("triet79", "Minh Triết", "gold9999", "gold9999", true).success);
        firstRun.recordSettlement(new SettlementSummary(850, 2, false, 85, 50));

        assertTrue(Files.isRegularFile(store.getAccountsPath()));
        assertTrue(Files.isRegularFile(store.getSessionPath()));

        AccountService secondRun = new AccountService(new LocalJsonAccountStore(tempDir));
        AccountRecord autoLogged = secondRun.tryAutoLogin();
        assertNotNull(autoLogged);
        assertEquals("triet79", autoLogged.username);
        assertEquals("Minh Triết", autoLogged.displayName);
        assertEquals(135, autoLogged.coins);
        assertEquals(2, autoLogged.highestClearedLevel);

        secondRun.logout();
        AccountService thirdRun = new AccountService(new LocalJsonAccountStore(tempDir));
        assertNull(thirdRun.tryAutoLogin());
    }

    @Test
    void persistsMapProgressionCoinsAndUnlockAllAcrossRestarts() {
        LocalJsonAccountStore store = new LocalJsonAccountStore(tempDir);
        AccountService firstRun = new AccountService(store);
        assertTrue(firstRun.register("outback1", "Outback", "1234", "1234", true).success);

        assertEquals(115, firstRun.recordLevelWin(2, 1150));
        firstRun.unlockAllLevels();

        AccountService restarted = new AccountService(new LocalJsonAccountStore(tempDir));
        AccountRecord rec = restarted.tryAutoLogin();
        assertNotNull(rec);
        assertEquals(115, rec.coins);
        assertEquals(2, rec.highestClearedLevel);
        assertEquals(6, rec.maxUnlockedLevel);
        assertTrue(restarted.isLevelCleared(2));
        assertFalse(restarted.isLevelCleared(3));
        assertTrue(restarted.isLevelUnlocked(6));
    }
}
