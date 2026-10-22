package com.example.gps.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.gps.network.ApiClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class SimpleImageLoader {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, Bitmap> CACHE = new ConcurrentHashMap<>();

    private SimpleImageLoader() {
    }

    public static void load(Context context, @Nullable String url, ImageView target, int placeholderRes) {
        target.setTag(url);
        target.setImageResource(placeholderRes);
        if (url == null || url.isBlank()) {
            return;
        }
        Bitmap cached = CACHE.get(url);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }
        OkHttpClient client = ApiClient.httpClient(context.getApplicationContext());
        EXECUTOR.execute(() -> {
            Bitmap bitmap = fetch(client, url);
            if (bitmap != null) {
                CACHE.put(url, bitmap);
            }
            MAIN.post(() -> {
                if (!url.equals(target.getTag())) {
                    return;
                }
                if (bitmap != null) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }

    @Nullable
    private static Bitmap fetch(OkHttpClient client, String url) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            byte[] bytes = response.body().bytes();
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception ignored) {
            return null;
        }
    }
}
