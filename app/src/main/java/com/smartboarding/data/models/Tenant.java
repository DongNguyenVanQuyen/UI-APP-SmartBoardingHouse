package com.smartboarding.data.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// ─── TENANT ─────────────────────────────────────────────────
public class Tenant {
    @SerializedName("_id")        public String id;
    @SerializedName("fullName")   public String fullName;
    @SerializedName("email")      public String email;
    @SerializedName("phone")      public String phone;
    @SerializedName("avatar")     public String avatar;
    @SerializedName("idCard")     public String idCard;
    @SerializedName("address")    public String address;
    @SerializedName("isActive")   public boolean isActive;
    @SerializedName("createdAt")  public String createdAt;
    @SerializedName("frontImage") public String frontImage;
    @SerializedName("backImage")  public String backImage;
}