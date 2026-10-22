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

public class AdminLocationAdapter extends RecyclerView.Adapter<AdminLocationAdapter.ViewHolder> {

    public interface ActionListener {
        void onSelect(LocationDto location);

        void onDelete(LocationDto location);
    }

    private final List<LocationDto> data = new ArrayList<>();
    private final ActionListener actionListener;

    public AdminLocationAdapter(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submit(List<LocationDto> locations) {
        data.clear();
        if (locations != null) {
            data.addAll(locations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationDto item = data.get(position);
        holder.tvName.setText(item.name);
        holder.tvCategory.setText("分类：" + (item.category == null ? "-" : item.category));
        holder.tvAddress.setText("地址：" + (item.address == null ? "-" : item.address));
        holder.tvLatLng.setText("经纬度：" + item.latitude + ", " + item.longitude);
        holder.tvRadius.setText("触发半径：" + item.radiusMeters + " 米");
        holder.itemView.setOnClickListener(v -> actionListener.onSelect(item));
        holder.btnDelete.setOnClickListener(v -> actionListener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvCategory;
        TextView tvAddress;
        TextView tvLatLng;
        TextView tvRadius;
        TextView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvLatLng = itemView.findViewById(R.id.tvLatLng);
            tvRadius = itemView.findViewById(R.id.tvRadius);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
