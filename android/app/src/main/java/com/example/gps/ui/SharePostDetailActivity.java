package com.example.gps.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.network.dto.ShareCommentDto;
import com.example.gps.network.dto.SharePostDto;
import com.example.gps.storage.SessionManager;
import com.example.gps.util.MediaUrlResolver;
import com.example.gps.util.SimpleImageLoader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharePostDetailActivity extends AppCompatActivity {

    private static final String EXTRA_POST_ID = "post_id";

    private ApiService api;
    private SessionManager session;
    private long postId;
    private SharePostDto post;
    private TextView tvTitle;
    private TextView tvContent;
    private ImageView ivCover;
    private MaterialButton btnLike;
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;
    private LinearLayout layoutComments;
    private boolean sendingComment;

    public static Intent buildIntent(@NonNull Context context, long postId) {
        Intent intent = new Intent(context, SharePostDetailActivity.class);
        intent.putExtra(EXTRA_POST_ID, postId);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_post_detail);
        api = ApiClient.service(this);
        session = SessionManager.from(this);
        postId = getIntent().getLongExtra(EXTRA_POST_ID, -1L);
        if (postId <= 0) {
            toast(R.string.share_post_load_failed);
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        tvTitle = findViewById(R.id.tvTitle);
        tvContent = findViewById(R.id.tvContent);
        ivCover = findViewById(R.id.ivCover);
        btnLike = findViewById(R.id.btnLike);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        layoutComments = findViewById(R.id.layoutComments);
        EditText etComment = findViewById(R.id.etComment);
        MaterialButton btnSend = findViewById(R.id.btnSendComment);

        btnLike.setOnClickListener(v -> toggleLike());
        btnSend.setOnClickListener(v -> sendComment(etComment));
        ivCover.setOnClickListener(v -> openImageViewer());
        loadPost();
        loadComments();
    }

    private void loadPost() {
        api.sharePostDetail(postId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<SharePostDto>> call, Response<ApiResponse<SharePostDto>> response) {
                SharePostDto data = response.body() == null ? null : response.body().data;
                if (!response.isSuccessful() || data == null) {
                    toast(R.string.share_post_load_failed);
                    return;
                }
                bindPost(data);
            }

            @Override
            public void onFailure(Call<ApiResponse<SharePostDto>> call, Throwable t) {
                toast(R.string.toast_network_error);
            }
        });
    }

    private void openImageViewer() {
        if (post == null || TextUtils.isEmpty(post.imageUrl)) {
            return;
        }
        startActivity(ShareImageViewerActivity.buildIntent(this, post.imageUrl, post.title));
    }

    private void bindPost(@NonNull SharePostDto data) {
        post = data;
        tvTitle.setText(post.title);
        tvContent.setText(post.content);
        SimpleImageLoader.load(this,
                MediaUrlResolver.resolve(this, post.imageUrl),
                ivCover, R.drawable.bg_glass_card);
        ivCover.setClickable(!TextUtils.isEmpty(post.imageUrl));
        btnLike.setText(getString(R.string.share_like_count, post.likeCount));
        boolean mine = session.getUserId() > 0 && session.getUserId() == post.userId;
        btnEdit.setVisibility(mine ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(mine ? View.VISIBLE : View.GONE);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(SharePostDetailActivity.this, ShareCreateActivity.class);
            intent.putExtra(ShareCreateActivity.EXTRA_CATEGORY, post.category);
            intent.putExtra(ShareCreateActivity.EXTRA_POST_ID, post.id);
            startActivity(intent);
        });
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void loadComments() {
        api.shareComments(postId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ShareCommentDto>>> call,
                                   Response<ApiResponse<List<ShareCommentDto>>> response) {
                layoutComments.removeAllViews();
                List<ShareCommentDto> list = response.body() == null ? null : response.body().data;
                if (list == null || list.isEmpty()) {
                    addEmptyCommentHint();
                    return;
                }
                LayoutInflater inflater = LayoutInflater.from(SharePostDetailActivity.this);
                for (ShareCommentDto c : list) {
                    if (c == null) {
                        continue;
                    }
                    View row = inflater.inflate(R.layout.item_share_comment, layoutComments, false);
                    TextView user = row.findViewById(R.id.tvCommentUser);
                    TextView content = row.findViewById(R.id.tvCommentContent);
                    user.setText(TextUtils.isEmpty(c.username) ? getString(R.string.label_user_title) : c.username);
                    content.setText(c.content == null ? "" : c.content);
                    layoutComments.addView(row);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ShareCommentDto>>> call, Throwable t) {
                layoutComments.removeAllViews();
                addEmptyCommentHint();
            }
        });
    }

    private void addEmptyCommentHint() {
        TextView empty = new TextView(this);
        empty.setText(R.string.share_no_comments);
        empty.setTextColor(getColor(R.color.text_sub));
        layoutComments.addView(empty);
    }

    private void toggleLike() {
        if (!session.isLoggedIn()) {
            toast(R.string.toast_feedback_need_login);
            return;
        }
        api.toggleShareLike(postId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<SharePostDto>> call, Response<ApiResponse<SharePostDto>> response) {
                if (response.body() != null && response.body().data != null) {
                    bindPost(response.body().data);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SharePostDto>> call, Throwable t) {
                toast(R.string.toast_network_error);
            }
        });
    }

    private void sendComment(EditText et) {
        if (sendingComment) {
            return;
        }
        if (!session.isLoggedIn()) {
            toast(R.string.toast_feedback_need_login);
            return;
        }
        String text = et.getText() == null ? "" : et.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        sendingComment = true;
        Map<String, String> body = new HashMap<>();
        body.put("content", text);
        api.addShareComment(postId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<ShareCommentDto>> call,
                                   Response<ApiResponse<ShareCommentDto>> response) {
                sendingComment = false;
                if (response.isSuccessful() && response.body() != null && response.body().code == 0) {
                    et.setText("");
                    toast(R.string.share_comment_ok);
                    loadComments();
                    loadPost();
                    return;
                }
                toast(R.string.share_comment_failed);
            }

            @Override
            public void onFailure(Call<ApiResponse<ShareCommentDto>> call, Throwable t) {
                sendingComment = false;
                toast(R.string.share_comment_failed);
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.share_delete_confirm)
                .setPositiveButton(R.string.share_delete, (d, w) -> api.deleteSharePost(postId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        finish();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        toast(R.string.toast_network_error);
                    }
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void toast(int res) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
    }
}
