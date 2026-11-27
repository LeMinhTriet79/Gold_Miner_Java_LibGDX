package com.minhtriet79.goldminer.domain.account;

/**
 * Immutable outcome of an account registration, login, or profile update operation.
 */
public final class AuthResult {

    public final boolean success;
    public final String errorCode;
    public final String message;
    public final AccountRecord account;

    private AuthResult(boolean success, String errorCode, String message, AccountRecord account) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.account = account;
    }

    public static AuthResult ok(AccountRecord account, String message) {
        return new AuthResult(true, "", message, account);
    }

    public static AuthResult fail(String errorCode, String message) {
        return new AuthResult(false, errorCode, message, null);
    }
}
