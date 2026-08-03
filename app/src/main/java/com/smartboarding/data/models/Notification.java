package com.smartboarding.data.models;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("_id")       public String id;
    @SerializedName("title")     public String title;
    @SerializedName("body")      public String body;
    @SerializedName("type")      public String type;
    @SerializedName("refId")     public String refId;
    @SerializedName("refModel")  public String refModel;
    @SerializedName("isRead")    public boolean isRead;
    @SerializedName("createdAt") public String createdAt;
    @SerializedName("meta")      public JsonObject meta; // dữ liệu phụ, cấu trúc thay đổi theo từng loại thông báo
}