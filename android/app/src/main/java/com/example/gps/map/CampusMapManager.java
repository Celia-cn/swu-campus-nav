package com.example.gps.map;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.example.gps.R;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Campus map wrapper backed by the official AMap Android SDK.
 */
public class CampusMapManager {

    public static final double CAMPUS_CENTER_LAT = 29.8215;
    public static final double CAMPUS_CENTER_LNG = 106.4210;
    /** 默认地图锚点：西南大学中心图书馆 */
    public static final double MAP_ANCHOR_LAT = 29.8230614;
    public static final double MAP_ANCHOR_LNG = 106.4204318;
    public static final double CAMPUS_MIN_LAT = 29.8060;
    public static final double CAMPUS_MAX_LAT = 29.8365;
    public static final double CAMPUS_MIN_LNG = 106.4060;
    public static final double CAMPUS_MAX_LNG = 106.4370;
    public static final double CAMPUS_BOUND_BUFFER_LAT = 0.0020;
    public static final double CAMPUS_BOUND_BUFFER_LNG = 0.0025;

    private static final double[][] CAMPUS_BOUNDARY_GCJ = {
            {29.8068, 106.4105},
            {29.8088, 106.4175},
            {29.8118, 106.4325},
            {29.8165, 106.4365},
            {29.8210, 106.4372},
            {29.8255, 106.4368},
            {29.8295, 106.4355},
            {29.8338, 106.4338},
            {29.8362, 106.4280},
            {29.8350, 106.4200},
            {29.8342, 106.4088},
            {29.8300, 106.4068},
            {29.8250, 106.4072},
            {29.8200, 106.4078},
            {29.8140, 106.4095},
            {29.8090, 106.4110},
    };

    public static final int LAYER_VECTOR = 0;
    public static final int LAYER_SATELLITE = 1;

    private static final float MIN_ZOOM = 16.4f;
    private static final float MAX_ZOOM = 19.0f;
    private static final float DEFAULT_ZOOM = 18.2f;
    /** 进入应用时的校园全景缩放（以中心图书馆为中心） */
    private static final float PANORAMA_ZOOM = 16.65f;
    private static final float ZOOM_STEP = 0.5f;

    private final MapView mapView;
    private final String[] layerNames = new String[]{
            "\u6821\u56ed\u8be6\u56fe",
            "\u536b\u661f\u5b9e\u666f"
    };

    private AMap aMap;
    private int currentTileSourceIndex = LAYER_VECTOR;
    @Nullable
    private Polygon campusBoundaryOverlay;

    public CampusMapManager(@NonNull MapView mapView) {
        this.mapView = mapView;
    }

    public void initialize(@NonNull Context context, @Nullable Bundle savedInstanceState) {
        // 不使用 savedInstanceState 恢复镜头，避免偏离中心图书馆默认全景
        mapView.onCreate(null);
        aMap = mapView.getMap();
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        aMap.setMinZoomLevel(MIN_ZOOM);
        aMap.setMaxZoomLevel(MAX_ZOOM);
        aMap.setMapStatusLimits(buildCampusBounds());

        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setMyLocationButtonEnabled(false);
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);

