package com.example.gps.util;

import android.content.Context;
import android.text.TextUtils;

import com.example.gps.storage.SessionManager;

public final class MediaUrlResolver {

    private MediaUrlResolver() {
    }

    public static String resolve(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String base = SessionManager.from(context).getBaseUrl();
        if (TextUtils.isEmpty(base)) {
            return trimmed;
        }
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return base + trimmed;
    }
}
