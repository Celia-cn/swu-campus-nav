package com.example.gps.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** 解决 RecyclerView 放在 NestedScrollView 内时只显示首项的问题。 */
public final class RecyclerViewHeightUtil {

    private RecyclerViewHeightUtil() {
    }

    public static void expandToFitChildren(@NonNull RecyclerView recyclerView) {
        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter == null || adapter.getItemCount() == 0) {
            ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            recyclerView.setLayoutParams(params);
            return;
        }
        int width = recyclerView.getWidth();
        if (width <= 0) {
            width = recyclerView.getResources().getDisplayMetrics().widthPixels
                    - recyclerView.getPaddingStart() - recyclerView.getPaddingEnd();
        }
        int widthSpec = View.MeasureSpec.makeMeasureSpec(Math.max(width, 0), View.MeasureSpec.EXACTLY);
        int totalHeight = 0;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            int viewType = adapter.getItemViewType(i);
            RecyclerView.ViewHolder holder = adapter.createViewHolder(recyclerView, viewType);
            bindViewHolder(adapter, holder, i);
            holder.itemView.measure(widthSpec,
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight += holder.itemView.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
        params.height = totalHeight + recyclerView.getPaddingTop() + recyclerView.getPaddingBottom();
        recyclerView.setLayoutParams(params);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void bindViewHolder(RecyclerView.Adapter adapter,
                                       RecyclerView.ViewHolder holder,
                                       int position) {
        adapter.onBindViewHolder(holder, position);
    }
}
