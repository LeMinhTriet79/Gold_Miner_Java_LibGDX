package com.minhtriet79.goldminer.persistence;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.minhtriet79.goldminer.domain.account.AccountDatabase;
import com.minhtriet79.goldminer.domain.account.AccountRecord;
import com.minhtriet79.goldminer.domain.account.AccountStore;
import com.minhtriet79.goldminer.domain.account.SessionState;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Local JSON implementation of {@link AccountStore} persisting to {@code data/accounts.json}
 * and {@code data/session.json}.
 */
public class LocalJsonAccountStore implements AccountStore {

    private static final String ACCOUNTS_FILE = "accounts.json";
    private static final String SESSION_FILE = "session.json";

    private final Path dataDir;
    private final Json json;

    public LocalJsonAccountStore() {
        this(resolveDefaultDataDir());
    }

    public LocalJsonAccountStore(Path dataDir) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir must not be null");
        this.json = createJson();
    }

    private static Json createJson() {
        Json j = new Json();
        j.setOutputType(JsonWriter.OutputType.json);
        j.setUsePrototypes(false);
        j.setElementType(AccountDatabase.class, "accounts", AccountRecord.class);
        return j;
    }

    public static Path resolveDefaultDataDir() {
        File cwd = new File(".").getAbsoluteFile();
        if (cwd.getName().equals(".")) {
            cwd = cwd.getParentFile();
        }
        if (cwd != null && "assets".equalsIgnoreCase(cwd.getName())) {
            File parent = cwd.getParentFile();
            if (parent != null && (new File(parent, "settings.gradle").isFile()
                || new File(parent, "build.gradle").isFile())) {
                return parent.toPath().resolve("data");
            }
        }
        return (cwd != null ? cwd.toPath() : Path.of(".")).resolve("data");
    }

    public Path getAccountsPath() {
        return dataDir.resolve(ACCOUNTS_FILE);
    }

    public Path getSessionPath() {
        return dataDir.resolve(SESSION_FILE);
    }

    @Override
    public AccountDatabase loadAccounts() {
        Path path = getAccountsPath();
        if (!Files.isRegularFile(path)) {
            return new AccountDatabase();
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            AccountDatabase db = json.fromJson(AccountDatabase.class, raw);
            if (db == null) {
                return new AccountDatabase();
            }
            if (db.accounts == null) {
                db.accounts = new ArrayList<>();
            }
            return db;
        } catch (Exception ignored) {
            return new AccountDatabase();
        }
    }

    @Override
    public void saveAccounts(AccountDatabase database) {
        AccountDatabase toSave = database != null ? database : new AccountDatabase();
        writeJson(getAccountsPath(), toSave);
    }

    @Override
    public SessionState loadSession() {
        Path path = getSessionPath();
        if (!Files.isRegularFile(path)) {
            return new SessionState();
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            SessionState loaded = json.fromJson(SessionState.class, raw);
            if (loaded == null) {
                return new SessionState();
            }
            if (loaded.lastUsername == null) {
                loaded.lastUsername = "";
            }
            return loaded;
        } catch (Exception ignored) {
            return new SessionState();
        }
    }

    @Override
    public void saveSession(SessionState session) {
        SessionState toSave = session != null ? session : new SessionState();
        writeJson(getSessionPath(), toSave);
    }

    private void writeJson(Path target, Object value) {
        try {
            Files.createDirectories(dataDir);
            Files.writeString(target, json.prettyPrint(value), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write account file: " + target, e);
        }
    }
}
