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
import com.example.gps.network.dto.AuthDto;
import com.example.gps.network.dto.RegisterReq;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final Gson GSON = new Gson();

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private ProgressBar progressBar;
    private MaterialButton btnRegister;
    private TextView tvGoLogin;

    private ApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = ApiClient.service(this);

        bindViews();
        setupGoLoginLink();
        btnRegister.setOnClickListener(v -> submitRegister());
    }

    private void bindViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);
    }

    private void setupGoLoginLink() {
        String prefix = getString(R.string.label_has_account);
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
                ds.setColor(ContextCompat.getColor(RegisterActivity.this, R.color.brand_accent));
                ds.setUnderlineText(false);
            }
        }, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvGoLogin.setText(spannable);
        tvGoLogin.setMovementMethod(LinkMovementMethod.getInstance());
        tvGoLogin.setHighlightColor(Color.TRANSPARENT);
    }

    private void submitRegister() {
        String username = safeText(etUsername);
        String password = safeText(etPassword);

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            toast(getString(R.string.toast_register_failed));
            return;
        }
        setLoading(true);

        apiService.register(new RegisterReq(username, password)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthDto>> call, Response<ApiResponse<AuthDto>> response) {
                setLoading(false);
                ApiResponse<AuthDto> body = response.body();
                if (body == null) {
                    body = parseErrorBody(response);
                }

                if (response.isSuccessful() && body != null && body.code == 0) {
                    toast(getString(R.string.toast_register_success));
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra(LoginActivity.EXTRA_PREFILL_USERNAME, username);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return;
                }

                if (isAccountExists(response, body)) {
                    toast(getString(R.string.toast_register_exists));
                    return;
                }

                toast(getString(R.string.toast_register_failed));
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthDto>> call, Throwable throwable) {
                setLoading(false);
                toast(getString(R.string.toast_register_failed));
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private String safeText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private ApiResponse<AuthDto> parseErrorBody(Response<ApiResponse<AuthDto>> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            String json = response.errorBody().string();
            return GSON.fromJson(json, new TypeToken<ApiResponse<AuthDto>>() {}.getType());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isAccountExists(Response<ApiResponse<AuthDto>> response, @Nullable ApiResponse<AuthDto> body) {
        if (response.code() == 409) {
            return true;
        }
        if (body == null) {
            return false;
        }
        if (body.code == 409) {
            return true;
        }
        String message = body.message;
        if (TextUtils.isEmpty(message)) {
            return false;
        }
        return message.contains("已存在");
    }
}
