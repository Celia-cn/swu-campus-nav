package com.example.gps.location;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

/**
 * 高德定位 SDK 封装：负责 AMapLocationClient 初始化、生命周期与连续定位。
 */
public class LocationService implements AMapLocationListener {

    public interface Callback {
        void onLocationUpdated(@NonNull AMapLocation location);

        void onLocationFailed(int errorCode, @NonNull String errorInfo);
    }

    private final Context appContext;
    @Nullable
    private AMapLocationClient locationClient;
    @Nullable
    private AMapLocationClientOption locationOption;
    @Nullable
    private Callback callback;
    private boolean updating;

    public LocationService(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public boolean initialize() {
        if (locationClient != null) {
            return true;
        }
        try {
            // 【修复闪退关键】在初始化 Client 之前，必须调用这两个隐私合规接口！
            // 否则新版高德 SDK 会直接导致 APP 闪退或初始化失败。
            AMapLocationClient.updatePrivacyShow(appContext, true, true);
            AMapLocationClient.updatePrivacyAgree(appContext, true);

            locationClient = new AMapLocationClient(appContext);
        } catch (Exception e) {
            e.printStackTrace(); // 建议加上日志打印，方便排查
            locationClient = null;
            return false;
        }

        locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        locationOption.setInterval(3000L);
        locationOption.setOnceLocation(false);
        locationOption.setNeedAddress(true);
        locationOption.setMockEnable(true);
        locationOption.setGpsFirst(true);
        locationOption.setHttpTimeOut(8000L);
        locationClient.setLocationOption(locationOption);
        locationClient.setLocationListener(this);
        return true;
    }

    public void start() {
        if (locationClient == null && !initialize()) {
            return;
        }
        if (locationClient == null) {
            return;
        }
        locationClient.startLocation();
        updating = true;
    }

    public void stop() {
        if (locationClient != null) {
            locationClient.stopLocation();
        }
        updating = false;
    }

    public boolean isUpdating() {
        return updating;
    }

    public void destroy() {
        stop();
        if (locationClient != null) {
            locationClient.onDestroy();
            locationClient = null;
        }
        locationOption = null;
    }

    @Nullable
    public String describeLocation(@NonNull AMapLocation location) {
        StringBuilder builder = new StringBuilder();
        if (location.getAddress() != null && !location.getAddress().isEmpty()) {
            builder.append(location.getAddress());
        } else {
            builder.append(String.format(java.util.Locale.CHINA, "%.6f, %.6f",
                    location.getLatitude(), location.getLongitude()));
        }
        if (location.getSpeed() >= 0) {
            builder.append(String.format(java.util.Locale.CHINA, " · %.1f m/s", location.getSpeed()));
        }
        return builder.toString();
    }

    @Override
    public void onLocationChanged(AMapLocation location) {
        if (callback == null || location == null) {
            return;
        }
        if (location.getErrorCode() == 0) {
            callback.onLocationUpdated(location);
        } else {
            String info = location.getErrorInfo() == null ? "" : location.getErrorInfo();
            callback.onLocationFailed(location.getErrorCode(), info);
        }
    }
}
