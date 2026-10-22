package com.example.gps.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.example.gps.R;
import com.example.gps.network.dto.LocationDto;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AMap POI markers and campus geofence circles.
 */
public class MarkerManager {

    public interface MarkerClickListener {
        void onMarkerClicked(@NonNull LocationDto location, @Nullable Double distanceMeters);
    }

    public interface DisplayPointResolver {
        @Nullable
        GeoPoint resolve(@NonNull LocationDto location);
    }

    public interface PoiRenderedCallback {
        void onPoiRendered(@NonNull LocationDto location, @NonNull GeoPoint displayPoint);
    }

    private final AMap aMap;
    private final Context context;
    private final Map<Long, Marker> locationMarkers = new HashMap<>();
    private final List<Circle> geofenceOverlays = new ArrayList<>();

    private MarkerClickListener clickListener;
    private PoiRenderedCallback renderedCallback;
    @Nullable
    private DistanceProvider distanceProvider;
    @Nullable
    private PoiInfoWindow.ActionListener infoWindowActionListener;
    private long selectedLocationId = -1L;
    private boolean geofenceVisible = true;

    public MarkerManager(@NonNull AMap aMap, @NonNull Context context) {
        this.aMap = aMap;
        this.context = context;
        attachMapListeners();
    }

    private void attachMapListeners() {
        aMap.setOnMapClickListener(latLng -> {
            for (Marker marker : locationMarkers.values()) {
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                }
            }
        });
        aMap.setOnMarkerClickListener(marker -> {
            Object related = marker.getObject();
            if (related instanceof LocationDto) {
                return handleMarkerClick((LocationDto) related, marker);
            }
            return false;
        });
        aMap.setOnInfoWindowClickListener(marker -> {
            Object related = marker.getObject();
            if (related instanceof LocationDto && infoWindowActionListener != null) {
                infoWindowActionListener.onDetail((LocationDto) related);
            }
        });
        aMap.setInfoWindowAdapter(new AMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return buildInfoWindow(marker);
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
    }

    public void setMarkerClickListener(@Nullable MarkerClickListener listener) {
        this.clickListener = listener;
    }

    public void setPoiRenderedCallback(@Nullable PoiRenderedCallback callback) {
        this.renderedCallback = callback;
    }

    public void setDistanceProvider(@Nullable DistanceProvider provider) {
        this.distanceProvider = provider;
    }

    public void setInfoWindowActionListener(@Nullable PoiInfoWindow.ActionListener listener) {
        this.infoWindowActionListener = listener;
    }

    public void setGeofenceVisible(boolean visible) {
        this.geofenceVisible = visible;
        for (Circle circle : geofenceOverlays) {
            circle.setVisible(visible);
        }
    }

    public boolean isGeofenceVisible() {
        return geofenceVisible;
    }

    public void toggleGeofenceVisible() {
        geofenceVisible = !geofenceVisible;
    }

    public void render(@NonNull List<LocationDto> locations,
                       @NonNull DisplayPointResolver displayPointResolver,
                       int defaultRadiusMeters) {
        clearOverlays();

        BitmapDescriptor pinIcon = descriptorFromDrawable(R.drawable.ic_map_pin);
        BitmapDescriptor selectedPinIcon = descriptorFromDrawable(R.drawable.ic_map_pin_selected);

        for (LocationDto location : locations) {
            GeoPoint point = displayPointResolver.resolve(location);
            if (point == null) {
                continue;
            }
            int radius = location.radiusMeters > 0 ? location.radiusMeters : defaultRadiusMeters;

            if (renderedCallback != null) {
                renderedCallback.onPoiRendered(location, point);
            }

            Circle circle = aMap.addCircle(new CircleOptions()
                    .center(CampusMapManager.toLatLng(point))
                    .radius(radius)
                    .fillColor(ContextCompat.getColor(context, R.color.brand_fence_fill))
                    .strokeColor(ContextCompat.getColor(context, R.color.brand_fence_stroke))
                    .strokeWidth(3f)
                    .zIndex(50f)
                    .visible(geofenceVisible));
            geofenceOverlays.add(circle);

            MarkerOptions options = new MarkerOptions()
                    .position(CampusMapManager.toLatLng(point))
                    .title(location.name == null ? "" : location.name)
                    .snippet(buildInfoSnippet(location))
                    // 与围栏圆心一致：管理端/后端 GCJ 坐标即圆心与 Marker 锚点
                    .anchor(0.5f, 0.5f);
            if (pinIcon != null) {
                options.icon(pinIcon);
            }

            Marker marker = aMap.addMarker(options);
            if (marker == null) {
                continue;
            }
            marker.setObject(location);
            locationMarkers.put(location.id, marker);
        }

        if (selectedLocationId >= 0) {
            applySelectionIcon(selectedLocationId, pinIcon, selectedPinIcon);
        }
    }

    public void focusMarker(long locationId) {
        Marker marker = locationMarkers.get(locationId);
        if (marker == null) {
            return;
        }
        setSelectedLocationId(locationId);
        marker.showInfoWindow();
        aMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
    }

