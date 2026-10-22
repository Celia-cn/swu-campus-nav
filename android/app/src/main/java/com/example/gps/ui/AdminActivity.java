package com.example.gps.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.AdminStatsDto;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.LocationDto;
import com.example.gps.network.dto.LocationUpsertReq;
import com.example.gps.storage.SessionManager;
import com.example.gps.ui.adapter.AdminLocationAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvStats;
    private EditText etName;
    private EditText etCategory;
    private EditText etAddress;
    private EditText etLat;
    private EditText etLng;
    private EditText etRadius;
    private EditText etDesc;
    private EditText etAudioUrl;
    private EditText etImageUrl;
    private MaterialButton btnSaveLocation;

    private SessionManager session;
    private ApiService api;
    private AdminLocationAdapter adapter;
    private final List<LocationDto> locationCache = new ArrayList<>();
    private long editingId = -1L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        session = SessionManager.from(this);
        if (!session.isLoggedIn() || !session.isAdmin()) {
            toast("需要管理员权限");
            finish();
            return;
        }
        api = ApiClient.service(this);

        bindViews();
        initToolbar();
        initList();
        initActions();
        loadAll();
    }

    private void bindViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        tvStats = findViewById(R.id.tvStats);
        etName = findViewById(R.id.etName);
        etCategory = findViewById(R.id.etCategory);
        etAddress = findViewById(R.id.etAddress);
        etLat = findViewById(R.id.etLat);
        etLng = findViewById(R.id.etLng);
        etRadius = findViewById(R.id.etRadius);
        etDesc = findViewById(R.id.etDesc);
        etAudioUrl = findViewById(R.id.etAudioUrl);
        etImageUrl = findViewById(R.id.etImageUrl);
        btnSaveLocation = findViewById(R.id.btnSaveLocation);
    }

    private void initToolbar() {
        MaterialToolbar topBar = findViewById(R.id.adminTopBar);
        topBar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        topBar.setNavigationOnClickListener(v -> finish());
    }

    private void initList() {
        RecyclerView recyclerView = findViewById(R.id.rvLocations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminLocationAdapter(new AdminLocationAdapter.ActionListener() {
            @Override
            public void onSelect(LocationDto location) {
                fillForm(location);
            }

            @Override
            public void onDelete(LocationDto location) {
                confirmDelete(location);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void initActions() {
        swipeRefreshLayout.setOnRefreshListener(this::loadAll);
        btnSaveLocation.setOnClickListener(v -> submitUpsert());
    }

    private void loadAll() {
        swipeRefreshLayout.setRefreshing(true);
        loadStats();
        loadLocations();
    }

    private void loadStats() {
        api.stats().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AdminStatsDto>> call, Response<ApiResponse<AdminStatsDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                    doneRefresh();
                    return;
                }
                AdminStatsDto s = response.body().data;
                StringBuilder builder = new StringBuilder();
                builder.append("用户总数：").append(s.totalUsers).append('\n');
                builder.append("地点总数：").append(s.totalLocations).append('\n');
                builder.append("讲解总次数：").append(s.totalPlayCount).append('\n');
                builder.append("今日讲解次数：").append(s.todayPlayCount).append('\n');
                builder.append("自动讲解次数：").append(s.autoPlayCount).append('\n');
                builder.append("手动讲解次数：").append(s.manualPlayCount).append('\n');
                if (s.hotLocations != null && !s.hotLocations.isEmpty()) {
                    builder.append("热门讲解点：\n");
                    for (int i = 0; i < s.hotLocations.size(); i++) {
                        builder.append(i + 1).append(". ")
                                .append(s.hotLocations.get(i).locationName)
                                .append(" (").append(s.hotLocations.get(i).playCount).append(")\n");
                    }
                }
                tvStats.setText(builder.toString());
                doneRefresh();
            }

            @Override
            public void onFailure(Call<ApiResponse<AdminStatsDto>> call, Throwable throwable) {
                doneRefresh();
            }
        });
    }

    private void loadLocations() {
        api.locations().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LocationDto>>> call, Response<ApiResponse<List<LocationDto>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().code != 0 || response.body().data == null) {
                    toast("加载地点失败");
                    doneRefresh();
                    return;
                }
                locationCache.clear();
                locationCache.addAll(response.body().data);
                adapter.submit(locationCache);
                doneRefresh();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LocationDto>>> call, Throwable throwable) {
                toast("网络异常：" + throwable.getMessage());
                doneRefresh();
            }
        });
    }

    private void submitUpsert() {
        String name = text(etName);
        String category = text(etCategory);
        String address = text(etAddress);
        String latText = text(etLat);
        String lngText = text(etLng);
        String radiusText = text(etRadius);
        String desc = text(etDesc);
        String audio = text(etAudioUrl);
        String image = text(etImageUrl);

        if (TextUtils.isEmpty(name)
                || TextUtils.isEmpty(category)
                || TextUtils.isEmpty(latText)
                || TextUtils.isEmpty(lngText)
                || TextUtils.isEmpty(radiusText)
                || TextUtils.isEmpty(desc)) {
            toast("请填写所有必填项");
            return;
        }

        double lat;
        double lng;
        int radius;
        try {
            lat = Double.parseDouble(latText);
            lng = Double.parseDouble(lngText);
            radius = Integer.parseInt(radiusText);
        } catch (Exception e) {
            toast("纬度/经度/半径格式错误");
            return;
        }

        LocationUpsertReq req = new LocationUpsertReq(
                name,
                category,
                emptyToNull(address),
                lat,
                lng,
                radius,
                120,
                desc,
                emptyToNull(audio),
                emptyToNull(image)
        );

        Call<ApiResponse<LocationDto>> call = editingId > 0
                ? api.updateLocation(editingId, req)
                : api.createLocation(req);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<LocationDto>> call, Response<ApiResponse<LocationDto>> response) {
                ApiResponse<LocationDto> body = response.body();
                if (!response.isSuccessful() || body == null || body.code != 0) {
                    toast("保存失败");
                    return;
                }
                toast(editingId > 0 ? "地点更新成功" : "地点创建成功");
                clearForm();
                loadLocations();
                loadStats();
            }

            @Override
            public void onFailure(Call<ApiResponse<LocationDto>> call, Throwable throwable) {
                toast("网络异常：" + throwable.getMessage());
            }
        });
    }

    private void confirmDelete(LocationDto location) {
        new AlertDialog.Builder(this)
                .setTitle("删除地点")
                .setMessage("确定删除「" + location.name + "」吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> deleteLocation(location))
                .show();
    }

    private void deleteLocation(LocationDto location) {
        api.deleteLocation(location.id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().code != 0) {
                    toast("删除失败");
                    return;
                }
                toast("已删除");
                if (editingId == location.id) {
                    clearForm();
                }
                loadLocations();
                loadStats();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                toast("网络异常：" + throwable.getMessage());
            }
        });
    }

    private void fillForm(LocationDto location) {
        editingId = location.id;
        etName.setText(location.name);
        etCategory.setText(location.category == null ? "" : location.category);
        etAddress.setText(location.address == null ? "" : location.address);
        etLat.setText(String.valueOf(location.latitude));
        etLng.setText(String.valueOf(location.longitude));
        etRadius.setText(String.valueOf(location.radiusMeters));
        etDesc.setText(location.description);
        etAudioUrl.setText(location.audioUrl == null ? "" : location.audioUrl);
        etImageUrl.setText(location.imageUrl == null ? "" : location.imageUrl);
        btnSaveLocation.setText(R.string.btn_update_location);
    }

    private void clearForm() {
        editingId = -1L;
        etName.setText("");
        etCategory.setText("");
        etAddress.setText("");
        etLat.setText("");
        etLng.setText("");
        etRadius.setText("");
        etDesc.setText("");
        etAudioUrl.setText("");
        etImageUrl.setText("");
        btnSaveLocation.setText(R.string.btn_save_location);
    }

    private void doneRefresh() {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private String text(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String emptyToNull(String value) {
        return TextUtils.isEmpty(value) ? null : value;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
