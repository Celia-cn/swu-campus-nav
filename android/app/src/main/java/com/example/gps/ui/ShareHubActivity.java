package com.example.gps.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.SharePostDto;
import com.example.gps.storage.SessionManager;
import com.example.gps.ui.adapter.SharePostListAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareHubActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "category";

    private ApiService api;
    private SharePostListAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView chipFood;
    private TextView chipHotel;
    private String category = "FOOD";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.from(this).isLoggedIn()) {
            Toast.makeText(this, R.string.toast_feedback_need_login, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_share_hub);
        api = ApiClient.service(this);

        String initial = getIntent().getStringExtra(EXTRA_CATEGORY);
        if ("HOTEL".equalsIgnoreCase(initial)) {
            category = "HOTEL";
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        chipFood = findViewById(R.id.chipFood);
        chipHotel = findViewById(R.id.chipHotel);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        RecyclerView rv = findViewById(R.id.rvPosts);
        FloatingActionButton fab = findViewById(R.id.fabCreate);

        adapter = new SharePostListAdapter(post ->
                startActivity(SharePostDetailActivity.buildIntent(this, post.id)));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        chipFood.setOnClickListener(v -> switchCategory("FOOD"));
        chipHotel.setOnClickListener(v -> switchCategory("HOTEL"));
        swipeRefresh.setOnRefreshListener(this::loadPosts);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShareCreateActivity.class);
            intent.putExtra(EXTRA_CATEGORY, category);
            startActivity(intent);
        });

        updateChipUi();
        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void switchCategory(@NonNull String cat) {
        category = cat;
        updateChipUi();
        loadPosts();
    }

    private void updateChipUi() {
        boolean food = "FOOD".equals(category);
        chipFood.setBackgroundResource(food ? R.drawable.bg_nav_active : R.drawable.bg_service_pill);
        chipFood.setTextColor(ContextCompat.getColor(this, food ? R.color.brand_primary_dark : R.color.text_sub));
        chipHotel.setBackgroundResource(food ? R.drawable.bg_service_pill : R.drawable.bg_nav_active);
        chipHotel.setTextColor(ContextCompat.getColor(this, food ? R.color.text_sub : R.color.brand_primary_dark));
        setTitle(food ? R.string.share_hub_food_title : R.string.share_hub_hotel_title);
    }

    private void loadPosts() {
        swipeRefresh.setRefreshing(true);
        api.sharePosts(category).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SharePostDto>>> call,
                                   Response<ApiResponse<List<SharePostDto>>> response) {
                swipeRefresh.setRefreshing(false);
                ApiResponse<List<SharePostDto>> body = response.body();
                if (response.isSuccessful() && body != null && body.code == 0 && body.data != null) {
                    adapter.submit(body.data);
                } else {
                    toast(getString(R.string.toast_load_failed));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SharePostDto>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                toast(getString(R.string.toast_network_error));
            }
        });
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
