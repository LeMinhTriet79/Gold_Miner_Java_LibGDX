package com.minhtriet79.goldminer.domain.account;

import com.minhtriet79.goldminer.domain.settlement.SettlementSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {

    private InMemoryAccountStore store;
    private AccountService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryAccountStore();
        service = new AccountService(store);
    }

    @Test
    void firstLaunchWithNoSessionDoesNotAutoLogin() {
        assertNull(service.tryAutoLogin());
        assertFalse(service.isLoggedIn());
        assertNull(service.getActiveAccount());
    }

    @Test
    void registerCreatesZeroCoinAccountHashesPasswordAndAutoLoginsImmediately() {
        AuthResult result = service.register("  MinhTriet_79 ", "Minh Triết", "pass1234", "pass1234", true);

        assertTrue(result.success);
        assertNotNull(result.account);
        assertTrue(service.isLoggedIn());
        assertEquals("minhtriet_79", result.account.username);
        assertEquals("Minh Triết", result.account.displayName);
        assertEquals(0, result.account.coins);
        assertNotEquals("pass1234", result.account.passwordHash);
        assertTrue(PasswordHasher.verify("pass1234", result.account.passwordHash));

        assertTrue(store.session.autoLogin);
        assertEquals("minhtriet_79", store.session.lastUsername);
        assertEquals(1, store.database.accounts.size());
    }

    @Test
    void registerWithBlankDisplayNameDefaultsToTrimmedUsername() {
        AuthResult result = service.register("  MinerOne ", "   ", "1234", "1234", false);

        assertTrue(result.success);
        assertEquals("minerone", result.account.username);
        assertEquals("MinerOne", result.account.displayName);
        assertFalse(store.session.autoLogin);
    }

    @Test
    void registerValidatesUsernameDuplicatesDisplayNameAndPasswords() {
        assertFalse(service.register("ab", "Name", "1234", "1234", true).success);
        assertFalse(service.register("bad user!", "Name", "1234", "1234", true).success);
        assertFalse(service.register("valid_user", "123456789012345678901", "1234", "1234", true).success);
        assertFalse(service.register("valid_user", "Name", "123", "123", true).success);
        assertFalse(service.register("valid_user", "Name", "1234", "4321", true).success);

        assertTrue(service.register("valid_user", "Miner", "1234", "1234", true).success);
        AuthResult dup = service.register("VALID_USER", "Other", "1234", "1234", true);
        assertFalse(dup.success);
        assertEquals("USERNAME_EXISTS", dup.errorCode);
    }

    @Test
    void subsequentLaunchRespectsAutoLoginFlagAndMissingAccount() {
        service.register("gold_king", "Gold King", "abcd", "abcd", true);

        AccountService relaunched = new AccountService(store);
        AccountRecord autoLogged = relaunched.tryAutoLogin();
        assertNotNull(autoLogged);
        assertEquals("gold_king", autoLogged.username);
        assertTrue(relaunched.isLoggedIn());

        relaunched.setAutoLogin(false);
        AccountService relaunchedOff = new AccountService(store);
        assertNull(relaunchedOff.tryAutoLogin());
        assertFalse(relaunchedOff.isLoggedIn());

        store.session.autoLogin = true;
        store.session.lastUsername = "deleted_user";
        assertNull(new AccountService(store).tryAutoLogin());
    }

    @Test
    void loginLogoutSwitchAndRenameWorkCorrectly() {
        service.register("alice", "Alice", "secret1", "secret1", true);
        service.logout();

        assertFalse(service.isLoggedIn());
        assertFalse(store.session.autoLogin);
        assertEquals("", store.session.lastUsername);

        assertFalse(service.login("alice", "wrong", true).success);
        assertTrue(service.login("ALICE", "secret1", false).success);
        assertTrue(service.isLoggedIn());
        assertFalse(service.isAutoLoginEnabled());

        AuthResult renameFail = service.updateDisplayName("   ");
        assertFalse(renameFail.success);
        AuthResult renameOk = service.updateDisplayName("Thợ Mỏ Alice");
        assertTrue(renameOk.success);
        assertEquals("Thợ Mỏ Alice", service.getActiveAccount().displayName);

        service.switchAccount();
        assertFalse(service.isLoggedIn());
        assertFalse(store.session.autoLogin);
        assertEquals(1, store.database.accounts.size());
    }

    @Test
    void settlementUpdatesCoinsProgressAndLeaderboardOrder() {
        assertTrue(service.register("player1", "Player 1", "1111", "1111", true).success);
        service.recordSettlement(new SettlementSummary(1200, 3, false, 120, 75));
        assertEquals(195, service.getActiveAccount().coins);
        assertEquals(1200, service.getActiveAccount().bestMineMoney);
        assertEquals(3, service.getActiveAccount().highestClearedLevel);
        assertEquals(0, service.getActiveAccount().totalRunsCompleted);

        service.switchAccount();
        assertTrue(service.register("player2", "Player 2", "2222", "2222", true).success);
        service.recordSettlement(new SettlementSummary(2500, 6, true, 250, 350));
        assertEquals(600, service.getActiveAccount().coins);
        assertEquals(1, service.getActiveAccount().totalRunsCompleted);

        List<AccountRecord> board = service.getLeaderboard();
        assertEquals(2, board.size());
        assertEquals("player2", board.get(0).username);
        assertEquals("player1", board.get(1).username);
    }

    private static final class InMemoryAccountStore implements AccountStore {
        AccountDatabase database = new AccountDatabase();
        SessionState session = new SessionState();

        @Override
        public AccountDatabase loadAccounts() {
            AccountDatabase copy = new AccountDatabase();
            copy.schemaVersion = database.schemaVersion;
            copy.accounts = new ArrayList<>();
            for (AccountRecord r : database.accounts) {
                copy.accounts.add(r.copy());
            }
            return copy;
        }

        @Override
        public void saveAccounts(AccountDatabase database) {
            AccountDatabase copy = new AccountDatabase();
            copy.schemaVersion = database.schemaVersion;
            copy.accounts = new ArrayList<>();
            for (AccountRecord r : database.accounts) {
                copy.accounts.add(r.copy());
            }
            this.database = copy;
        }

        @Override
        public SessionState loadSession() {
            SessionState copy = new SessionState();
            copy.autoLogin = session.autoLogin;
            copy.lastUsername = session.lastUsername;
            return copy;
        }

        @Override
        public void saveSession(SessionState session) {
            SessionState copy = new SessionState();
            copy.autoLogin = session.autoLogin;
            copy.lastUsername = session.lastUsername;
            this.session = copy;
        }
    }
}
