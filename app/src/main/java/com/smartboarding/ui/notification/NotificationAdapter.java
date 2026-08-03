package com.smartboarding.ui.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartboarding.R;
import com.smartboarding.data.models.Notification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnItemClick {
        void onClick(Notification item, int position);
    }

    private final List<Notification> items = new ArrayList<>();
    private final OnItemClick listener;

    public NotificationAdapter(OnItemClick listener) {
        this.listener = listener;
    }

    public void setItems(List<Notification> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void appendItems(List<Notification> more) {
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public void markReadLocally(String id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(id)) {
                if (!items.get(i).isRead) {
                    items.get(i).isRead = true;
                    notifyItemChanged(i);
                }
                break;
            }
        }
    }

    public void markAllReadLocally() {
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isRead) {
                items.get(i).isRead = true;
            }
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getUnreadCount() {
        int count = 0;
        for (Notification n : items) if (!n.isRead) count++;
        return count;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Notification item = items.get(position);

        holder.tvTitle.setText(item.title);
        holder.tvBody.setText(item.body);
        holder.tvTime.setText(formatTime(item.createdAt));

        holder.dotUnread.setVisibility(item.isRead ? View.INVISIBLE : View.VISIBLE);
        holder.tvTitle.setAlpha(item.isRead ? 0.6f : 1f);
        holder.tvBody.setAlpha(item.isRead ? 0.6f : 1f);

        holder.itemView.setOnClickListener(v -> listener.onClick(item, holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(String iso) {
        if (iso == null) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat out = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            return out.format(in.parse(iso));
        } catch (ParseException e) {
            return iso;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTime;
        View dotUnread;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvBody = itemView.findViewById(R.id.tvBody);
            tvTime = itemView.findViewById(R.id.tvTime);
            dotUnread = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}