    public void setSelectedLocationId(long locationId) {
        if (selectedLocationId == locationId) {
            return;
        }
        selectedLocationId = locationId;
        applySelectionIcon(
                locationId,
                descriptorFromDrawable(R.drawable.ic_map_pin),
                descriptorFromDrawable(R.drawable.ic_map_pin_selected)
        );
    }

    @Nullable
    public Marker getMarker(long locationId) {
        return locationMarkers.get(locationId);
    }

    @Nullable
    public GeoPoint getMarkerPosition(long locationId) {
        Marker marker = locationMarkers.get(locationId);
        if (marker == null || marker.getPosition() == null) {
            return null;
        }
        return new GeoPoint(marker.getPosition().latitude, marker.getPosition().longitude);
    }

    public void clearOverlays() {
        for (Marker marker : locationMarkers.values()) {
            marker.remove();
        }
        locationMarkers.clear();

        for (Circle circle : geofenceOverlays) {
            circle.remove();
        }
        geofenceOverlays.clear();
        selectedLocationId = -1L;
    }

    private void applySelectionIcon(long selectedId,
                                    @Nullable BitmapDescriptor normalPin,
                                    @Nullable BitmapDescriptor selectedPin) {
        for (Map.Entry<Long, Marker> entry : locationMarkers.entrySet()) {
            Marker marker = entry.getValue();
            if (entry.getKey() == selectedId) {
                if (selectedPin != null) {
                    marker.setIcon(selectedPin);
                }
            } else if (normalPin != null) {
                marker.setIcon(normalPin);
            }
        }
    }

    @Nullable
    private BitmapDescriptor descriptorFromDrawable(int resId) {
        Drawable drawable = AppCompatResources.getDrawable(context, resId);
        if (drawable == null) {
            return null;
        }
        int width = drawable.getIntrinsicWidth() > 0
                ? drawable.getIntrinsicWidth()
                : Math.round(28 * context.getResources().getDisplayMetrics().density);
        int height = drawable.getIntrinsicHeight() > 0
                ? drawable.getIntrinsicHeight()
                : Math.round(32 * context.getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @NonNull
    private String buildInfoSnippet(@NonNull LocationDto location) {
        String address = location.address == null ? "" : location.address.trim();
        if (!address.isEmpty()) {
            return shorten(address, 48);
        }
        String category = location.category == null ? "" : location.category.trim();
        String desc = location.description == null ? "" : location.description.trim();
        if (!category.isEmpty() && !desc.isEmpty()) {
            return category + " | " + shorten(desc, 36);
        }
        if (!desc.isEmpty()) {
            return shorten(desc, 48);
        }
        return category.isEmpty() ? "\u70b9\u51fb\u67e5\u770b\u7b80\u4ecb" : category;
    }

    @NonNull
    private String shorten(@NonNull String text, int max) {
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private boolean handleMarkerClick(@NonNull LocationDto location, @NonNull Marker marker) {
        setSelectedLocationId(location.id);
        Double distance = distanceProvider == null ? null : distanceProvider.distanceTo(location);
        if (clickListener != null) {
            clickListener.onMarkerClicked(location, distance);
        }
        marker.showInfoWindow();
        return true;
    }

    private View buildInfoWindow(@NonNull Marker marker) {
        View root = LayoutInflater.from(context).inflate(R.layout.marker_info_window, null, false);
        root.setClickable(true);
        root.setFocusable(true);
        TextView title = root.findViewById(R.id.bubble_title);
        TextView desc = root.findViewById(R.id.bubble_description);
        title.setText(TextUtils.isEmpty(marker.getTitle()) ? "" : marker.getTitle());
        desc.setText(TextUtils.isEmpty(marker.getSnippet()) ? "" : marker.getSnippet());

        View btnGuide = root.findViewById(R.id.btn_poi_guide);
        View btnRoute = root.findViewById(R.id.btn_poi_route);
        View btnDetail = root.findViewById(R.id.btn_poi_detail);

        Object related = marker.getObject();
        LocationDto location = related instanceof LocationDto ? (LocationDto) related : null;
        if (location != null && infoWindowActionListener != null) {
            if (btnGuide != null) {
                btnGuide.setOnClickListener(v -> infoWindowActionListener.onGuide(location));
            }
            if (btnRoute != null) {
                btnRoute.setOnClickListener(v -> {
                    marker.hideInfoWindow();
                    infoWindowActionListener.onRoute(location);
                });
            }
            if (btnDetail != null) {
                btnDetail.setOnClickListener(v -> {
                    marker.hideInfoWindow();
                    infoWindowActionListener.onDetail(location);
                });
            }
        } else {
            View actions = btnGuide == null ? null : (View) btnGuide.getParent();
            if (actions != null) {
                actions.setVisibility(View.GONE);
            }
        }
        return root;
    }

    public interface DistanceProvider {
        @Nullable
        Double distanceTo(@NonNull LocationDto location);
    }
}
