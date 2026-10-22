package com.example.gps.location;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import com.example.gps.R;
import com.example.gps.map.CampusMapManager;
import com.example.gps.util.DistanceUtil;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户轨迹绘制：记录移动点并用 Polyline 展示游览路线。
 */
public class TrackManager {

    private static final int MAX_POINTS = 800;
    private static final double MIN_SEGMENT_METERS = 3.0;

    private final CampusMapManager campusMapManager;
    private final Context context;
    private final List<GeoPoint> trackPoints = new ArrayList<>();
    @androidx.annotation.Nullable
    private Polyline trackPolyline;
    private boolean enabled = true;

    public TrackManager(@NonNull CampusMapManager campusMapManager, @NonNull Context context) {
        this.campusMapManager = campusMapManager;
        this.context = context;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void appendPoint(@NonNull GeoPoint point) {
        if (!enabled) {
            return;
        }
        if (trackPoints.isEmpty()) {
            trackPoints.add(point);
        } else {
            GeoPoint last = trackPoints.get(trackPoints.size() - 1);
            if (DistanceUtil.distanceMeters(last, point) >= MIN_SEGMENT_METERS) {
                trackPoints.add(point);
            }
        }
        if (trackPoints.size() > MAX_POINTS) {
            trackPoints.remove(0);
        }
        renderPolyline();
    }

    public void clear() {
        trackPoints.clear();
        if (trackPolyline != null) {
            trackPolyline.remove();
            trackPolyline = null;
        }
    }

    @NonNull
    public List<GeoPoint> getTrackPoints() {
        return new ArrayList<>(trackPoints);
    }

    private void renderPolyline() {
        if (trackPoints.isEmpty()) {
            return;
        }
        if (trackPolyline == null) {
            trackPolyline = campusMapManager.addPolyline(
                    trackPoints,
                    ContextCompat.getColor(context, R.color.brand_track),
                    5f);
        } else {
            trackPolyline.setPoints(toLatLngList(trackPoints));
        }
    }

    @NonNull
    private static List<LatLng> toLatLngList(@NonNull List<GeoPoint> points) {
        List<LatLng> converted = new ArrayList<>(points.size());
        for (GeoPoint point : points) {
            converted.add(CampusMapManager.toLatLng(point));
        }
        return converted;
    }
}
