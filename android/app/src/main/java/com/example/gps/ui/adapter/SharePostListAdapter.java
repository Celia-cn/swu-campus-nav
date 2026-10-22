package com.example.gps.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.network.dto.SharePostDto;
import com.example.gps.util.MediaUrlResolver;
import com.example.gps.util.SimpleImageLoader;

import java.util.ArrayList;
import java.util.List;

public class SharePostListAdapter extends RecyclerView.Adapter<SharePostListAdapter.Holder> {

    public interface Listener {
        void onClick(@NonNull SharePostDto post);
    }

    private final List<SharePostDto> items = new ArrayList<>();
    private final Listener listener;

    public SharePostListAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<SharePostDto> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_post, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SharePostDto post = items.get(position);
        String imageUrl = MediaUrlResolver.resolve(holder.itemView.getContext(), post.imageUrl);
        SimpleImageLoader.load(holder.itemView.getContext(), imageUrl, holder.cover, R.drawable.bg_glass_card);
        holder.title.setText(post.title);
        holder.content.setText(post.content);
        holder.author.setText(post.username == null ? "" : post.username);
        holder.stats.setText(holder.itemView.getContext().getString(
                R.string.share_post_stats, post.likeCount, post.commentCount));
        holder.cover.setOnClickListener(v -> listener.onClick(post));
        holder.itemView.setOnClickListener(v -> listener.onClick(post));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final TextView title;
        final TextView content;
        final TextView author;
        final TextView stats;

        Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.ivCover);
            title = itemView.findViewById(R.id.tvTitle);
            content = itemView.findViewById(R.id.tvContent);
            author = itemView.findViewById(R.id.tvAuthor);
            stats = itemView.findViewById(R.id.tvStats);
        }
    }
}
