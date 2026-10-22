package com.example.gps.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.network.dto.LocationDto;
import com.example.gps.util.MediaUrlResolver;
import com.example.gps.util.SimpleImageLoader;

import java.util.ArrayList;
import java.util.List;

public class SpotIntroAdapter extends RecyclerView.Adapter<SpotIntroAdapter.Holder> {

    public interface Listener {
        void onSpotClick(LocationDto location);

        void onLikeClick(LocationDto location, int position);
    }

    private final Listener listener;
    private final List<LocationDto> items = new ArrayList<>();

    public SpotIntroAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<LocationDto> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public List<LocationDto> getItems() {
        return items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_spot_intro, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LocationDto item = items.get(position);
        holder.name.setText(item.name);
        holder.category.setText(TextUtils.isEmpty(item.category)
                ? holder.itemView.getContext().getString(R.string.label_spot_default_category)
                : item.category);
        holder.description.setText(TextUtils.isEmpty(item.description)
                ? holder.itemView.getContext().getString(R.string.label_spot_no_description)
                : item.description);
        boolean hasAudio = !TextUtils.isEmpty(item.audioUrl);
        holder.guideTag.setVisibility(hasAudio ? View.VISIBLE : View.GONE);

        String cover = com.example.gps.util.LocationImageUrls.cover(item);
        String imageUrl = MediaUrlResolver.resolve(holder.itemView.getContext(), cover);
        SimpleImageLoader.load(holder.itemView.getContext(), imageUrl, holder.cover, R.drawable.bg_glass_card);

        bindLikeUi(holder, item);
        holder.btnLike.setOnClickListener(v -> listener.onLikeClick(item, holder.getBindingAdapterPosition()));
        holder.itemView.setOnClickListener(v -> listener.onSpotClick(item));
    }

    private void bindLikeUi(Holder holder, LocationDto item) {
        holder.likeCount.setText(String.valueOf(Math.max(0, item.likeCount)));
        holder.btnLike.setImageResource(item.liked
                ? R.drawable.ic_heart_filled
                : R.drawable.ic_heart_outline);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final ImageButton btnLike;
        final TextView likeCount;
        final TextView name;
        final TextView category;
        final TextView description;
        final TextView guideTag;

        Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.ivCover);
            btnLike = itemView.findViewById(R.id.btnLike);
            likeCount = itemView.findViewById(R.id.tvLikeCount);
            name = itemView.findViewById(R.id.tvName);
            category = itemView.findViewById(R.id.tvCategory);
            description = itemView.findViewById(R.id.tvDescription);
            guideTag = itemView.findViewById(R.id.tvGuideTag);
        }
    }
}
