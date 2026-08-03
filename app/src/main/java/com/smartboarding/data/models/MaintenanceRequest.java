package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MaintenanceRequest {
    @SerializedName("_id")         public String id;
    @SerializedName("room")        public Room room;
    @SerializedName("title")       public String title;
    @SerializedName("description") public String description;
    @SerializedName("images")      public List<String> images;
    @SerializedName("status")      public String status;
    @SerializedName("priority")    public String priority;
    @SerializedName("category")    public String category;
    @SerializedName("adminNote")   public String adminNote;
    @SerializedName("resolvedAt")  public String resolvedAt;
    @SerializedName("createdAt")   public String createdAt;
}