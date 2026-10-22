package com.example.gps.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.gps.R;
import com.example.gps.network.dto.LocationDto;
import com.example.gps.ui.adapter.SpotImagePagerAdapter;
import com.example.gps.util.DistanceFormat;
import com.example.gps.util.LocationImageUrls;
import com.example.gps.util.MediaUrlResolver;
import com.example.gps.util.SimpleImageLoader;
import com.example.gps.util.SpotDetailTextUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * 景点详情页：与景点介绍列表、地图气泡同一套卡片与配色，分段展示讲解内容。
 */
public class SpotDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LOCATION_ID = "location_id";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_IMAGE_URL = "image_url";
    public static final String EXTRA_IMAGE_URLS = "image_urls";
    public static final String EXTRA_AUDIO_URL = "audio_url";
    public static final String EXTRA_DISTANCE_METERS = "distance_meters";
    public static final String EXTRA_NEARBY_NAMES = "nearby_names";
    public static final String EXTRA_ACTION = "action";

    public static final String ACTION_ROUTE = "route";
    public static final String ACTION_GUIDE = "guide";

    private long locationId = -1L;

    @NonNull
    public static Intent buildIntent(@NonNull Context context,
                                     @NonNull LocationDto location,
                                     @Nullable Double distanceMeters,
                                     @Nullable List<String> nearbyNames) {
        Intent intent = new Intent(context, SpotDetailActivity.class);
        intent.putExtra(EXTRA_LOCATION_ID, location.id);
        intent.putExtra(EXTRA_NAME, location.name);
        intent.putExtra(EXTRA_CATEGORY, location.category);
        intent.putExtra(EXTRA_ADDRESS, location.address);
        intent.putExtra(EXTRA_DESCRIPTION, location.description);
        List<String> images = LocationImageUrls.resolve(location);
        if (!images.isEmpty()) {
            intent.putExtra(EXTRA_IMAGE_URLS, images.toArray(new String[0]));
            intent.putExtra(EXTRA_IMAGE_URL, images.get(0));
        } else {
            intent.putExtra(EXTRA_IMAGE_URL, location.imageUrl);
        }
        intent.putExtra(EXTRA_AUDIO_URL, location.audioUrl);
        if (distanceMeters != null) {
            intent.putExtra(EXTRA_DISTANCE_METERS, distanceMeters);
        }
        if (nearbyNames != null && !nearbyNames.isEmpty()) {
            intent.putExtra(EXTRA_NEARBY_NAMES, TextUtils.join("、", nearbyNames));
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spot_detail);
        applyWindowInsets();

        locationId = getIntent().getLongExtra(EXTRA_LOCATION_ID, -1L);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        String address = getIntent().getStringExtra(EXTRA_ADDRESS);
        String description = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        String audioUrl = getIntent().getStringExtra(EXTRA_AUDIO_URL);
        String nearbyJoined = getIntent().getStringExtra(EXTRA_NEARBY_NAMES);
        boolean hasDistance = getIntent().hasExtra(EXTRA_DISTANCE_METERS);
        double distanceMeters = getIntent().getDoubleExtra(EXTRA_DISTANCE_METERS, 0d);

        List<String> imageUrls = readImageUrlsFromIntent();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (!TextUtils.isEmpty(name)) {
            toolbar.setTitle(name);
        }

        ViewPager2 vpImages = findViewById(R.id.vpImages);
        ImageView ivCover = findViewById(R.id.ivCover);
        TextView tvImageIndicator = findViewById(R.id.tvImageIndicator);
        TextView tvAudioBadge = findViewById(R.id.tvAudioBadge);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvCategory = findViewById(R.id.tvCategory);
        TextView tvDistance = findViewById(R.id.tvDistance);
        LinearLayout llDescriptionParagraphs = findViewById(R.id.llDescriptionParagraphs);
        MaterialCardView cardAddress = findViewById(R.id.cardAddress);
        TextView tvAddress = findViewById(R.id.tvAddress);
        MaterialCardView cardNearby = findViewById(R.id.cardNearby);
        TextView tvNearby = findViewById(R.id.tvNearby);
        MaterialButton btnGuide = findViewById(R.id.btnGuide);
        MaterialButton btnRoute = findViewById(R.id.btnRoute);

        tvName.setText(TextUtils.isEmpty(name) ? getString(R.string.label_not_selected) : name);
        tvCategory.setText(TextUtils.isEmpty(category)
                ? getString(R.string.label_spot_default_category)
                : category);

        if (hasDistance && DistanceFormat.isUsable(distanceMeters)) {
            tvDistance.setVisibility(View.VISIBLE);
            tvDistance.setText(DistanceFormat.formatWithLabel(this, distanceMeters));
        } else {
            tvDistance.setVisibility(View.GONE);
        }

        boolean hasAudio = !TextUtils.isEmpty(audioUrl);
        tvAudioBadge.setVisibility(hasAudio ? View.VISIBLE : View.GONE);
        btnGuide.setVisibility(hasAudio || !TextUtils.isEmpty(description) ? View.VISIBLE : View.GONE);

        bindImages(vpImages, ivCover, tvImageIndicator, imageUrls);

        bindDescriptionParagraphs(llDescriptionParagraphs, description);

        if (!TextUtils.isEmpty(address)) {
            cardAddress.setVisibility(View.VISIBLE);
            tvAddress.setText(address);
        }

        if (!TextUtils.isEmpty(nearbyJoined)) {
            cardNearby.setVisibility(View.VISIBLE);
            tvNearby.setText(nearbyJoined);
        }

        btnGuide.setOnClickListener(v -> finishWithAction(ACTION_GUIDE));
        btnRoute.setOnClickListener(v -> finishWithAction(ACTION_ROUTE));
    }

    @NonNull
    private List<String> readImageUrlsFromIntent() {
        String[] array = getIntent().getStringArrayExtra(EXTRA_IMAGE_URLS);
        List<String> urls = new ArrayList<>();
        if (array != null) {
            for (String url : array) {
                if (url != null && !url.trim().isEmpty()) {
                    urls.add(url.trim());
                }
            }
        }
        if (urls.isEmpty()) {
            String single = getIntent().getStringExtra(EXTRA_IMAGE_URL);
            if (!TextUtils.isEmpty(single)) {
                urls.add(single.trim());
            }
        }
        return urls;
    }

    private void bindImages(@NonNull ViewPager2 vpImages,
                            @NonNull ImageView ivCover,
                            @NonNull TextView tvImageIndicator,
                            @NonNull List<String> imageUrls) {
        if (imageUrls.isEmpty()) {
            vpImages.setVisibility(View.GONE);
            ivCover.setVisibility(View.VISIBLE);
            tvImageIndicator.setVisibility(View.GONE);
            SimpleImageLoader.load(this, null, ivCover, R.drawable.bg_glass_card);
            return;
        }
        if (imageUrls.size() == 1) {
            vpImages.setVisibility(View.GONE);
            ivCover.setVisibility(View.VISIBLE);
            tvImageIndicator.setVisibility(View.GONE);
            SimpleImageLoader.load(
                    this,
                    MediaUrlResolver.resolve(this, imageUrls.get(0)),
                    ivCover,
                    R.drawable.bg_glass_card);
            return;
        }
        ivCover.setVisibility(View.GONE);
        vpImages.setVisibility(View.VISIBLE);
        tvImageIndicator.setVisibility(View.VISIBLE);
        SpotImagePagerAdapter adapter = new SpotImagePagerAdapter(this);
        adapter.submit(imageUrls);
        vpImages.setAdapter(adapter);
        tvImageIndicator.setText(getString(R.string.label_spot_image_page, 1, imageUrls.size()));
        vpImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tvImageIndicator.setText(getString(
                        R.string.label_spot_image_page,
                        position + 1,
                        imageUrls.size()));
            }
        });
    }

    private void bindDescriptionParagraphs(@NonNull LinearLayout container,
                                          @Nullable String description) {
        container.removeAllViews();
        List<String> paragraphs = SpotDetailTextUtil.splitParagraphs(description);
        LayoutInflater inflater = LayoutInflater.from(this);
        if (paragraphs.isEmpty()) {
            TextView empty = (TextView) inflater.inflate(
                    R.layout.item_spot_detail_paragraph, container, false);
            empty.setText(getString(R.string.label_spot_no_description));
            container.addView(empty);
            return;
        }
        for (int i = 0; i < paragraphs.size(); i++) {
            TextView paragraph = (TextView) inflater.inflate(
                    R.layout.item_spot_detail_paragraph, container, false);
            paragraph.setText(paragraphs.get(i));
            if (i > 0) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) paragraph.getLayoutParams();
                lp.topMargin = (int) (10 * getResources().getDisplayMetrics().density);
                paragraph.setLayoutParams(lp);
            }
            container.addView(paragraph);
        }
    }

    private void finishWithAction(@NonNull String action) {
        Intent data = new Intent();
        data.putExtra(EXTRA_LOCATION_ID, locationId);
        data.putExtra(EXTRA_ACTION, action);
        setResult(RESULT_OK, data);
        finish();
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.spotDetailRoot);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (root == null || toolbar == null) {
            return;
        }
        final int toolbarPadTop = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    toolbarPadTop + bars.top,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
