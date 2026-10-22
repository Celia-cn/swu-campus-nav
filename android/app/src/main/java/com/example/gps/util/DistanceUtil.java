package com.example.gps.util;

import android.location.Location;

import androidx.annotation.NonNull;

import com.example.gps.map.CampusMapManager;
import com.example.gps.network.dto.LocationDto;

import org.osmdroid.util.GeoPoint;

/**
 * GIS 距离计算工具：封装 {@link Location#distanceBetween} 及景点距离换算。
 */
public final class DistanceUtil {

    private DistanceUtil() {
    }

    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }

    public static double distanceMeters(@NonNull GeoPoint from, @NonNull GeoPoint to) {
        return distanceMeters(from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude());
    }

    public static double distanceToCampusCenterMeters(@NonNull GeoPoint point) {
        return distanceMeters(point.getLatitude(), point.getLongitude(),
                CampusMapManager.CAMPUS_CENTER_LAT, CampusMapManager.CAMPUS_CENTER_LNG);
    }

    /**
     * 用户原始 GCJ 坐标到景点展示点的距离（米）；无法计算时返回 {@link Double#NaN}。
     */
    public static double distanceRawToDisplayPoint(@NonNull GeoPoint rawGcjPoint,
                                                   @NonNull LocationDto location,
                                                   @NonNull java.util.Map<Long, GeoPoint> displayOverrides) {
        GeoPoint target = displayOverrides.get(location.id);
        if (target == null && isValidCoordinate(location.latitude, location.longitude)) {
            target = new GeoPoint(location.latitude, location.longitude);
        }
        if (target == null) {
            return Double.NaN;
        }
        return distanceMeters(rawGcjPoint, target);
    }

    public static boolean isValidCoordinate(double lat, double lng) {
        return Math.abs(lat) > 0.0001
                && Math.abs(lng) > 0.0001
                && lat >= -90.0 && lat <= 90.0
                && lng >= -180.0 && lng <= 180.0;
    }

    public static boolean isWithinRadius(double distanceMeters, double radiusMeters) {
        return distanceMeters <= radiusMeters;
    }
}
