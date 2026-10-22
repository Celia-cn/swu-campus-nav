package com.example.gps.ui.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;

import java.util.ArrayList;
import java.util.List;

public class RouteWaypointAdapter extends RecyclerView.Adapter<RouteWaypointAdapter.Holder> {

    public interface Listener {
        void onWaypointClick(@NonNull Item item);
    }

    public static final class Item {
        public final String label;
        public final long locationId;
        public final boolean campusSpot;

        public Item(@NonNull String label, long locationId, boolean campusSpot) {
            this.label = label;
            this.locationId = locationId;
            this.campusSpot = campusSpot;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final Listener listener;
    @ColorInt
    private final int spotColor;
    @ColorInt
    private final int normalColor;

    public RouteWaypointAdapter(@NonNull Listener listener,
                                @ColorInt int spotColor,
                                @ColorInt int normalColor) {
        this.listener = listener;
        this.spotColor = spotColor;
        this.normalColor = normalColor;
    }

    public void submit(@NonNull List<Item> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_waypoint, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Item item = items.get(position);
        holder.label.setText((position + 1) + ". " + item.label);
        if (item.campusSpot) {
            holder.label.setTextColor(spotColor);
            holder.label.setTypeface(Typeface.DEFAULT_BOLD);
            holder.itemView.setClickable(true);
            holder.label.setClickable(true);
            View.OnClickListener click = v -> listener.onWaypointClick(item);
            holder.itemView.setOnClickListener(click);
            holder.label.setOnClickListener(click);
        } else {
            holder.label.setTextColor(normalColor);
            holder.label.setTypeface(Typeface.DEFAULT);
            holder.itemView.setClickable(false);
            holder.itemView.setOnClickListener(null);
            holder.label.setClickable(false);
            holder.label.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.tvWaypointLabel);
        }
    }
}
