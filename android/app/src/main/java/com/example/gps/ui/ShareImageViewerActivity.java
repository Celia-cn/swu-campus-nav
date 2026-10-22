package com.example.gps.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gps.R;
import com.example.gps.util.MediaUrlResolver;
import com.example.gps.util.SimpleImageLoader;
import com.google.android.material.appbar.MaterialToolbar;

public class ShareImageViewerActivity extends AppCompatActivity {

    private static final String EXTRA_IMAGE_URL = "image_url";
    private static final String EXTRA_TITLE = "title";

    public static Intent buildIntent(@NonNull Context context,
                                     @Nullable String imageUrl,
                                     @Nullable String title) {
        Intent intent = new Intent(context, ShareImageViewerActivity.class);
        intent.putExtra(EXTRA_IMAGE_URL, imageUrl == null ? "" : imageUrl);
        intent.putExtra(EXTRA_TITLE, title == null ? "" : title);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_image_viewer);

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, R.string.share_image_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (!TextUtils.isEmpty(title)) {
            toolbar.setTitle(title);
        }

        ImageView ivFullImage = findViewById(R.id.ivFullImage);
        SimpleImageLoader.load(this,
                MediaUrlResolver.resolve(this, imageUrl),
                ivFullImage,
                R.drawable.bg_glass_card);
    }
}
