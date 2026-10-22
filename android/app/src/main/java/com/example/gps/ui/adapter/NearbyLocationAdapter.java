package com.example.gps.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.network.dto.LocationDto;

import java.util.ArrayList;
import java.util.List;

public class NearbyLocationAdapter extends RecyclerView.Adapter<NearbyLocationAdapter.ViewHolder> {

    public static class NearbyItem {
        public LocationDto location;
        public double distance;

        public NearbyItem(LocationDto location, double distance) {
            this.location = location;
            this.distance = distance;
        }
    }

    public interface ClickListener {
        void onSelect(LocationDto location, double distance);
    }

    private final List<NearbyItem> data = new ArrayList<>();
    private final ClickListener clickListener;

    public NearbyLocationAdapter(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submit(List<NearbyItem> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NearbyItem item = data.get(position);
        holder.tvName.setText(item.location.name);
        holder.tvDistance.setText(
                com.example.gps.util.DistanceFormat.formatApprox(holder.itemView.getContext(), item.distance));
        holder.itemView.setOnClickListener(v -> clickListener.onSelect(item.location, item.distance));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDistance;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }
    }
}
