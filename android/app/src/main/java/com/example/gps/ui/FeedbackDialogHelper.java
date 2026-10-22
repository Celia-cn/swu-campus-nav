package com.example.gps.ui;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.AppConfig;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.FeedbackSubmitReq;
import com.example.gps.storage.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 地图页、个人页共用的意见反馈弹窗。
 */
public final class FeedbackDialogHelper {

    private FeedbackDialogHelper() {
    }

    public static void show(@NonNull AppCompatActivity activity) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_feedback, null, false);
        TextInputEditText etFeedback = dialogView.findViewById(R.id.etFeedback);
        SessionManager session = SessionManager.from(activity);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.profile_menu_feedback)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.profile_submit_feedback, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String content = etFeedback.getText() == null ? "" : etFeedback.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    Toast.makeText(activity, R.string.profile_feedback_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!session.isLoggedIn()) {
                    Toast.makeText(activity, R.string.toast_feedback_need_login, Toast.LENGTH_SHORT).show();
                    return;
                }
                submitFeedback(activity, dialog, content);
            });
        });
        dialog.show();
    }

    private static void submitFeedback(@NonNull AppCompatActivity activity,
                                     @NonNull AlertDialog dialog,
                                     @NonNull String content) {
        SessionManager session = SessionManager.from(activity);
        session.syncBaseUrlFromConfig();
        ApiClient.reset();
        ApiService api = ApiClient.service(activity);
        api.submitFeedback(new FeedbackSubmitReq(content)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                   @NonNull Response<ApiResponse<Void>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().code != 0) {
                    String message = response.body() != null && !TextUtils.isEmpty(response.body().message)
                            ? response.body().message
                            : activity.getString(R.string.toast_feedback_failed);
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                Toast.makeText(activity, R.string.toast_feedback_saved, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                String base = session.getBaseUrl();
                String hint = activity.getString(R.string.toast_feedback_failed)
                        + "\n" + activity.getString(R.string.toast_feedback_server, base);
                if (!AppConfig.isProbablyEmulator()) {
                    hint += "\n" + activity.getString(R.string.toast_feedback_check_wifi);
                }
                Toast.makeText(activity, hint, Toast.LENGTH_LONG).show();
            }
        });
    }
}
