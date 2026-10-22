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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.AuthDto;
import com.example.gps.network.dto.LoginReq;
import com.example.gps.storage.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final Gson GSON = new Gson();

    public static final String EXTRA_PREFILL_USERNAME = "extra_prefill_username";

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private ProgressBar progressBar;
    private MaterialButton btnLogin;
    private TextView btnRegister;
    private TextView tvForgotPassword;
    private TextView tvAgreement;
    private MaterialCheckBox cbAgreement;

    private SessionManager session;
    private ApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        session = SessionManager.from(this);
        if (session.isLoggedIn()) {
            gotoMap();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = ApiClient.service(this);

        bindViews();
        prefillUsernameIfNeeded();
        setupAgreementText();
        setupRegisterLink();
        btnLogin.setOnClickListener(v -> submitLogin());
        tvForgotPassword.setOnClickListener(v -> openForgotPassword());
    }

    private void openForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        String username = safeText(etUsername);
        if (!TextUtils.isEmpty(username)) {
            intent.putExtra(ForgotPasswordActivity.EXTRA_PREFILL_USERNAME, username);
        }
        startActivity(intent);
    }

    private void prefillUsernameIfNeeded() {
        String username = getIntent().getStringExtra(EXTRA_PREFILL_USERNAME);
        if (!TextUtils.isEmpty(username)) {
            etUsername.setText(username);
            etUsername.setSelection(username.length());
            etPassword.requestFocus();
        }
    }

    private void bindViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvAgreement = findViewById(R.id.tvAgreement);
        cbAgreement = findViewById(R.id.cbAgreement);
    }

    private void setupAgreementText() {
        String prefix = getString(R.string.label_agreement_prefix);
        String userAgreement = getString(R.string.label_agreement_user);
        String connector = getString(R.string.label_agreement_and);
        String privacyPolicy = getString(R.string.label_agreement_privacy);
        String fullText = prefix + userAgreement + connector + privacyPolicy;

        SpannableString spannable = new SpannableString(fullText);
        int linkColor = ContextCompat.getColor(this, R.color.login_link);

        int userStart = prefix.length();
        int userEnd = userStart + userAgreement.length();
        applyAgreementLink(spannable, userStart, userEnd, linkColor,
                () -> showAgreementDialog(R.string.title_user_agreement, R.string.content_user_agreement));

        int privacyStart = userEnd + connector.length();
        int privacyEnd = privacyStart + privacyPolicy.length();
        applyAgreementLink(spannable, privacyStart, privacyEnd, linkColor,
                () -> showAgreementDialog(R.string.title_privacy_policy, R.string.content_privacy_policy));

        tvAgreement.setText(spannable);
        tvAgreement.setMovementMethod(LinkMovementMethod.getInstance());
        tvAgreement.setHighlightColor(Color.TRANSPARENT);
    }

    private void applyAgreementLink(@NonNull SpannableString spannable, int start, int end,
                                    int linkColor, @NonNull Runnable onClick) {
        spannable.setSpan(new ForegroundColorSpan(linkColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                onClick.run();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(linkColor);
                ds.setUnderlineText(false);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    private void setupRegisterLink() {
        String prefix = getString(R.string.label_no_account);
        String action = getString(R.string.label_register_now_short);
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
                startActivity(new android.content.Intent(LoginActivity.this, RegisterActivity.class));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.brand_accent));
                ds.setUnderlineText(false);
            }
        }, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        btnRegister.setText(spannable);
        btnRegister.setMovementMethod(LinkMovementMethod.getInstance());
        btnRegister.setHighlightColor(Color.TRANSPARENT);
    }

    private void submitLogin() {
        if (!cbAgreement.isChecked()) {
            toast(getString(R.string.toast_agreement_required));
            return;
        }

        String username = safeText(etUsername);
        String password = safeText(etPassword);

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            toast(getString(R.string.toast_login_failed));
            return;
        }
        setLoading(true);

        apiService.login(new LoginReq(username, password)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthDto>> call, Response<ApiResponse<AuthDto>> response) {
                setLoading(false);
                ApiResponse<AuthDto> body = response.body();
                if (body == null) {
                    body = parseErrorBody(response);
                }

                if (response.isSuccessful() && body != null && body.code == 0
                        && body.data != null && body.data.user != null) {
                    session.saveAuth(body.data.token, body.data.user.role, body.data.user.username, body.data.user.id);
                    toast(getString(R.string.toast_login_success));
                    gotoMap();
                    return;
                }

                if (isInvalidCredentials(response, body)) {
                    toast(getString(R.string.toast_login_invalid_credentials));
                    return;
                }

                toast(getString(R.string.toast_login_failed));
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthDto>> call, Throwable throwable) {
                setLoading(false);
                toast(getString(R.string.toast_login_failed));
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        cbAgreement.setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private void gotoMap() {
        Intent intent = new Intent(this, MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    private boolean isInvalidCredentials(Response<ApiResponse<AuthDto>> response,
                                           @Nullable ApiResponse<AuthDto> body) {
        if (response.code() == 401) {
            return true;
        }
        if (body == null) {
            return false;
        }
        if (body.code == 401) {
            return true;
        }
        String message = body.message;
        if (TextUtils.isEmpty(message)) {
            return false;
        }
        return message.contains("用户名或密码") || message.contains("密码错误");
    }
}