        applyMapAnchorImmediate();
        attachCampusBoundaryOverlay(context);
    }

    /** 立即将镜头移动到中心图书馆全景（同步）。 */
    public void applyMapAnchorImmediate() {
        if (aMap == null) {
            return;
        }
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(MAP_ANCHOR_LAT, MAP_ANCHOR_LNG),
                PANORAMA_ZOOM));
    }

    @NonNull
    public static GeoPoint getMapAnchorPoint() {
        return new GeoPoint(MAP_ANCHOR_LAT, MAP_ANCHOR_LNG);
    }

    @NonNull
    public AMap getMap() {
        return aMap;
    }

    @NonNull
    public MapView getMapView() {
        return mapView;
    }

    public boolean isSatelliteLayerActive() {
        return currentTileSourceIndex == LAYER_SATELLITE;
    }

    public void onResume() {
        mapView.onResume();
    }

    public void onPause() {
        mapView.onPause();
    }

    public void onDetach() {
        removeCampusBoundaryOverlay();
        mapView.onDestroy();
    }

    public void invalidate() {
        mapView.invalidate();
    }

    /**
     * 启用高德官方蓝点定位（成员2：setMyLocationEnabled）。
     */
    @SuppressLint("MissingPermission")
    public void enableMyLocationBlueDot(boolean enabled) {
        aMap.setMyLocationEnabled(enabled);
        if (!enabled) {
            return;
        }
        MyLocationStyle style = new MyLocationStyle();
        // 连续更新蓝点，但不把地图中心拉到定位点（ROTATE 会每几秒抢镜头，导致聚焦失效）
        style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        style.interval(3000L);
        style.strokeColor(Color.TRANSPARENT);
        style.radiusFillColor(Color.argb(48, 0, 120, 255));
        aMap.setMyLocationStyle(style);
    }

    public void zoomIn() {
        float current = aMap.getCameraPosition().zoom;
        setZoom(Math.min(MAX_ZOOM, current + ZOOM_STEP));
    }

    public void zoomOut() {
        float current = aMap.getCameraPosition().zoom;
        setZoom(Math.max(MIN_ZOOM, current - ZOOM_STEP));
    }

    public void animateTo(@NonNull GeoPoint point) {
        aMap.animateCamera(CameraUpdateFactory.newLatLng(toLatLng(point)));
    }

    public void setCenter(@NonNull GeoPoint point) {
        aMap.moveCamera(CameraUpdateFactory.newLatLng(toLatLng(point)));
    }

    public void setZoom(double zoom) {
        float fixed = (float) Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        aMap.moveCamera(CameraUpdateFactory.zoomTo(fixed));
    }

    public void setCenterAndZoom(@NonNull GeoPoint point, double zoom) {
        float fixed = (float) Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(toLatLng(point), fixed));
    }

    public void focusCampus(@NonNull GeoPoint campusCenter) {
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(toLatLng(campusCenter), DEFAULT_ZOOM));
    }

    /** 缩放到以中心图书馆为中心的校园全景。 */
    public void focusCampusOverview(boolean animated) {
        if (aMap == null) {
            return;
        }
        Runnable action = () -> {
            LatLng anchor = toLatLng(getMapAnchorPoint());
            if (animated) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(anchor, PANORAMA_ZOOM));
            } else {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(anchor, PANORAMA_ZOOM));
            }
        };
        mapView.post(action);
    }

    /** 将镜头缩放到路线折线范围（四边等距内边距）。 */
    public void zoomToRoutePoints(@NonNull List<GeoPoint> points, boolean animated) {
        zoomToRoutePoints(points, 120, 120, 120, 120, animated);
    }

    /** 将镜头缩放到路线折线范围，支持不对称内边距（适配底部面板等遮挡）。 */
    public void zoomToRoutePoints(@NonNull List<GeoPoint> points,
                                  int paddingLeft,
                                  int paddingTop,
                                  int paddingRight,
                                  int paddingBottom,
                                  boolean animated) {
        if (aMap == null || points.size() < 2) {
            return;
        }
        Runnable focusAction = () -> focusRoutePointsNow(
                points, paddingLeft, paddingTop, paddingRight, paddingBottom, animated);
        if (mapView.getWidth() > 0 && mapView.getHeight() > 0) {
            mapView.post(focusAction);
        } else {
            mapView.post(() -> {
                if (mapView.getWidth() > 0 && mapView.getHeight() > 0) {
                    focusAction.run();
                } else {
                    mapView.post(focusAction);
                }
            });
        }
    }

    private void focusRoutePointsNow(@NonNull List<GeoPoint> points,
                                     int paddingLeft,
                                     int paddingTop,
                                     int paddingRight,
                                     int paddingBottom,
                                     boolean animated) {
        if (aMap == null || points.size() < 2) {
            return;
        }
        try {
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (GeoPoint point : points) {
                if (point != null) {
                    builder.include(toLatLng(point));
                }
            }
            LatLngBounds bounds = builder.build();
            int mapW = Math.max(1, mapView.getWidth());
            int mapH = Math.max(1, mapView.getHeight());
            int fitW = Math.max(1, mapW - paddingLeft - paddingRight);
            int fitH = Math.max(1, mapH - paddingTop - paddingBottom);
            int edgePadding = Math.max(8, Math.min(paddingLeft, Math.min(paddingRight, Math.min(paddingTop, paddingBottom))));
            if (animated) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, fitW, fitH, edgePadding));
            } else {
                aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, fitW, fitH, edgePadding));
            }
            float fittedZoom = aMap.getCameraPosition().zoom;
            if (fittedZoom < MIN_ZOOM + 0.15f) {
                setZoom(MIN_ZOOM + 0.15f);
            }
        } catch (Throwable ignored) {
            GeoPoint center = centerOf(points);
            setCenterAndZoom(center, Math.max(MIN_ZOOM + 0.3f, DEFAULT_ZOOM - 0.4f));
        }
        invalidate();
    }

    @NonNull
    public Polyline addRoutePolyline(@NonNull Context context,
                                     @NonNull List<GeoPoint> points,
                                     int color) {
        return addRoutePolyline(context, points, color, 10f, 1000f);
    }

    @NonNull
    public Polyline addRoutePolyline(@NonNull Context context,
                                     @NonNull List<GeoPoint> points,
                                     int color,
                                     float widthDp,
                                     float zIndex) {
        float widthPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                widthDp,
                context.getResources().getDisplayMetrics());
        PolylineOptions options = new PolylineOptions()
                .addAll(toLatLngList(points))
                .color(color)
                .width(widthPx)
                .zIndex(zIndex)
                .visible(true)
                .geodesic(false)
                .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound);
        options.setReducePoint(false);
        options.setShownRatio(1.0f);
        Polyline polyline = aMap.addPolyline(options);
        if (polyline != null) {
            polyline.setVisible(true);
            polyline.setZIndex(zIndex);
            polyline.setAboveMaskLayer(true);
            polyline.setWidth(widthPx);
            polyline.setColor(color);
        }
        return polyline;
    }

    public void zoomToBoundingBox(@NonNull BoundingBox box, boolean animated) {
        if (aMap == null) {
            return;
        }
        try {
            LatLngBounds bounds = LatLngBounds.builder()
                    .include(new LatLng(box.getLatNorth(), box.getLonEast()))
                    .include(new LatLng(box.getLatSouth(), box.getLonWest()))
                    .build();
            int padding = 100;
            if (animated) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } else {
                aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }
            float fittedZoom = aMap.getCameraPosition().zoom;
            if (fittedZoom < MIN_ZOOM + 0.2f) {
                setZoom(MIN_ZOOM + 0.2f);
            }
        } catch (Throwable ignored) {
            GeoPoint center = new GeoPoint((box.getLatNorth() + box.getLatSouth()) / 2.0,
                    (box.getLonEast() + box.getLonWest()) / 2.0);
            setCenterAndZoom(center, DEFAULT_ZOOM);
        }
    }

    @NonNull
    public String switchTileLayer(@NonNull Context context) {
        int next = (currentTileSourceIndex + 1) % layerNames.length;
        currentTileSourceIndex = next;
        aMap.setMapType(next == LAYER_SATELLITE ? AMap.MAP_TYPE_SATELLITE : AMap.MAP_TYPE_NORMAL);
        refreshCampusBoundaryOverlay(context);
        return layerNames[next];
    }

    public void onTileLayerChanged(@NonNull Context context) {
        refreshCampusBoundaryOverlay(context);
    }

    @NonNull
    public String getCurrentLayerName() {
        return layerNames[currentTileSourceIndex];
    }

    public boolean isGcjMapLayerActive() {
        return true;
    }

    public int getCurrentTileSourceIndex() {
        return currentTileSourceIndex;
    }

    @NonNull
    public Polyline addPolyline(@NonNull List<GeoPoint> points, int color, float width) {
        PolylineOptions options = new PolylineOptions()
                .addAll(toLatLngList(points))
                .color(color)
                .width(width)
                .geodesic(false);
        return aMap.addPolyline(options);
    }

    public void refreshCampusBoundaryOverlay(@NonNull Context context) {
        removeCampusBoundaryOverlay();
        List<LatLng> ring = new ArrayList<>(CAMPUS_BOUNDARY_GCJ.length + 1);
        for (double[] point : CAMPUS_BOUNDARY_GCJ) {
            ring.add(new LatLng(point[0], point[1]));
        }
        if (!ring.isEmpty()) {
            ring.add(ring.get(0));
        }
        campusBoundaryOverlay = aMap.addPolygon(new PolygonOptions()
                .addAll(ring)
                .fillColor(ContextCompat.getColor(context, R.color.campus_boundary_fill))
                .strokeColor(ContextCompat.getColor(context, R.color.campus_boundary_stroke))
                .strokeWidth(4.5f)
                .zIndex(1f));
    }

    private void attachCampusBoundaryOverlay(@NonNull Context context) {
        refreshCampusBoundaryOverlay(context);
    }

    private void removeCampusBoundaryOverlay() {
        if (campusBoundaryOverlay != null) {
            campusBoundaryOverlay.remove();
            campusBoundaryOverlay = null;
        }
    }

    @NonNull
    private LatLngBounds buildCampusBounds() {
        return LatLngBounds.builder()
                .include(new LatLng(
                        CAMPUS_MIN_LAT - CAMPUS_BOUND_BUFFER_LAT,
                        CAMPUS_MIN_LNG - CAMPUS_BOUND_BUFFER_LNG))
                .include(new LatLng(
                        CAMPUS_MAX_LAT + CAMPUS_BOUND_BUFFER_LAT,
                        CAMPUS_MAX_LNG + CAMPUS_BOUND_BUFFER_LNG))
                .build();
    }

    @NonNull
    private static List<LatLng> toLatLngList(@NonNull List<GeoPoint> points) {
        List<LatLng> converted = new ArrayList<>(points.size());
        for (GeoPoint point : points) {
            converted.add(toLatLng(point));
        }
        return converted;
    }

    @NonNull
    public static LatLng toLatLng(@NonNull GeoPoint point) {
        return new LatLng(point.getLatitude(), point.getLongitude());
    }

    public static boolean isCampusPoint(double lat, double lng) {
        return lat >= (CAMPUS_MIN_LAT - CAMPUS_BOUND_BUFFER_LAT)
                && lat <= (CAMPUS_MAX_LAT + CAMPUS_BOUND_BUFFER_LAT)
                && lng >= (CAMPUS_MIN_LNG - CAMPUS_BOUND_BUFFER_LNG)
                && lng <= (CAMPUS_MAX_LNG + CAMPUS_BOUND_BUFFER_LNG);
    }

    public static boolean isInCampusRect(double lat, double lng) {
        return lat >= CAMPUS_MIN_LAT && lat <= CAMPUS_MAX_LAT
                && lng >= CAMPUS_MIN_LNG && lng <= CAMPUS_MAX_LNG;
    }

    @NonNull
    private static GeoPoint centerOf(@NonNull List<GeoPoint> points) {
        double sumLat = 0;
        double sumLon = 0;
        int count = 0;
        for (GeoPoint point : points) {
            if (point == null) {
                continue;
            }
            sumLat += point.getLatitude();
            sumLon += point.getLongitude();
            count++;
        }
        if (count == 0) {
            return new GeoPoint(CAMPUS_CENTER_LAT, CAMPUS_CENTER_LNG);
        }
        return new GeoPoint(sumLat / count, sumLon / count);
    }
}
