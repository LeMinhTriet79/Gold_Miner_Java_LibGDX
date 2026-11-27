package com.minhtriet79.goldminer.domain.account;

/**
 * Local session state stored in {@code data/session.json} for automatic login.
 */
public class SessionState {

    public boolean autoLogin = false;
    public String lastUsername = "";

    public SessionState() {
    }
}
