package com.example.gps.util;

import android.content.Context;

import com.example.gps.R;

/**
 * 距离展示：不足 1km 用 m，达到 1km 及以上用 km。
 */
public final class DistanceFormat {

    private static final double KM_THRESHOLD = 1000.0;
    private static final double INVALID_DISTANCE_CEILING = 1_000_000_000.0;

    private DistanceFormat() {
    }

    public static boolean isUsable(double meters) {
        return !Double.isNaN(meters)
                && !Double.isInfinite(meters)
                && meters >= 0
                && meters < INVALID_DISTANCE_CEILING;
    }

    public static String formatWithLabel(Context context, double meters) {
        if (!isUsable(meters)) {
            return context.getString(R.string.label_distance_unknown);
        }
        double safe = Math.max(0, meters);
        if (safe < KM_THRESHOLD) {
            return context.getString(R.string.label_distance_m, Math.round(safe));
        }
        double km = safe / KM_THRESHOLD;
        if (km >= 100) {
            return context.getString(R.string.label_distance_km_int, Math.round(km));
        }
        return context.getString(R.string.label_distance_km, km);
    }

    public static String formatApprox(Context context, double meters) {
        double safe = Math.max(0, meters);
        if (safe < KM_THRESHOLD) {
            return context.getString(R.string.label_distance_approx_m, Math.round(safe));
        }
        double km = safe / KM_THRESHOLD;
        if (km >= 100) {
            return context.getString(R.string.label_distance_approx_km_int, Math.round(km));
        }
        return context.getString(R.string.label_distance_approx_km, km);
    }
}
