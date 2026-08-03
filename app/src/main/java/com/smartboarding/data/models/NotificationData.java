package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificationData {
    @SerializedName("notifications") public List<Notification> notifications;
    @SerializedName("unreadCount")   public int unreadCount;
}