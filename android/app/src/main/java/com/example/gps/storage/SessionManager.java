package com.example.gps.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.gps.AppConfig;

public class SessionManager {

    private static final String PREF = "gps_pref";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_BASE_URL = "base_url";

    private final Context appContext;
    private final SharedPreferences preferences;

    private SessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.preferences = appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static SessionManager from(Context context) {
        return new SessionManager(context);
    }

    public void saveAuth(String token, String role, String username) {
        saveAuth(token, role, username, -1L);
    }

    public void saveAuth(String token, String role, String username, long userId) {
        preferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_ROLE, role)
                .putString(KEY_USERNAME, username)
                .putLong(KEY_USER_ID, userId)
                .apply();
    }

    public void clearAuth() {
        preferences.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_ROLE)
                .remove(KEY_USERNAME)
                .remove(KEY_USER_ID)
                .apply();
    }

    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1L);
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getRole());
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, "");
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, "");
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "");
    }

    public void setBaseUrl(String url) {
        preferences.edit().putString(KEY_BASE_URL, normalizeBaseUrl(url, AppConfig.defaultBaseUrl(appContext))).apply();
    }

    /** 真机始终跟随 strings.xml，避免沿用 10.0.2.2 / 127.0.0.1 等无效历史地址。 */
    public void syncBaseUrlFromConfig() {
        String configured = AppConfig.defaultBaseUrl(appContext);
        preferences.edit().putString(KEY_BASE_URL, configured).apply();
        com.example.gps.network.ApiClient.reset();
    }

    public String getBaseUrl() {
        String fallback = AppConfig.defaultBaseUrl(appContext);
        if (!AppConfig.isProbablyEmulator()) {
            String legacy = preferences.getString(KEY_BASE_URL, "");
            String normalized = normalizeBaseUrl(legacy, fallback);
            if (!fallback.equalsIgnoreCase(normalized)) {
                preferences.edit().putString(KEY_BASE_URL, fallback).apply();
            }
            return fallback;
        }
        String legacy = preferences.getString(KEY_BASE_URL, "");
        String normalized = normalizeBaseUrl(legacy, fallback);
        String oldPort = "http://10.0.2.2:8090/";
        if (oldPort.equalsIgnoreCase(normalized)) {
            preferences.edit().putString(KEY_BASE_URL, fallback).apply();
            return fallback;
        }
        if (legacy == null || legacy.trim().isEmpty()) {
            return fallback;
        }
        return normalized;
    }

    public static String normalizeBaseUrl(String raw) {
        return normalizeBaseUrl(raw, AppConfig.EMULATOR_BASE_URL);
    }

    public static String normalizeBaseUrl(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return fallback;
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }
        while (value.endsWith("//")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }
}
