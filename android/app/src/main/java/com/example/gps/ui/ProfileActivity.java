package com.example.gps.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.RecentVisitDto;
import com.example.gps.network.dto.UserDto;
import com.example.gps.network.dto.UserStatsDto;
import com.example.gps.storage.FeedbackStore;
import com.example.gps.storage.SearchHistoryStore;
import com.example.gps.storage.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private ApiService apiService;
    private SearchHistoryStore searchHistoryStore;
    private FeedbackStore feedbackStore;

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvAvatar;
    private TextView tvUsername;
    private TextView tvRole;
    private TextView tvJoinDate;
    private TextView tvStatPlay;
    private TextView tvStatVisited;
    private TextView tvStatAuto;
    private LinearLayout layoutRecentVisits;
    private TextView tvRecentEmpty;
    private View dividerAdmin;
    private View menuAdmin;

    private String currentUsername = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = SessionManager.from(this);
        apiService = ApiClient.service(this);
        searchHistoryStore = new SearchHistoryStore(this);
        feedbackStore = new FeedbackStore(this);

        bindViews();
        setupToolbar();
        setupMenus();
        btnLogoutClick();

        if (!session.isLoggedIn()) {
            gotoLogin();
            return;
        }
        showCachedProfile();
        loadProfile(true);
    }

    private void bindViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvAvatar = findViewById(R.id.tvAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvRole = findViewById(R.id.tvRole);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        tvStatPlay = findViewById(R.id.tvStatPlay);
        tvStatVisited = findViewById(R.id.tvStatVisited);
        tvStatAuto = findViewById(R.id.tvStatAuto);
        layoutRecentVisits = findViewById(R.id.layoutRecentVisits);
        tvRecentEmpty = findViewById(R.id.tvRecentEmpty);
        dividerAdmin = findViewById(R.id.dividerAdmin);
        menuAdmin = findViewById(R.id.menuAdmin);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.brand_accent));
        swipeRefresh.setOnRefreshListener(() -> loadProfile(false));
    }

    private void setupMenus() {
        bindMenuRow(findViewById(R.id.menuChangePassword), R.string.profile_menu_change_password,
                v -> openChangePassword());
        bindMenuRow(findViewById(R.id.menuSearchHistory), R.string.profile_menu_search_history,
                v -> showSearchHistoryDialog());
        bindMenuRow(findViewById(R.id.menuFeedback), R.string.profile_menu_feedback,
                v -> showFeedbackDialog());
        bindMenuRow(findViewById(R.id.menuMyShare), R.string.profile_menu_my_share,
                v -> startActivity(new Intent(this, MyShareActivity.class)));
        bindMenuRow(findViewById(R.id.menuUserAgreement), R.string.profile_menu_user_agreement,
                v -> showAgreementDialog(R.string.title_user_agreement, R.string.content_user_agreement));
        bindMenuRow(findViewById(R.id.menuPrivacy), R.string.profile_menu_privacy,
                v -> showAgreementDialog(R.string.title_privacy_policy, R.string.content_privacy_policy));
        bindMenuRow(findViewById(R.id.menuAbout), R.string.profile_menu_about,
                v -> showAboutDialog());

        dividerAdmin.setVisibility(View.GONE);
        menuAdmin.setVisibility(View.GONE);
    }

    private void bindMenuRow(@NonNull View row, @StringRes int titleRes, @NonNull View.OnClickListener listener) {
        TextView title = (TextView) ((ViewGroup) row).getChildAt(0);
        title.setText(titleRes);
        row.setOnClickListener(listener);
    }

    private void btnLogoutClick() {
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void showCachedProfile() {
        currentUsername = session.getUsername();
        bindUserHeader(currentUsername, session.getRole(), null);
        bindStats(0, 0, 0);
    }

    private void loadProfile(boolean showRefreshing) {
        if (showRefreshing) {
            swipeRefresh.setRefreshing(true);
        }
        apiService.me().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserDto>> call,
                                   @NonNull Response<ApiResponse<UserDto>> response) {
                ApiResponse<UserDto> body = response.body();
                if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                    finishRefresh();
                    if (response.code() == 401) {
                        session.clearAuth();
                        gotoLogin();
                    }
                    return;
                }
                UserDto user = body.data;
                currentUsername = user.username;
                session.saveAuth(session.getToken(), user.role, user.username, user.id);
                bindUserHeader(user.username, user.role, user.createdAt);
                loadStats();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserDto>> call, @NonNull Throwable t) {
                finishRefresh();
                toast(getString(R.string.profile_load_failed));
            }
        });
    }

    private void loadStats() {
        apiService.myStats().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserStatsDto>> call,
                                   @NonNull Response<ApiResponse<UserStatsDto>> response) {
                finishRefresh();
                ApiResponse<UserStatsDto> body = response.body();
                if (!response.isSuccessful() || body == null || body.code != 0 || body.data == null) {
                    return;
                }
                UserStatsDto stats = body.data;
                bindStats(stats.totalPlayCount, stats.visitedLocationCount, stats.autoPlayCount);
                bindRecentVisits(stats.recentVisits);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserStatsDto>> call, @NonNull Throwable t) {
                finishRefresh();
            }
        });
    }

    private void bindUserHeader(@Nullable String username, @Nullable String role,
                                @Nullable String createdAt) {
        String name = TextUtils.isEmpty(username) ? getString(R.string.profile_guest) : username;
        tvUsername.setText(name);
        tvRole.setText(formatRole(role));
        tvJoinDate.setText(getString(R.string.profile_join_date,
                TextUtils.isEmpty(createdAt) ? getString(R.string.profile_unknown_date) : formatDate(createdAt)));

        String initial = name.substring(0, 1).toUpperCase(Locale.getDefault());
        tvAvatar.setText(initial);
    }

    private void bindStats(long playCount, long visitedCount, long autoCount) {
        tvStatPlay.setText(String.valueOf(playCount));
        tvStatVisited.setText(String.valueOf(visitedCount));
        tvStatAuto.setText(String.valueOf(autoCount));
    }

    private void bindRecentVisits(@Nullable List<RecentVisitDto> visits) {
        layoutRecentVisits.removeAllViews();
        if (visits == null || visits.isEmpty()) {
            tvRecentEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvRecentEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (RecentVisitDto visit : visits) {
            View row = inflater.inflate(R.layout.item_profile_recent, layoutRecentVisits, false);
            TextView tvName = row.findViewById(R.id.tvLocationName);
            TextView tvTime = row.findViewById(R.id.tvVisitTime);
            tvName.setText(TextUtils.isEmpty(visit.locationName)
                    ? getString(R.string.profile_unknown_location) : visit.locationName);
            tvTime.setText(formatDate(visit.visitedAt));
            layoutRecentVisits.addView(row);
        }
    }

    private void openChangePassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        if (!TextUtils.isEmpty(currentUsername)) {
            intent.putExtra(ForgotPasswordActivity.EXTRA_PREFILL_USERNAME, currentUsername);
        }
        startActivity(intent);
    }

    private void showSearchHistoryDialog() {
        List<String> history = searchHistoryStore.getAll();
        String message;
        if (history.isEmpty()) {
            message = getString(R.string.profile_search_history_empty);
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < history.size(); i++) {
                if (i > 0) {
                    builder.append('\n');
                }
                builder.append("• ").append(history.get(i));
            }
            message = builder.toString();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_menu_search_history)
                .setMessage(message)
                .setNegativeButton(R.string.btn_close, null)
                .setPositiveButton(R.string.profile_clear_history, (d, w) -> {
                    searchHistoryStore.clear();
                    toast(getString(R.string.profile_history_cleared));
                })
                .show();
    }

    private void showFeedbackDialog() {
        FeedbackDialogHelper.show(this);
    }

    private void showAboutDialog() {
        String version = getAppVersionName();
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_menu_about)
                .setMessage(getString(R.string.profile_about_content, version))
                .setPositiveButton(R.string.btn_close, null)
                .show();
    }

    private void showAgreementDialog(@StringRes int titleRes, @StringRes int contentRes) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agreement, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvContent = dialogView.findViewById(R.id.tvDialogContent);
        ImageButton btnClose = dialogView.findViewById(R.id.btnCloseAgreement);

        tvTitle.setText(titleRes);
        tvContent.setText(contentRes);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_msg)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_yes_logout, (d, w) -> logout())
                .show();
    }

    private void logout() {
        session.clearAuth();
        gotoLogin();
    }

    private void gotoLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void finishRefresh() {
        swipeRefresh.setRefreshing(false);
    }

    private String formatRole(@Nullable String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return getString(R.string.profile_role_admin);
        }
        return getString(R.string.profile_role_user);
    }

    private String formatDate(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return getString(R.string.profile_unknown_date);
        }
        try {
            String value = raw.length() >= 10 ? raw.substring(0, 10) : raw;
            String[] parts = value.split("-");
            if (parts.length == 3) {
                return String.format(Locale.getDefault(), "%s年%s月%s日", parts[0], parts[1], parts[2]);
            }
        } catch (Exception ignored) {
            // Fall through.
        }
        return raw.replace('T', ' ');
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private String getAppVersionName() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return TextUtils.isEmpty(version) ? "1.0" : version;
        } catch (Exception ignored) {
            return "1.0";
        }
    }
}
