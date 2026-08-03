package com.smartboarding.ui.message;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.smartboarding.R;
import com.smartboarding.data.models.Message;

import java.util.List;
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT_TEXT = 1;
    private static final int TYPE_RECEIVED_TEXT = 2;
    private static final int TYPE_SENT_IMAGE = 3;
    private static final int TYPE_RECEIVED_IMAGE = 4;

    private final List<Message> messages;
    private final String currentUserId; // tenantId đang đăng nhập

    public ChatAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message m = messages.get(position);
        boolean isImage = "Image".equals(m.type);
        boolean mine = m.isMine(currentUserId);

        if (isImage) return mine ? TYPE_SENT_IMAGE : TYPE_RECEIVED_IMAGE;
        return mine ? TYPE_SENT_TEXT : TYPE_RECEIVED_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_SENT_TEXT:
                return new TextViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
            case TYPE_RECEIVED_TEXT:
                return new TextViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
            case TYPE_SENT_IMAGE:
                return new ImageViewHolder(inflater.inflate(R.layout.item_message_image_sent, parent, false));
            case TYPE_RECEIVED_IMAGE:
            default:
                return new ImageViewHolder(inflater.inflate(R.layout.item_message_image_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message m = messages.get(position);

        if (holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).bind(m);
        } else if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).bind(m);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setMessages(List<Message> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void prependMessages(List<Message> olderMessages) {
        messages.addAll(0, olderMessages);
        notifyItemRangeInserted(0, olderMessages.size());
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Message m) {
            tvContent.setText(m.content);
            tvTime.setText(formatTime(m.createdAt));
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTime;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Message m) {
            Glide.with(itemView.getContext())
                    .load(m.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(ivImage);
            tvTime.setText(formatTime(m.createdAt));

            // Chạm vào ảnh -> xem toàn màn hình
            ivImage.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ImagePreviewActivity.class);
                intent.putExtra(ImagePreviewActivity.EXTRA_IMAGE_URL, m.imageUrl);
                itemView.getContext().startActivity(intent);
            });
        }
    }

    private static String formatTime(String isoTime) {
        try {
            java.text.SimpleDateFormat isoFormat =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = isoFormat.parse(isoTime);

            java.text.SimpleDateFormat displayFormat =
                    new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }
}