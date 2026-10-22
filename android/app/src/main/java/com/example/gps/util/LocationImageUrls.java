package com.example.gps.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gps.network.dto.LocationDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 景点配图：兼容单字段 imageUrl 与多图 imageUrls。
 */
public final class LocationImageUrls {

    private LocationImageUrls() {
    }

    @NonNull
    public static List<String> resolve(@Nullable LocationDto location) {
        if (location == null) {
            return Collections.emptyList();
        }
        if (location.imageUrls != null && !location.imageUrls.isEmpty()) {
            List<String> out = new ArrayList<>();
            for (String url : location.imageUrls) {
                if (url != null && !url.trim().isEmpty()) {
                    out.add(url.trim());
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        }
        if (location.imageUrl != null && !location.imageUrl.trim().isEmpty()) {
            return Collections.singletonList(location.imageUrl.trim());
        }
        return Collections.emptyList();
    }

    @Nullable
    public static String cover(@Nullable LocationDto location) {
        List<String> urls = resolve(location);
        return urls.isEmpty() ? null : urls.get(0);
    }
}
