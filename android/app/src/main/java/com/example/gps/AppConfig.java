package com.example.gps;

import android.content.Context;
import android.os.Build;

import com.example.gps.storage.SessionManager;

import java.util.Locale;

/**
 * 后端地址：模拟器用 10.0.2.2；真机见 strings.xml 的 backend_base_url（可用 adb reverse）。
 */
public final class AppConfig {

    public static final String EMULATOR_BASE_URL = "http://10.0.2.2:8091/";

    private AppConfig() {
    }

    public static String defaultBaseUrl(Context context) {
        if (isProbablyEmulator()) {
            return EMULATOR_BASE_URL;
        }
        String configured = context.getString(R.string.backend_base_url).trim();
        if (!configured.isEmpty()) {
            return SessionManager.normalizeBaseUrl(configured, EMULATOR_BASE_URL);
        }
        return SessionManager.normalizeBaseUrl("http://127.0.0.1:8091/", EMULATOR_BASE_URL);
    }

    public static boolean isProbablyEmulator() {
        String fingerprint = value(Build.FINGERPRINT);
        String model = value(Build.MODEL);
        String manufacturer = value(Build.MANUFACTURER);
        String brand = value(Build.BRAND);
        String device = value(Build.DEVICE);
        String product = value(Build.PRODUCT);
        String hardware = value(Build.HARDWARE);

        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || fingerprint.contains("sdk_gphone")
                || fingerprint.contains("emu64")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for x86")
                || model.contains("sdk_gphone")
                || manufacturer.contains("genymotion")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || device.contains("emu")
                || product.contains("google_sdk")
                || product.contains("sdk_gphone")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("qemu");
    }

    private static String value(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }
}
