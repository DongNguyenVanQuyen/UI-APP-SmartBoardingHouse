package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("_id")            public String id;
    @SerializedName("conversationId") public String conversationId; // = tenantId
    @SerializedName("senderRole")     public String senderRole;     // "Admin" | "Tenant"
    @SerializedName("content")        public String content;
    @SerializedName("type")           public String type; // "Text" | "Image"
    @SerializedName("imageUrl")       public String imageUrl;
    @SerializedName("isRead")         public boolean isRead;
    @SerializedName("createdAt")      public String createdAt;

    /**
     * App này chỉ đóng vai Tenant, nên "tin của mình" đơn giản là tin có
     * senderRole = "Tenant". Tham số currentUserId không còn cần thiết để
     * so sánh nữa (giữ lại chữ ký hàm để khỏi phải sửa nơi gọi:
     * ChatFragment, ChatAdapter đang gọi isMine(currentUserId)).
     */
    public boolean isMine(String currentUserId) {
        return "Tenant".equals(senderRole);
    }
}