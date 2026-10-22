package com.example.gps.map;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gps.R;
import com.example.gps.network.dto.LocationDto;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

/**
 * 景点 Marker 地图气泡：标题、简介，以及讲解 / 路线 / 详情按钮。
 */
public class PoiInfoWindow extends MarkerInfoWindow {

    public interface ActionListener {
        void onGuide(@NonNull LocationDto location);

        void onRoute(@NonNull LocationDto location);

        void onDetail(@NonNull LocationDto location);
    }

    @Nullable
    private ActionListener actionListener;

    public PoiInfoWindow(int layoutResId, @NonNull MapView mapView) {
        super(layoutResId, mapView);
    }

    public void setActionListener(@Nullable ActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public void onOpen(Object item) {
        if (!(item instanceof Marker)) {
            close();
            return;
        }
        Marker marker = (Marker) item;
        Object related = marker.getRelatedObject();
        if (!(related instanceof LocationDto)) {
            close();
            return;
        }
        LocationDto location = (LocationDto) related;
        super.onOpen(item);

        View root = mView;
        TextView btnGuide = root.findViewById(R.id.btn_poi_guide);
        TextView btnRoute = root.findViewById(R.id.btn_poi_route);
        TextView btnDetail = root.findViewById(R.id.btn_poi_detail);

        btnGuide.setOnClickListener(v -> {
            android.util.Log.d("PoiInfoWindow", "点击讲解按钮: " + location.name);
            if (actionListener != null) {
                actionListener.onGuide(location);
            }
        });
        btnRoute.setOnClickListener(v -> {
            android.util.Log.d("PoiInfoWindow", "点击路线按钮: " + location.name);
            if (actionListener != null) {
                actionListener.onRoute(location);
            }
        });
        btnDetail.setOnClickListener(v -> {
            android.util.Log.d("PoiInfoWindow", "点击详情按钮: " + location.name);
            if (actionListener != null) {
                actionListener.onDetail(location);
            }
        });
    }
}
