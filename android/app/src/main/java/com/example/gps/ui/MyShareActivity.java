package com.example.gps.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.gps.ui.adapter.SharePostListAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** 我的 → 我的分享：查看/编辑/删除自己发布的帖子 */
public class MyShareActivity extends AppCompatActivity {

    private ApiService api;
    private SharePostListAdapter adapter;
    private String category = "FOOD";
    private TextView chipFood;
    private TextView chipHotel;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_hub);
        setTitle(R.string.profile_menu_my_share);
        api = ApiClient.service(this);

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
        swipeRefresh.setOnRefreshListener(this::load);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShareCreateActivity.class);
            intent.putExtra(ShareCreateActivity.EXTRA_CATEGORY, category);
            startActivity(intent);
        });
        updateChipUi();
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void switchCategory(String cat) {
        category = cat;
        updateChipUi();
        load();
    }

    private void updateChipUi() {
        boolean food = "FOOD".equals(category);
        chipFood.setBackgroundResource(food ? R.drawable.bg_nav_active : R.drawable.bg_service_pill);
        chipFood.setTextColor(ContextCompat.getColor(this, food ? R.color.brand_primary_dark : R.color.text_sub));
        chipHotel.setBackgroundResource(food ? R.drawable.bg_service_pill : R.drawable.bg_nav_active);
        chipHotel.setTextColor(ContextCompat.getColor(this, food ? R.color.text_sub : R.color.brand_primary_dark));
    }

    private void load() {
        swipeRefresh.setRefreshing(true);
        api.mySharePosts(category).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SharePostDto>>> call,
                                   Response<ApiResponse<List<SharePostDto>>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    adapter.submit(response.body().data);
                } else {
                    Toast.makeText(MyShareActivity.this, R.string.toast_load_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SharePostDto>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MyShareActivity.this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
