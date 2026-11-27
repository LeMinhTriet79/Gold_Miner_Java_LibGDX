package com.minhtriet79.goldminer.domain.account;

import com.minhtriet79.goldminer.domain.settlement.SettlementSummary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pure-Java offline account service supporting registration, SHA-256 login,
 * editable display names, automatic session login, and leaderboard queries.
 */
public class AccountService {

    private static final int MIN_USERNAME_LEN = 3;
    private static final int MAX_USERNAME_LEN = 20;
    private static final int MAX_DISPLAY_NAME_LEN = 20;
    private static final int MIN_PASSWORD_LEN = 4;
    private static final int MAX_PASSWORD_LEN = 64;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_.-]+$");

    private final AccountStore store;
    private AccountDatabase database;
    private SessionState session;
    private AccountRecord activeAccount;

    public AccountService(AccountStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        reloadFromStore();
    }

    private void reloadFromStore() {
        AccountDatabase loadedDb = store.loadAccounts();
        this.database = loadedDb != null ? loadedDb : new AccountDatabase();
        if (this.database.accounts == null) {
            this.database.accounts = new ArrayList<>();
        }
        SessionState loadedSession = store.loadSession();
        this.session = loadedSession != null ? loadedSession : new SessionState();
        if (this.session.lastUsername == null) {
            this.session.lastUsername = "";
        }
    }

    public AccountRecord tryAutoLogin() {
        reloadFromStore();
        if (!session.autoLogin || session.lastUsername.trim().isEmpty()) {
            activeAccount = null;
            return null;
        }
        AccountRecord matched = findByUsername(session.lastUsername);
        activeAccount = matched;
        return matched;
    }

    public AuthResult register(
        String rawUsername,
        String rawDisplayName,
        String password,
        String confirmPassword,
        boolean autoLogin
    ) {
        String trimmedUser = rawUsername == null ? "" : rawUsername.trim();
        String canonicalUser = trimmedUser.toLowerCase(Locale.ROOT);
        if (canonicalUser.length() < MIN_USERNAME_LEN || canonicalUser.length() > MAX_USERNAME_LEN
            || !USERNAME_PATTERN.matcher(canonicalUser).matches()) {
            return AuthResult.fail(
                "INVALID_USERNAME",
                "Username dài 3-20 ký tự (a-z, 0-9, _, ., -)."
            );
        }
        if (findByUsername(canonicalUser) != null) {
            return AuthResult.fail("USERNAME_EXISTS", "Tên tài khoản đã tồn tại!");
        }

        String trimmedDisplay = rawDisplayName == null ? "" : rawDisplayName.trim();
        if (trimmedDisplay.isEmpty()) {
            trimmedDisplay = trimmedUser;
        }
        if (trimmedDisplay.length() > MAX_DISPLAY_NAME_LEN) {
            return AuthResult.fail("INVALID_DISPLAY_NAME", "Tên hiển thị tối đa 20 ký tự.");
        }

        if (password == null || password.length() < MIN_PASSWORD_LEN || password.length() > MAX_PASSWORD_LEN) {
            return AuthResult.fail("INVALID_PASSWORD", "Mật khẩu phải từ 4 đến 64 ký tự.");
        }
        if (!password.equals(confirmPassword)) {
            return AuthResult.fail("PASSWORD_MISMATCH", "Xác nhận mật khẩu không khớp.");
        }

        AccountRecord created = new AccountRecord();
        created.username = canonicalUser;
        created.displayName = trimmedDisplay;
        created.passwordHash = PasswordHasher.hash(password);
        created.coins = 0;
        created.bestMineMoney = 0;
        created.highestClearedLevel = 0;
        created.totalRunsCompleted = 0;
        created.createdAtEpochMs = System.currentTimeMillis();

        database.accounts.add(created);
        store.saveAccounts(database);

        activeAccount = created;
        session.autoLogin = autoLogin;
        session.lastUsername = created.username;
        store.saveSession(session);

        return AuthResult.ok(created, "Đăng ký thành công!");
    }

    public AuthResult login(String rawUsername, String password, boolean autoLogin) {
        String canonicalUser = rawUsername == null ? "" : rawUsername.trim().toLowerCase(Locale.ROOT);
        if (canonicalUser.isEmpty() || password == null || password.isEmpty()) {
            return AuthResult.fail("INVALID_CREDENTIALS", "Vui lòng nhập đầy đủ Username và Mật khẩu.");
        }

        AccountRecord found = findByUsername(canonicalUser);
        if (found == null || !PasswordHasher.verify(password, found.passwordHash)) {
            return AuthResult.fail("INVALID_CREDENTIALS", "Sai tên đăng nhập hoặc mật khẩu!");
        }

        activeAccount = found;
        session.autoLogin = autoLogin;
        session.lastUsername = found.username;
        store.saveSession(session);

        return AuthResult.ok(found, "Đăng nhập thành công!");
    }

    public void setAutoLogin(boolean enabled) {
        session.autoLogin = enabled;
        if (activeAccount != null) {
            session.lastUsername = activeAccount.username;
        }
        store.saveSession(session);
    }

    public boolean isAutoLoginEnabled() {
        return session.autoLogin;
    }

    public void logout() {
        activeAccount = null;
        session.autoLogin = false;
        session.lastUsername = "";
        store.saveSession(session);
    }

    public void switchAccount() {
        logout();
    }

    public AuthResult updateDisplayName(String rawDisplayName) {
        if (activeAccount == null) {
            return AuthResult.fail("NOT_LOGGED_IN", "Bạn chưa đăng nhập.");
        }
        String trimmed = rawDisplayName == null ? "" : rawDisplayName.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_DISPLAY_NAME_LEN) {
            return AuthResult.fail("INVALID_DISPLAY_NAME", "Tên hiển thị từ 1 đến 20 ký tự.");
        }
        activeAccount.displayName = trimmed;
        store.saveAccounts(database);
        return AuthResult.ok(activeAccount, "Đã đổi tên hiển thị!");
    }

    public void recordSettlement(SettlementSummary summary) {
        if (activeAccount == null || summary == null) {
            return;
        }
        if (summary.totalAwardedCoins > 0) {
            activeAccount.coins += summary.totalAwardedCoins;
        }
        activeAccount.highestClearedLevel = Math.max(activeAccount.highestClearedLevel, summary.levelsCleared);
        activeAccount.bestMineMoney = Math.max(activeAccount.bestMineMoney, summary.finalMineMoney);
        if (summary.completedWorld1) {
            activeAccount.totalRunsCompleted++;
        }
        store.saveAccounts(database);
    }

    public AccountRecord getActiveAccount() {
        return activeAccount;
    }

    public boolean isLoggedIn() {
        return activeAccount != null;
    }

    public List<AccountRecord> getLeaderboard() {
        List<AccountRecord> list = new ArrayList<>(database.accounts.size());
        for (AccountRecord record : database.accounts) {
            if (record != null) {
                list.add(record.copy());
            }
        }
        list.sort(
            Comparator.comparingInt((AccountRecord r) -> r.coins).reversed()
                .thenComparing(Comparator.comparingInt((AccountRecord r) -> r.bestMineMoney).reversed())
                .thenComparing(r -> r.username == null ? "" : r.username)
        );
        return list;
    }

    private AccountRecord findByUsername(String rawUsername) {
        if (rawUsername == null) {
            return null;
        }
        String canonical = rawUsername.trim().toLowerCase(Locale.ROOT);
        if (canonical.isEmpty()) {
            return null;
        }
        for (AccountRecord record : database.accounts) {
            if (record != null && record.username != null
                && record.username.trim().toLowerCase(Locale.ROOT).equals(canonical)) {
                return record;
            }
        }
        return null;
    }
}
