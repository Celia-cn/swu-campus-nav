package com.example.gps.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.UserDto;
import com.example.gps.storage.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 800L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigated;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SessionManager.from(this).syncBaseUrlFromConfig();
        handler.postDelayed(this::navigateNext, SPLASH_DELAY_MS);
    }

    private void navigateNext() {
        if (navigated || isFinishing()) {
            return;
        }
        SessionManager session = SessionManager.from(this);
        if (!session.isLoggedIn()) {
            openLogin();
            return;
        }
        verifySessionAndNavigate(session);
    }

    private void verifySessionAndNavigate(SessionManager session) {
        ApiService api = ApiClient.service(this);
        api.me().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<UserDto>> call, Response<ApiResponse<UserDto>> response) {
                ApiResponse<UserDto> body = response.body();
                if (response.isSuccessful() && body != null && body.code == 0 && body.data != null) {
                    openMap();
                    return;
                }
                session.clearAuth();
                Toast.makeText(SplashActivity.this, R.string.toast_auth_expired, Toast.LENGTH_SHORT).show();
                openLogin();
            }

            @Override
            public void onFailure(Call<ApiResponse<UserDto>> call, Throwable throwable) {
                // 网络异常时仍进入地图，避免已登录用户被强制看到登录页
                openMap();
            }
        });
    }

    private void openMap() {
        if (navigated) {
            return;
        }
        navigated = true;
        Intent intent = new Intent(this, MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openLogin() {
        if (navigated) {
            return;
        }
        navigated = true;
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
