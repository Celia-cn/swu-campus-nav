package com.example.gps.ui.adapter;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiMessageAdapter extends RecyclerView.Adapter<AiMessageAdapter.Holder> {

    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");

    public static class Message {
        public final boolean fromUser;
        public final String text;
        public final boolean typing;
        @Nullable
        public final List<String> sources;

        public Message(boolean fromUser, String text) {
            this(fromUser, text, false, null);
        }

        public Message(boolean fromUser, String text, boolean typing, @Nullable List<String> sources) {
            this.fromUser = fromUser;
            this.text = text;
            this.typing = typing;
            this.sources = sources;
        }
    }

    private final List<Message> items = new ArrayList<>();

    public List<Message> getItems() {
        return items;
    }

    public void add(Message message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    public void removeLast() {
        if (items.isEmpty()) {
            return;
        }
        int index = items.size() - 1;
        items.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).fromUser ? 1 : 0;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == 1 ? R.layout.item_ai_message_user : R.layout.item_ai_message_bot;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view, viewType == 0);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Message item = items.get(position);
        if (item.typing) {
            holder.text.setText(item.text);
            holder.text.setAlpha(0.75f);
        } else {
            holder.text.setText(formatBold(item.text));
            holder.text.setAlpha(1f);
        }
        if (holder.sources != null) {
            if (item.sources != null && !item.sources.isEmpty()) {
                String joined = String.join("、", item.sources);
                holder.sources.setText(holder.itemView.getContext()
                        .getString(R.string.ai_chat_sources, joined));
                holder.sources.setVisibility(View.VISIBLE);
            } else {
                holder.sources.setVisibility(View.GONE);
            }
        }
    }

    private CharSequence formatBold(String raw) {
        if (raw == null || !raw.contains("**")) {
            return raw == null ? "" : raw;
        }
        SpannableStringBuilder sb = new SpannableStringBuilder();
        Matcher m = BOLD.matcher(raw);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                sb.append(raw, last, m.start());
            }
            int start = sb.length();
            sb.append(m.group(1));
            sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            last = m.end();
        }
        if (last < raw.length()) {
            sb.append(raw.substring(last));
        }
        return sb;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView text;
        @Nullable
        final TextView sources;

        Holder(@NonNull View itemView, boolean bot) {
            super(itemView);
            text = itemView.findViewById(R.id.tvMessage);
            sources = bot ? itemView.findViewById(R.id.tvSources) : null;
        }
    }
}
