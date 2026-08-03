package com.smartboarding.data.models;
import com.google.gson.annotations.SerializedName;

public class AuthData {
    @SerializedName("tenant")       public Tenant tenant;
    @SerializedName("accessToken")  public String accessToken;
    @SerializedName("refreshToken") public String refreshToken;
}