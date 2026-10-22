package com.example.gps.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.example.gps.util.MediaUrlResolver;
import com.example.gps.util.SimpleImageLoader;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.SharePostDto;
import com.example.gps.network.dto.SharePostUpsertReq;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareCreateActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_POST_ID = "post_id";

    private ApiService api;
    private String category = "FOOD";
    private long editPostId = -1L;
    private Uri pickedUri;
    @Nullable
    private String existingImageUrl;
    private TextInputEditText etTitle;
    private TextInputEditText etContent;
    private ImageView ivPreview;
    private MaterialSwitch switchTitleImage;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    pickedUri = uri;
                    ivPreview.setVisibility(View.VISIBLE);
                    ivPreview.setImageURI(uri);
                    switchTitleImage.setChecked(false);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_create);
        api = ApiClient.service(this);
        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (category == null) {
            category = "FOOD";
        }
        editPostId = getIntent().getLongExtra(EXTRA_POST_ID, -1L);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        ivPreview = findViewById(R.id.ivPreview);
        switchTitleImage = findViewById(R.id.switchTitleImage);
        MaterialButton btnPick = findViewById(R.id.btnPickImage);
        MaterialButton btnPublish = findViewById(R.id.btnPublish);

        btnPick.setOnClickListener(v -> pickImage.launch("image/*"));
        btnPublish.setOnClickListener(v -> publish());

        if (editPostId > 0) {
            toolbar.setTitle(R.string.share_edit_post);
            loadForEdit();
        }
    }

    private void loadForEdit() {
        api.sharePostDetail(editPostId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<SharePostDto>> call, Response<ApiResponse<SharePostDto>> response) {
                SharePostDto post = response.body() == null ? null : response.body().data;
                if (post == null) {
                    return;
                }
                etTitle.setText(post.title);
                etContent.setText(post.content);
                switchTitleImage.setChecked(post.titleAsImage);
                existingImageUrl = post.imageUrl;
                if (!TextUtils.isEmpty(post.imageUrl)) {
                    ivPreview.setVisibility(View.VISIBLE);
                    SimpleImageLoader.load(ShareCreateActivity.this,
                            MediaUrlResolver.resolve(ShareCreateActivity.this, post.imageUrl),
                            ivPreview, R.drawable.bg_glass_card);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SharePostDto>> call, Throwable t) {
            }
        });
    }

    private void publish() {
        String title = text(etTitle);
        String content = text(etContent);
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            toast(R.string.share_fill_required);
            return;
        }
        boolean titleAsImage = switchTitleImage.isChecked();
        if (!titleAsImage && pickedUri == null && TextUtils.isEmpty(existingImageUrl)) {
            toast(R.string.share_need_image);
            return;
        }
        if (titleAsImage) {
            api.uploadShareTitleImage(category, title).enqueue(uploadCallback(title, content, true, null));
        } else if (pickedUri != null) {
            uploadPickedImage(title, content);
        } else {
            createOrUpdate(title, content, existingImageUrl, false);
        }
    }

    private void uploadPickedImage(String title, String content) {
        try {
            File temp = File.createTempFile("share_", ".jpg", getCacheDir());
            try (InputStream in = getContentResolver().openInputStream(pickedUri);
                 FileOutputStream out = new FileOutputStream(temp)) {
                if (in == null) {
                    toast(R.string.toast_load_failed);
                    return;
                }
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            RequestBody body = RequestBody.create(temp, MediaType.parse("image/*"));
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", temp.getName(), body);
            api.uploadShareImage(category, part).enqueue(uploadCallback(title, content, false, temp));
        } catch (Exception e) {
            toast(R.string.toast_load_failed);
        }
    }

    private Callback<ApiResponse<Map<String, String>>> uploadCallback(String title, String content,
                                                                      boolean titleAsImage, File temp) {
        return new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, String>>> call,
                                   Response<ApiResponse<Map<String, String>>> response) {
                if (temp != null) {
                    temp.delete();
                }
                Map<String, String> data = response.body() == null ? null : response.body().data;
                if (data == null || data.get("url") == null) {
                    toast(R.string.toast_load_failed);
                    return;
                }
                createOrUpdate(title, content, data.get("url"), titleAsImage);
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                if (temp != null) {
                    temp.delete();
                }
                toast(R.string.toast_network_error);
            }
        };
    }

    private void createOrUpdate(String title, String content, String imageUrl, boolean titleAsImage) {
        SharePostUpsertReq req = new SharePostUpsertReq(category, title, content, imageUrl, titleAsImage);
        Call<ApiResponse<SharePostDto>> call = editPostId > 0
                ? api.updateSharePost(editPostId, req)
                : api.createSharePost(req);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<SharePostDto>> call, Response<ApiResponse<SharePostDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().code == 0) {
                    toast(R.string.share_publish_ok);
                    finish();
                } else {
                    toast(R.string.toast_load_failed);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SharePostDto>> call, Throwable t) {
                toast(R.string.toast_network_error);
            }
        });
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(int res) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
    }
}
