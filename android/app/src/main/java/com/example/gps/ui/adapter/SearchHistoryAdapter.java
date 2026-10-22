package com.example.gps.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;

import java.util.ArrayList;
import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    public interface ClickListener {
        void onHistoryClick(@NonNull String keyword);
    }

    private final List<String> data = new ArrayList<>();
    private final ClickListener clickListener;

    public SearchHistoryAdapter(@NonNull ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submit(@NonNull List<String> keywords) {
        data.clear();
        data.addAll(keywords);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String keyword = data.get(position);
        holder.tvKeyword.setText(keyword);
        holder.itemView.setOnClickListener(v -> clickListener.onHistoryClick(keyword));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvKeyword;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKeyword = itemView.findViewById(R.id.tvHistoryKeyword);
        }
    }
}
