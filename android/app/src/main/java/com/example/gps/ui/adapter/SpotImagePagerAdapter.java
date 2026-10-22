package com.example.gps.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.util.MediaUrlResolver;
import com.example.gps.util.SimpleImageLoader;

import java.util.ArrayList;
import java.util.List;

public class SpotImagePagerAdapter extends RecyclerView.Adapter<SpotImagePagerAdapter.PageHolder> {

    private final Context context;
    private final List<String> imageUrls = new ArrayList<>();

    public SpotImagePagerAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void submit(@NonNull List<String> urls) {
        imageUrls.clear();
        imageUrls.addAll(urls);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_spot_image_page, parent, false);
        return new PageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        String resolved = MediaUrlResolver.resolve(context, imageUrls.get(position));
        SimpleImageLoader.load(context, resolved, holder.image, R.drawable.bg_glass_card);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        final ImageView image;

        PageHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivPageImage);
        }
    }
}
