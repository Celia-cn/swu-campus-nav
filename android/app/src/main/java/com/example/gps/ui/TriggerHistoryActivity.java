package com.example.gps.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.TriggerHistoryDto;
import com.example.gps.ui.adapter.TriggerHistoryAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TriggerHistoryActivity extends AppCompatActivity {

    private static final String TAG = "TriggerHistory";

    private long locationId;
    private String locationName;
    private ListView listView;
    private TextView tvEmpty;
    private List<TriggerHistoryDto> historyList;
    private TriggerHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trigger_history);

        locationId = getIntent().getLongExtra("location_id", -1);
        locationName = getIntent().getStringExtra("location_name");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(locationName + " - 触发历史");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        loadTriggerHistory();
    }

    private void initViews() {
        listView = findViewById(R.id.listView);
        tvEmpty = findViewById(R.id.tvEmpty);
        historyList = new ArrayList<>();
        adapter = new TriggerHistoryAdapter(this, historyList);
        listView.setAdapter(adapter);
    }

    private void loadTriggerHistory() {
        ApiService apiService = ApiClient.service(this);
        apiService.getTriggerHistory(locationId, 50).enqueue(new Callback<ApiResponse<List<TriggerHistoryDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TriggerHistoryDto>>> call, Response<ApiResponse<List<TriggerHistoryDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().code == 0) {
                    List<TriggerHistoryDto> data = response.body().data;
                    if (data != null && !data.isEmpty()) {
                        historyList.clear();
                        historyList.addAll(data);
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(TextView.GONE);
                        listView.setVisibility(ListView.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(TextView.VISIBLE);
                        listView.setVisibility(ListView.GONE);
                    }
                } else {
                    Toast.makeText(TriggerHistoryActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<TriggerHistoryDto>>> call, Throwable t) {
                Log.e(TAG, "加载触发历史失败", t);
                Toast.makeText(TriggerHistoryActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}