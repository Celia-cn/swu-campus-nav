package com.example.gps.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.gps.R;
import com.example.gps.network.dto.TriggerHistoryDto;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TriggerHistoryAdapter extends ArrayAdapter<TriggerHistoryDto> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public TriggerHistoryAdapter(Context context, List<TriggerHistoryDto> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_trigger_history, parent, false);
        }

        TriggerHistoryDto item = getItem(position);
        if (item == null) return convertView;

        TextView tvTriggerType = convertView.findViewById(R.id.tvTriggerType);
        TextView tvDistance = convertView.findViewById(R.id.tvDistance);
        TextView tvSuccess = convertView.findViewById(R.id.tvSuccess);
        TextView tvTime = convertView.findViewById(R.id.tvTime);

        tvTriggerType.setText("触发类型: " + item.triggerType);
        tvDistance.setText(String.format("距离: %.2f 米", item.distanceMeters));
        tvSuccess.setText(item.success ? "✓ 成功" : "✗ 失败");
        tvSuccess.setTextColor(item.success ?
                getContext().getColor(R.color.brand_accent) :
                getContext().getColor(android.R.color.holo_red_dark));

        try {
            tvTime.setText(dateFormat.format(new java.util.Date(
                    java.sql.Timestamp.valueOf(item.createdAt).getTime())));
        } catch (Exception e) {
            tvTime.setText(item.createdAt);
        }

        return convertView;
    }
}