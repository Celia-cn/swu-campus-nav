package com.example.gps.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiErrorParser;
import com.example.gps.network.ApiService;
import com.example.gps.storage.SessionManager;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.LocationDto;
import com.example.gps.network.dto.LocationLikeDto;
import com.example.gps.ui.adapter.SpotIntroAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SpotsIntroActivity extends AppCompatActivity {

    public static final String EXTRA_LOCATION_ID = "location_id";

    private ApiService apiService;
    private SpotIntroAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progress;
    private final List<LocationDto> allSpots = new ArrayList<>();
    private boolean liking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.from(this).isLoggedIn()) {
            Toast.makeText(this, R.string.toast_feedback_need_login, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_spots_intro);
        apiService = ApiClient.service(this);
        applyWindowInsets();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipeRefresh);
        progress = findViewById(R.id.progress);
        RecyclerView rvSpots = findViewById(R.id.rvSpots);
        adapter = new SpotIntroAdapter(new SpotIntroAdapter.Listener() {
            @Override
            public void onSpotClick(LocationDto location) {
                startActivity(SpotDetailActivity.buildIntent(
                        SpotsIntroActivity.this, location, null, null));
            }

            @Override
            public void onLikeClick(LocationDto location, int position) {
                toggleLike(location);
            }
        });
        rvSpots.setLayoutManager(new LinearLayoutManager(this));
        rvSpots.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadSpots);
        loadSpots();
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.spotsRoot);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (root == null || toolbar == null) {
            return;
        }
        final int toolbarPadTop = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    toolbarPadTop + bars.top,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void loadSpots() {
        progress.setVisibility(View.VISIBLE);
        apiService.locations().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<LocationDto>>> call,
                                   @NonNull Response<ApiResponse<List<LocationDto>>> response) {
                progress.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                ApiResponse<List<LocationDto>> body = response.body();
                if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                    Toast.makeText(SpotsIntroActivity.this, R.string.toast_load_spots_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                allSpots.clear();
                allSpots.addAll(filterGuidedSpots(body.data));
                submitSorted();
                if (allSpots.isEmpty()) {
                    Toast.makeText(SpotsIntroActivity.this, R.string.toast_no_guide_spots, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<LocationDto>>> call, @NonNull Throwable t) {
                progress.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(SpotsIntroActivity.this, R.string.toast_load_spots_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleLike(LocationDto location) {
        if (liking) {
            return;
        }
        if (!SessionManager.from(this).isLoggedIn()) {
            Toast.makeText(this, R.string.toast_feedback_need_login, Toast.LENGTH_SHORT).show();
            return;
        }
        liking = true;
        apiService.toggleLocationLike(location.id, Collections.emptyMap()).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LocationLikeDto>> call,
                                   @NonNull Response<ApiResponse<LocationLikeDto>> response) {
                liking = false;
                ApiResponse<LocationLikeDto> body = response.body();
                if (body == null && !response.isSuccessful()) {
                    body = parseLikeErrorBody(response);
                }
                if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                    showLikeError(response, body);
                    return;
                }
                location.likeCount = body.data.likeCount;
                location.liked = body.data.liked;
                submitSorted();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LocationLikeDto>> call, @NonNull Throwable t) {
                liking = false;
                String hint = t.getMessage() != null ? t.getMessage() : "";
                Toast.makeText(SpotsIntroActivity.this,
                        getString(R.string.toast_like_failed) + "（" + hint + "）",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLikeError(Response<ApiResponse<LocationLikeDto>> response,
                             @Nullable ApiResponse<LocationLikeDto> body) {
        String serverMsg = body != null ? body.message : ApiErrorParser.message(response);
        if (serverMsg != null && serverMsg.contains("接口不存在")) {
            Toast.makeText(this, R.string.toast_like_need_restart_backend, Toast.LENGTH_LONG).show();
            return;
        }
        if (serverMsg != null && !serverMsg.isBlank()) {
            Toast.makeText(this, serverMsg, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.toast_like_failed, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private ApiResponse<LocationLikeDto> parseLikeErrorBody(Response<ApiResponse<LocationLikeDto>> response) {
        String msg = ApiErrorParser.message(response);
        if (msg == null) {
            return null;
        }
        ApiResponse<LocationLikeDto> err = new ApiResponse<>();
        err.message = msg;
        err.code = response.code();
        return err;
    }

    private void submitSorted() {
        List<LocationDto> sorted = new ArrayList<>(allSpots);
        sorted.sort(Comparator
                .comparingLong((LocationDto loc) -> -Math.max(0, loc.likeCount))
                .thenComparing(loc -> loc.name == null ? "" : loc.name));
        adapter.submit(sorted);
    }

    private List<LocationDto> filterGuidedSpots(List<LocationDto> source) {
        List<LocationDto> result = new ArrayList<>();
        for (LocationDto item : source) {
            if (item == null) {
                continue;
            }
            boolean hasGuide = !TextUtils.isEmpty(item.audioUrl)
                    || !TextUtils.isEmpty(item.description);
            if (hasGuide) {
                result.add(item);
            }
        }
        return result;
    }
}
