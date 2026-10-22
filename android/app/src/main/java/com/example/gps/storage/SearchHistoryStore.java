package com.example.gps.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 本地搜索历史（最近关键词，最多保留 10 条）。
 */
public class SearchHistoryStore {

    private static final String PREF = "gps_search_history";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_SIZE = 10;

    private final SharedPreferences preferences;

    public SearchHistoryStore(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void addKeyword(@NonNull String keyword) {
        String value = keyword.trim();
        if (value.isEmpty()) {
            return;
        }
        List<String> items = loadAll();
        items.remove(value);
        items.add(0, value);
        while (items.size() > MAX_SIZE) {
            items.remove(items.size() - 1);
        }
        saveAll(items);
    }

    @NonNull
    public List<String> getAll() {
        return loadAll();
    }

    public void clear() {
        saveAll(new ArrayList<>());
    }

    @NonNull
    public List<String> filterByPrefix(@NonNull String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        List<String> all = loadAll();
        if (normalized.isEmpty()) {
            return all;
        }
        List<String> matched = new ArrayList<>();
        for (String item : all) {
            if (item.toLowerCase(Locale.ROOT).contains(normalized)) {
                matched.add(item);
            }
        }
        return matched;
    }

    @NonNull
    private List<String> loadAll() {
        List<String> items = new ArrayList<>();
        String raw = preferences.getString(KEY_ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "").trim();
                if (!TextUtils.isEmpty(item) && !items.contains(item)) {
                    items.add(item);
                }
            }
        } catch (Exception ignored) {
            // Ignore corrupt cache.
        }
        return items;
    }

    private void saveAll(@NonNull List<String> items) {
        JSONArray array = new JSONArray();
        for (String item : items) {
            array.put(item);
        }
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply();
    }
}
