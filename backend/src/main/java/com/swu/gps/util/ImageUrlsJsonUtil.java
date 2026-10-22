package com.swu.gps.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 景点多图 URL 列表的 JSON 序列化（存于 guide_location.image_urls_json）。
 */
public final class ImageUrlsJsonUtil {

    private ImageUrlsJsonUtil() {
    }

    public static List<String> parse(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return new ArrayList<>();
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (!inString) {
                if (c == '"') {
                    inString = true;
                    current.setLength(0);
                }
                continue;
            }
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                String value = current.toString().trim();
                if (!value.isEmpty()) {
                    result.add(value);
                }
                inString = false;
                continue;
            }
            current.append(c);
        }
        return result;
    }

    public static String serialize(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            sb.append('"').append(escape(url.trim())).append('"');
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    public static String firstImageUrl(List<String> urls, String legacyImageUrl) {
        if (urls != null && !urls.isEmpty()) {
            return urls.get(0);
        }
        return legacyImageUrl;
    }

    public static List<String> mergeLegacy(List<String> urls, String legacyImageUrl) {
        List<String> result = urls == null ? new ArrayList<>() : new ArrayList<>(urls);
        if (result.isEmpty() && legacyImageUrl != null && !legacyImageUrl.isBlank()) {
            result.add(legacyImageUrl.trim());
        }
        return Collections.unmodifiableList(result);
    }
}
