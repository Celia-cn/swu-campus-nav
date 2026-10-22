package com.example.gps.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.ResetPasswordReq;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_USERNAME = "extra_prefill_username";

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private ProgressBar progressBar;
    private MaterialButton btnReset;
    private TextView tvGoLogin;

    private ApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        apiService = ApiClient.service(this);

        bindViews();
        prefillUsernameIfNeeded();
        setupGoLoginLink();
        btnReset.setOnClickListener(v -> submitReset());
    }

    private void bindViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnReset = findViewById(R.id.btnReset);
        tvGoLogin = findViewById(R.id.tvGoLogin);
    }

    private void prefillUsernameIfNeeded() {
        String username = getIntent().getStringExtra(EXTRA_PREFILL_USERNAME);
        if (!TextUtils.isEmpty(username)) {
            etUsername.setText(username);
            etUsername.setSelection(username.length());
            etPassword.requestFocus();
        }
    }

    private void setupGoLoginLink() {
        String prefix = getString(R.string.label_remember_password);
        String action = getString(R.string.label_go_login_action);
        String fullText = prefix + action;

        SpannableString spannable = new SpannableString(fullText);
        int normalColor = ContextCompat.getColor(this, R.color.text_sub);
        int linkStart = prefix.length();
        int linkEnd = fullText.length();

        spannable.setSpan(new ForegroundColorSpan(normalColor), 0, linkStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_accent)),
                linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                finish();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(ContextCompat.getColor(ForgotPasswordActivity.this, R.color.brand_accent));
                ds.setUnderlineText(false);
            }
        }, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvGoLogin.setText(spannable);
        tvGoLogin.setMovementMethod(LinkMovementMethod.getInstance());
        tvGoLogin.setHighlightColor(Color.TRANSPARENT);
    }

    private void submitReset() {
        String username = safeText(etUsername);
        String password = safeText(etPassword);

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            toast(getString(R.string.toast_reset_password_failed));
            return;
        }
        setLoading(true);

        apiService.resetPassword(new ResetPasswordReq(username, password)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                setLoading(false);
                ApiResponse<Void> body = response.body();
                if (response.isSuccessful() && body != null && body.code == 0) {
                    toast(getString(R.string.toast_reset_password_success));
                    Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                    intent.putExtra(LoginActivity.EXTRA_PREFILL_USERNAME, username);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return;
                }
                toast(getString(R.string.toast_reset_password_failed));
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                setLoading(false);
                toast(getString(R.string.toast_reset_password_failed));
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnReset.setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
