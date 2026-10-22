package com.example.gps.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 本地意见反馈记录（最近 20 条）。
 */
public class FeedbackStore {

    private static final String PREF = "gps_feedback";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_SIZE = 20;

    private final SharedPreferences preferences;

    public FeedbackStore(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void add(@NonNull String content) {
        String value = content.trim();
        if (value.isEmpty()) {
            return;
        }
        List<Entry> items = loadAll();
        items.add(0, new Entry(value, System.currentTimeMillis()));
        while (items.size() > MAX_SIZE) {
            items.remove(items.size() - 1);
        }
        saveAll(items);
    }

    @NonNull
    public List<Entry> getAll() {
        return loadAll();
    }

    @NonNull
    private List<Entry> loadAll() {
        List<Entry> items = new ArrayList<>();
        String raw = preferences.getString(KEY_ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String content = obj.optString("content", "").trim();
                long time = obj.optLong("time", 0L);
                if (!TextUtils.isEmpty(content)) {
                    items.add(new Entry(content, time));
                }
            }
        } catch (Exception ignored) {
            // Ignore corrupt cache.
        }
        return items;
    }

    private void saveAll(@NonNull List<Entry> items) {
        JSONArray array = new JSONArray();
        for (Entry item : items) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("content", item.content);
                obj.put("time", item.timeMillis);
                array.put(obj);
            } catch (Exception ignored) {
                // Skip invalid entry.
            }
        }
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    public static class Entry {
        public final String content;
        public final long timeMillis;

        public Entry(String content, long timeMillis) {
            this.content = content;
            this.timeMillis = timeMillis;
        }

        @NonNull
        public String formatTime() {
            if (timeMillis <= 0L) {
                return "";
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(new Date(timeMillis));
        }
    }
}
