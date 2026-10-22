package com.example.gps.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 定位权限工具：统一处理 ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION。
 */
public final class PermissionUtil {

    public static final int REQ_LOCATION = 101;

    private PermissionUtil() {
    }

    public static boolean hasLocationPermission(@NonNull Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(@NonNull Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQ_LOCATION
        );
    }

    public static boolean isLocationPermissionGranted(int requestCode,
                                                      @NonNull int[] grantResults) {
        if (requestCode != REQ_LOCATION || grantResults.length == 0) {
            return false;
        }
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }
}
