package com.example.gps.map;

import androidx.annotation.NonNull;

import org.osmdroid.util.GeoPoint;

/**
 * 坐标转换：GPS 为 WGS84，高德底图与后端景点坐标为 GCJ-02。
 */
public final class CoordinateUtils {

    private static final double PI = 3.1415926535897932384626;
    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;

    private CoordinateUtils() {
    }

    /** 景点/路线节点：后端与高德瓦片均使用 GCJ-02。 */
    @NonNull
    public static GeoPoint poiToMapDisplay(double gcjLat, double gcjLng, boolean mapUsesGcj) {
        if (mapUsesGcj) {
            return new GeoPoint(gcjLat, gcjLng);
        }
        double[] wgs = gcj02ToWgs84(gcjLat, gcjLng);
        return new GeoPoint(wgs[0], wgs[1]);
    }

    /** 手机 GPS：WGS84 → 高德底图显示坐标。 */
    @NonNull
    public static GeoPoint gpsToMapDisplay(double wgsLat, double wgsLng, boolean mapUsesGcj) {
        if (mapUsesGcj) {
            double[] gcj = wgs84ToGcj02(wgsLat, wgsLng);
            return new GeoPoint(gcj[0], gcj[1]);
        }
        return new GeoPoint(wgsLat, wgsLng);
    }

    @NonNull
    public static double[] wgs84ToGcj02(double lat, double lng) {
        if (isOutOfChina(lat, lng)) {
            return new double[]{lat, lng};
        }
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{lat + dLat, lng + dLng};
    }

    @NonNull
    public static double[] gcj02ToWgs84(double lat, double lng) {
        if (isOutOfChina(lat, lng)) {
            return new double[]{lat, lng};
        }
        double delta = 0.01;
        double minLat = lat - delta;
        double maxLat = lat + delta;
        double minLng = lng - delta;
        double maxLng = lng + delta;
        double wgsLat = lat;
        double wgsLng = lng;
        for (int i = 0; i < 10; i++) {
            wgsLat = (minLat + maxLat) / 2.0;
            wgsLng = (minLng + maxLng) / 2.0;
            double[] tmp = wgs84ToGcj02(wgsLat, wgsLng);
            double dLat = tmp[0] - lat;
            double dLng = tmp[1] - lng;
            if (Math.abs(dLat) < 1e-7 && Math.abs(dLng) < 1e-7) {
                return new double[]{wgsLat, wgsLng};
            }
            if (dLat > 0) {
                maxLat = wgsLat;
            } else {
                minLat = wgsLat;
            }
            if (dLng > 0) {
                maxLng = wgsLng;
            } else {
                minLng = wgsLng;
            }
        }
        return new double[]{wgsLat, wgsLng};
    }

    public static boolean isOutOfChina(double lat, double lng) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
}
